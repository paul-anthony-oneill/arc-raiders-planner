export interface LootType {
  id: number;
  name: string;
}

export interface Item {
  id: number;
  name: string;
  description: string;
  rarity: string;
  itemType: string;
  iconUrl: string;
  value: number;
  weight: number;
  stackSize: number;
  lootType: string | null; // Just the loot type name from backend

  // Detail panel fields (populated by /api/items/{id}/details endpoint)
  droppedBy?: string[];           // Enemy IDs that drop this item
  usedInRecipes?: Recipe[];   // Recipes that use this item as ingredient
  craftingRecipe?: Recipe;        // Recipe to craft this item (if craftable)
  hasRecipe?: boolean;            // Whether this item has a crafting recipe (for grid view)
}

export interface RecipeIngredientChain {
  itemId: number;
  itemName: string;
  quantity: number;
  isPrerequisite: boolean;
  recipeId: number | null;
}

export interface RecipeChain {
  itemId: number;
  itemName: string;
  recipeId: number;
  ingredients: RecipeIngredientChain[];
}

// Represents an enemy type for selection (not a specific spawn)
export type EnemyType = string; // e.g., "sentinel", "guardian"

// Represents a specific enemy spawn location on a map
export interface EnemySpawn {
  id: string; // UUID of spawn point
  type: string; // Enemy type (e.g., "sentinel")
  mapName: string;
  lat: number;
  lng: number;
  onRoute: boolean; // Whether this spawn is near the planned route
  distanceToRoute?: number; // Distance in units from nearest route point
  droppedItems?: string[]; // New field for items dropped by this enemy type
}

// Represents a targetable container type (e.g., "Red Locker")
export interface ContainerType {
  id: number;
  name: string;
  subcategory: string; // e.g., "red-locker"
  description: string;
  iconUrl?: string;
}

// Represents a clustered group of container markers
export interface MarkerGroup {
  id: number;
  name: string;
  mapId: number;
  mapName: string;
  containerType: ContainerType;
  centerLat: number;
  centerLng: number;
  markerCount: number;
  radius: number;
  markerIds: string[];
}

export interface MapRecommendation {
  mapId: number;
  mapName: string;
  matchingAreaCount: number; // The count used for ranking
}

export interface Waypoint {
  id: string; // Changed to string to support both IDs and UUIDs
  name: string;
  x: number;
  y: number;
  type: "AREA" | "MARKER" | "MARKER_GROUP";
  lootTypes?: string[];
  lootAbundance?: number;
  containerType?: string; // Only relevant for MARKER_GROUP type
  markerCount?: number;   // Only relevant for MARKER_GROUP type
  radius?: number;        // Only relevant for MARKER_GROUP type
  ongoingMatchItems?: string[];
  targetMatchItems?: string[];
}

export interface Area {
  id: number;
  name: string;
  mapX: number;
  mapY: number;
  coordinates?: string;
  lootTypes: string[];
  lootAbundance?: number;
  ongoingMatchItems?: string[];
  targetMatchItems?: string[];
}

export const RoutingProfile = {
  PURE_SCAVENGER: "PURE_SCAVENGER",
  EASY_EXFIL: "EASY_EXFIL",
  AVOID_PVP: "AVOID_PVP",
  SAFE_EXFIL: "SAFE_EXFIL",
} as const;

export type RoutingProfile =
  (typeof RoutingProfile)[keyof typeof RoutingProfile];



export const RecipeType = {
  CRAFTING: "CRAFTING",
  WORKBENCH_UPGRADE: "WORKBENCH_UPGRADE",
} as const;

export type RecipeType = (typeof RecipeType)[keyof typeof RecipeType];


export interface RecipeIngredient {
  itemId: number;
  itemName: string;
  quantity: number;
}

export interface Recipe {
  id?: number;
  metaforgeItemId: string;  // Used for planner targeting
  name: string;
  description: string;
  type: RecipeType;
  ingredients: RecipeIngredient[];
}

export interface PlannerRequest {
  targetItemNames: string[];
  targetEnemyTypes: string[];
  targetRecipeIds: string[];
  targetContainerTypes: string[];
  hasRaiderKey: boolean;
  routingProfile: RoutingProfile;
  ongoingItemNames?: string[];
}

export interface PlannerResponse {
  mapId: number;
  mapName: string;
  score: number;
  path: Waypoint[];
  extractionPoint?: string;
  extractionLat?: number;  // Calibrated Y coordinate of extraction point
  extractionLng?: number;  // Calibrated X coordinate of extraction point
  nearbyEnemySpawns: EnemySpawn[]; // All spawns of selected enemy types with proximity info
}

// Session state management types
export interface TargetSelection {
  id: string | number;
  type: "ITEM" | "RECIPE" | "ENEMY" | "CONTAINER";
  priority: "PRIORITY" | "ONGOING";
  name: string;
  iconUrl?: string;
  rarity?: string;
  lootZone?: string;
  data?: Item | Recipe | EnemyType | ContainerType; // Store full object for convenience
}

export interface SessionState {
  priorityTargets: TargetSelection[];
  ongoingTargets: TargetSelection[];
  routingProfile: RoutingProfile;
  hasRaiderKey: boolean;
  lastCalculatedHash: string | null;
}
