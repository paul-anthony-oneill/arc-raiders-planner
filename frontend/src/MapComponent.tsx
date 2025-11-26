import React from 'react'
import { ImageOverlay, MapContainer, Marker, Polygon, Polyline, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import type { Area, EnemySpawn, RoutingProfile } from './types'
import L from 'leaflet'

interface MapProps {
    mapName: string
    areas: Area[]
    routePath?: Area[]
    extractionPoint?: string
    extractionLat?: number
    extractionLng?: number
    routingProfile?: RoutingProfile
    showRoutePath?: boolean
    enemySpawns?: EnemySpawn[]
}

// Custom icon setup
const defaultIcon = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

// Green flag icon for extraction points
const exitIcon = L.icon({
    iconUrl:
        'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJncmVlbiI+PHBhdGggZD0iTTUgMjFWNGgyTDEzIDhsLTYgNGg2bC02IDR2NXoiLz48L3N2Zz4=',
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -40],
})

// Create enemy icon based on whether it's on route or not
const createEnemyIcon = (onRoute: boolean) => {
    const bgColor = onRoute ? '#f44336' : '#9e9e9e'
    const borderColor = onRoute ? '#ff8a80' : '#bdbdbd'
    const shadow = onRoute ? '0 3px 8px rgba(244, 67, 54, 0.6)' : '0 2px 4px rgba(0, 0, 0, 0.3)'
    const size = onRoute ? 36 : 28
    const fontSize = onRoute ? 20 : 16

    return L.divIcon({
        className: 'enemy-marker',
        html: `<div style="
            background-color: ${bgColor};
            color: white;
            font-weight: bold;
            font-size: ${fontSize}px;
            width: ${size}px;
            height: ${size}px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            border: 3px solid ${borderColor};
            box-shadow: ${shadow};
            opacity: ${onRoute ? 1 : 0.6};
        ">⚡</div>`,
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        popupAnchor: [0, -(size / 2)],
    })
}

// Create numbered DivIcon for route sequence
const createNumberedIcon = (number: number, isDanger: boolean = false, hasOngoing: boolean = false) => {
    const borderColor = hasOngoing ? '#2196F3' : 'white'; // Blue border for ongoing
    const borderWidth = hasOngoing ? '4px' : '3px';
    const size = hasOngoing ? 36 : 32;
    
    return L.divIcon({
        className: 'numbered-marker',
        html: `<div style="
            background-color: ${isDanger ? '#ff4444' : '#4CAF50'};
            color: white;
            font-weight: bold;
            font-size: 16px;
            width: ${size}px;
            height: ${size}px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            border: ${borderWidth} solid ${borderColor};
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        ">${number}
        ${hasOngoing ? '<span style="position:absolute; top:-5px; right:-5px; background:#2196F3; border-radius:50%; width:12px; height:12px; border:2px solid white;"></span>' : ''}
        </div>`,
        iconSize: [size, size],
        iconAnchor: [size/2, size/2],
    })
}

