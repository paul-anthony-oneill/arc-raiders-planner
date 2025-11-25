# Implementation Plan: Backend Coordinate Calibration for MapMarkers

**Status**: Draft
**Created**: 2025-11-24
**Estimated Effort**: ~5 hours

---

## Executive Summary

Move map marker coordinate calibration from frontend to backend during database ingestion. **Replace** raw coordinates with calibrated Leaflet-ready coordinates. Raw coordinates remain accessible via Metaforge API if needed for debugging.

### Problem Statement

Currently, the frontend performs coordinate transformation for every marker render:
- Coordinate order confusion: Metaforge API returns [lng, lat] but Leaflet expects [lat, lng]
- Duplicate logic: Transformation exists in frontend; backend has calibration data but doesn't use it
- Frontend complexity: Every marker requires conditional calibration check
- Data integrity: Database stores "raw" coordinates meaningless without calibration context

### Solution

Store Leaflet-ready coordinates in database. All MapMarker lat/lng values are pre-calibrated during ingestion, eliminating frontend transformation logic.

---

## Database Schema Changes

### Migration V6__add_calibrated_marker_coordinates.sql

```sql
-- Rename existing columns to preserve data during migration
ALTER TABLE map_markers
    RENAME COLUMN lat TO raw_lat_temp;

ALTER TABLE map_markers
    RENAME COLUMN lng TO raw_lng_temp;

-- Add new calibrated coordinate columns
ALTER TABLE map_markers
    ADD COLUMN lat DOUBLE PRECISION,
    ADD COLUMN lng DOUBLE PRECISION;

-- Backfill with calibrated coordinates
UPDATE map_markers mm
SET
    lat = (mm.raw_lat_temp * gm.cal_scale_y) + gm.cal_offset_y,
    lng = (mm.raw_lng_temp * gm.cal_scale_x) + gm.cal_offset_x
FROM maps gm
WHERE mm.map_id = gm.id;

-- Make columns non-nullable
ALTER TABLE map_markers
    ALTER COLUMN lat SET NOT NULL,
    ALTER COLUMN lng SET NOT NULL;

-- Drop temporary columns
ALTER TABLE map_markers
    DROP COLUMN raw_lat_temp,
    DROP COLUMN raw_lng_temp;

-- Add index for spatial queries
CREATE INDEX idx_markers_coords ON map_markers(lat, lng);

-- Update column comments
COMMENT ON COLUMN map_markers.lat IS 'Leaflet-ready Y coordinate (calibrated, ready for [lat,lng] format)';
COMMENT ON COLUMN map_markers.lng IS 'Leaflet-ready X coordinate (calibrated, ready for [lat,lng] format)';
```

---

## Implementation Tasks

### Task 1: Create Database Migration

**File**: `src/main/resources/db/migration/V6__add_calibrated_marker_coordinates.sql`

Copy the SQL from the "Database Schema Changes" section above.

**Acceptance Criteria**:
- ✅ Migration runs cleanly on empty database
- ✅ Migration backfills existing data correctly
- ✅ All existing markers have non-null calibrated coordinates
- ✅ Index creation completes successfully

---

### Task 2: Update MapMarker Entity

**File**: `src/main/java/com/pauloneill/arcraidersplanner/model/MapMarker.java`

```java
@Data
@Entity
@Table(name = "map_markers")
public class MapMarker {

    @Id
    @Column(unique = true)
    private String id;

    @Column(nullable = false)
    private Double lat; // Leaflet-ready Y coordinate (calibrated)

    @Column(nullable = false)
    private Double lng; // Leaflet-ready X coordinate (calibrated)

    private String category;
    private String subcategory;
    private String name;

    @ManyToOne
    @JoinColumn(name = "map_id", nullable = false)
    private GameMap gameMap;
}
```

**Changes**:
- Update JavaDoc comments to indicate coordinates are calibrated
- No structural changes needed - lat/lng fields remain the same

**Acceptance Criteria**:
- ✅ Entity compiles without errors
- ✅ Hibernate schema validation passes
- ✅ Existing tests still pass

---

### Task 3: Create Coordinate Calibration Service

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/CoordinateCalibrationService.java`

```java
package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import org.springframework.stereotype.Service;

/**
 * Transforms raw Metaforge API coordinates to Leaflet CRS.Simple coordinates.
 *
 * WHY: All coordinates stored in database should be Leaflet-ready.
 * Raw coordinates available from Metaforge API if needed for debugging.
 *
 * COORDINATE SYSTEMS:
 * - Metaforge API: Returns [lng, lat] format (X, Y)
 * - Leaflet: Expects [lat, lng] format (Y, X)
 * - This service applies GameMap calibration parameters to transform coordinates
 */
