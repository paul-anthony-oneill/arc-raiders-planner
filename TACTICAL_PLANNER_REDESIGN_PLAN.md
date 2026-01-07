# Tactical Planner UI Redesign - Technical Implementation Plan

## Executive Summary

**Goal**: Transform the current dual-page architecture (Setup → Planner) into a unified tactical planning interface with three states: Selection, Planning, and Back-to-Selection.

**Key Changes**:
- Single page with left (objectives), center (detail), right (map) layout
- Map minimizes during selection, expands during planning
- Immediate visual feedback (zone highlights) + deferred route calculation
- Enhanced item detail view with crafting/usage/drop information

**Timeline**: 4 weeks for full implementation + testing

---

## Design Decisions from Brainstorming Session

### Three-State UI Model

1. **SELECTION Mode** (Default)
   - Left panel: Objective list with category filters
   - Center panel: Selected item detail view
   - Right panel: Minimized map with zone highlights
   - FAB: "Calculate Route" button (bottom-right)

2. **PLANNING Mode** (After calculation)
   - Left/Center panels: Collapse/hide
   - Right panel: Expands to full width
   - Map: Shows calculated route (similar to current PlannerPage)
   - Control: "Minimize" button to return to selection

3. **Transition Behavior**
   - Selecting objectives → highlights zones immediately (no route calc)
   - Switching maps → clears route, keeps selections, re-highlights zones
   - Calculate Route → transitions to PLANNING mode
   - Minimize → clears route, keeps selections, returns to SELECTION mode

### User Interaction Model

**Selection Flow**:
```
1. User selects item from left panel
   → Item added to selection queue
   → Center panel shows item details
   → Map highlights zones containing item

2. User switches maps (dropdown)
   → Previous route cleared
   → Selections persist
   → Zones re-highlighted for new map

3. User clicks "Calculate Route" FAB
   → API call to generate route
   → Map expands (PLANNING mode)
   → Route visualized

4. User clicks "Minimize"
   → Route cleared
   → Map shrinks (SELECTION mode)
   → Selections still active
```

### Design System

- **Layout**: Reference image structure, NOT styling
- **Theming**: Keep existing color scheme and aesthetics
- **Typography**: Maintain current fonts (no monospace changes)
- **Spacing**: Use Tailwind spacing scale (existing pattern)

---

## Phase 1: Backend Enhancements

### 1.1 Data Availability ✅

**Status**: All required data already exists!

| Feature | Data Source | Status |
|---------|-------------|--------|
| Crafting recipes | `Recipe` entity + `/api/recipes` | ✅ Available |
| Recycling info | `Recipe.isRecyclable` flag | ✅ Available |
| Enemy drops | `Item.droppedBy` set | ✅ Available |
| Recipe ingredients | `RecipeIngredient` entity | ✅ Available |
| Workbench upgrades | `RecipeType.WORKBENCH_UPGRADE` | ✅ Available |

### 1.2 Backend Tasks

#### Task 1.1: Extend ItemDto for Detail Panel

**File**: `src/main/java/com/pauloneill/arcraidersplanner/dto/ItemDto.java`

**Changes**:
```java
@Data
public class ItemDto {
    // Existing fields...
    private Long id;
    private String name;
    private String description;
    private String rarity;
    private String itemType;
    private String lootType;
    private String iconUrl;
    private Integer value;
    private Double weight;
    private Integer stackSize;

    // NEW: Detail panel fields
    private Set<String> droppedBy;           // Enemy IDs
    private List<RecipeDto> usedInRecipes;   // Recipes using this item
    private RecipeDto craftingRecipe;        // How to craft this item
}
```

**Why**: Current ItemDto doesn't include crafting/usage context needed for center panel.

---

#### Task 1.2: Create Enhanced ItemService

**File**: `src/main/java/com/pauloneill/arcraidersplanner/service/ItemService.java`

