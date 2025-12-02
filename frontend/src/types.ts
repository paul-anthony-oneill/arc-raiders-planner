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
  type: "AREA" | "MARKER";
  lootTypes?: string[];
  lootAbundance?: number;
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

export interface PlannerRequest {
  targetItemNames: string[];
  targetEnemyTypes: string[]; // Enemy type names to hunt (e.g., ["sentinel", "guardian"])
  targetRecipeIds: string[]; // Recipe IDs for crafting/upgrades (e.g., ["hideout_weapon_bench_lvl2", "item_123"])
  hasRaiderKey: boolean;
  routingProfile: RoutingProfile;
  ongoingItemNames?: string[];
}

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