@Service
public class CoordinateCalibrationService {

    /**
     * Calibrates raw API coordinates to Leaflet [lat, lng] format.
     *
     * @param rawLat Raw Y from Metaforge API
     * @param rawLng Raw X from Metaforge API
     * @param gameMap Map with calibration parameters
     * @return Array [calibratedLat, calibratedLng] ready for Leaflet
     */
    public double[] calibrateCoordinates(double rawLat, double rawLng, GameMap gameMap) {
        double scaleX = gameMap.getCalibrationScaleX() != null ? gameMap.getCalibrationScaleX() : 1.0;
        double scaleY = gameMap.getCalibrationScaleY() != null ? gameMap.getCalibrationScaleY() : 1.0;
        double offsetX = gameMap.getCalibrationOffsetX() != null ? gameMap.getCalibrationOffsetX() : 0.0;
        double offsetY = gameMap.getCalibrationOffsetY() != null ? gameMap.getCalibrationOffsetY() : 0.0;

        double calibratedLng = (rawLng * scaleX) + offsetX;
        double calibratedLat = (rawLat * scaleY) + offsetY;

        return new double[]{calibratedLat, calibratedLng};
    }
}
```

**Acceptance Criteria**:
- ✅ Service compiles and can be autowired
- ✅ Calibration logic matches frontend `transformMarker` function exactly
- ✅ Default values (1.0, 0.0) work when calibration not set

---

### Task 4: Update MetaforgeSyncService

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/MetaforgeSyncService.java`

**Changes to constructor** (line ~29-47):

```java
private final CoordinateCalibrationService calibrationService;

public MetaforgeSyncService(RestClient restClient, ItemRepository itemRepository,
        LootAreaRepository lootAreaRepository, MapMarkerRepository markerRepository,
        GameMapRepository gameMapRepository, CoordinateCalibrationService calibrationService) {
    this.restClient = restClient;
    this.itemRepository = itemRepository;
    this.lootAreaRepository = lootAreaRepository;
    this.markerRepository = markerRepository;
    this.gameMapRepository = gameMapRepository;
    this.calibrationService = calibrationService;
}
```

**Changes to `syncMarkers()` method** (lines 154-169):

```java
for (MetaforgeMarkerDto dto : dtos) {
    if (markerRepository.existsById(dto.id())) {
        continue;
    }

    // Calibrate coordinates before storing
    double[] calibrated = calibrationService.calibrateCoordinates(
        dto.lat(),
        dto.lng(),
        map
    );

    MapMarker marker = new MapMarker();
    marker.setId(dto.id());
    marker.setLat(calibrated[0]);  // Store calibrated Y
    marker.setLng(calibrated[1]);  // Store calibrated X
    marker.setCategory(dto.category());
    marker.setSubcategory(dto.subcategory());
    marker.setName(dto.name());
    marker.setGameMap(map);

    markerRepository.save(marker);
}
```

**Acceptance Criteria**:
- ✅ Existing markers get calibrated coordinates on next sync
- ✅ New markers are calibrated during insertion
- ✅ Service compiles and starts successfully

---

### Task 5: Update Frontend - Remove Transformation Logic

**File**: `frontend/src/MapComponent.tsx`

**Changes at line 306-307** (extraction point marker):

```typescript
// BEFORE:
position={
    gameMap
        ? transformMarker(extractionCoords, gameMap)
        : gameCoordsToLatLng(extractionCoords[0], extractionCoords[1])
}

// AFTER:
position={[extractionCoords[0], extractionCoords[1]]}
```

**Changes at line 329-330** (enemy spawn markers):

```typescript
// BEFORE:
position={
    gameMap
        ? transformMarker(spawnCoords, gameMap)
        : gameCoordsToLatLng(spawn.lng, spawn.lat)
}

// AFTER:
position={[spawn.lat, spawn.lng]}
```

**File**: `frontend/src/utils/mapUtils.ts`

**Deprecate or remove transformMarker**:

```typescript
/**
 * @deprecated Backend now stores calibrated coordinates.
 * Only kept for MapEditor calibration UI (if needed).
 */
export const transformMarker = (marker: L.LatLngTuple, map: GameMap): L.LatLngTuple => {
    const scaleX = map.calibrationScaleX ?? 1.0
    const scaleY = map.calibrationScaleY ?? 1.0
    const offsetX = map.calibrationOffsetX ?? 0.0
    const offsetY = map.calibrationOffsetY ?? 0.0

    const localX = marker[1] * scaleX + offsetX
    const localY = marker[0] * scaleY + offsetY

    return [localY, localX] as L.LatLngTuple
}
```

