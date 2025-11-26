## Implementation Plan: Crafting Recipes & Workbench Upgrades

### 1. Understanding the Goal

The objective is to allow users to define and track two types of item compositions:
1.  **Crafting Recipes**: Repeatable creations (e.g., "Zipline" = 1 Rope + 1 Mechanical Component).
2.  **Workbench Upgrades**: One-time upgrades (e.g., "Scrappy Level 2" = 1 Dog Collar).

Furthermore, users can track these in the Planner in two modes:
1.  **Priority**: Ingredients are added to the route calculation, directly influencing the path.
2.  **Ongoing**: Ingredients *do not* influence the path. Instead, if the generated route passes through an area containing these items, the UI notifies the user (e.g., "Bonus: Batteries available here").

### 2. Analysis

**Files Examined:**
-   `src/main/java/com/pauloneill/arcraidersplanner/model/Item.java`: Base entity.
-   `src/main/java/com/pauloneill/arcraidersplanner/dto/PlannerRequestDto.java`: Needs new field for 'ongoing' items.
-   `src/main/java/com/pauloneill/arcraidersplanner/dto/AreaDto.java`: Needs field to return matched 'ongoing' items.
-   `frontend/src/Sidebar.tsx`: Needs tri-state selection logic.

### 3. Methodology

**Test Driven Development (TDD)**
We will strictly adhere to TDD principles throughout the implementation:
1.  **Red**: Write a failing test case (e.g., for the new `PlannerService` logic or `RecipeController` endpoints).
2.  **Green**: Implement the minimal code necessary to pass the test.
3.  **Refactor**: Improve code quality while ensuring tests remain green.

This approach will ensure robust handling of the new "Ongoing" items logic and the Recipe CRUD operations.

### 4. Proposed Implementation Strategy

#### Backend (Spring Boot)

1.  **Database Migration (`V8__Create_Recipe_Schema.sql`)**:
    -   `recipes`: `id`, `name` (Unique), `description`, `type` ('CRAFTING', 'UPGRADE').
    -   `recipe_ingredients`: `id`, `recipe_id` (FK), `item_id` (FK), `quantity`.

2.  **Entities**:
    -   `Recipe`: JPA Entity with `RecipeType` enum.
    -   `RecipeIngredient`: JPA Entity.

3.  **DTO Updates**:
    -   `PlannerRequestDto`: Add `List<String> ongoingItemNames`.
    -   `AreaDto`: Add `List<String> ongoingMatchItems` (Items from the ongoing list found in this area).

4.  **Service Logic (`PlannerService`)**:
    -   Resolve `ongoingItemNames` to their `LootType`s.
    -   During route generation (after selecting areas), iterate through the chosen `Area`s.
    -   If an Area's `lootTypes` match an Ongoing Item's `LootType`, add that item name to the Area's `ongoingMatchItems` list.

5.  **Recipe Controller**:
    -   CRUD endpoints for Recipes (`/api/recipes`).

#### Frontend (React)

1.  **Recipe Management**:
    -   **`RecipeEditor.tsx`**: Page to create/edit Recipes and Upgrades.
    -   **`api/recipeApi.ts`**: API client.

2.  **Planner UI (`Sidebar.tsx`)**:
    -   **Recipe Selector**: List all Recipes/Upgrades.
    -   **Tri-State Toggle**:
        -   [ ] Unselected
        -   [!] Priority (Green/Bold) -> Adds to `targetItemNames`
        -   [~] Ongoing (Blue/Italic) -> Adds to `ongoingItemNames`

3.  **Route Visualization (`MapComponent.tsx` / `Planner.tsx`)**:
    -   When displaying the route steps (Areas), check for `ongoingMatchItems`.
    -   **Visual Cue**: Add a badge or icon (e.g., "Blue Loot Bag") next to the Area name if it contains Ongoing items.
    -   **Tooltip/Details**: "Also look for: Battery (Ongoing)"

### 5. Testing Strategy

-   **Backend Unit**:
    -   `RecipeServiceTest`: CRUD logic.
    -   `PlannerServiceTest`: Verify `ongoingMatchItems` are populated correctly when an area matches the loot type.
-   **Frontend**:
    -   Test Recipe Editor creation.
    -   Test Planner sends correct payload for Priority vs Ongoing.
    -   Test Map displays notifications.

### 6. Risks & Considerations

-   **Performance**: Resolving loot types for ongoing items adds a small overhead. Caching or map-based lookups should be used if the list is large (unlikely).
-   **Complexity**: UI for tri-state selection needs to be intuitive.

**AWAITING YOUR APPROVAL TO PROCEED TO IMPLEMENTATION**
