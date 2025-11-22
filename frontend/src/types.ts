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
    PURE_SCAVENGER: 'PURE_SCAVENGER',
    EASY_EXFIL: 'EASY_EXFIL',
    AVOID_PVP: 'AVOID_PVP',
    SAFE_EXFIL: 'SAFE_EXFIL'
} as const;

export type RoutingProfile = typeof RoutingProfile[keyof typeof RoutingProfile];

export interface PlannerRequest {
    targetItemNames: string[];
    hasRaiderKey: boolean;
    routingProfile: RoutingProfile;
}

export interface PlannerResponse {
    mapId: number;
    mapName: string;
    score: number;
    routePath: Area[];
    extractionPoint?: string;
}