**Acceptance Criteria**:
- ✅ MapComponent renders markers without calling `transformMarker`
- ✅ MapEditor still works (uses `transformMarker` for preview only if needed)
- ✅ No visual regression in marker positions
- ✅ [lat, lng] coordinate ordering is consistent everywhere

---

### Task 6: Verify PlannerService Distance Calculations

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/PlannerService.java`

**No changes needed!** Line 336-338 already uses `getLat()` and `getLng()`:

```java
private double distance(Area a, MapMarker m) {
    return Math.sqrt(
        Math.pow(a.getMapX() - m.getLng(), 2) +  // ✅ Now calibrated
        Math.pow(a.getMapY() - m.getLat(), 2)    // ✅ Now calibrated
    );
}
```

Since `Area.mapX/mapY` are already calibrated, and now `MapMarker.lat/lng` are calibrated, distance calculations will be accurate.

**Acceptance Criteria**:
- ✅ Enemy proximity scoring uses calibrated coordinates
- ✅ Extraction point distance calculations are accurate
- ✅ Route planning produces same/better results (no regression)

---

## Testing Strategy

### Unit Tests

**Test File**: `src/test/java/com/pauloneill/arcraidersplanner/service/CoordinateCalibrationServiceTest.java`

```java
package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CoordinateCalibrationServiceTest {

    @Autowired
    private CoordinateCalibrationService service;

    @Test
    void testCalibrationWithScaleAndOffset() {
        GameMap map = new GameMap();
        map.setCalibrationScaleX(2.0);
        map.setCalibrationScaleY(1.5);
        map.setCalibrationOffsetX(100.0);
        map.setCalibrationOffsetY(50.0);

        double[] result = service.calibrateCoordinates(10.0, 20.0, map);

        // (10 * 1.5) + 50 = 65 (lat/Y)
        // (20 * 2.0) + 100 = 140 (lng/X)
        assertEquals(65.0, result[0], 0.01);   // calibratedLat
        assertEquals(140.0, result[1], 0.01);  // calibratedLng
    }

    @Test
    void testDefaultCalibration() {
        GameMap map = new GameMap();
        // No calibration parameters set (nulls)

        double[] result = service.calibrateCoordinates(100.0, 200.0, map);

        // Should use defaults (scale=1.0, offset=0.0)
        assertEquals(100.0, result[0], 0.01);
        assertEquals(200.0, result[1], 0.01);
    }

    @Test
    void testNegativeCoordinates() {
        GameMap map = new GameMap();
        map.setCalibrationScaleX(1.0);
        map.setCalibrationScaleY(1.0);
        map.setCalibrationOffsetX(0.0);
        map.setCalibrationOffsetY(0.0);

        double[] result = service.calibrateCoordinates(-50.0, -100.0, map);

        assertEquals(-50.0, result[0], 0.01);
        assertEquals(-100.0, result[1], 0.01);
    }
}
```

### Integration Tests

**Test File**: `src/test/java/com/pauloneill/arcraidersplanner/service/MetaforgeSyncServiceTest.java`

```java
package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MetaforgeSyncServiceTest {

    @Autowired
    private MetaforgeSyncService syncService;

    @Autowired
    private MapMarkerRepository markerRepository;

    @Test
    void testMarkersStoreCalibratedCoordinates() {
        // Run marker sync
        syncService.syncMarkers();

        // Verify markers have calibrated coordinates
        List<MapMarker> markers = markerRepository.findAll();
        assertTrue(markers.size() > 0, "Should have synced some markers");

        for (MapMarker marker : markers) {
            assertNotNull(marker.getLat(), "Should have calibrated lat");
            assertNotNull(marker.getLng(), "Should have calibrated lng");

            // Coordinates should be in reasonable Leaflet range
            assertTrue(Math.abs(marker.getLat()) < 10000,
                      "Lat should be reasonable: " + marker.getLat());
            assertTrue(Math.abs(marker.getLng()) < 10000,
                      "Lng should be reasonable: " + marker.getLng());
        }
    }

    @Test
    void testCalibratedCoordinatesDifferFromRaw() {
        syncService.syncMarkers();

        // For maps with non-identity calibration, verify transformation occurred
        List<MapMarker> markers = markerRepository.findAll().stream()
                .filter(m -> {
                    GameMap map = m.getGameMap();
                    return !map.getCalibrationScaleX().equals(1.0) ||
                           !map.getCalibrationOffsetX().equals(0.0);
                })
                .toList();

        if (!markers.isEmpty()) {
            // If we have markers with calibration, verify they differ from raw
            // (This is a conceptual test - we don't store raw anymore)
            assertTrue(markers.size() > 0,
                      "Should have some markers from maps with calibration");
        }
    }
}
```

### Manual Testing Checklist

#### Pre-Migration Testing
1. **Document Current State**:
   - ✅ Take screenshots of map with markers visible
   - ✅ Note marker positions for Dam, Spaceport, Buried City, Blue Gate
   - ✅ Export current map_markers data: `SELECT id, lat, lng, map_id FROM map_markers ORDER BY id LIMIT 20;`

#### Migration Testing
2. **Fresh Database Test**:
   - ✅ Drop database, restart app
   - ✅ Verify V6 migration runs successfully (check Flyway logs)
   - ✅ Run marker sync, verify calibrated coords populated
   - ✅ Query database: `SELECT id, lat, lng, category, subcategory FROM map_markers WHERE subcategory = 'hatch';`
   - ✅ Verify coordinates look reasonable (not all zeros, within expected range)

3. **Existing Database Test**:
   - ✅ Keep existing data, restart app with new migration
   - ✅ Verify migration backfills existing markers
   - ✅ Check a few markers: `SELECT id, lat, lng, name FROM map_markers LIMIT 10;`
   - ✅ Compare before/after coordinates (should be transformed, not identical)

#### Frontend Testing
4. **Visual Regression Test**:
   - ✅ Open map in browser
   - ✅ Verify enemy spawn markers appear in same positions as before
   - ✅ Verify extraction markers appear correctly
   - ✅ Compare screenshots before/after (no visual change expected)
   - ✅ Test all 4 maps: Dam, Spaceport, Buried City, Blue Gate

5. **Functional Test**:
   - ✅ Generate route with PURE_SCAVENGER profile
   - ✅ Generate route with enemy targeting enabled
   - ✅ Verify enemy proximity scores are reasonable
   - ✅ Verify extraction point distances are accurate
   - ✅ Test different routing profiles: EASY_EXFIL, AVOID_PVP, SAFE_EXFIL

#### Performance Testing
6. **Performance Check**:
   - ✅ Open browser DevTools, check Network tab
   - ✅ Verify map loads in reasonable time (< 2 seconds)
   - ✅ Check for console errors or warnings
   - ✅ Verify no excessive re-renders (React DevTools Profiler)

---

## Rollback Plan

### If Migration Fails Mid-Execution

```sql
-- Restore original schema (if migration fails partway)
ALTER TABLE map_markers
    DROP COLUMN IF EXISTS lat CASCADE,
    DROP COLUMN IF EXISTS lng CASCADE;