const MapComponent: React.FC<MapProps> = ({
    mapName,
    areas,
    routePath = [],
    extractionPoint,
    extractionLat,
    extractionLng,
    routingProfile,
    showRoutePath = true,
    enemySpawns = [],
}) => {
    const bounds: L.LatLngBoundsLiteral = [
        [-1000, -1000],
        [1000, 1000],
    ]
    const maxBounds: L.LatLngBoundsLiteral = [
        [-1500, -1500],
        [1500, 1500],
    ]

    const coordsToLatLng = (x: number, y: number): L.LatLngTuple => {
        return [y, x] as L.LatLngTuple
    }

    const getMapImageUrl = (name: string) => {
        const filename = name.toLowerCase().replace(/ /g, '_')
        return `/maps/${filename}.png`
    }

    // Determine if we should show danger zones
    const showDangerZones = routingProfile === 'AVOID_PVP' || routingProfile === 'SAFE_EXFIL'

    // Filter danger zones (lootAbundance === 1)
    const dangerZones = showDangerZones ? areas.filter((area) => area.lootAbundance === 1) : []

    // Get route path coordinates for polyline
    const routePathCoords: L.LatLngExpression[] = routePath.map((area) => coordsToLatLng(area.mapX, area.mapY))

    // Find extraction point coordinates (if exists)
    // Backend now provides calibrated coordinates directly
    let extractionCoords: L.LatLngTuple | null = null
    if (extractionPoint && extractionLat != null && extractionLng != null) {
        extractionCoords = [extractionLat, extractionLng] as L.LatLngTuple
    }

    // Check if an area is in the route
    const isInRoute = (areaId: number) => {
        return routePath.some((a) => a.id === areaId)
    }

    // Get route index for an area
    const getRouteIndex = (areaId: number) => {
        return routePath.findIndex((a) => a.id === areaId)
    }
    
    // Get ongoing matches for an area
    const getOngoingMatches = (areaId: number) => {
        const area = routePath.find(a => a.id === areaId);
        return area?.ongoingMatchItems || [];
    }

    return (
        <div
            style={{
                height: '100%',
                width: '100%',
                position: 'relative',
                backgroundColor: '#1a1a1a', // Dark background to match map edges
            }}
        >
            <MapContainer
                center={[0, 0]}
                zoom={0}
                minZoom={-1} // Limit zoom out (was -2, now less extreme)
                maxZoom={2}
                crs={L.CRS.Simple}
                bounds={bounds}
                maxBounds={maxBounds}
                maxBoundsViscosity={0.8}
                style={{ height: '100%', width: '100%', backgroundColor: '#1a1a1a' }}
            >
                <ImageOverlay url={getMapImageUrl(mapName)} bounds={bounds} attribution="&copy; Embark Studios" />

                {/* Render danger zones first (bottom layer) */}
                {dangerZones.map((area) => {
                    let polygonPositions: L.LatLngExpression[] | null = null

                    if (area.coordinates) {
                        try {
                            polygonPositions = JSON.parse(area.coordinates)
                        } catch (e) {
                            console.error(`Failed to parse coordinates for area ${area.name}`, e)
                        }
                    }

                    return polygonPositions ? (
                        <Polygon
                            key={`danger-${area.id}`}
                            positions={polygonPositions}
                            pathOptions={{
                                color: '#ff0000',
                                fillColor: '#ff0000',
                                fillOpacity: 0.3,
                                weight: 3,
                                dashArray: '10, 5',
                            }}
                        >
                            <Popup>
                                <strong>⚠️ DANGER ZONE</strong>
                                <br />
                                <strong>{area.name}</strong>
                                <br />
                                High Traffic Area - Avoid!
                            </Popup>
                        </Polygon>
                    ) : null
                })}

                {/* Render route path polyline */}
                {showRoutePath && routePathCoords.length > 1 && (
                    <Polyline
                        positions={routePathCoords}
                        pathOptions={{
                            color: '#2196F3',
                            weight: 4,
                            opacity: 0.8,
                            dashArray: showDangerZones ? '10, 5' : undefined,
                        }}
                    />
                )}

                {/* Render all areas */}
                {areas.map((area) => {
                    let polygonPositions: L.LatLngExpression[] | null = null

                    if (area.coordinates) {
                        try {
                            polygonPositions = JSON.parse(area.coordinates)
                        } catch (e) {
                            console.error(`Failed to parse coordinates for area ${area.name}`, e)
                        }
                    }

                    const inRoute = isInRoute(area.id)
                    const routeIndex = getRouteIndex(area.id)
                    const isDanger = area.lootAbundance === 1
                    const ongoingMatches = inRoute ? getOngoingMatches(area.id) : []
                    const hasOngoing = ongoingMatches.length > 0

                    // Skip rendering polygon if it's a danger zone (already rendered)
                    if (isDanger && showDangerZones) {
                        return null
                    }

                    return (
                        <React.Fragment key={area.id}>
                            {/* Draw the Polygon if coordinates exist */}
                            {polygonPositions && (
                                <Polygon
                                    positions={polygonPositions}
                                    pathOptions={{
                                        color: hasOngoing ? '#2196F3' : (inRoute ? '#4CAF50' : '#cccccc'),
                                        fillColor: hasOngoing ? '#2196F3' : (inRoute ? '#4CAF50' : '#cccccc'),
                                        fillOpacity: inRoute ? 0.4 : 0.1,
                                        weight: inRoute ? 3 : 1,
                                    }}
                                >
                                    <Popup>
                                        <strong>{area.name}</strong>
                                        <br />
                                        {area.lootTypes && area.lootTypes.length > 0 && (
                                            <>
                                                Types: {area.lootTypes.join(', ')}
                                                <br />
                                            </>
                                        )}
                                        {inRoute && (
                                            <>
                                                <strong>Route Position: #{routeIndex + 1}</strong>
                                                <br/>
                                            </>
                                        )}
                                        {hasOngoing && (
                                            <div style={{marginTop: '4px', color: '#2196F3'}}>
                                                <strong>Bonus Loot:</strong>
                                                <ul style={{margin: '0', paddingLeft: '16px'}}>
                                                    {ongoingMatches.map(item => <li key={item}>{item}</li>)}
                                                </ul>
                                            </div>
                                        )}
                                    </Popup>
                                </Polygon>
                            )}

                            {/* Draw numbered marker for areas in route */}
                            {inRoute ? (
                                <Marker
                                    position={coordsToLatLng(area.mapX || 0, area.mapY || 0)}
                                    icon={createNumberedIcon(routeIndex + 1, isDanger, hasOngoing)}
                                >
                                    <Popup>
                                        <strong>
                                            Stop #{routeIndex + 1}: {area.name}
                                        </strong>
                                        <br />
                                        {area.lootTypes && area.lootTypes.length > 0 && (
                                            <>Types: {area.lootTypes.join(', ')}</>
                                        )}
                                        {hasOngoing && (
                                            <div style={{marginTop: '8px', borderTop: '1px solid #ccc', paddingTop: '4px'}}>
                                                <strong style={{color: '#2196F3'}}>Bonus Loot (Ongoing):</strong>
                                                <ul style={{margin: '0', paddingLeft: '16px', color: '#2196F3'}}>
                                                    {ongoingMatches.map(item => <li key={item}>{item}</li>)}
                                                </ul>
                                            </div>
                                        )}
                                    </Popup>
                                </Marker>
                            ) : (
                                <Marker position={coordsToLatLng(area.mapX || 0, area.mapY || 0)} icon={defaultIcon}>
                                    <Popup>
                                        <strong>{area.name}</strong>
                                        <br />
                                        {area.lootTypes && area.lootTypes.length > 0 && (
                                            <>Types: {area.lootTypes.join(', ')}</>
                                        )}
                                    </Popup>
                                </Marker>
                            )}
                        </React.Fragment>
                    )
                })}

                {/* Render extraction point marker */}
                {extractionPoint && extractionCoords && (
                    <Marker
                        position={[extractionCoords[0], extractionCoords[1]]}
                        icon={exitIcon}
                    >
                        <Popup>
                            <strong>🚪 EXTRACTION POINT</strong>
                            <br />
                            {extractionPoint}
                        </Popup>
                    </Marker>
                )}

                {/* Render enemy spawn markers */}
                {enemySpawns.map(
                    (spawn) => (
                        <Marker
                            key={spawn.id}
                            position={[spawn.lat, spawn.lng]}
                            icon={createEnemyIcon(spawn.onRoute)}
                        >
                            <Popup>
                                <strong>⚡ {spawn.onRoute ? 'ON ROUTE' : 'OFF ROUTE'}</strong>
                                <br />
                                <strong>{spawn.type.charAt(0).toUpperCase() + spawn.type.slice(1)}</strong>
                                <br />
                                {spawn.distanceToRoute !== null && spawn.distanceToRoute !== undefined && (
                                    <>
                                        Distance to route: {Math.round(spawn.distanceToRoute)} units
                                        <br />
                                    </>
                                )}
                                {spawn.onRoute && (
                                    <span style={{ color: '#4CAF50', fontWeight: 'bold' }}>✓ Near your route</span>
                                )}
                            </Popup>
                        </Marker>
                    )
                )}
            </MapContainer>

            {/* Legend */}
            <div
                style={{
                    position: 'absolute',
                    bottom: '20px',
                    left: '20px',
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    padding: '12px',
                    borderRadius: '8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                    maxWidth: '250px',
                    zIndex: 1000,
                    fontSize: '13px',
                }}
            >
                <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>Map Legend</div>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '6px' }}>
                    <div
                        style={{
                            width: '20px',
                            height: '20px',
                            backgroundColor: '#4CAF50',
                            border: '2px solid white',
                            borderRadius: '50%',
                            marginRight: '8px',
                        }}
                    ></div>
                    <span>Route stops</span>
                </div>
                {showDangerZones && (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            marginBottom: '6px',
                        }}
                    >
                        <div
                            style={{
                                width: '20px',
                                height: '20px',
                                backgroundColor: '#ff0000',
                                opacity: 0.5,
                                border: '2px dashed #ff0000',
                                marginRight: '8px',
                            }}
                        ></div>
                        <span>Danger zones</span>
                    </div>
                )}
                {extractionPoint && (
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span style={{ fontSize: '20px', marginRight: '8px' }}>🚩</span>
                        <span>Extraction point</span>
                    </div>
                )}
            </div>
        </div>
    )
}

export default MapComponent