**New Service**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final RecipeRepository recipeRepository;
    private final DtoMapper dtoMapper;

    /**
     * Get item with full crafting/usage context.
     * WHY: Provides all data needed for center panel detail view
     *
     * @param itemId The item ID
     * @return ItemDto with crafting recipe, usage recipes, and drop sources
     */
    public ItemDto getItemWithContext(Long itemId) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(itemId));

        ItemDto dto = dtoMapper.toDto(item);

        // Add droppedBy (already in Item entity)
        dto.setDroppedBy(item.getDroppedBy());

        // Find crafting recipe for this item
        recipeRepository.findByMetaforgeItemId(item.getMetaforgeId())
            .ifPresent(recipe -> dto.setCraftingRecipe(dtoMapper.toDto(recipe)));

        // Find recipes that use this item as ingredient
        List<Recipe> usageRecipes = recipeRepository.findRecipesUsingItem(itemId);
        dto.setUsedInRecipes(dtoMapper.toRecipeDtos(usageRecipes));

        return dto;
    }
}
```

**Why**: Centralizes item detail logic, prevents N+1 queries, provides complete data for UI.

---

#### Task 1.3: Add Reverse Recipe Lookup

**File**: `src/main/java/com/pauloneill/arcraidersplanner/repository/RecipeRepository.java`

**New Query**:
```java
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // Existing methods...
    Optional<Recipe> findByName(String name);
    Optional<Recipe> findByMetaforgeItemId(String metaforgeItemId);

    /**
     * Find all recipes that use a specific item as ingredient.
     * WHY: Required for "Used In Recipes" section of item detail view
     *
     * @param itemId The item ID to search for
     * @return List of recipes using this item
     */
    @Query("SELECT r FROM Recipe r JOIN r.ingredients i WHERE i.item.id = :itemId")
    List<Recipe> findRecipesUsingItem(@Param("itemId") Long itemId);
}
```

**Why**: Enables reverse lookup for "what can I craft with this item?" queries.

---

#### Task 1.4: Create ItemController Endpoint

**File**: `src/main/java/com/pauloneill/arcraidersplanner/controller/ItemController.java`

**New Endpoint**:
```java
@GetMapping("/items/{id}/details")
@Transactional(readOnly = true)
@Operation(summary = "Get item with full crafting/usage context")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Item details retrieved"),
    @ApiResponse(responseCode = "404", description = "Item not found")
})
public ItemDto getItemDetails(@PathVariable Long id) {
    return itemService.getItemWithContext(id);
}
```

**Why**: Exposes enhanced item data to frontend.

---

## Phase 2: Frontend Component Architecture

### 2.1 Component Hierarchy

```
TacticalPlannerPage (NEW - replaces SetupPage + PlannerPage)
├── LeftPanel (Objective List)
│   ├── CategoryFilter (Tabs: ALL | ITEMS | ENEMIES | CONTAINERS | RECIPES)
│   ├── SearchBar (Filter within category)
│   └── ObjectiveList
│       └── TargetCard[] (REUSED from current SetupPage)
│
├── CenterPanel (Detail View - NEW)
│   ├── ItemDetailView
│   │   ├── ItemHeader (name, rarity, icon, value)
│   │   ├── CraftingSection (CollapsibleSection)
│   │   │   ├── RecipeCard (how to craft)
│   │   │   └── IngredientList
│   │   ├── UsageSection (CollapsibleSection)
│   │   │   └── RecipeList (recipes using this item)
│   │   └── DropSourceSection (CollapsibleSection)
│   │       └── EnemyList (enemies that drop this)
│   │
│   ├── EnemyDetailView (similar structure)
│   ├── ContainerDetailView
│   └── RecipeDetailView
│
├── RightPanel (Map)
│   ├── MinimizedMapView (SELECTION mode)
│   │   ├── MapSwitcher (dropdown)
│   │   ├── MapComponent (zone highlights only, no route)
│   │   └── CalculateRouteFAB (bottom-right)
│   │
│   └── MaximizedMapView (PLANNING mode)
│       ├── MapComponent (REUSED from current PlannerPage)
│       ├── RouteOverlay
│       ├── DataHUD (REUSED)
│       └── MinimizeButton (top-right)
│
└── SessionStateProvider (REUSED from useTargetSelection hook)
```

### 2.2 Layout Specifications

**Desktop (≥1024px)**:
```
┌────────────┬──────────────┬──────────────────────┐
│   Left     │   Center     │       Right          │
│  (300px)   │   (flex-1)   │      (flex-2)        │
│            │              │                      │
│ Objectives │ Item Details │  Map (minimized)     │
│            │              │  or                  │
│            │              │  Route (maximized)   │
└────────────┴──────────────┴──────────────────────┘