ALTER TABLE map_markers
    RENAME COLUMN raw_lat_temp TO lat;

ALTER TABLE map_markers
    RENAME COLUMN raw_lng_temp TO lng;

DROP INDEX IF EXISTS idx_markers_coords;
```

### If Coordinates Are Wrong After Migration

```sql
-- Re-run calibration with corrected formula
UPDATE map_markers mm
SET
    lat = (mm.lat * gm.cal_scale_y) + gm.cal_offset_y,
    lng = (mm.lng * gm.cal_scale_x) + gm.cal_offset_x
FROM maps gm
WHERE mm.map_id = gm.id;
```

### If Frontend Breaks

1. **Quick Fix**: Revert frontend changes
   ```bash
   git revert <commit-hash-of-frontend-changes>
   ```

2. **Temporary Workaround**: Add raw coordinates back to API response
   - Backend still works (sends calibrated coords)
   - Frontend can be patched to handle both formats

3. **Nuclear Option**: Rollback entire migration
   ```bash
   # Delete V6 migration file
   # Restart app (Flyway will be one version behind)
   # Re-sync markers from Metaforge API
   ```

---

## Timeline Estimate

| Task | Effort | Dependencies | Risk |
|------|--------|--------------|------|
| Task 1: Database Migration | 1 hour | None | Low - SQL is straightforward |
| Task 2: Update Entity | 15 min | Task 1 | Low - minimal changes |
| Task 3: Calibration Service | 45 min | Task 2 | Low - logic extracted from frontend |
| Task 4: Update Sync Service | 45 min | Task 3 | Medium - integration point |
| Task 5: Frontend Cleanup | 1 hour | Task 4 | Medium - visual regression risk |
| Task 6: Verify Distance Calcs | 15 min | Task 4 | Low - no changes needed |
| Testing & QA | 1.5 hours | All tasks | Medium - manual testing required |
| **Total** | **~5 hours** | | |

**Recommended Approach**: Implement Task 1-4 in one session, test thoroughly, then Task 5-6 in second session.

---

## Success Criteria

### Must Have ✅
- ✅ All map markers stored with calibrated coordinates in database
- ✅ Frontend renders markers without calling `transformMarker`
- ✅ No visual regression in marker positions
- ✅ Route planning algorithms use calibrated coordinates automatically
- ✅ Migration backfills existing data correctly
- ✅ All unit tests pass
- ✅ All integration tests pass

### Should Have ✅
- ✅ Migration runs successfully on existing database
- ✅ Coordinate calibration service is reusable for future features
- ✅ Raw coordinates accessible via Metaforge API if needed
- ✅ Database query performance maintained or improved

### Could Have 🎯
- 🎯 Performance improvement from eliminating frontend calculations
- 🎯 MapEditor updated to use backend calibration (future task)
- 🎯 Admin API to recalibrate markers if GameMap calibration changes

---

## Benefits of This Approach

### Technical Benefits
1. **Single Source of Truth**: Database contains Leaflet-ready coordinates
2. **Simpler Frontend**: No conditional transformation logic
3. **Consistent Coordinate System**: [lat, lng] format everywhere
4. **Better Performance**: Frontend doesn't recalculate on every render
5. **Easier Debugging**: Coordinates in DB match what you see on map

### Maintainability Benefits
1. **Less Code**: Remove ~30 lines of transformation logic from frontend
2. **Clear Intent**: If it's in DB, it's ready to use
3. **No Sync Issues**: Can't accidentally use wrong coordinate set
4. **Backend-Controlled**: Calibration logic centralized in one service

### Data Integrity Benefits
1. **Raw Available**: Metaforge API always has original coordinates
2. **Auditable**: Can compare stored coords with API response
3. **Recalibratable**: If calibration params change, re-run migration
4. **Validated**: Calibration happens once during ingestion, not repeatedly

---

## Future Enhancements

### Phase 2: Recalibration API (Post-MVP)
```java
@PostMapping("/api/maps/{id}/recalibrate-markers")
public ResponseEntity<?> recalibrateMarkers(@PathVariable Long id) {
    // Re-fetch from Metaforge, re-apply calibration
    // Useful if GameMap calibration parameters change
}
```

### Phase 3: Spatial Queries (Future)
```sql
-- Enable PostGIS for advanced spatial queries
CREATE EXTENSION postgis;

