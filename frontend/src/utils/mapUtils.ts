import L from 'leaflet'
import type { EnemySpawn } from '../types'

// --- TYPES for Map Editor and Calibration ---
export interface GameMap {
    id: number
    name: string
    description: string
    calibrationScaleX?: number
    calibrationScaleY?: number
    calibrationOffsetX?: number
    calibrationOffsetY?: number
}

export interface GameMarker {
    id: string
    lat: number
    lng: number
    category: string
    subcategory: string
    name: string
    description: string
}

// 1. Standardize Map Image URL generation
export const getMapImageUrl = (mapName: string): string => {
    const filename = mapName.toLowerCase().replace(/ /g, '_')
    return `/maps/${filename}.png`
}

// 2. Parse Polygon Coordinates safely
// Returns null if invalid, otherwise returns Leaflet-ready LatLngExpression[]
export const parseAreaCoordinates = (coordinateString?: string): L.LatLngExpression[] | null => {
    if (!coordinateString) return null
    try {
        return JSON.parse(coordinateString)
    } catch (e) {
        console.error('Failed to parse coordinates:', e)
        return null
    }
}

// 3. Standardize Coordinate Conversion (Game Grid -> Leaflet)
// In Leaflet CRS.Simple, [0,0] is bottom-left usually, but we map [y, x]
export const gameCoordsToLatLng = (x: number, y: number): L.LatLngTuple => {
    return [y, x] as L.LatLngTuple
}

// --- HELPER: Transform Coordinates ---
export const transformMarker = (marker: GameMarker, map: GameMap): L.LatLngTuple => {
    const scaleX = map.calibrationScaleX ?? 1.0
    const scaleY = map.calibrationScaleY ?? 1.0
    const offsetX = map.calibrationOffsetX ?? 0.0
    const offsetY = map.calibrationOffsetY ?? 0.0

    const localX = marker.lng * scaleX + offsetX
    const localY = marker.lat * scaleY + offsetY

    return [localY, localX] as L.LatLngTuple
}

export const transformEnemyMarker = (spawn: EnemySpawn, map: GameMap): L.LatLngTuple => {
    const scaleX = map.calibrationScaleX ?? 1.0
    const scaleY = map.calibrationScaleY ?? 1.0
    const offsetX = map.calibrationOffsetX ?? 0.0
    const offsetY = map.calibrationOffsetY ?? 0.0

    const localX = spawn.lng * scaleX + offsetX
    const localY = spawn.lat * scaleY + offsetY

    return [localY, localX] as L.LatLngTuple
}
