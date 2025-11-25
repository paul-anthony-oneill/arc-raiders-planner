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
  lootType: LootType | null;
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
}

export interface MapRecommendation {
  mapId: number;
  mapName: string;
  matchingAreaCount: number; // The count used for ranking
}

export interface Area {
  id: number;
  name: string;
  mapX: number;
  mapY: number;
  coordinates?: string;
  lootTypes: string[];
  lootAbundance?: number;
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
  hasRaiderKey: boolean;
  routingProfile: RoutingProfile;
}

export interface PlannerResponse {
  mapId: number;
  mapName: string;
  score: number;
  routePath: Area[];
  extractionPoint?: string;
  extractionLat?: number;  // Calibrated Y coordinate of extraction point
  extractionLng?: number;  // Calibrated X coordinate of extraction point
  nearbyEnemySpawns: EnemySpawn[]; // All spawns of selected enemy types with proximity info
}