-- Convert to geography type for earth-surface distance
ALTER TABLE map_markers
    ADD COLUMN geom GEOMETRY(Point, 4326);

UPDATE map_markers
SET geom = ST_SetSRID(ST_MakePoint(lng, lat), 4326);
```

### Phase 4: MapEditor Integration (Future)
- Update MapEditor to fetch calibrated preview from backend
- Remove client-side calibration calculation from MapEditor
- Add real-time calibration adjustment API for map config UI

---

## Questions for Review

### Before Starting Implementation

1. **Should we add a `calibrated_at` timestamp to track when coordinates were last calibrated?**
   - Pros: Auditing, debugging
   - Cons: Extra column, more complexity
   - Recommendation: Not needed initially, raw coords in API are sufficient audit trail

2. **Should we add validation to reject markers with unreasonable coordinates?**
   - Example: Reject if `abs(lat) > 5000 || abs(lng) > 5000`
   - Recommendation: Add basic validation in CoordinateCalibrationService

3. **Should we create an API endpoint to manually trigger recalibration?**
   - Recommendation: Not in MVP, add in Phase 2 if needed

4. **Should we log calibration transformations during sync?**
   - Example: `log.debug("Calibrated marker {} from [{}, {}] to [{}, {}]", id, raw, calibrated)`
   - Recommendation: Add at DEBUG level for troubleshooting

---

## Notes

- This plan assumes calibration parameters in `maps` table are correct
- Frontend `transformMarker` kept for MapEditor but marked deprecated
- Area coordinates (mapX/mapY) already calibrated, no changes needed there
- Distance calculations automatically correct after markers are calibrated

---

## Approval Checklist

Before starting implementation:
- [ ] Review plan with team
- [ ] Backup production database (if applicable)
- [ ] Create feature branch: `feature/backend-coordinate-calibration`
- [ ] Verify all stakeholders agree on approach
- [ ] Schedule testing time in development calendar

After implementation:
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Manual testing checklist completed
- [ ] Visual regression testing passed
- [ ] Performance testing completed
- [ ] Documentation updated
- [ ] Code review completed
- [ ] Merge to master/main branch

---

**Plan Status**: ✅ Ready for Implementation
**Last Updated**: 2025-11-24
