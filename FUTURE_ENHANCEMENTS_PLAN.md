# Future Enhancements - Detailed Implementation Plan

## Overview

This document outlines detailed implementation plans for optional enhancements to the unified tactical planner interface. Each enhancement is broken down into actionable tasks with technical specifications, API designs, and testing strategies.

**Status**: Planning Phase
**Priority**: To be determined based on user feedback
**Estimated Total Time**: 6-8 weeks for all enhancements

---

## Table of Contents

1. [Zone Highlighting on Item Selection](#1-zone-highlighting-on-item-selection)
2. [Advanced Filtering System](#2-advanced-filtering-system)
3. [Saved Loadouts](#3-saved-loadouts)
4. [Route Comparison](#4-route-comparison)
5. [Mobile Optimization](#5-mobile-optimization)
6. [Performance Optimizations](#6-performance-optimizations)
7. [Accessibility Improvements](#7-accessibility-improvements)
8. [Additional Features](#8-additional-features)

---

## 1. Zone Highlighting on Item Selection

### Overview

**WHY**: When users select an item in the LeftPanel, they should see which zones on the current map contain that item highlighted in real-time. This provides immediate visual feedback and helps users make informed decisions about target selection.

**Current State**: Placeholder - `highlightedZones` state exists but is never populated.

**Desired State**: Clicking an item fetches zones from the backend and highlights them on the minimized map.

### Backend Implementation

#### Task 1.1: Create Area Search Endpoint

**File**: `src/main/java/com/pauloneill/arcraidersplanner/controller/AreaController.java`

```java
@RestController
@RequestMapping("/api/areas")
@Tag(name = "Areas", description = "Loot area and zone endpoints")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    /**
     * Get all areas on a map that contain a specific item.
     * WHY: Enables zone highlighting in tactical planner when item selected
     *
     * @param mapName The map name (e.g., "Dam Battlegrounds")
     * @param itemName The item to search for
     * @return List of areas containing this item
     */
    @GetMapping("/by-map-and-item")
    @Operation(summary = "Get areas by map and item")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Areas retrieved"),
        @ApiResponse(responseCode = "404", description = "Map or item not found")
    })
    public List<AreaDto> getAreasByMapAndItem(
        @Parameter(description = "Map name", required = true)
        @RequestParam String mapName,
        @Parameter(description = "Item name", required = true)
        @RequestParam String itemName
    ) {
        return areaService.findAreasByMapAndItem(mapName, itemName);
    }
}
```

#### Task 1.2: Implement AreaService

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/AreaService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areaRepository;
    private final GameMapRepository gameMapRepository;
    private final ItemRepository itemRepository;
    private final DtoMapper dtoMapper;

    /**
     * Find all areas on a map that contain a specific item.
     * WHY: Used for zone highlighting when user selects an item
     */
    public List<AreaDto> findAreasByMapAndItem(String mapName, String itemName) {
        // Validate map exists
        GameMap map = gameMapRepository.findByName(mapName)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Map not found: " + mapName
            ));

        // Validate item exists
        Item item = itemRepository.findByNameIgnoreCase(itemName)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Item not found: " + itemName
            ));

        // Find areas by map and loot type
        String lootTypeName = item.getLootType() != null ? item.getLootType().getName() : null;

        if (lootTypeName == null) {
            return Collections.emptyList();
        }

        List<Area> areas = areaRepository.findByMapAndLootType(map.getId(), lootTypeName);
        return dtoMapper.toAreaDtos(areas);
    }
}
```

#### Task 1.3: Add Repository Query

**File**: `src/main/java/com/pauloneill/arcraidersplanner/repository/AreaRepository.java`

```java
@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    /**
     * Find all areas on a map that have a specific loot type.
     * WHY: Used for zone highlighting by item selection
     */
    @Query("SELECT DISTINCT a FROM Area a " +
           "JOIN a.lootTypes lt " +
           "WHERE a.gameMap.id = :mapId AND lt.name = :lootTypeName")
    List<Area> findByMapAndLootType(
        @Param("mapId") Long mapId,
        @Param("lootTypeName") String lootTypeName
    );
}
```

#### Task 1.4: Extend AreaDto

**File**: `src/main/java/com/pauloneill/arcraidersplanner/dto/AreaDto.java`

```java
@Data
public class AreaDto {
    private Long id;
    private String name;
    private Double mapX;
    private Double mapY;
    private String coordinates; // JSON polygon coordinates
    private List<String> lootTypes;
    private Integer lootAbundance;

    // NEW: For zone highlighting
    private String mapName;      // Which map this area belongs to
    private Boolean highlighted;  // Frontend can use this flag
}
```

### Frontend Implementation

#### Task 1.5: Create mapApi Helper

**File**: `frontend/src/api/mapApi.ts`

```typescript
import type { Area } from '../types'

const API_URL = 'http://localhost:8080/api/areas'

export const mapApi = {
  /**
   * Get all zones on a map that contain a specific item.
   * WHY: Enables zone highlighting when user selects an item
   *
   * @param mapName The map name
   * @param itemName The item to search for
   * @returns List of areas containing this item
   */
  getZonesWithItem: async (mapName: string, itemName: string): Promise<Area[]> => {
    const params = new URLSearchParams({
      mapName,
      itemName,
    })

    const response = await fetch(`${API_URL}/by-map-and-item?${params}`)

    if (!response.ok) {
      throw new Error(`Failed to fetch zones: ${response.statusText}`)
    }

    return response.json()
  },

  /**
   * Get all areas on a specific map.
   * WHY: Useful for map overview and debugging
   */
  getAreasByMap: async (mapName: string): Promise<Area[]> => {
    const response = await fetch(`${API_URL}?mapName=${encodeURIComponent(mapName)}`)

    if (!response.ok) {
      throw new Error(`Failed to fetch areas: ${response.statusText}`)
    }

    return response.json()
  },
}
```

#### Task 1.6: Wire Up Zone Highlighting in TacticalPlannerPage

**File**: `frontend/src/pages/TacticalPlannerPage.tsx`

```typescript
// Add mapApi import
import { mapApi } from '../api/mapApi'

// Update state
const [highlightedZones, setHighlightedZones] = useState<Area[]>([])

// Update handleItemSelect
const handleItemSelect = useCallback(async (item: Item) => {
  try {
    // Fetch enhanced item details
    const details = await itemApi.getItemDetails(item.id)
    setSelectedItem(details)

    // NEW: Highlight zones on current map
    try {
      const zones = await mapApi.getZonesWithItem(selectedMap, item.name)
      setHighlightedZones(zones)
    } catch (error) {
      console.error('Failed to fetch zones:', error)
      // Don't block item selection if zones fail to load
      setHighlightedZones([])
    }
  } catch (error) {
    console.error('Failed to fetch item details:', error)
    setSelectedItem(item) // Fallback to basic item data
  }
}, [selectedMap])

// Update handleMapChange
const handleMapChange = useCallback(async (newMap: string) => {
  setSelectedMap(newMap)
  setCalculatedRoute(null) // Clear old route

  // NEW: Re-highlight zones for selected item on new map
  if (selectedItem) {
    try {
      const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name)
      setHighlightedZones(zones)
    } catch (error) {
      console.error('Failed to fetch zones:', error)
      setHighlightedZones([])
    }
  }
}, [selectedItem])
```

#### Task 1.7: Enhance MinimizedMapView

**File**: `frontend/src/components/MinimizedMapView.tsx`

```typescript
// Add zone count indicator
<div className="p-4 border-b border-gray-700">
  <label className="block text-xs text-gray-400 mb-2 font-semibold">SELECT MAP</label>
  <select
    value={selectedMap}
    onChange={(e) => onMapChange(e.target.value)}
    className="w-full bg-gray-700 text-white px-4 py-2 rounded border border-gray-600 focus:outline-none focus:border-orange-500"
  >
    <option value="Dam Battlegrounds">Dam Battlegrounds</option>
    <option value="Ironwood Hydroponics">Ironwood Hydroponics</option>
    <option value="Scrapworks">Scrapworks</option>
  </select>

  {/* NEW: Show highlighted zone count */}
  {highlightedZones.length > 0 && (
    <div className="mt-2 text-xs text-orange-400">
      {highlightedZones.length} zone{highlightedZones.length !== 1 ? 's' : ''} highlighted
    </div>
  )}
</div>
```

### Testing Strategy

#### Unit Tests

**Backend**:
```java
// AreaServiceTest.java
@Test
void findAreasByMapAndItem_shouldReturnAreasWithMatchingLootType() {
    List<AreaDto> areas = areaService.findAreasByMapAndItem("Dam Battlegrounds", "Rocket Thruster Core");
    assertThat(areas).isNotEmpty();
    assertThat(areas.get(0).getLootTypes()).contains("Mechanical");
}

@Test
void findAreasByMapAndItem_shouldThrowWhenMapNotFound() {
    assertThrows(ResponseStatusException.class, () ->
        areaService.findAreasByMapAndItem("Invalid Map", "Some Item")
    );
}
```

**Frontend**:
```typescript
// mapApi.test.ts
describe('mapApi.getZonesWithItem', () => {
  it('should fetch zones for item on map', async () => {
    const zones = await mapApi.getZonesWithItem('Dam Battlegrounds', 'Rocket Thruster Core')
    expect(zones).toBeDefined()
    expect(zones.length).toBeGreaterThan(0)
  })
})
```

#### E2E Tests

```typescript
test('should highlight zones when item selected', async ({ page }) => {
  await page.goto('/planner')

  // Select an item
  await page.click('text=Rocket Thruster Core')

  // Wait for zones to load
  await page.waitForTimeout(500)

  // Verify highlighted zones on map
  const highlightedAreas = await page.locator('.leaflet-interactive[style*="4CAF50"]').count()
  expect(highlightedAreas).toBeGreaterThan(0)

  // Verify zone count indicator
  await expect(page.locator('text=/\\d+ zones? highlighted/')).toBeVisible()
})
```

### Timeline

- Backend (Tasks 1.1-1.4): **2 days**
- Frontend (Tasks 1.5-1.7): **2 days**
- Testing & polish: **1 day**
- **Total**: 5 days

---

## 2. Advanced Filtering System

### Overview

**WHY**: Users need to filter the large list of items/recipes by multiple criteria (rarity, type, loot zone) to quickly find what they're looking for. Advanced filters improve discoverability and reduce cognitive load.

**Current State**: Basic search by name only.

**Desired State**: Multi-criteria filters with checkboxes, dropdowns, and smart defaults.

### Frontend Implementation

#### Task 2.1: Create FilterPanel Component

**File**: `frontend/src/components/FilterPanel.tsx`

```typescript
import React from 'react'

export interface FilterState {
  rarities: string[]       // ["Common", "Rare", "Legendary"]
  itemTypes: string[]      // ["Weapon", "Material", "Consumable"]
  lootTypes: string[]      // ["Mechanical", "Industrial"]
  showOnlyTargeted: boolean // Show only selected targets
}

interface FilterPanelProps {
  filters: FilterState
  onFilterChange: (filters: FilterState) => void
  availableRarities: string[]
  availableItemTypes: string[]
  availableLootTypes: string[]
}

export const FilterPanel: React.FC<FilterPanelProps> = ({
  filters,
  onFilterChange,
  availableRarities,
  availableItemTypes,
  availableLootTypes,
}) => {
  const toggleRarity = (rarity: string) => {
    const newRarities = filters.rarities.includes(rarity)
      ? filters.rarities.filter(r => r !== rarity)
      : [...filters.rarities, rarity]

    onFilterChange({ ...filters, rarities: newRarities })
  }

  const toggleItemType = (type: string) => {
    const newTypes = filters.itemTypes.includes(type)
      ? filters.itemTypes.filter(t => t !== type)
      : [...filters.itemTypes, type]

    onFilterChange({ ...filters, itemTypes: newTypes })
  }

  const toggleLootType = (type: string) => {
    const newTypes = filters.lootTypes.includes(type)
      ? filters.lootTypes.filter(t => t !== type)
      : [...filters.lootTypes, type]

    onFilterChange({ ...filters, lootTypes: newTypes })
  }

  const clearFilters = () => {
    onFilterChange({
      rarities: [],
      itemTypes: [],
      lootTypes: [],
      showOnlyTargeted: false,
    })
  }

  const hasActiveFilters =
    filters.rarities.length > 0 ||
    filters.itemTypes.length > 0 ||
    filters.lootTypes.length > 0 ||
    filters.showOnlyTargeted

  return (
    <div className="bg-gray-700 rounded-lg p-3 mb-3">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-sm font-semibold text-white">Filters</h4>
        {hasActiveFilters && (
          <button
            onClick={clearFilters}
            className="text-xs text-orange-400 hover:text-orange-300"
          >
            Clear All
          </button>
        )}
      </div>

      {/* Rarity Filter */}
      <div className="mb-3">
        <div className="text-xs text-gray-400 mb-1 uppercase">Rarity</div>
        <div className="flex flex-wrap gap-1">
          {availableRarities.map(rarity => (
            <button
              key={rarity}
              onClick={() => toggleRarity(rarity)}
              className={`px-2 py-1 text-xs rounded transition-colors ${
                filters.rarities.includes(rarity)
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
              }`}
            >
              {rarity}
            </button>
          ))}
        </div>
      </div>

      {/* Item Type Filter */}
      <div className="mb-3">
        <div className="text-xs text-gray-400 mb-1 uppercase">Item Type</div>
        <div className="flex flex-wrap gap-1">
          {availableItemTypes.map(type => (
            <button
              key={type}
              onClick={() => toggleItemType(type)}
              className={`px-2 py-1 text-xs rounded transition-colors ${
                filters.itemTypes.includes(type)
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* Loot Type Filter */}
      <div className="mb-3">
        <div className="text-xs text-gray-400 mb-1 uppercase">Loot Zone</div>
        <div className="flex flex-wrap gap-1">
          {availableLootTypes.map(type => (
            <button
              key={type}
              onClick={() => toggleLootType(type)}
              className={`px-2 py-1 text-xs rounded transition-colors ${
                filters.lootTypes.includes(type)
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
              }`}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* Show Only Targeted */}
      <label className="flex items-center text-sm text-gray-300 cursor-pointer">
        <input
          type="checkbox"
          checked={filters.showOnlyTargeted}
          onChange={(e) => onFilterChange({ ...filters, showOnlyTargeted: e.target.checked })}
          className="mr-2"
        />
        Show only selected targets
      </label>
    </div>
  )
}
```

#### Task 2.2: Integrate FilterPanel into LeftPanel

**File**: `frontend/src/components/LeftPanel.tsx`

```typescript
import { FilterPanel, FilterState } from './FilterPanel'

// Add filter state
const [filters, setFilters] = useState<FilterState>({
  rarities: [],
  itemTypes: [],
  lootTypes: [],
  showOnlyTargeted: false,
})

// Calculate available filter options
const availableRarities = useMemo(() =>
  [...new Set(items.map(i => i.rarity))].filter(Boolean).sort(),
  [items]
)

const availableItemTypes = useMemo(() =>
  [...new Set(items.map(i => i.itemType))].filter(Boolean).sort(),
  [items]
)

const availableLootTypes = useMemo(() =>
  [...new Set(items.map(i => i.lootType).filter(Boolean))].sort(),
  [items]
)

// Apply filters
const filteredItems = items.filter(item => {
  // Search filter
  if (searchTerm && !item.name.toLowerCase().includes(searchTerm.toLowerCase())) {
    return false
  }

  // Category filter
  if (activeTab !== 'ALL' && activeTab !== 'ITEMS') return false

  // Rarity filter
  if (filters.rarities.length > 0 && !filters.rarities.includes(item.rarity)) {
    return false
  }

  // Item type filter
  if (filters.itemTypes.length > 0 && !filters.itemTypes.includes(item.itemType)) {
    return false
  }

  // Loot type filter
  if (filters.lootTypes.length > 0 && item.lootType && !filters.lootTypes.includes(item.lootType)) {
    return false
  }

  // Show only targeted
  if (filters.showOnlyTargeted) {
    const isSelected = priorityTargets.some(t => t.id === item.id && t.type === 'ITEM') ||
                       ongoingTargets.some(t => t.id === item.id && t.type === 'ITEM')
    if (!isSelected) return false
  }

  return true
})

// Render FilterPanel
<FilterPanel
  filters={filters}
  onFilterChange={setFilters}
  availableRarities={availableRarities}
  availableItemTypes={availableItemTypes}
  availableLootTypes={availableLootTypes}
/>
```

#### Task 2.3: Add Collapsible Filter Panel

```typescript
// Make filter panel collapsible to save space
const [filtersExpanded, setFiltersExpanded] = useState(false)

<button
  onClick={() => setFiltersExpanded(!filtersExpanded)}
  className="w-full flex items-center justify-between px-3 py-2 mb-2 bg-gray-700 rounded hover:bg-gray-600 transition-colors"
>
  <span className="text-sm font-semibold text-white">
    Filters {hasActiveFilters && `(${activeFilterCount})`}
  </span>
  <svg
    className={`w-4 h-4 transition-transform ${filtersExpanded ? 'rotate-180' : ''}`}
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
  >
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
  </svg>
</button>

{filtersExpanded && (
  <FilterPanel
    filters={filters}
    onFilterChange={setFilters}
    availableRarities={availableRarities}
    availableItemTypes={availableItemTypes}
    availableLootTypes={availableLootTypes}
  />
)}
```

### Backend Enhancement (Optional)

#### Task 2.4: Add Advanced Search Endpoint

**File**: `src/main/java/com/pauloneill/arcraidersplanner/controller/ItemController.java`

```java
@PostMapping("/search")
@Operation(summary = "Advanced item search with filters")
public List<ItemDto> advancedSearch(@RequestBody ItemSearchRequest request) {
    return itemService.searchWithFilters(request);
}

// ItemSearchRequest.java
public record ItemSearchRequest(
    String query,
    List<String> rarities,
    List<String> itemTypes,
    List<String> lootTypes,
    Integer minValue,
    Integer maxValue
) {}
```

### Testing Strategy

```typescript
test('should filter items by rarity', async ({ page }) => {
  await page.goto('/planner')

  // Expand filters
  await page.click('button:has-text("Filters")')

  // Select Legendary rarity
  await page.click('button:has-text("Legendary")')

  // Verify only legendary items shown
  const items = await page.locator('.target-card').all()
  for (const item of items) {
    const rarity = await item.locator('.rarity-badge').textContent()
    expect(rarity).toBe('Legendary')
  }
})
```

### Timeline

- FilterPanel component: **2 days**
- LeftPanel integration: **1 day**
- Backend advanced search (optional): **2 days**
- Testing: **1 day**
- **Total**: 4-6 days

---

## 3. Saved Loadouts

### Overview

**WHY**: Users frequently target the same combinations of items/enemies/containers. Saved loadouts allow them to quickly load pre-configured target selections without manually selecting each item.

**Use Cases**:
- "Mechanical Farm" loadout (all mechanical items)
- "ARC Hunting" loadout (specific enemies + their drops)
- "Speed Run" loadout (quick high-value targets)

### Database Schema

#### Task 3.1: Create Loadout Tables

**File**: `src/main/resources/db/migration/V5__Add_Loadout_Tables.sql`

```sql
-- Loadout table
CREATE TABLE loadouts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    routing_profile VARCHAR(50) NOT NULL,
    has_raider_key BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Loadout items (many-to-many)
CREATE TABLE loadout_items (
    loadout_id BIGINT NOT NULL REFERENCES loadouts(id) ON DELETE CASCADE,
    item_name VARCHAR(200) NOT NULL,
    priority VARCHAR(20) NOT NULL, -- PRIORITY or ONGOING
    PRIMARY KEY (loadout_id, item_name, priority)
);

-- Loadout enemies
CREATE TABLE loadout_enemies (
    loadout_id BIGINT NOT NULL REFERENCES loadouts(id) ON DELETE CASCADE,
    enemy_type VARCHAR(100) NOT NULL,
    PRIMARY KEY (loadout_id, enemy_type)
);

-- Loadout recipes
CREATE TABLE loadout_recipes (
    loadout_id BIGINT NOT NULL REFERENCES loadouts(id) ON DELETE CASCADE,
    recipe_metaforge_id VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    PRIMARY KEY (loadout_id, recipe_metaforge_id, priority)
);

-- Loadout containers
CREATE TABLE loadout_containers (
    loadout_id BIGINT NOT NULL REFERENCES loadouts(id) ON DELETE CASCADE,
    container_type VARCHAR(100) NOT NULL,
    PRIMARY KEY (loadout_id, container_type)
);

CREATE INDEX idx_loadouts_created ON loadouts(created_at DESC);
```

### Backend Implementation

#### Task 3.2: Create Loadout Entity

**File**: `src/main/java/com/pauloneill/arcraidersplanner/model/Loadout.java`

```java
@Entity
@Table(name = "loadouts")
@Data
public class Loadout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_profile", nullable = false)
    private RoutingProfile routingProfile;

    @Column(name = "has_raider_key")
    private Boolean hasRaiderKey = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "loadout_items", joinColumns = @JoinColumn(name = "loadout_id"))
    private Set<LoadoutItem> items = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "loadout_enemies", joinColumns = @JoinColumn(name = "loadout_id"))
    @Column(name = "enemy_type")
    private Set<String> enemies = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "loadout_recipes", joinColumns = @JoinColumn(name = "loadout_id"))
    private Set<LoadoutRecipe> recipes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "loadout_containers", joinColumns = @JoinColumn(name = "loadout_id"))
    @Column(name = "container_type")
    private Set<String> containers = new HashSet<>();
}

@Embeddable
@Data
class LoadoutItem {
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetPriority priority;
}

@Embeddable
@Data
class LoadoutRecipe {
    @Column(name = "recipe_metaforge_id", nullable = false)
    private String recipeMetaforgeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetPriority priority;
}

enum TargetPriority {
    PRIORITY,
    ONGOING
}
```

#### Task 3.3: Create LoadoutController

**File**: `src/main/java/com/pauloneill/arcraidersplanner/controller/LoadoutController.java`

```java
@RestController
@RequestMapping("/api/loadouts")
@Tag(name = "Loadouts", description = "Saved target configurations")
public class LoadoutController {

    private final LoadoutService loadoutService;

    @GetMapping
    @Operation(summary = "Get all loadouts")
    public List<LoadoutDto> getAllLoadouts() {
        return loadoutService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loadout by ID")
    public LoadoutDto getLoadout(@PathVariable Long id) {
        return loadoutService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create new loadout")
    public ResponseEntity<LoadoutDto> createLoadout(@Valid @RequestBody CreateLoadoutRequest request) {
        LoadoutDto created = loadoutService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update loadout")
    public LoadoutDto updateLoadout(@PathVariable Long id, @Valid @RequestBody UpdateLoadoutRequest request) {
        return loadoutService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete loadout")
    public ResponseEntity<Void> deleteLoadout(@PathVariable Long id) {
        loadoutService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    @Operation(summary = "Apply loadout to current session")
    public PlannerRequestDto applyLoadout(@PathVariable Long id) {
        return loadoutService.applyLoadout(id);
    }
}
```

#### Task 3.4: Create LoadoutService

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/LoadoutService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadoutService {

    private final LoadoutRepository loadoutRepository;
    private final DtoMapper dtoMapper;

    public List<LoadoutDto> findAll() {
        return loadoutRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());
    }

    public LoadoutDto findById(Long id) {
        return loadoutRepository.findById(id)
            .map(dtoMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Loadout not found: " + id
            ));
    }

    @Transactional
    public LoadoutDto create(CreateLoadoutRequest request) {
        Loadout loadout = new Loadout();
        loadout.setName(request.name());
        loadout.setDescription(request.description());
        loadout.setRoutingProfile(request.routingProfile());
        loadout.setHasRaiderKey(request.hasRaiderKey());

        // Add items
        request.priorityItems().forEach(itemName -> {
            LoadoutItem item = new LoadoutItem();
            item.setItemName(itemName);
            item.setPriority(TargetPriority.PRIORITY);
            loadout.getItems().add(item);
        });

        request.ongoingItems().forEach(itemName -> {
            LoadoutItem item = new LoadoutItem();
            item.setItemName(itemName);
            item.setPriority(TargetPriority.ONGOING);
            loadout.getItems().add(item);
        });

        // Add enemies, recipes, containers
        loadout.setEnemies(new HashSet<>(request.enemies()));
        // ... similar for recipes and containers

        Loadout saved = loadoutRepository.save(loadout);
        return dtoMapper.toDto(saved);
    }

    /**
     * Convert loadout to PlannerRequest for immediate use
     */
    public PlannerRequestDto applyLoadout(Long id) {
        Loadout loadout = loadoutRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Loadout not found: " + id
            ));

        return new PlannerRequestDto(
            loadout.getItems().stream()
                .filter(i -> i.getPriority() == TargetPriority.PRIORITY)
                .map(LoadoutItem::getItemName)
                .collect(Collectors.toList()),
            new ArrayList<>(loadout.getEnemies()),
            loadout.getRecipes().stream()
                .filter(r -> r.getPriority() == TargetPriority.PRIORITY)
                .map(LoadoutRecipe::getRecipeMetaforgeId)
                .collect(Collectors.toList()),
            new ArrayList<>(loadout.getContainers()),
            loadout.getHasRaiderKey(),
            loadout.getRoutingProfile(),
            loadout.getItems().stream()
                .filter(i -> i.getPriority() == TargetPriority.ONGOING)
                .map(LoadoutItem::getItemName)
                .collect(Collectors.toList())
        );
    }
}
```

### Frontend Implementation

#### Task 3.5: Create LoadoutManager Component

**File**: `frontend/src/components/LoadoutManager.tsx`

```typescript
import React, { useState, useEffect } from 'react'
import { loadoutApi } from '../api/loadoutApi'
import type { Loadout } from '../types'

interface LoadoutManagerProps {
  onApplyLoadout: (loadout: Loadout) => void
  onClose: () => void
}

export const LoadoutManager: React.FC<LoadoutManagerProps> = ({
  onApplyLoadout,
  onClose,
}) => {
  const [loadouts, setLoadouts] = useState<Loadout[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadoutApi.getAll()
      .then(setLoadouts)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this loadout?')) return

    try {
      await loadoutApi.delete(id)
      setLoadouts(loadouts.filter(l => l.id !== id))
    } catch (error) {
      console.error('Failed to delete loadout:', error)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-gray-800 rounded-lg p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl font-bold text-white">Saved Loadouts</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-white">
            ✕
          </button>
        </div>

        {loading ? (
          <div className="text-center py-8 text-gray-400">Loading...</div>
        ) : loadouts.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            No saved loadouts yet
          </div>
        ) : (
          <div className="space-y-3">
            {loadouts.map(loadout => (
              <div key={loadout.id} className="bg-gray-700 rounded-lg p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-white mb-1">
                      {loadout.name}
                    </h3>
                    {loadout.description && (
                      <p className="text-sm text-gray-400 mb-2">
                        {loadout.description}
                      </p>
                    )}
                    <div className="flex gap-2 text-xs text-gray-300">
                      <span className="px-2 py-1 bg-gray-600 rounded">
                        {loadout.routingProfile}
                      </span>
                      {loadout.priorityItems.length > 0 && (
                        <span className="px-2 py-1 bg-gray-600 rounded">
                          {loadout.priorityItems.length} items
                        </span>
                      )}
                      {loadout.enemies.length > 0 && (
                        <span className="px-2 py-1 bg-gray-600 rounded">
                          {loadout.enemies.length} enemies
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2 ml-4">
                    <button
                      onClick={() => onApplyLoadout(loadout)}
                      className="px-3 py-1 bg-orange-500 hover:bg-orange-600 text-white rounded text-sm"
                    >
                      Apply
                    </button>
                    <button
                      onClick={() => handleDelete(loadout.id)}
                      className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded text-sm"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
```

#### Task 3.6: Add Save Loadout Button to LeftPanel

```typescript
// Add save button in LeftPanel
<button
  onClick={handleSaveLoadout}
  disabled={priorityTargets.length === 0}
  className="w-full px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded text-sm font-semibold"
>
  💾 Save Current Loadout
</button>

const handleSaveLoadout = async () => {
  const name = prompt('Loadout name:')
  if (!name) return

  const description = prompt('Description (optional):') || undefined

  try {
    await loadoutApi.create({
      name,
      description,
      priorityItems: priorityTargets.filter(t => t.type === 'ITEM').map(t => t.name),
      ongoingItems: ongoingTargets.filter(t => t.type === 'ITEM').map(t => t.name),
      enemies: priorityTargets.filter(t => t.type === 'ENEMY').map(t => t.name),
      recipes: priorityTargets.filter(t => t.type === 'RECIPE').map(t => String(t.id)),
      containers: priorityTargets.filter(t => t.type === 'CONTAINER').map(t => t.name),
      routingProfile,
      hasRaiderKey,
    })

    alert('Loadout saved!')
  } catch (error) {
    alert('Failed to save loadout')
    console.error(error)
  }
}
```

### Timeline

- Database migration: **1 day**
- Backend (entities, service, controller): **3 days**
- Frontend (LoadoutManager, integration): **3 days**
- Testing: **2 days**
- **Total**: 9 days

---

## 4. Route Comparison

### Overview

**WHY**: The planner API returns multiple route options ranked by score. Users should be able to compare these routes side-by-side to choose the one that fits their playstyle (shortest, safest, most loot, etc.).

**Current State**: Only the top-ranked route is shown.

**Desired State**: Show top 3 routes with comparison metrics, allow switching between them.

### Backend (Already Complete)

No backend changes needed - API already returns multiple routes!

### Frontend Implementation

#### Task 4.1: Store All Routes

**File**: `frontend/src/pages/TacticalPlannerPage.tsx`

```typescript
// Change state to store all routes
const [availableRoutes, setAvailableRoutes] = useState<PlannerResponse[]>([])
const [selectedRouteIndex, setSelectedRouteIndex] = useState(0)

// Update handleCalculateRoute
const handleCalculateRoute = useCallback(async () => {
  setIsCalculating(true)
  setError(null)

  try {
    const request: PlannerRequest = {
      // ... build request
    }

    const routes = await plannerApi.generateRoute(request)

    if (routes && routes.length > 0) {
      setAvailableRoutes(routes)
      setSelectedRouteIndex(0) // Select first route
      setCalculatedRoute(routes[0])
      setUiMode('PLANNING')
    } else {
      setError('No routes found for selected targets')
    }
  } catch (err) {
    console.error('Failed to calculate route:', err)
    setError(err instanceof Error ? err.message : 'Failed to calculate route')
  } finally {
    setIsCalculating(false)
  }
}, [priorityTargets, ongoingTargets, routingProfile, hasRaiderKey])
```

#### Task 4.2: Create RouteComparison Component

**File**: `frontend/src/components/RouteComparison.tsx`

```typescript
import React from 'react'
import type { PlannerResponse } from '../types'

interface RouteComparisonProps {
  routes: PlannerResponse[]
  selectedIndex: number
  onSelectRoute: (index: number) => void
}

export const RouteComparison: React.FC<RouteComparisonProps> = ({
  routes,
  selectedIndex,
  onSelectRoute,
}) => {
  if (routes.length <= 1) return null

  return (
    <div className="absolute top-20 right-4 z-20 bg-gray-900/95 backdrop-blur-sm rounded-lg p-4 max-w-md">
      <h3 className="text-sm font-semibold text-orange-500 mb-3 uppercase">
        Route Options ({routes.length})
      </h3>

      <div className="space-y-2">
        {routes.slice(0, 3).map((route, index) => (
          <button
            key={index}
            onClick={() => onSelectRoute(index)}
            className={`w-full text-left p-3 rounded-lg transition-colors ${
              selectedIndex === index
                ? 'bg-orange-500 text-white'
                : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="font-semibold">
                {index === 0 ? '⭐ Best' : `Option ${index + 1}`}
              </span>
              <span className="text-sm">
                Score: {route.score.toFixed(0)}
              </span>
            </div>

            <div className="text-xs opacity-80 space-y-1">
              <div className="flex justify-between">
                <span>Waypoints:</span>
                <span>{route.path.length}</span>
              </div>
              {route.extractionPoint && (
                <div className="flex justify-between">
                  <span>Extraction:</span>
                  <span>{route.extractionPoint}</span>
                </div>
              )}
              {route.nearbyEnemySpawns && route.nearbyEnemySpawns.length > 0 && (
                <div className="flex justify-between">
                  <span>Enemies:</span>
                  <span>{route.nearbyEnemySpawns.length}</span>
                </div>
              )}
            </div>
          </button>
        ))}
      </div>

      {routes.length > 3 && (
        <div className="mt-2 text-xs text-gray-400 text-center">
          +{routes.length - 3} more options available
        </div>
      )}
    </div>
  )
}
```

#### Task 4.3: Integrate into MaximizedMapView

```typescript
// Add to MaximizedMapView
<MaximizedMapView
  route={calculatedRoute}
  onMinimize={handleMinimize}
  routingProfile={routingProfile}

  // NEW: Route comparison props
  availableRoutes={availableRoutes}
  selectedRouteIndex={selectedRouteIndex}
  onSelectRoute={(index) => {
    setSelectedRouteIndex(index)
    setCalculatedRoute(availableRoutes[index])
  }}
/>

// In MaximizedMapView component
{availableRoutes && availableRoutes.length > 1 && (
  <RouteComparison
    routes={availableRoutes}
    selectedIndex={selectedRouteIndex}
    onSelectRoute={onSelectRoute}
  />
)}
```

#### Task 4.4: Add Route Metrics Comparison Table

```typescript
// Enhanced comparison with detailed metrics
<div className="overflow-x-auto">
  <table className="w-full text-xs">
    <thead className="bg-gray-800">
      <tr>
        <th className="px-2 py-1 text-left">Metric</th>
        {routes.slice(0, 3).map((_, index) => (
          <th key={index} className="px-2 py-1 text-center">
            Route {index + 1}
          </th>
        ))}
      </tr>
    </thead>
    <tbody className="text-gray-300">
      <tr>
        <td className="px-2 py-1">Score</td>
        {routes.slice(0, 3).map((route, index) => (
          <td key={index} className="px-2 py-1 text-center">
            {route.score.toFixed(0)}
          </td>
        ))}
      </tr>
      <tr className="bg-gray-800/50">
        <td className="px-2 py-1">Waypoints</td>
        {routes.slice(0, 3).map((route, index) => (
          <td key={index} className="px-2 py-1 text-center">
            {route.path.length}
          </td>
        ))}
      </tr>
      <tr>
        <td className="px-2 py-1">Enemies</td>
        {routes.slice(0, 3).map((route, index) => (
          <td key={index} className="px-2 py-1 text-center">
            {route.nearbyEnemySpawns?.length || 0}
          </td>
        ))}
      </tr>
    </tbody>
  </table>
</div>
```

### Testing Strategy

```typescript
test('should allow switching between route options', async ({ page }) => {
  await page.goto('/planner')

  // Select targets and calculate
  await page.click('text=Rocket Thruster Core')
  await page.click('button:has-text("Calculate Route")')

  // Wait for routes to load
  await page.waitForSelector('text=Route Options')

  // Verify multiple routes shown
  const routeOptions = await page.locator('.route-option').count()
  expect(routeOptions).toBeGreaterThan(1)

  // Click second route
  await page.click('text=Option 2')

  // Verify map updated
  await page.waitForTimeout(500)

  // Check that different route is displayed
  const selectedRoute = await page.locator('.route-option.selected')
  await expect(selectedRoute).toContainText('Option 2')
})
```

### Timeline

- RouteComparison component: **2 days**
- Integration with state management: **1 day**
- Enhanced metrics table: **1 day**
- Testing: **1 day**
- **Total**: 5 days

---

## 5. Mobile Optimization

### Overview

**WHY**: Current layout uses fixed 3-column grid that breaks on mobile. Need responsive design with collapsible panels and touch-optimized controls.

**Target Devices**:
- Mobile phones (320px - 768px)
- Tablets (768px - 1024px)
- Desktop (1024px+)

### Implementation Strategy

#### Task 5.1: Add Responsive Layout Breakpoints

**File**: `frontend/src/pages/TacticalPlannerPage.tsx`

```typescript
// Add mobile state
const [mobilePanel, setMobilePanel] = useState<'left' | 'center' | 'right'>('left')
const [isMobile, setIsMobile] = useState(false)

useEffect(() => {
  const checkMobile = () => {
    setIsMobile(window.innerWidth < 768)
  }

  checkMobile()
  window.addEventListener('resize', checkMobile)

  return () => window.removeEventListener('resize', checkMobile)
}, [])

// Conditional layout
return isMobile ? (
  <MobileLayout
    activePanel={mobilePanel}
    onPanelChange={setMobilePanel}
    // ... other props
  />
) : (
  <DesktopLayout /* ... */ />
)
```

#### Task 5.2: Create MobileLayout Component

**File**: `frontend/src/components/MobileLayout.tsx`

```typescript
export const MobileLayout: React.FC<MobileLayoutProps> = ({
  activePanel,
  onPanelChange,
  // ... other props
}) => {
  return (
    <div className="h-screen flex flex-col bg-gray-900">
      {/* Mobile Navigation */}
      <div className="flex bg-gray-800 border-b border-gray-700">
        <button
          onClick={() => onPanelChange('left')}
          className={`flex-1 py-3 text-sm font-semibold ${
            activePanel === 'left'
              ? 'bg-orange-500 text-white'
              : 'text-gray-300'
          }`}
        >
          Targets
        </button>
        <button
          onClick={() => onPanelChange('center')}
          className={`flex-1 py-3 text-sm font-semibold ${
            activePanel === 'center'
              ? 'bg-orange-500 text-white'
              : 'text-gray-300'
          }`}
        >
          Details
        </button>
        <button
          onClick={() => onPanelChange('right')}
          className={`flex-1 py-3 text-sm font-semibold ${
            activePanel === 'right'
              ? 'bg-orange-500 text-white'
              : 'text-gray-300'
          }`}
        >
          Map
        </button>
      </div>

      {/* Panel Content */}
      <div className="flex-1 overflow-hidden">
        {activePanel === 'left' && (
          <div className="h-full overflow-y-auto p-4">
            <LeftPanel onItemSelect={onItemSelect} />
          </div>
        )}

        {activePanel === 'center' && (
          <div className="h-full overflow-y-auto p-4">
            <CenterPanel item={selectedItem} />
          </div>
        )}

        {activePanel === 'right' && (
          <div className="h-full">
            {/* Map view fullscreen on mobile */}
            <MinimizedMapView
              selectedMap={selectedMap}
              onMapChange={handleMapChange}
              highlightedZones={highlightedZones}
              onCalculateRoute={handleCalculateRoute}
              hasTargets={priorityTargets.length > 0}
            />
          </div>
        )}
      </div>
    </div>
  )
}
```

#### Task 5.3: Touch-Optimized Controls

```typescript
// Increase touch target sizes for mobile
const touchOptimizedClasses = isMobile ? 'min-h-[44px] text-base' : 'min-h-[32px] text-sm'

<button
  className={`px-4 py-2 rounded ${touchOptimizedClasses}`}
>
  Calculate Route
</button>

// Add swipe gestures for panel navigation
import { useSwipeable } from 'react-swipeable'

const swipeHandlers = useSwipeable({
  onSwipedLeft: () => {
    if (mobilePanel === 'left') setMobilePanel('center')
    else if (mobilePanel === 'center') setMobilePanel('right')
  },
  onSwipedRight: () => {
    if (mobilePanel === 'right') setMobilePanel('center')
    else if (mobilePanel === 'center') setMobilePanel('left')
  },
  trackMouse: false,
})

<div {...swipeHandlers} className="h-full">
  {/* Panel content */}
</div>
```

#### Task 5.4: Mobile Map Optimizations

```typescript
// Reduce map complexity on mobile
const mapSettings = isMobile ? {
  minZoom: -2,
  maxZoom: 1,
  zoomControl: false, // Use custom larger controls
  touchZoom: true,
  doubleClickZoom: true,
} : {
  minZoom: -1,
  maxZoom: 2,
  zoomControl: true,
}

// Custom zoom controls for mobile
<div className="absolute bottom-20 right-4 flex flex-col gap-2">
  <button
    onClick={handleZoomIn}
    className="w-12 h-12 bg-gray-800 rounded-full text-white text-2xl"
  >
    +
  </button>
  <button
    onClick={handleZoomOut}
    className="w-12 h-12 bg-gray-800 rounded-full text-white text-2xl"
  >
    −
  </button>
</div>
```

#### Task 5.5: Responsive Typography & Spacing

**File**: `frontend/tailwind.config.js`

```javascript
module.exports = {
  theme: {
    extend: {
      fontSize: {
        // Mobile-first responsive scales
        'xs': ['0.75rem', { lineHeight: '1rem' }],
        'sm': ['0.875rem', { lineHeight: '1.25rem' }],
        'base': ['1rem', { lineHeight: '1.5rem' }],
        'lg': ['1.125rem', { lineHeight: '1.75rem' }],
      },
      spacing: {
        // Touch-friendly spacing
        'touch': '44px', // Minimum touch target size
      },
    },
  },
}
```

### Testing Strategy

```typescript
// Mobile viewport tests
test.describe('Mobile Layout', () => {
  test.use({ viewport: { width: 375, height: 667 } }) // iPhone SE

  test('should show panel navigation', async ({ page }) => {
    await page.goto('/planner')

    // Verify tab navigation visible
    await expect(page.locator('button:has-text("Targets")')).toBeVisible()
    await expect(page.locator('button:has-text("Details")')).toBeVisible()
    await expect(page.locator('button:has-text("Map")')).toBeVisible()
  })

  test('should switch panels on tab click', async ({ page }) => {
    await page.goto('/planner')

    // Switch to Details
    await page.click('button:has-text("Details")')
    await expect(page.locator('.center-panel')).toBeVisible()

    // Switch to Map
    await page.click('button:has-text("Map")')
    await expect(page.locator('.map-container')).toBeVisible()
  })

  test('should have touch-sized buttons', async ({ page }) => {
    await page.goto('/planner')

    const button = page.locator('button:has-text("Calculate Route")')
    const box = await button.boundingBox()

    expect(box?.height).toBeGreaterThanOrEqual(44) // iOS touch target minimum
  })
})
```

### Timeline

- Responsive layout system: **3 days**
- MobileLayout component: **2 days**
- Touch optimizations: **2 days**
- Mobile map controls: **1 day**
- Testing on devices: **2 days**
- **Total**: 10 days

---

## 6. Performance Optimizations

### Overview

**WHY**: Large item lists (100+ items) and complex map rendering can cause lag. Performance optimizations ensure smooth 60fps experience.

### Specific Optimizations

#### Task 6.1: Virtual Scrolling for Item Lists

**File**: `frontend/src/components/LeftPanel.tsx`

```typescript
import { useVirtualizer } from '@tanstack/react-virtual'

// Replace standard map with virtual list
const parentRef = useRef<HTMLDivElement>(null)

const virtualizer = useVirtualizer({
  count: filteredItems.length,
  getScrollElement: () => parentRef.current,
  estimateSize: () => 80, // Estimated item height
  overscan: 5, // Render 5 extra items above/below viewport
})

return (
  <div ref={parentRef} className="flex-1 overflow-y-auto">
    <div
      style={{
        height: `${virtualizer.getTotalSize()}px`,
        width: '100%',
        position: 'relative',
      }}
    >
      {virtualizer.getVirtualItems().map((virtualItem) => {
        const item = filteredItems[virtualItem.index]

        return (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            <TargetCard
              id={item.id}
              type="ITEM"
              name={item.name}
              // ... other props
            />
          </div>
        )
      })}
    </div>
  </div>
)
```

#### Task 6.2: Debounce Search Input

```typescript
import { useDebouncedValue } from '../hooks/useDebouncedValue'

// Debounce search to reduce re-renders
const [searchInput, setSearchInput] = useState('')
const debouncedSearch = useDebouncedValue(searchInput, 300) // 300ms delay

// Use debounced value for filtering
const filteredItems = useMemo(() => {
  return items.filter(item => {
    if (debouncedSearch && !item.name.toLowerCase().includes(debouncedSearch.toLowerCase())) {
      return false
    }
    // ... other filters
  })
}, [items, debouncedSearch, filters])

// useDebouncedValue hook
export function useDebouncedValue<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => clearTimeout(handler)
  }, [value, delay])

  return debouncedValue
}
```

#### Task 6.3: Memoize Expensive Calculations

```typescript
// Memoize filtered items
const filteredItems = useMemo(() => {
  return items.filter(item => {
    // ... filtering logic
  })
}, [items, searchTerm, filters, activeTab])

// Memoize available filter options
const availableRarities = useMemo(() =>
  [...new Set(items.map(i => i.rarity))].filter(Boolean).sort(),
  [items]
)

// Memoize area conversions in MaximizedMapView
const areas = useMemo<Area[]>(() => {
  if (!route || !route.path) return []

  return route.path
    .filter(wp => wp.type === 'AREA')
    .map(wp => ({
      // ... conversion logic
    }))
}, [route])
```

#### Task 6.4: React.memo for Components

```typescript
// Memoize expensive components
export const TargetCard = React.memo<TargetCardProps>(({ ... }) => {
  // Component implementation
}, (prevProps, nextProps) => {
  // Custom comparison - only re-render if these change
  return (
    prevProps.selectionState === nextProps.selectionState &&
    prevProps.id === nextProps.id
  )
})

export const RecipeCard = React.memo<RecipeCardProps>(({ recipe, compact }) => {
  // Component implementation
})

export const MinimizedMapView = React.memo<MinimizedMapViewProps>(({ ... }) => {
  // Component implementation
})
```

#### Task 6.5: Code Splitting & Lazy Loading

```typescript
// Lazy load heavy components
const MapComponent = React.lazy(() => import('../MapComponent'))
const LoadoutManager = React.lazy(() => import('./LoadoutManager'))

// Use Suspense for loading states
<Suspense fallback={<div className="flex items-center justify-center h-full">
  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500" />
</div>}>
  <MapComponent {...props} />
</Suspense>
```

#### Task 6.6: Image Optimization

```typescript
// Lazy load item icons
import { LazyLoadImage } from 'react-lazy-load-image-component'

<LazyLoadImage
  src={item.iconUrl}
  alt={item.name}
  className="w-12 h-12 rounded"
  effect="opacity"
  placeholder={<div className="w-12 h-12 bg-gray-700 rounded animate-pulse" />}
/>

// Preload map images
const preloadMapImage = (mapName: string) => {
  const img = new Image()
  img.src = getMapImageUrl(mapName)
}

useEffect(() => {
  preloadMapImage('Dam Battlegrounds')
  preloadMapImage('Ironwood Hydroponics')
}, [])
```

### Monitoring & Metrics

#### Task 6.7: Add Performance Monitoring

```typescript
// Add performance marks
const startTime = performance.now()

// ... expensive operation

const endTime = performance.now()
console.log(`Operation took ${endTime - startTime}ms`)

// React Profiler for component render times
import { Profiler } from 'react'

<Profiler id="LeftPanel" onRender={(id, phase, actualDuration) => {
  if (actualDuration > 16) { // > 1 frame at 60fps
    console.warn(`${id} took ${actualDuration}ms to ${phase}`)
  }
}}>
  <LeftPanel {...props} />
</Profiler>
```

### Timeline

- Virtual scrolling: **2 days**
- Debouncing & memoization: **2 days**
- React.memo optimization: **1 day**
- Code splitting: **1 day**
- Image optimization: **1 day**
- Performance monitoring: **1 day**
- **Total**: 8 days

---

## 7. Accessibility Improvements

### Overview

**WHY**: Ensure the application is usable by everyone, including users with disabilities. Meet WCAG 2.1 Level AA standards.

### Implementation Tasks

#### Task 7.1: Keyboard Navigation

```typescript
// Add keyboard handlers to all interactive elements
const handleKeyDown = (e: React.KeyboardEvent, action: () => void) => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    action()
  }
}

// Make card clickable with keyboard
<div
  role="button"
  tabIndex={0}
  onKeyDown={(e) => handleKeyDown(e, () => handleTargetToggle(target))}
  onClick={() => handleTargetToggle(target)}
  className="target-card"
>
  {/* Card content */}
</div>

// Add focus visible styles
.target-card:focus-visible {
  @apply ring-2 ring-orange-500 ring-offset-2 ring-offset-gray-900 outline-none;
}

// Skip to main content link
<a
  href="#main-content"
  className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 z-50 bg-orange-500 text-white px-4 py-2 rounded"
>
  Skip to main content
</a>

<main id="main-content">
  {/* Main content */}
</main>
```

#### Task 7.2: ARIA Labels & Roles

```typescript
// Proper ARIA labels
<button
  aria-label="Calculate route with selected targets"
  aria-disabled={!hasTargets}
>
  Calculate Route
</button>

// Live region for dynamic content
<div
  role="status"
  aria-live="polite"
  aria-atomic="true"
  className="sr-only"
>
  {isCalculating ? 'Calculating route...' : ''}
  {calculatedRoute ? `Route calculated with ${calculatedRoute.path.length} waypoints` : ''}
</div>

// Proper heading hierarchy
<h1 className="sr-only">ARC Raiders Loot Planner</h1>
<h2>Objectives</h2>
<h3>Items</h3>

// Proper form labels
<label htmlFor="map-select" className="block text-xs text-gray-400 mb-2">
  Select Map
</label>
<select id="map-select" {...props}>
  {/* Options */}
</select>
```

#### Task 7.3: Color Contrast & Visual Indicators

```typescript
// Ensure WCAG AA contrast ratios (4.5:1 for normal text, 3:1 for large)
// Check with: https://webaim.org/resources/contrastchecker/

// Don't rely on color alone - add icons/patterns
<div className="flex items-center gap-2">
  {selectionState === 'priority' && (
    <>
      <span className="text-orange-500" aria-hidden="true">⭐</span>
      <span className="text-orange-500">Priority</span>
    </>
  )}
  {selectionState === 'ongoing' && (
    <>
      <span className="text-blue-500" aria-hidden="true">➕</span>
      <span className="text-blue-500">Ongoing</span>
    </>
  )}
</div>

// High contrast mode support
@media (prefers-contrast: high) {
  .target-card {
    @apply border-2;
  }
}

// Reduced motion support
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

#### Task 7.4: Screen Reader Support

```typescript
// Descriptive button text
<button aria-label={`Remove ${item.name} from priority targets`}>
  <span aria-hidden="true">✕</span>
  <span className="sr-only">Remove {item.name}</span>
</button>

// Table accessibility
<table>
  <caption className="sr-only">Recipe Ingredients</caption>
  <thead>
    <tr>
      <th scope="col">Ingredient</th>
      <th scope="col">Quantity</th>
    </tr>
  </thead>
  <tbody>
    {/* Rows */}
  </tbody>
</table>

// Image alt text
<img
  src={item.iconUrl}
  alt={`${item.name} - ${item.rarity} ${item.itemType}`}
/>

// Loading states
<div
  role="alert"
  aria-busy={isCalculating}
  aria-live="assertive"
>
  {isCalculating && 'Calculating route, please wait...'}
</div>
```

#### Task 7.5: Focus Management

```typescript
// Trap focus in modals
import { useFocusTrap } from '../hooks/useFocusTrap'

const LoadoutManager: React.FC<Props> = ({ onClose }) => {
  const modalRef = useRef<HTMLDivElement>(null)
  useFocusTrap(modalRef)

  return (
    <div ref={modalRef} role="dialog" aria-modal="true">
      {/* Modal content */}
    </div>
  )
}

// Restore focus after closing
const previousFocusRef = useRef<HTMLElement | null>(null)

const openModal = () => {
  previousFocusRef.current = document.activeElement as HTMLElement
  setIsOpen(true)
}

const closeModal = () => {
  setIsOpen(false)
  previousFocusRef.current?.focus()
}

// Focus first error on form submit
const firstErrorRef = useRef<HTMLInputElement>(null)

useEffect(() => {
  if (errors.length > 0) {
    firstErrorRef.current?.focus()
  }
}, [errors])
```

#### Task 7.6: Accessible Map Interactions

```typescript
// Keyboard controls for map
const handleMapKeyDown = (e: React.KeyboardEvent) => {
  switch (e.key) {
    case 'ArrowUp':
      e.preventDefault()
      panMap('up')
      break
    case 'ArrowDown':
      e.preventDefault()
      panMap('down')
      break
    case '+':
      e.preventDefault()
      zoomIn()
      break
    case '-':
      e.preventDefault()
      zoomOut()
      break
  }
}

// Announce map changes
<div
  role="region"
  aria-label="Interactive map"
  tabIndex={0}
  onKeyDown={handleMapKeyDown}
>
  <MapComponent {...props} />
</div>

<div role="status" aria-live="polite" className="sr-only">
  {selectedMap} map selected. {highlightedZones.length} zones highlighted.
</div>
```

### Testing Strategy

#### Automated Accessibility Tests

```typescript
// Using jest-axe
import { axe, toHaveNoViolations } from 'jest-axe'

expect.extend(toHaveNoViolations)

test('should have no accessibility violations', async () => {
  const { container } = render(<TacticalPlannerPage />)
  const results = await axe(container)
  expect(results).toHaveNoViolations()
})

// Using Playwright
test('should be keyboard navigable', async ({ page }) => {
  await page.goto('/planner')

  // Tab through interactive elements
  await page.keyboard.press('Tab')
  await expect(page.locator(':focus')).toHaveText('Targets')

  await page.keyboard.press('Tab')
  await expect(page.locator(':focus')).toHaveText('Details')

  // Activate with Enter
  await page.keyboard.press('Enter')
  await expect(page.locator('.details-panel')).toBeVisible()
})
```

#### Manual Testing Checklist

- [ ] Screen reader testing (NVDA, JAWS, VoiceOver)
- [ ] Keyboard-only navigation
- [ ] High contrast mode
- [ ] Zoom to 200%
- [ ] Color blindness simulation
- [ ] Focus indicators visible
- [ ] No keyboard traps

### Timeline

- Keyboard navigation: **2 days**
- ARIA labels & semantics: **2 days**
- Color contrast fixes: **1 day**
- Screen reader optimization: **2 days**
- Focus management: **1 day**
- Testing & validation: **2 days**
- **Total**: 10 days

---

## 8. Additional Features

### Quick Wins

#### Task 8.1: Export Route to JSON

```typescript
const exportRoute = (route: PlannerResponse) => {
  const data = JSON.stringify(route, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `route-${route.mapName}-${Date.now()}.json`
  a.click()

  URL.revokeObjectURL(url)
}

<button onClick={() => exportRoute(calculatedRoute)}>
  📥 Export Route
</button>
```

#### Task 8.2: Print Route

```typescript
const printRoute = () => {
  window.print()
}

// Print-specific styles
@media print {
  .no-print {
    display: none !important;
  }

  .print-only {
    display: block !important;
  }

  .map-container {
    page-break-inside: avoid;
  }
}
```

#### Task 8.3: Share Route via URL

```typescript
// Encode route in URL parameters
const shareRoute = (route: PlannerResponse) => {
  const encoded = encodeURIComponent(JSON.stringify({
    targets: priorityTargets,
    map: selectedMap,
    profile: routingProfile,
  }))

  const shareUrl = `${window.location.origin}/planner?route=${encoded}`

  navigator.clipboard.writeText(shareUrl)
  alert('Route link copied to clipboard!')
}

// Load route from URL on mount
useEffect(() => {
  const params = new URLSearchParams(window.location.search)
  const routeData = params.get('route')

  if (routeData) {
    try {
      const decoded = JSON.parse(decodeURIComponent(routeData))
      // Apply route data to state
    } catch (error) {
      console.error('Invalid route data in URL')
    }
  }
}, [])
```

#### Task 8.4: Dark/Light Mode Toggle

```typescript
const [theme, setTheme] = useState<'dark' | 'light'>('dark')

useEffect(() => {
  document.documentElement.classList.toggle('light', theme === 'light')
}, [theme])

// Tailwind config
module.exports = {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Define light mode colors
      },
    },
  },
}
```

#### Task 8.5: Recent Searches/History

```typescript
const [recentSearches, setRecentSearches] = useState<string[]>([])

useEffect(() => {
  const saved = localStorage.getItem('recentSearches')
  if (saved) setRecentSearches(JSON.parse(saved))
}, [])

const addToRecent = (search: string) => {
  const updated = [search, ...recentSearches.filter(s => s !== search)].slice(0, 5)
  setRecentSearches(updated)
  localStorage.setItem('recentSearches', JSON.stringify(updated))
}

// Show recent searches below search input
{recentSearches.length > 0 && (
  <div className="mt-2">
    <div className="text-xs text-gray-400 mb-1">Recent:</div>
    <div className="flex flex-wrap gap-1">
      {recentSearches.map(search => (
        <button
          key={search}
          onClick={() => setSearchTerm(search)}
          className="px-2 py-1 bg-gray-700 text-gray-300 text-xs rounded hover:bg-gray-600"
        >
          {search}
        </button>
      ))}
    </div>
  </div>
)}
```

---

## Implementation Priority Matrix

| Feature | Impact | Effort | Priority | Timeline |
|---------|--------|--------|----------|----------|
| Zone Highlighting | High | Low | 🔴 Critical | 5 days |
| Advanced Filters | High | Medium | 🟠 High | 6 days |
| Route Comparison | Medium | Low | 🟡 Medium | 5 days |
| Saved Loadouts | High | High | 🟠 High | 9 days |
| Mobile Optimization | Medium | High | 🟡 Medium | 10 days |
| Performance | Medium | Medium | 🟡 Medium | 8 days |
| Accessibility | Medium | High | 🟢 Low* | 10 days |
| Export/Share | Low | Low | 🟢 Low | 2 days |

*Low priority for MVP, but important for long-term product quality

---

## Recommended Implementation Order

### Phase 1: Quick Wins (2 weeks)
1. Zone Highlighting (5 days)
2. Advanced Filters (6 days)
3. Export/Share features (2 days)

### Phase 2: User Experience (3 weeks)
4. Route Comparison (5 days)
5. Saved Loadouts (9 days)
6. Mobile Optimization (10 days)

### Phase 3: Quality & Performance (3 weeks)
7. Performance Optimizations (8 days)
8. Accessibility Improvements (10 days)
9. Testing & polish (4 days)

**Total Estimated Time**: 8 weeks for all enhancements

---

## Success Metrics

Track these metrics to measure success of enhancements:

### User Engagement
- Average session duration
- Number of routes calculated per session
- Loadout usage rate
- Filter usage rate

### Performance
- Time to Interactive (TTI) < 3s
- Largest Contentful Paint (LCP) < 2.5s
- First Input Delay (FID) < 100ms
- Cumulative Layout Shift (CLS) < 0.1

### Accessibility
- Keyboard task completion rate
- Screen reader compatibility score
- WCAG 2.1 AA compliance
- Zero critical accessibility violations

### Mobile
- Mobile bounce rate
- Mobile vs desktop session duration
- Touch target success rate

---

## Maintenance Considerations

### Documentation
- Update API documentation for new endpoints
- Add user guides for new features
- Document mobile-specific behaviors

### Testing
- Add E2E tests for each new feature
- Performance regression tests
- Accessibility audit automation

### Monitoring
- Track feature usage with analytics
- Monitor performance metrics
- Log errors and user feedback

---

## Conclusion

This plan provides a roadmap for 8+ weeks of enhancements to the tactical planner. Each section includes detailed technical specifications, testing strategies, and timelines.

**Recommended Approach**:
1. Start with Phase 1 (Quick Wins) to deliver immediate value
2. Gather user feedback to prioritize Phase 2 and 3 features
3. Iterate based on usage metrics and user requests

The plan is modular - each feature can be implemented independently based on priority and resources available.

**Status**: Ready for implementation
**Last Updated**: December 3, 2025
