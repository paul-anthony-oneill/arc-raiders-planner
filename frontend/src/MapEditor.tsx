import React, {useEffect, useState} from 'react';
import {CircleMarker, ImageOverlay, MapContainer, Polygon, Polyline, Popup, useMapEvents} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type {Area} from './types';

// --- TYPES ---
interface GameMap {
    id: number;
    name: string;
    description: string;
    calibrationScaleX?: number;
    calibrationScaleY?: number;
    calibrationOffsetX?: number;
    calibrationOffsetY?: number;
}

interface GameMarker {
    id: string;
    lat: number;
    lng: number;
    category: string;
    subcategory: string;
    name: string;
    description: string;
}

interface MapEditorProps {
    onExit: () => void;
}

// --- STYLES ---
const drawOptions = {color: '#00ff00', dashArray: '5, 5'};
const existingAreaOptions = {color: 'gray', fillColor: 'gray', fillOpacity: 0.2, weight: 1};

const markerOptions = {
    radius: 4,
    fillColor: "#ff7800",
    color: "#000",
    weight: 1,
    opacity: 1,
    fillOpacity: 0.8
};

const calibrationPointOptions = {
    radius: 8,
    fillColor: "#ff0000",
    color: "#fff",
    weight: 2,
    fillOpacity: 1
};

const drawingPointOptions = {
    radius: 5,
    fillColor: "#00ff00",
    color: "#fff",
    weight: 1,
    fillOpacity: 0.8
};

// --- HELPER: Click Handler ---
const ClickHandler: React.FC<{ onClick: (latlng: L.LatLng) => void }> = ({ onClick }) => {
    useMapEvents({
        click(e) { onClick(e.latlng); },
    });
    return null;
};