PLANNING mode (map maximized):
┌────────────┬─────────────────────────────────────┐
│   Left     │          Right (expanded)           │
│  (300px)   │           (flex-1)                  │
│            │                                     │
│ Objectives │       Route Visualization           │
│            │                                     │
└────────────┴─────────────────────────────────────┘
```

**Mobile (≤768px)**:
- Stack vertically: Left → Center → Right
- Map minimized by default
- Expand map to fullscreen on calculate

---

### 2.3 Frontend Tasks

#### Task 2.1: Create TacticalPlannerPage Shell

**File**: `frontend/src/pages/TacticalPlannerPage.tsx`

**Component Structure**:
```tsx
export const TacticalPlannerPage: React.FC = () => {
  // State from useTargetSelection hook
  const {
    priorityTargets,
    ongoingTargets,
    routingProfile,
    hasRaiderKey,
    addPriorityTarget,
    // ... other methods
  } = useTargetSelection();

  // UI state (local to page)
  const [uiMode, setUiMode] = useState<'SELECTION' | 'PLANNING'>('SELECTION');
  const [selectedMap, setSelectedMap] = useState<string>('Dam Battlegrounds');
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);
  const [highlightedZones, setHighlightedZones] = useState<Area[]>([]);
  const [calculatedRoute, setCalculatedRoute] = useState<PlannerResponse | null>(null);

  // Handlers
  const handleItemSelect = useCallback(async (item: Item) => {
    setSelectedItem(item);

    // Fetch item details for center panel
    const details = await itemApi.getItemDetails(item.id);
    setSelectedItem(details);

    // Highlight zones on current map
    const zones = await mapApi.getZonesWithItem(selectedMap, item.name);
    setHighlightedZones(zones);
  }, [selectedMap]);

  const handleCalculateRoute = useCallback(async () => {
    const request = buildPlannerRequest(
      priorityTargets,
      ongoingTargets,
      selectedMap,
      routingProfile,
      hasRaiderKey
    );

    const route = await plannerApi.generateRoute(request);
    setCalculatedRoute(route);
    setUiMode('PLANNING');
  }, [priorityTargets, ongoingTargets, selectedMap, routingProfile, hasRaiderKey]);

  const handleMinimize = useCallback(() => {
    setCalculatedRoute(null);
    setUiMode('SELECTION');
  }, []);

  const handleMapChange = useCallback(async (newMap: string) => {
    setSelectedMap(newMap);
    setCalculatedRoute(null); // Clear old route

    // Re-highlight zones for selected item on new map
    if (selectedItem) {
      const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name);
      setHighlightedZones(zones);
    }
  }, [selectedItem]);

  return (
    <div className="h-screen grid grid-cols-[300px_1fr_2fr] gap-4 p-4">
      {/* Left Panel - Always visible */}
      <LeftPanel onItemSelect={handleItemSelect} />

      {/* Center Panel - Hidden in PLANNING mode */}
      {uiMode === 'SELECTION' && (
        <CenterPanel item={selectedItem} />
      )}

      {/* Right Panel - Expands in PLANNING mode */}
      <div className={clsx(
        'transition-all duration-300',
        uiMode === 'PLANNING' && 'col-span-2'
      )}>
        {uiMode === 'SELECTION' ? (
          <MinimizedMapView
            selectedMap={selectedMap}
            onMapChange={handleMapChange}
            highlightedZones={highlightedZones}
            onCalculateRoute={handleCalculateRoute}
          />
        ) : (
          <MaximizedMapView
            route={calculatedRoute}
            onMinimize={handleMinimize}
          />
        )}
      </div>
    </div>
  );
};
```

**Why**: Central orchestration of three-state UI model.

---

#### Task 2.2: Create CenterPanel Components

**File**: `frontend/src/components/CenterPanel/ItemDetailView.tsx`

```tsx
interface ItemDetailViewProps {
  item: Item & {
    droppedBy?: string[];
    usedInRecipes?: Recipe[];
    craftingRecipe?: Recipe;
  };
}

