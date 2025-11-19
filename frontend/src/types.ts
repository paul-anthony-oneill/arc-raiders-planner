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
    lootTypes: LootType[];
}