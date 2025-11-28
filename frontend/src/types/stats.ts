import type { Area } from '../types';

export interface LootProbability {
    rare: number;
    epic: number;
}

export type ThreatLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME';

export interface RouteStats {
    sectorName: string;
    environment: string;
    lootProbability: LootProbability;
    threatLevel: ThreatLevel;
}

export interface MapDataResponse {
    areas: Area[];
    name: string;
}