export const ItemDetailView: React.FC<ItemDetailViewProps> = ({ item }) => {
  if (!item) {
    return (
      <div className="flex items-center justify-center h-full text-gray-400">
        Select an item to view details
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full overflow-y-auto bg-gray-800 rounded-lg p-6">
      {/* Header */}
      <div className="flex items-start gap-4 mb-6">
        <img
          src={item.iconUrl}
          alt={item.name}
          className="w-16 h-16 rounded"
        />
        <div className="flex-1">
          <h2 className="text-2xl font-bold text-white">{item.name}</h2>
          <div className="flex items-center gap-2 mt-1">
            <span className={`px-2 py-1 rounded text-sm ${getRarityColor(item.rarity)}`}>
              {item.rarity}
            </span>
            <span className="text-gray-400">{item.itemType}</span>
          </div>
          <p className="text-sm text-gray-300 mt-2">{item.description}</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6 p-4 bg-gray-700 rounded">
        <div>
          <div className="text-xs text-gray-400">Value</div>
          <div className="text-lg font-semibold text-white">{item.value}</div>
        </div>
        <div>
          <div className="text-xs text-gray-400">Weight</div>
          <div className="text-lg font-semibold text-white">{item.weight}kg</div>
        </div>
        <div>
          <div className="text-xs text-gray-400">Stack Size</div>
          <div className="text-lg font-semibold text-white">{item.stackSize}</div>
        </div>
      </div>

      {/* Crafting Recipe (PRIMARY FOCUS per user feedback) */}
      {item.craftingRecipe && (
        <CollapsibleSection title="Crafting Recipe" defaultOpen>
          <RecipeCard recipe={item.craftingRecipe} />
        </CollapsibleSection>
      )}

      {/* Used In Recipes */}
      {item.usedInRecipes && item.usedInRecipes.length > 0 && (
        <CollapsibleSection title="Used In Recipes" defaultOpen>
          <div className="space-y-2">
            {item.usedInRecipes.map(recipe => (
              <RecipeCard key={recipe.id} recipe={recipe} compact />
            ))}
          </div>
        </CollapsibleSection>
      )}

      {/* Dropped By */}
      {item.droppedBy && item.droppedBy.length > 0 && (
        <CollapsibleSection title="Dropped By">
          <div className="space-y-2">
            {item.droppedBy.map(enemyId => (
              <EnemyCard key={enemyId} enemyId={enemyId} />
            ))}
          </div>
        </CollapsibleSection>
      )}
    </div>
  );
};
```

**File**: `frontend/src/components/CenterPanel/RecipeCard.tsx`

```tsx
interface RecipeCardProps {
  recipe: Recipe;
  compact?: boolean;
}

export const RecipeCard: React.FC<RecipeCardProps> = ({ recipe, compact }) => {
  return (
    <div className="bg-gray-700 rounded-lg p-4">
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-semibold text-white">{recipe.name}</h4>
        <span className="px-2 py-1 bg-gray-600 rounded text-xs text-gray-300">
          {recipe.type}
        </span>
      </div>

      {!compact && (
        <p className="text-sm text-gray-400 mb-3">{recipe.description}</p>
      )}

      <div className="space-y-2">
        <div className="text-xs text-gray-400 uppercase">Ingredients</div>
        {recipe.ingredients.map(ingredient => (
          <div key={ingredient.itemId} className="flex items-center justify-between text-sm">
            <span className="text-gray-300">{ingredient.itemName}</span>
            <span className="text-white font-semibold">×{ingredient.quantity}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
```

**Why**: Focuses on crafting chains (user priority), reuses CollapsibleSection component.

---

#### Task 2.3: Create MinimizedMapView Component

**File**: `frontend/src/components/Map/MinimizedMapView.tsx`

```tsx
interface MinimizedMapViewProps {
  selectedMap: string;
  onMapChange: (mapName: string) => void;
  highlightedZones: Area[];
  onCalculateRoute: () => void;
  hasTargets: boolean; // Enable FAB only if targets selected
}

export const MinimizedMapView: React.FC<MinimizedMapViewProps> = ({
  selectedMap,
  onMapChange,
  highlightedZones,
  onCalculateRoute,
  hasTargets,
}) => {
  return (
    <div className="relative h-full bg-gray-800 rounded-lg overflow-hidden">
      {/* Map Switcher */}
      <div className="absolute top-4 left-4 z-10">
        <select
          value={selectedMap}
          onChange={(e) => onMapChange(e.target.value)}
          className="bg-gray-700 text-white px-4 py-2 rounded-lg border border-gray-600"
        >
          <option value="Dam Battlegrounds">Dam Battlegrounds</option>
          <option value="Ironwood Hydroponics">Ironwood Hydroponics</option>
          <option value="Scrapworks">Scrapworks</option>
          {/* Add other maps */}
        </select>
      </div>

      {/* Map Visualization */}
      <MapComponent
        mapName={selectedMap}
        highlightedAreas={highlightedZones}
        showRoute={false}
        interactive={false}
        className="h-full"
      />

      {/* Calculate Route FAB */}
      <button
        onClick={onCalculateRoute}
        disabled={!hasTargets}
        className={clsx(
          'absolute bottom-6 right-6 z-20',
          'px-6 py-3 rounded-full font-semibold',
          'shadow-lg transition-all duration-200',
          hasTargets
            ? 'bg-orange-500 hover:bg-orange-600 text-white cursor-pointer'
            : 'bg-gray-600 text-gray-400 cursor-not-allowed'
        )}
      >
        Calculate Route
      </button>
    </div>
  );
};
```

**Why**: Encapsulates minimized map state, FAB positioned as per reference image.

---

#### Task 2.4: Create MaximizedMapView Component

**File**: `frontend/src/components/Map/MaximizedMapView.tsx`

```tsx
interface MaximizedMapViewProps {
  route: PlannerResponse | null;
  onMinimize: () => void;
}

export const MaximizedMapView: React.FC<MaximizedMapViewProps> = ({
  route,
  onMinimize,
}) => {
  if (!route) {
    return <div className="flex items-center justify-center h-full">Loading route...</div>;
  }

  return (
    <div className="relative h-full bg-gray-800 rounded-lg overflow-hidden">
      {/* Minimize Button */}
      <button
        onClick={onMinimize}
        className="absolute top-4 right-4 z-20 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg"
      >
        ✕ Minimize
      </button>

      {/* DataHUD - Reused from current PlannerPage */}
      <div className="absolute top-4 left-4 z-10">
        <DataHUD route={route} />
      </div>

      {/* Map with Route - Reused MapComponent */}
      <MapComponent
        mapName={route.mapName}
        route={route.path}
        enemySpawns={route.nearbyEnemySpawns}
        extractionPoint={route.extractionPoint}
        showRoute={true}
        interactive={true}
        className="h-full"
      />
    </div>
  );
};
```

**Why**: Reuses existing PlannerPage components, minimal new code.

---

## Phase 3: State Management

### 3.1 Extend useTargetSelection Hook

**File**: `frontend/src/hooks/useTargetSelection.ts`

**New State & Methods**:
```typescript
export const useTargetSelection = () => {
  // Existing session state (preserved)
  const [priorityTargets, setPriorityTargets] = useState<TargetSelection[]>([]);
  const [ongoingTargets, setOngoingTargets] = useState<TargetSelection[]>([]);
  // ... existing methods

  // NEW: UI state for three-state system
  const [uiMode, setUiMode] = useState<'SELECTION' | 'PLANNING'>('SELECTION');
  const [selectedMap, setSelectedMap] = useState<string>('Dam Battlegrounds');
  const [selectedItem, setSelectedItem] = useState<Item | null>(null);
  const [highlightedZones, setHighlightedZones] = useState<Area[]>([]);
  const [calculatedRoute, setCalculatedRoute] = useState<PlannerResponse | null>(null);

  /**
   * Calculate route and transition to planning mode
   * WHY: Encapsulates state transition logic
   */
  const calculateRoute = useCallback(async () => {
    const request: PlannerRequest = {
      targetItemNames: priorityTargets
        .filter(t => t.type === 'ITEM')
        .map(t => t.name),
      targetEnemyTypes: priorityTargets
        .filter(t => t.type === 'ENEMY')
        .map(t => t.name),
      targetRecipeIds: priorityTargets
        .filter(t => t.type === 'RECIPE')
        .map(t => String(t.id)),
      targetContainerTypes: priorityTargets
        .filter(t => t.type === 'CONTAINER')
        .map(t => t.name),
      ongoingItemNames: ongoingTargets
        .filter(t => t.type === 'ITEM')
        .map(t => t.name),
      routingProfile,
      hasRaiderKey,
    };

    const route = await plannerApi.generateRoute(request);
    setCalculatedRoute(route);
    setUiMode('PLANNING');
  }, [priorityTargets, ongoingTargets, routingProfile, hasRaiderKey]);

  /**
   * Minimize map - clear route, keep selections
   * WHY: Implements "cancel planning" behavior
   */
  const minimizeMap = useCallback(() => {
    setCalculatedRoute(null);
    setUiMode('SELECTION');
  }, []);

  /**
   * Handle item selection with zone highlighting
   * WHY: Immediate visual feedback without route calculation
   */
  const selectItem = useCallback(async (item: Item) => {
    setSelectedItem(item);

    // Fetch enhanced item details
    const details = await itemApi.getItemDetails(item.id);
    setSelectedItem(details);

    // Highlight zones on current map
    const zones = await mapApi.getZonesWithItem(selectedMap, item.name);
    setHighlightedZones(zones);
  }, [selectedMap]);

  /**
   * Switch maps - clear route, keep selections, re-highlight
   * WHY: Preserves user selections across map changes
   */
  const changeMap = useCallback(async (newMap: string) => {
    setSelectedMap(newMap);
    setCalculatedRoute(null); // Clear old route

    // Re-highlight zones for selected item on new map
    if (selectedItem) {
      const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name);
      setHighlightedZones(zones);
    }
  }, [selectedItem]);

  return {
    // Existing session state methods
    priorityTargets,
    ongoingTargets,
    addPriorityTarget,
    removePriorityTarget,
    // ... other existing methods

    // NEW: UI state & methods
    uiMode,
    selectedMap,
    selectedItem,
    highlightedZones,
    calculatedRoute,
    calculateRoute,
    minimizeMap,
    selectItem,
    changeMap,
  };
};
```

**Why**: Centralized state management, clear separation between session data and UI state.

---

### 3.2 Create mapApi Helper

**File**: `frontend/src/api/mapApi.ts`

```typescript
export const mapApi = {
  /**
   * Get all zones on a map that contain a specific item
   * WHY: Enables zone highlighting without full route calculation
   */
  async getZonesWithItem(mapName: string, itemName: string): Promise<Area[]> {
    const response = await fetch(
      `/api/maps/${encodeURIComponent(mapName)}/zones?item=${encodeURIComponent(itemName)}`
    );
    if (!response.ok) throw new Error('Failed to fetch zones');
    return response.json();
  },

  async getAllMaps(): Promise<GameMap[]> {
    const response = await fetch('/api/maps');
    if (!response.ok) throw new Error('Failed to fetch maps');
    return response.json();
  },
};
```

**Why**: Abstracts API calls, enables zone highlighting feature.

---

## Phase 4: Routing & Layout

### 4.1 Update TanStack Router

**File**: `frontend/src/Router.tsx`

**Changes**:
```tsx
import { TacticalPlannerPage } from './pages/TacticalPlannerPage';

const rootRoute = createRootRoute();

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => {
    const navigate = useNavigate();
    useEffect(() => {
      navigate({ to: '/planner' });
    }, []);
    return null;
  },
});

const plannerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/planner',
  component: TacticalPlannerPage,
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  plannerRoute,
]);

// REMOVED: Old /setup and /planner routes
```

**Why**: Consolidates to single route, removes dual-page architecture.

---

### 4.2 Responsive Layout System

**File**: `frontend/src/pages/TacticalPlannerPage.tsx`

**Layout Classes**:
```tsx
<div className={clsx(
  'h-screen grid gap-4 p-4',
  // Desktop: 3-column grid
  'lg:grid-cols-[300px_1fr_2fr]',
  // Tablet: 2-column (left + right, center hidden)
  'md:grid-cols-[250px_1fr]',
  // Mobile: 1-column stack
  'grid-cols-1'
)}>
  {/* Left Panel */}
  <div className="overflow-y-auto">
    <LeftPanel />
  </div>

  {/* Center Panel - Hidden on tablet/mobile */}
  {uiMode === 'SELECTION' && (
    <div className="hidden lg:block overflow-y-auto">
      <CenterPanel />
    </div>
  )}

  {/* Right Panel */}
  <div className={clsx(
    'overflow-hidden',
    uiMode === 'PLANNING' && 'lg:col-span-2' // Expand to cover center
  )}>
    {/* Map component */}
  </div>
</div>
```

**Mobile Adaptations**:
- Left panel: Collapsible drawer
- Center panel: Modal overlay on item tap
- Map: Fullscreen when calculating route

**Why**: Mobile-first Tailwind approach, maintains usability across devices.

---

## Phase 5: Migration & Cleanup

### 5.1 Component Reuse Matrix

| Component | Status | Usage |
|-----------|--------|-------|
| TargetCard | ✅ Reuse | Left panel objective list |
| CollapsibleSection | ✅ Reuse | Center panel sections |
| MapComponent | ✅ Reuse | Both minimized/maximized states |
| DataHUD | ✅ Reuse | Planning mode stats |
| useTargetSelection | ✅ Extend | Add UI state management |
| ShoppingCart | ❌ Remove | Replaced by left panel badges |
| SetupPage | ❌ Remove | Merged into TacticalPlannerPage |
| PlannerPage | ❌ Remove | Merged into TacticalPlannerPage |

### 5.2 File Removal Checklist

```bash
# Files to delete after migration:
- frontend/src/pages/SetupPage.tsx
- frontend/src/pages/PlannerPage.tsx (rename to TacticalPlannerPage.tsx)
- frontend/src/components/ShoppingCart.tsx (if not needed)
```

### 5.3 Database Migrations

**No database changes required!** All data already exists in current schema.

---

## Testing Strategy

### Unit Tests

**Backend**:
```java
// ItemServiceTest.java
@Test
void getItemWithContext_shouldIncludeCraftingRecipe() {
    // Test that crafting recipe is populated
}

@Test
void getItemWithContext_shouldIncludeUsageRecipes() {
    // Test reverse recipe lookup
}
```

**Frontend**:
```typescript
// ItemDetailView.test.tsx
describe('ItemDetailView', () => {
  it('should display crafting recipe when available', () => {
    // Test rendering
  });

  it('should handle missing crafting data gracefully', () => {
    // Test null states
  });
});
```

### Integration Tests

**E2E Flow**:
```typescript
// tactical-planner.spec.ts
test('should highlight zones when item selected', async ({ page }) => {
  await page.goto('/planner');
  await page.click('text=Rocket Thruster Core');

  // Verify zones highlighted
  await expect(page.locator('.zone-highlight')).toBeVisible();
});

test('should transition to planning mode on calculate', async ({ page }) => {
  // Select targets
  await page.click('text=Rocket Thruster Core');
  await page.click('button:has-text("Calculate Route")');

  // Verify map expanded
  await expect(page.locator('.maximized-map')).toBeVisible();
});

test('should preserve selections when minimizing', async ({ page }) => {
  // Calculate route
  await page.click('button:has-text("Calculate Route")');

  // Minimize
  await page.click('button:has-text("Minimize")');

  // Verify selections still present
  await expect(page.locator('.selected-target')).toHaveCount(1);
});
```

---

## Implementation Timeline

### Week 1: Backend Foundation
- [ ] Task 1.1: Extend ItemDto
- [ ] Task 1.2: Create ItemService
- [ ] Task 1.3: Add reverse recipe lookup
- [ ] Task 1.4: Create /items/{id}/details endpoint
- [ ] Test: Backend unit tests

### Week 2: Frontend Core UI
- [ ] Task 2.1: Create TacticalPlannerPage shell
- [ ] Task 2.2: Create CenterPanel components
- [ ] Task 2.3: Create MinimizedMapView
- [ ] Task 2.4: Create MaximizedMapView
- [ ] Test: Component unit tests

### Week 3: State & Integration
- [ ] Task 3.1: Extend useTargetSelection hook
- [ ] Task 3.2: Create mapApi helper
- [ ] Task 4.1: Update TanStack Router
- [ ] Task 4.2: Implement responsive layout
- [ ] Test: Integration tests

### Week 4: Migration & Polish
- [ ] Task 5.1: Remove old components
- [ ] Task 5.2: Clean up unused code
- [ ] E2E testing
- [ ] Performance optimization
- [ ] Documentation updates

---

## Risk Mitigation

### Potential Issues

1. **Zone Highlighting Performance**
   - **Risk**: Highlighting many zones could lag
   - **Mitigation**: Limit highlights to top 10 zones, add pagination

2. **State Synchronization**
   - **Risk**: Route/selection state desync
   - **Mitigation**: Single source of truth in useTargetSelection

3. **Mobile UX Complexity**
   - **Risk**: 3-panel layout doesn't scale well
   - **Mitigation**: Modal-based detail view on mobile

4. **API Latency**
   - **Risk**: Zone highlighting feels slow
   - **Mitigation**: Add loading skeletons, debounce requests

---

## Success Metrics

- **User Flows**:
  - ✅ Select item → see zones highlighted < 500ms
  - ✅ Switch maps → zones re-highlight < 500ms
  - ✅ Calculate route → transition to planning mode < 2s
  - ✅ Minimize → return to selection, keep targets

- **Code Quality**:
  - ✅ Backend test coverage > 80%
  - ✅ Frontend component tests for all new components
  - ✅ E2E tests for critical user flows
  - ✅ No console errors in production build

- **Performance**:
  - ✅ Lighthouse score > 90 (performance)
  - ✅ First Contentful Paint < 1.5s
  - ✅ Route calculation < 2s for typical loadout

---

## Appendix: API Endpoints Summary

### New Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/items/{id}/details` | Get item with crafting/usage context |
| GET | `/api/maps/{mapName}/zones?item={name}` | Get zones containing item (for highlights) |

### Existing Endpoints (Reused)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/recipes` | Get all recipes |
| POST | `/api/planner/generate-route` | Calculate optimized route |
| GET | `/api/items` | Search items |
| GET | `/api/enemies` | Search enemies |

---

## Next Steps

After reviewing this plan, we can:

1. **Proceed with implementation** - Start with Week 1 tasks (backend)
2. **Refine specific sections** - Deep dive into particular components
3. **Create prototypes** - Build interactive mockups for user testing
4. **Adjust timeline** - Modify based on team capacity

Please let me know which path you'd like to take!
