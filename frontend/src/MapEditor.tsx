import React, { useState, useEffect } from 'react';
import { MapContainer, ImageOverlay, Polyline, Marker, Polygon, Popup, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Area } from './types';

// --- TYPES ---
interface GameMap {
    id: number;
    name: string;
    description: string;
}

interface MapEditorProps {
    onExit: () => void;
}


const defaultIcon = L.icon({
    iconUrl: '/marker-icon.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: '/marker-shadow.png'
});

const drawOptions = { color: '#00ff00', dashArray: '5, 5' }; // Green dashed line for drawing
const existingAreaOptions = { color: 'gray', fillColor: 'gray', fillOpacity: 0.9, weight: 1 }; // Faded gray for context

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

    // Draw State
    const [drawPoints, setDrawPoints] = useState<L.LatLng[]>([]);

    // Form State
    const [areaName, setAreaName] = useState('');
    const [lootAbundance, setLootAbundance] = useState<number>(2);

    // Output
    const [generatedCode, setGeneratedCode] = useState('');

    // --- INITIAL LOAD ---
    useEffect(() => {
        const loadMapsAndAreas = async () => {
            try {
                // 1. Fetch Maps List
                const res = await fetch('/api/maps');
                if (!res.ok) throw new Error("Failed to fetch map list");
                const mapData: GameMap[] = await res.json();

                setMaps(mapData);

                if (mapData.length > 0) {
                    const defaultMap = mapData[0];
                    setSelectedMap(defaultMap);

                    // 2. Fetch Existing Areas for the default map
                    const areaRes = await fetch(`/api/maps/${encodeURIComponent(defaultMap.name)}/data`);
                    if (!areaRes.ok) throw new Error("Failed to fetch area data");

                    const areaJson: { areas: Area[] } = await areaRes.json();
                    setExistingAreas(areaJson.areas || []);
                }
            } catch (err) {
                console.error("Initialization error:", err);
            }
        };

        loadMapsAndAreas();
    }, []);

    // --- HANDLERS ---

    const handleMapSelect = async (mapName: string) => {
        const map = maps.find(m => m.name === mapName);
        if (map) {
            setSelectedMap(map);
            setDrawPoints([]); // Clear drawing
            setGeneratedCode('');

            // Refetch areas for the newly selected map
            try {
                const mapUrl = `/api/maps/${encodeURIComponent(mapName)}/data`;
                console.log("DEBUG: Fetching existing areas from:", mapUrl); // LOG 1

                const areaRes = await fetch(mapUrl);
                if (!areaRes.ok) throw new Error("Failed to fetch area data");

                const areaJson: { areas: Area[] } = await areaRes.json();
                setExistingAreas(areaJson.areas || []);
                console.log("DEBUG: Areas received:", areaJson.areas.length); // LOG 2
            } catch (err) {
                console.error(`Error loading areas for ${mapName}`, err);
                setExistingAreas([]);
            }
        }
    };

    const handleMapClick = (latlng: L.LatLng) => {
        setDrawPoints(prev => [...prev, latlng]);
    };

    const calculateCenter = (points: L.LatLng[]): { x: number, y: number } => {
        if (points.length === 0) return { x: 0, y: 0 };

        let totalLat = 0;
        let totalLng = 0;

        points.forEach(p => {
            totalLat += p.lat;
            totalLng += p.lng;
        });

        // Average coordinates
        const avgLat = totalLat / points.length;
        const avgLng = totalLng / points.length;

        // Convert to Map Integers (Rounding)
        // Remember: In our setup, Leaflet Lat = Y, Leaflet Lng = X
        return {
            x: Math.round(avgLng),
            y: Math.round(avgLat)
        };
    };

    const handleGenerate = () => {
        if (drawPoints.length < 3) return alert("Polygon needs at least 3 points.");
        if (!selectedMap) return;
        if (!areaName) return alert("Please enter an Area Name.");

        // 1. Calculate Center
        const center = calculateCenter(drawPoints);

        // 2. Format Coordinates JSON
        // We map points to [lat, lng] arrays
        const coordString = JSON.stringify(drawPoints.map(p => [p.lat, p.lng]));

        // 3. Construct the SQL Tuple
        // Format: ('Name', mapX, mapY, 'JSON', abundance, 'MapName')
        // We escape single quotes in the name just in case
        const safeName = areaName.replace(/'/g, "''");

        const sqlTuple = `('${safeName}', ${center.x}, ${center.y}, '${coordString}', ${lootAbundance}, '${selectedMap.name}'),`;

        setGeneratedCode(sqlTuple);
    };

    const getMapImageUrl = (map: GameMap) => {
        // Convert "Dam Battlegrounds" -> "dam_battlegrounds.png"
        // Or use the 'description' field if you stored the code "dam" there:
        // return `/maps/${map.description}_battlegrounds.png`;

        // Assuming file naming convention: lowercase with underscores
        const filename = map.name.toLowerCase().replace(/ /g, '_');
        return `/maps/dam_battlegrounds.png`;
    };

    return (
        <div style={{ display: 'flex', height: '100vh', flexDirection: 'column' }}>
            {/* TOOLBAR */}
            <div style={{ padding: '10px', background: '#222', color: 'white', display: 'flex', gap: '20px', alignItems: 'center', width: '100vw' }}>
                <h3>🛠️ Map Editor</h3>

                <select
                    value={selectedMap?.name || ''}
                    onChange={e => handleMapSelect(e.target.value)}
                    style={{ padding: '5px' }}
                >
                    {maps.map(m => <option key={m.id} value={m.name}>{m.name}</option>)}
                </select>

                <button onClick={onExit} style={{ marginLeft: 'auto', background: '#d32f2f' }}>Exit</button>
            </div>

            {/* MAIN CONTENT */}
            <div style={{ display: 'grid', gridTemplateColumns: '350px 1fr', flexGrow: 1, overflow: 'hidden' }}>

                {/* SIDEBAR CONTROLS */}
                <div style={{ padding: '20px', background: '#1a1a1a', color: '#eee', overflowY: 'auto', borderRight: '1px solid #444' }}>
                    <h4>New Area Details</h4>

                    <div style={{ marginBottom: '15px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9em' }}>Area Name</label>
                        <input
                            type="text"
                            value={areaName}
                            onChange={e => setAreaName(e.target.value)}
                            placeholder="e.g. Storage Facility"
                            style={{ width: '100%', padding: '8px' }}
                        />
                    </div>

                    <div style={{ marginBottom: '20px' }}>
                        <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9em' }}>Loot Abundance (1-3)</label>
                        <select
                            value={lootAbundance}
                            onChange={e => setLootAbundance(Number(e.target.value))}
                            style={{ width: '100%', padding: '8px' }}
                        >
                            <option value={1}>1 - Low</option>
                            <option value={2}>2 - Medium</option>
                            <option value={3}>3 - High</option>
                        </select>
                    </div>

                    <div style={{ marginBottom: '20px', padding: '10px', background: '#333', borderRadius: '5px' }}>
                        <div style={{ fontSize: '0.8em', color: '#aaa' }}>Points Drawn: {drawPoints.length}</div>
                        <div style={{ fontSize: '0.8em', color: '#aaa' }}>
                            Center: {drawPoints.length > 0 ?
                                `${calculateCenter(drawPoints).x}, ${calculateCenter(drawPoints).y}` : 'N/A'}
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                        <button
                            onClick={handleGenerate}
                            disabled={drawPoints.length < 3}
                            style={{ flex: 1, background: '#2196F3' }}
                        >
                            Generate SQL
                        </button>
                        <button
                            onClick={() => { setDrawPoints([]); setGeneratedCode(''); }}
                            style={{ background: '#555' }}
                        >
                            Clear
                        </button>
                    </div>

                    {generatedCode && (
                        <div>
                            <label style={{ fontSize: '0.9em', color: '#4CAF50' }}>Ready for Flyway V1:</label>
                            <textarea
                                value={generatedCode}
                                readOnly
                                style={{ width: '100%', height: '150px', fontSize: '0.75rem', fontFamily: 'monospace', marginTop: '5px', padding: '5px' }}
                            />
                            <button
                                onClick={() => navigator.clipboard.writeText(generatedCode)}
                                style={{ width: '100%', marginTop: '5px' }}
                            >
                                Copy to Clipboard
                            </button>
                        </div>
                    )}
                </div>

                {/* MAP CANVAS */}
                <div style={{ position: 'relative', background: '#000', height: '700px', width: '100%', overflow: 'hidden' }}>
                    {selectedMap ? (
                        <MapContainer
                            center={[0, 0]}
                            zoom={0}
                            minZoom={-2}
                            maxZoom={2}
                            crs={L.CRS.Simple}
                            bounds={bounds}
                            style={{ height: '100%', width: '100%' }}
                        >
                            <ClickHandler onClick={handleMapClick} />

                            <ImageOverlay
                                url="/maps/dam_battlegrounds.png"
                                bounds={bounds}
                            />

                            {/* 1. Render Existing Areas (Context) */}
                            {existingAreas.map(area => {
                                let positions = null;
                                if (area.coordinates) {
                                    try {
                                        positions = JSON.parse(area.coordinates);
                                        console.log(`DEBUG: Successfully parsed coordinates for ${area.name}`); // LOG 3
                                    } catch (e) {
                                        console.error(`ERROR: Failed to parse coordinates for ${area.name}:`, area.coordinates); // LOG 4
                                    }
                                }

                                // Final check before rendering
                                if (!positions || positions.length < 3) {
                                    console.log(`DEBUG: Skipping ${area.name} (No valid polygon data). Coords exist: ${!!area.coordinates}`); // LOG 5
                                    return null;
                                }

                                return (
                                    <Polygon
                                        key={area.id}
                                        positions={positions}
                                        pathOptions={existingAreaOptions}
                                    >
                                        <Popup>{area.name} (Existing)</Popup>
                                    </Polygon>
                                );
                            })}

                            {/* 2. Render Current Drawing */}
                            {drawPoints.length > 0 && (
                                <>
                                    <Polyline positions={drawPoints} pathOptions={drawOptions} />
                                    {drawPoints.map((p, i) => (
                                        <Marker key={i} position={p} icon={defaultIcon} opacity={0.6} />
                                    ))}
                                    {/* Close the loop visually */}
                                    {drawPoints.length > 2 && (
                                        <Polyline
                                            positions={[drawPoints[drawPoints.length - 1], drawPoints[0]]}
                                            pathOptions={{ ...drawOptions, opacity: 0.4 }}
                                        />
                                    )}
                                </>
                            )}
                        </MapContainer>
                    ) : (
                        <div style={{ color: 'white', display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                            Loading Maps...
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MapEditor;