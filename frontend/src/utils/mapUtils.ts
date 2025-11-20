import L from 'leaflet';
import type { Area } from '../types';

// 1. Standardize Map Image URL generation
export const getMapImageUrl = (mapName: string): string => {
    const filename = mapName.toLowerCase().replace(/ /g, '_');
    return `/maps/${filename}.png`;
};

// 2. Parse Polygon Coordinates safely
// Returns null if invalid, otherwise returns Leaflet-ready LatLngExpression[]
export const parseAreaCoordinates = (coordinateString?: string): L.LatLngExpression[] | null => {
    if (!coordinateString) return null;
    try {
        return JSON.parse(coordinateString);
    } catch (e) {
        console.error("Failed to parse coordinates:", e);
        return null;
    }
};

// 3. Standardize Coordinate Conversion (Game Grid -> Leaflet)
// In Leaflet CRS.Simple, [0,0] is bottom-left usually, but we map [y, x]
export const gameCoordsToLatLng = (x: number, y: number): L.LatLngTuple => {
    return [y, x] as L.LatLngTuple;
};