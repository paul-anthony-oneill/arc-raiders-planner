import React from 'react'
import { ImageOverlay, MapContainer, Marker, Polygon, Polyline, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import type { Area, Waypoint, EnemySpawn, RoutingProfile } from './types'
import L from 'leaflet'

interface MapProps {
    mapName: string
    areas: Area[]
    routePath?: Waypoint[]
    extractionPoint?: string
    extractionLat?: number
    extractionLng?: number
    routingProfile?: RoutingProfile
    showRoutePath?: boolean
    enemySpawns?: EnemySpawn[]
    itemContextMap?: Record<string, string[]>
}

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
const createNumberedIcon = (number: number, isDanger: boolean = false, hasOngoing: boolean = false, isMarker: boolean = false) => {
    const borderColor = hasOngoing ? '#2196F3' : (isMarker ? '#ff9800' : 'white'); // Orange for marker targets
    const borderWidth = hasOngoing || isMarker ? '4px' : '3px';
    const size = hasOngoing || isMarker ? 36 : 32;
    const bgColor = isMarker ? '#e65100' : (isDanger ? '#ff4444' : '#4CAF50'); // Orange/Red background for marker/danger
    
    return L.divIcon({
        className: 'numbered-marker',
        html: `<div style="
            background-color: ${bgColor};
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
    itemContextMap = {},
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
    // Waypoint uses x,y. Area uses mapX, mapY. Waypoint normalizes this.
    const routePathCoords: L.LatLngExpression[] = routePath.map((wp) => coordsToLatLng(wp.x, wp.y))

    // Find extraction point coordinates (if exists)
    let extractionCoords: L.LatLngTuple | null = null
    if (extractionPoint && extractionLat != null && extractionLng != null) {
        extractionCoords = [extractionLat, extractionLng] as L.LatLngTuple
    }

    // Helpers for checking route status
    const getWaypointForArea = (areaId: number) => {
        return routePath.find((wp) => wp.type === 'AREA' && String(wp.id) === String(areaId))
    }

    const formatItemWithContext = (itemName: string) => {
        const context = itemContextMap[itemName];
        if (!context || context.length === 0) return itemName;
        return `${itemName} [${context.join(', ')}]`;
    }

    return (
        <div
            style={{
                height: '100%',
                width: '100%',
                position: 'relative',
                backgroundColor: '#1a1a1a',
            }}
        >
            <MapContainer
                center={[0, 0]}
                zoom={0}
                minZoom={-1}
                maxZoom={2}
                crs={L.CRS.Simple}
                bounds={bounds}
                maxBounds={maxBounds}
                maxBoundsViscosity={0.8}
                style={{ height: '100%', width: '100%', backgroundColor: '#1a1a1a' }}
            >
                <ImageOverlay url={getMapImageUrl(mapName)} bounds={bounds} attribution="&copy; Embark Studios" />

                {/* Render danger zones (bottom layer) */}
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
                                <strong>⚠️ DANGER ZONE</strong><br />{area.name}
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

                {/* Render Area Polygons */}
                {areas.map((area) => {
                    let polygonPositions: L.LatLngExpression[] | null = null
                    if (area.coordinates) {
                        try {
                            polygonPositions = JSON.parse(area.coordinates)
                        } catch (e) { console.error(e) }
                    }

                    const waypoint = getWaypointForArea(area.id)
                    const inRoute = !!waypoint
                    const isDanger = area.lootAbundance === 1 && showDangerZones
                    const hasOngoing = waypoint?.ongoingMatchItems && waypoint.ongoingMatchItems.length > 0

                    if (isDanger) return null // Already rendered

                    return (
                        <React.Fragment key={`area-${area.id}`}>
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
                                        {area.lootTypes && <><br/>Types: {area.lootTypes.join(', ')}</>}
                                        
                                        {waypoint && waypoint.targetMatchItems && waypoint.targetMatchItems.length > 0 && (
                                            <div style={{marginTop: '8px', color: '#d32f2f'}}>
                                                <strong>🎯 Target Loot:</strong>
                                                <ul style={{margin: '0', paddingLeft: '16px'}}>
                                                    {waypoint.targetMatchItems.map(item => (
                                                        <li key={item}>{formatItemWithContext(item)}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        {waypoint && waypoint.ongoingMatchItems && waypoint.ongoingMatchItems.length > 0 && (
                                            <div style={{marginTop: '8px', color: '#2196F3'}}>
                                                <strong>Bonus Loot:</strong>
                                                <ul style={{margin: '0', paddingLeft: '16px'}}>
                                                    {waypoint.ongoingMatchItems.map(item => (
                                                        <li key={item}>{formatItemWithContext(item)}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                    </Popup>
                                </Polygon>
                            )}
                            {/* Non-route areas get a simple marker if needed, or just polygon. 
                                We skip non-route markers to reduce clutter, only showing route stops below. */}
                        </React.Fragment>
                    )
                })}

                {/* Render Route Waypoints (Stops) */}
                {routePath.map((wp, index) => {
                    const isDanger = wp.lootAbundance === 1
                    const hasOngoing = wp.ongoingMatchItems && wp.ongoingMatchItems.length > 0
                    const isMarker = wp.type === 'MARKER'
                    
                    return (
                        <Marker
                            key={`wp-${index}`}
                            position={coordsToLatLng(wp.x, wp.y)}
                            icon={createNumberedIcon(index + 1, isDanger, hasOngoing, isMarker)}
                        >
                            <Popup>
                                <strong>Stop #{index + 1}: {wp.name}</strong>
                                <br />
                                {isMarker && <strong style={{color: '#e65100'}}>⚡ ENEMY TARGET</strong>}
                                {wp.targetMatchItems && wp.targetMatchItems.length > 0 && (
                                    <div style={{marginBottom: '8px', color: '#d32f2f'}}>
                                        <strong>🎯 Target Loot:</strong>
                                        <ul style={{margin: '0', paddingLeft: '16px'}}>
                                            {wp.targetMatchItems.map(item => (
                                                <li key={item}>{formatItemWithContext(item)}</li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                                {wp.ongoingMatchItems && wp.ongoingMatchItems.length > 0 && (
                                    <div style={{marginTop: '8px', color: '#2196F3'}}>
                                        <strong>Bonus Loot:</strong>
                                        <ul style={{margin: '0', paddingLeft: '16px'}}>
                                            {wp.ongoingMatchItems.map(item => (
                                                <li key={item}>{formatItemWithContext(item)}</li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                            </Popup>
                        </Marker>
                    )
                })}

                {/* Render extraction point */}
                {extractionPoint && extractionCoords && (
                    <Marker position={extractionCoords} icon={exitIcon}>
                        <Popup><strong>🚪 EXTRACTION POINT</strong><br />{extractionPoint}</Popup>
                    </Marker>
                )}

                {/* Render enemy spawns (Opportunity Targets) */}
                {enemySpawns.map((spawn) => {
                    // Deduplicate: Don't show an "Opportunity" marker if a "Primary" marker (Waypoint) is already there
                    const isPrimaryTarget = routePath.some(wp => wp.type === 'MARKER' && Math.abs(wp.x - spawn.lng) < 1 && Math.abs(wp.y - spawn.lat) < 1)
                    
                    if (isPrimaryTarget) return null;

                    return (
                        <Marker
                            key={`spawn-${spawn.id}`}
                            position={[spawn.lat, spawn.lng]}
                            icon={createEnemyIcon(spawn.onRoute)}
                        >
                            <Popup>
                                <strong>⚡ {spawn.onRoute ? 'ON ROUTE' : 'OFF ROUTE'}</strong><br />
                                <strong>{spawn.type}</strong><br />
                                {spawn.onRoute && <span style={{ color: '#4CAF50' }}>✓ Near route</span>}
                            </Popup>
                        </Marker>
                    )
                })}
            </MapContainer>
            
            {/* Legend - Keeping existing style but updated */}
            <div style={{
                position: 'absolute', bottom: '20px', left: '20px',
                backgroundColor: 'rgba(255, 255, 255, 0.95)', padding: '12px',
                borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                zIndex: 1000, fontSize: '13px'
            }}>
                <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>Map Legend</div>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '6px' }}>
                    <div style={{ width: '20px', height: '20px', backgroundColor: '#4CAF50', borderRadius: '50%', marginRight: '8px', border: '2px solid white' }}></div>
                    <span>Loot Area Stop</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '6px' }}>
                    <div style={{ width: '20px', height: '20px', backgroundColor: '#e65100', borderRadius: '50%', marginRight: '8px', border: '3px solid #ff9800' }}></div>
                    <span>Enemy Target Stop</span>
                </div>
                {/* ... other legend items ... */}
            </div>
        </div>
    )
}

export default MapComponent
