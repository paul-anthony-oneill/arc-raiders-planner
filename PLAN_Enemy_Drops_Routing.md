## Implementation Plan: Integrate Enemy Drops into Route Planning (Revised)

### 1. Understanding
The goal is to update the route planner to consider items dropped by enemies.
- **Hybrid Items** (Loot Areas + Enemy Drop): Treat enemies as "Opportunity Targets". Route primarily through loot areas, but give score bonuses for passing near these enemies (existing behavior).
- **Exclusive Items** (Enemy Drop Only): Treat enemies as "Primary Destinations". The route must explicitly visit these spawn points.

### 2. Analysis

**Files Examined:**
- `src/main/java/com/pauloneill/arcraidersplanner/model/Item.java`
- `src/main/java/com/pauloneill/arcraidersplanner/service/PlannerService.java`
- `src/main/java/com/pauloneill/arcraidersplanner/model/Area.java`
- `src/main/java/com/pauloneill/arcraidersplanner/model/MapMarker.java`

**Architectural Change:**
The current routing algorithm (`findOptimalRoute`) is tightly coupled to the `Area` entity. To allow routing to `MapMarker`s (enemies) without creating "fake" Area objects, we will introduce a `RoutablePoint` interface.

### 3. Proposed Changes

#### Backend - Data Model
1.  **Item Entity & DTO**:
    -   Add `@ElementCollection private Set<String> droppedBy` to `Item`.
    -   Update `MetaforgeItemDto` to parse this field.
    -   Update `MetaforgeSyncService` to populate it.

2.  **Routable Abstraction**:
    -   Create interface `RoutablePoint` with methods: `String getId()`, `double getX()`, `double getY()`.
    -   `Area` implements `RoutablePoint` (using `mapX`, `mapY`).
    -   `MapMarker` implements `RoutablePoint` (using `lng`, `lat`).

#### Backend - Planner Logic (`PlannerService.java`)

1.  **Refactor Routing Algorithm**:
    -   Change `findOptimalRoute(List<Area>)` to `findOptimalRoute(List<? extends RoutablePoint>)`.
    -   Update distance calculations to use the interface methods.

2.  **Target Resolution (The "Hybrid" Logic)**:
    -   When a user requests an item:
        -   **Check Sources**: Does it have `loot_type`? Does it have `dropped_by`?
        -   **Case A (Loot + Drop)**: Add `Area`s to routing list. Add Enemy Type to `targetEnemyTypes` (for proximity scoring).
        -   **Case B (Drop Only)**: Fetch `MapMarker`s for that enemy. Add them directly to the routing list as `RoutablePoint`s.
        -   **Case C (Loot Only)**: Add `Area`s to routing list (existing behavior).

3.  **Route Construction**:
    -   The resulting route will be a mixed list of `Area` and `MapMarker` objects.
    -   Update `PlannerResponseDto` mapping to handle this mix (e.g., a generic "Waypoint" in the response or adapting Markers to the existing structure).

#### Database
-   `V009__add_item_dropped_by.sql`: Create table for `Item.droppedBy`.

### 4. Testing
-   **Unit**: Verify `PlannerService` correctly distinguishes Case A (Hybrid) vs Case B (Exclusive).
-   **Unit**: Verify `findOptimalRoute` handles mixed lists of Areas and Markers.

### 5. Risks
-   **Coordinate Systems**: `Area` uses integer `mapX`, `MapMarker` uses double `lng`. The `RoutablePoint` interface will standardize on `double` to maintain precision for markers.

**AWAITING YOUR APPROVAL TO PROCEED TO IMPLEMENTATION**