const MapEditor: React.FC<MapEditorProps> = ({ onExit }) => {
    const bounds: L.LatLngBoundsLiteral = [[-1000, -1000], [1000, 1000]];

    // --- STATE ---
    const [maps, setMaps] = useState<GameMap[]>([]);
    const [selectedMap, setSelectedMap] = useState<GameMap | null>(null);
    const [existingAreas, setExistingAreas] = useState<Area[]>([]);
    const [gameMarkers, setGameMarkers] = useState<GameMarker[]>([]);
    const [visibleCategories, setVisibleCategories] = useState<Set<string>>(new Set());

    // --- CALIBRATION STATE (4 Points) ---
    const [isCalibrating, setIsCalibrating] = useState(false);
    const [calPoints, setCalPoints] = useState<{ local: L.LatLng, api: { x: number, y: number } }[]>([]);
    const [tempApiX, setTempApiX] = useState('');
    const [tempApiY, setTempApiY] = useState('');
    const [showCalInput, setShowCalInput] = useState(false);
    const [currentLocalPoint, setCurrentLocalPoint] = useState<L.LatLng | null>(null);

    // Draw State
    const [drawPoints, setDrawPoints] = useState<L.LatLng[]>([]);
    const [areaName, setAreaName] = useState('');
    const [lootAbundance, setLootAbundance] = useState<number>(2);
    const [generatedCode, setGeneratedCode] = useState('');

    // --- HELPER: Transform Coordinates ---
// --- HELPER: Transform Coordinates ---
    const transformMarker = (marker: GameMarker, map: GameMap): L.LatLngTuple => {
        const scaleX = map.calibrationScaleX ?? 1.0;
        const scaleY = map.calibrationScaleY ?? 1.0;
        const offsetX = map.calibrationOffsetX ?? 0.0;
        const offsetY = map.calibrationOffsetY ?? 0.0;

        // DEBUG LOG (Check your console!)
        if (marker.name === "a-symbol-of-unification" || Math.random() < 0.01) {
            console.log("Transforming Marker:", {
                mapName: map.name,
                scaleX, offsetX,
                originalLng: marker.lng,
                calculatedX: (marker.lng * scaleX) + offsetX
            });
        }

        const localX = (marker.lng * scaleX) + offsetX;
        const localY = (marker.lat * scaleY) + offsetY;

        return [localY, localX] as L.LatLngTuple;
    };

    // --- INITIAL LOAD ---
    useEffect(() => {
        const loadMapsAndAreas = async () => {
            try {
                const res = await fetch('/api/maps');
                const mapData: GameMap[] = await res.json();
                setMaps(mapData);

                if (mapData.length > 0) {
                    handleMapSelect(mapData[0].name, mapData);
                }
            } catch (err) {
                console.error("Initialization error:", err);
            }
        };
        loadMapsAndAreas();
    }, []);

    // --- HANDLERS ---
    const handleMapSelect = async (mapName: string, currentMaps: GameMap[] = maps) => {
        const map = currentMaps.find(m => m.name === mapName);
        if (map) {
            setSelectedMap(map);
            setDrawPoints([]);
            setGeneratedCode('');
            setCalPoints([]);
            setIsCalibrating(false);

            try {
                const areaRes = await fetch(`/api/maps/${encodeURIComponent(mapName)}/data`);
                if (areaRes.ok) {
                    const areaJson: { areas: Area[] } = await areaRes.json();
                    setExistingAreas(areaJson.areas || []);
                }

                const markerRes = await fetch(`/api/maps/${map.id}/markers`);
                if (markerRes.ok) {
                    const markers: GameMarker[] = await markerRes.json();
                    setGameMarkers(markers);
                    const categories = new Set(markers.map(m => m.category));
                    setVisibleCategories(categories);
                } else {
                    setGameMarkers([]);
                }
            } catch (err) {
                console.error(`Error loading data for ${mapName}`, err);
            }
        }
    };

    // --- CALIBRATION HANDLERS (4-Point Logic) ---
    const handleCalibrationClick = (latlng: L.LatLng) => {
        if (calPoints.length >= 4) return alert("Calibration complete (4 points set). Click 'Calculate & Save'.");
        setCurrentLocalPoint(latlng);
        setShowCalInput(true);
    };

    const saveCalibrationPoint = () => {
        if (!currentLocalPoint) return;
        const point = {
            local: currentLocalPoint,
            api: {x: parseFloat(tempApiX), y: parseFloat(tempApiY)}
        };
        setCalPoints([...calPoints, point]);
        setShowCalInput(false);
        setTempApiX('');
        setTempApiY('');
    };

    const calculateAndSaveCalibration = async () => {
        if (calPoints.length < 4 || !selectedMap) return;

        // Pair A: X-Axis (Points 0 and 1)
        // We ONLY look at Longitude (Local X) and API X
        const px1 = calPoints[0];
        const px2 = calPoints[1];

        const scaleX = (px2.local.lng - px1.local.lng) / (px2.api.x - px1.api.x);
        const offsetX = px1.local.lng - (px1.api.x * scaleX);

        // Pair B: Y-Axis (Points 2 and 3)
        // We ONLY look at Latitude (Local Y) and API Y
        const py1 = calPoints[2];
        const py2 = calPoints[3];

        const scaleY = (py2.local.lat - py1.local.lat) / (py2.api.y - py1.api.y);
        const offsetY = py1.local.lat - (py1.api.y * scaleY);

        console.log("Calculated 4-Point Calibration:", {scaleX, scaleY, offsetX, offsetY});

        if (!isFinite(scaleX) || !isFinite(scaleY)) return alert("Invalid calculation.");

        try {
            const res = await fetch(`/api/maps/${selectedMap.id}/calibration`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({scaleX, scaleY, offsetX, offsetY})
            });

            if (res.ok) {
                alert(`Calibration Saved!\nScale X: ${scaleX.toFixed(5)}\nScale Y: ${scaleY.toFixed(5)}`);
                setIsCalibrating(false);
                setCalPoints([]);

                setSelectedMap({
                    ...selectedMap,
                    calibrationScaleX: scaleX,
                    calibrationScaleY: scaleY,
                    calibrationOffsetX: offsetX,
                    calibrationOffsetY: offsetY
                });
            } else {
                alert("Failed to save calibration.");
            }
        } catch (e) {
            console.error(e);
        }
    };

    const onMapClick = (latlng: L.LatLng) => {
        if (isCalibrating) {
            handleCalibrationClick(latlng);
        } else {
            setDrawPoints(prev => [...prev, latlng]);
        }
    };

    // --- DRAWING HANDLERS ---
    const calculateCenter = (points: L.LatLng[]): { x: number, y: number } => {
        if (points.length === 0) return { x: 0, y: 0 };
        let totalLat = 0, totalLng = 0;
        points.forEach(p => {
            totalLat += p.lat;
            totalLng += p.lng;
        });
        return {x: Math.round(totalLng / points.length), y: Math.round(totalLat / points.length)};
    };

    const handleGenerate = () => {
        if (drawPoints.length < 3) return alert("Polygon needs at least 3 points.");
        if (!selectedMap || !areaName) return alert("Please enter an Area Name.");

        const center = calculateCenter(drawPoints);
        const coordString = JSON.stringify(drawPoints.map(p => [p.lat, p.lng]));
        const safeName = areaName.replace(/'/g, "''");
        const sqlTuple = `('${safeName}', ${center.x}, ${center.y}, '${coordString}', ${lootAbundance}, '${selectedMap.name}'),`;
        setGeneratedCode(sqlTuple);
    };

    const getMapImageUrl = (map: GameMap) => {
        const filename = map.name.toLowerCase().replace(/ /g, '_');
        return `/maps/${filename}.png`;
    };

    return (
        <div style={{
            display: 'flex',
            height: '100vh',
            flexDirection: 'column',
            width: '100vw',
            marginLeft: 'calc(50% - 50vw)',
            marginRight: 'calc(50% - 50vw)'
        }}>
            {/* TOOLBAR */}
            <div style={{
                padding: '10px',
                background: '#222',
                color: 'white',
                display: 'flex',
                gap: '20px',
                alignItems: 'center'
            }}>
                <h3>🛠️ Map Editor</h3>
                <select
                    value={selectedMap?.name || ''}
                    onChange={e => handleMapSelect(e.target.value)}
                    style={{ padding: '5px' }}
                >
                    {maps.map(m => <option key={m.id} value={m.name}>{m.name}</option>)}
                </select>
                <button onClick={onExit} style={{ marginLeft: 'auto', background: '#d32f2f' }}>Exit</button>
                <button
                    onClick={() => setIsCalibrating(!isCalibrating)}
                    style={{background: isCalibrating ? '#ff9800' : '#555'}}
                >
                    {isCalibrating ? 'Cancel Calibration' : 'Calibrate Map (4-Point)'}
                </button>
            </div>

            {/* MAIN CONTENT */}
            <div style={{ display: 'grid', gridTemplateColumns: '350px 1fr', flexGrow: 1, overflow: 'hidden' }}>

                {/* SIDEBAR CONTROLS */}
                <div style={{ padding: '20px', background: '#1a1a1a', color: '#eee', overflowY: 'auto', borderRight: '1px solid #444' }}>
                    {isCalibrating ? (
                        <div style={{
                            background: '#333',
                            padding: '15px',
                            borderRadius: '8px',
                            border: '2px solid #ff9800',
                            marginBottom: '20px'
                        }}>
                            <h4>📐 4-Point Calibration</h4>
                            <p style={{fontSize: '0.85em', lineHeight: '1.5'}}>
                                <strong>X-Axis (Width):</strong><br/>
                                1. Click Far <b>LEFT</b> point -{">"} Enter Coords<br/>
                                2. Click Far <b>RIGHT</b> point -{">"} Enter Coords<br/>
                                <br/>
                                <strong>Y-Axis (Height):</strong><br/>
                                3. Click Far <b>TOP</b> point -{">"} Enter Coords<br/>
                                4. Click Far <b>BOTTOM</b> point -{">"} Enter Coords
                            </p>

                            <div style={{marginBottom: '10px', fontSize: '0.9em'}}>
                                <div>X1 (Left): {calPoints[0] ? "✅" : "Waiting..."}</div>
                                <div>X2 (Right): {calPoints[1] ? "✅" : "Waiting..."}</div>
                                <div>Y1 (Top): {calPoints[2] ? "✅" : "Waiting..."}</div>
                                <div>Y2 (Bottom): {calPoints[3] ? "✅" : "Waiting..."}</div>
                            </div>

                            <button
                                onClick={calculateAndSaveCalibration}
                                disabled={calPoints.length < 4}
                                style={{width: '100%', background: '#4CAF50', padding: '10px'}}
                            >
                                Calculate & Save
                            </button>

                            <button
                                onClick={() => setCalPoints([])}
                                style={{width: '100%', background: '#d32f2f', marginTop: '5px', padding: '5px'}}
                            >
                                Reset Points
                            </button>
                        </div>
                    ) : (
                        <>
                            <h4>New Area Details</h4>
                            <div style={{marginBottom: '15px'}}>
                                <label style={{display: 'block', marginBottom: '5px', fontSize: '0.9em'}}>Area
                                    Name</label>
                                <input type="text" value={areaName} onChange={e => setAreaName(e.target.value)}
                                       style={{width: '100%', padding: '8px'}}/>
                            </div>
                            <div style={{marginBottom: '20px'}}>
                                <label style={{display: 'block', marginBottom: '5px', fontSize: '0.9em'}}>Loot Abundance
                                    (1-3)</label>
                                <select value={lootAbundance} onChange={e => setLootAbundance(Number(e.target.value))}
                                        style={{width: '100%', padding: '8px'}}>
                                    <option value={1}>1 - Low</option>
                                    <option value={2}>2 - Medium</option>
                                    <option value={3}>3 - High</option>
                                </select>
                            </div>
                            <div style={{display: 'flex', gap: '10px', marginBottom: '20px'}}>
                                <button onClick={handleGenerate} disabled={drawPoints.length < 3}
                                        style={{flex: 1, background: '#2196F3'}}>Generate SQL
                                </button>
                                <button onClick={() => {
                                    setDrawPoints([]);
                                    setGeneratedCode('');
                                }} style={{background: '#555'}}>Clear
                                </button>
                            </div>
                            {generatedCode && <textarea value={generatedCode} readOnly style={{
                                width: '100%',
                                height: '100px',
                                fontSize: '0.75rem',
                                fontFamily: 'monospace'
                            }}/>}
                        </>
                    )}

                    {/* MARKER FILTERS */}
                    <div style={{
                        marginTop: '20px',
                        padding: '10px',
                        background: '#222',
                        color: 'white',
                        borderTop: '1px solid #444'
                    }}>
                        <h5>Marker Filters (Debug)</h5>
                        <div style={{display: 'flex', flexWrap: 'wrap', gap: '5px'}}>
                            {Array.from(new Set(gameMarkers.map(m => m.category))).map(cat => (
                                <label key={cat} style={{
                                    fontSize: '0.8em',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '5px',
                                    cursor: 'pointer'
                                }}>
                                    <input
                                        type="checkbox"
                                        checked={visibleCategories.has(cat)}
                                        onChange={e => {
                                            const newSet = new Set(visibleCategories);
                                            if (e.target.checked) newSet.add(cat);
                                            else newSet.delete(cat);
                                            setVisibleCategories(newSet);
                                        }}
                                    />
                                    {cat}
                                </label>
                            ))}
                        </div>
                    </div>
                </div>

                {/* MAP CANVAS */}
                <div style={{position: 'relative', background: '#000', height: '900px', width: '100%'}}>
                    {/* CALIBRATION POPUP */}
                    {showCalInput && (
                        <div style={{
                            position: 'absolute',
                            top: '50%',
                            left: '50%',
                            transform: 'translate(-50%, -50%)',
                            background: 'white',
                            color: 'black',
                            padding: '20px',
                            zIndex: 9999,
                            borderRadius: '8px',
                            boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
                        }}>
                            <h4>Enter API Coordinates</h4>
                            <div style={{display: 'flex', gap: '10px', marginBottom: '10px'}}>
                                {/* SWAPPED: Lat (Y) first, then Lng (X) */}
                                <input
                                    placeholder="API Lat (Y)"
                                    value={tempApiY}
                                    onChange={e => setTempApiY(e.target.value)}
                                    autoFocus
                                />
                                <input
                                    placeholder="API Lng (X)"
                                    value={tempApiX}
                                    onChange={e => setTempApiX(e.target.value)}
                                />
                            </div>
                            <button onClick={saveCalibrationPoint}>Confirm Point</button>
                            <button onClick={() => setShowCalInput(false)}
                                    style={{marginLeft: '10px', background: '#ccc'}}>Cancel
                            </button>
                        </div>
                    )}

                    {selectedMap ? (
                        <MapContainer center={[0, 0]} zoom={0} minZoom={-2} maxZoom={2} crs={L.CRS.Simple}
                                      bounds={bounds} style={{height: '100%', width: '100%'}}>
                            <ClickHandler onClick={onMapClick}/>
                            <ImageOverlay url={getMapImageUrl(selectedMap)} bounds={bounds}/>

                            {/* 1. Calibration Points */}
                            {isCalibrating && calPoints.map((p, i) => (
                                <CircleMarker key={`cal-${i}`} center={p.local} {...calibrationPointOptions}>
                                    <Popup>
                                        Calibration Point {i + 1}<br/>
                                        {i < 2 ? "(X-Axis)" : "(Y-Axis)"}
                                    </Popup>
                                </CircleMarker>
                            ))}

                            {/* 2. Existing Areas */}
                            {!isCalibrating && existingAreas.map(area => {
                                let positions = null;
                                if (area.coordinates) {
                                    try {
                                        positions = JSON.parse(area.coordinates);
                                    } catch (e) {
                                    }
                                }
                                if (!positions) return null;
                                return <Polygon key={area.id} positions={positions}
                                                pathOptions={existingAreaOptions}><Popup>{area.name}</Popup></Polygon>;
                            })}

                            {/* 3. Game Markers (Dots) */}
                            {!isCalibrating && gameMarkers
                                .filter(m => visibleCategories.has(m.category))
                                .map(marker => (
                                    <CircleMarker
                                        key={marker.id}
                                        center={transformMarker(marker, selectedMap)}
                                        {...markerOptions}
                                    >
                                        <Popup>
                                            <strong>{marker.name || marker.category}</strong><br/>
                                            <small>{marker.subcategory}</small><br/>
                                            <small
                                                style={{color: '#666'}}>API: {marker.lng.toFixed(0)}, {marker.lat.toFixed(0)}</small>
                                        </Popup>
                                    </CircleMarker>
                                ))
                            }

                            {/* 4. Current Drawing */}
                            {!isCalibrating && drawPoints.length > 0 && (
                                <>
                                    <Polyline positions={drawPoints} pathOptions={drawOptions} />
                                    {drawPoints.map((p, i) => <CircleMarker key={`draw-${i}`}
                                                                            center={p} {...drawingPointOptions} />)}
                                    {drawPoints.length > 2 &&
                                        <Polyline positions={[drawPoints[drawPoints.length - 1], drawPoints[0]]}
                                                  pathOptions={{...drawOptions, opacity: 0.4}}/>}
                                </>
                            )}
                        </MapContainer>
                    ) : (
                        <div style={{
                            color: 'white',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            height: '100%'
                        }}>Loading Maps...</div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MapEditor;