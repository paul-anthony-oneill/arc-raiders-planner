## Implementation Plan: Frontend Updates for Enemy Drop Routing

### 1. Understanding
The backend now returns a route that can include both `Area`s (loot zones) and `MapMarker`s (enemy spawn points) as waypoints. The `PlannerResponseDto` path is now a list of `WaypointDto`s. The frontend needs to handle this mixed list to visualize the route correctly.

### 2. Analysis

**Files Examined:**
- `frontend/src/types.ts`: Need to update `Area` interface to `Waypoint` and adjust `PlannerResponse`.
- `frontend/src/MapComponent.tsx`: Likely iterates over `routePath` to draw lines. Needs to use `x/y` instead of `mapX/mapY`.
- `frontend/src/Planner.tsx`: May need updates if it displays the route list textually.

**Data Structure Changes:**
- New `Waypoint` interface:
    ```typescript
    export interface Waypoint {
      id: string; // Changed from number to string to accommodate UUIDs
      name: string;
      x: number;
      y: number;
      type: "AREA" | "MARKER";
      lootTypes?: string[]; // Optional (AREA only)
      lootAbundance?: number; // Optional (AREA only)
      ongoingMatchItems?: string[];
      targetMatchItems?: string[];
    }
    ```
- Update `PlannerResponse` to use `path: Waypoint[]` instead of `routePath: Area[]`.

### 3. Proposed Changes

1.  **Update `types.ts`**:
    -   Rename `Area` to `Waypoint` (or keep `Area` for backward compat if needed, but better to refactor).
    -   Add `x`, `y`, `type` to `Waypoint`.
    -   Update `PlannerResponse` to use `path`.

2.  **Update `MapComponent.tsx`**:
    -   Update loop rendering the route polyline to use `.x` and `.y`.
    -   Update marker rendering:
        -   Existing Area markers (circles/polygons) should still render for Waypoints of type "AREA".
        -   New Marker waypoints (type "MARKER") should render distinct icons (e.g., a skull or target icon) to differentiate them from loot zones.

3.  **Update `Planner.tsx`**:
    -   Update references from `.routePath` to `.path`.
    -   Update UI list generation to handle Waypoints (e.g., showing "Target: Sentinel" instead of just area name).

### 4. Verification
-   **Manual Test**: Run frontend, search for an item dropped by an enemy (e.g., "Looting Mk 3" or a known drop).
-   **Visual Check**: Confirm route connects loot areas AND the enemy spawn point on the map. Confirm the enemy spawn point has a distinct visual style.

**AWAITING YOUR APPROVAL TO PROCEED TO FRONTEND IMPLEMENTATION**