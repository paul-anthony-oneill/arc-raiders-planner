import React, {useState} from 'react';
import {ImageOverlay, MapContainer, Marker, Polyline, useMapEvents} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Custom icon setup
const defaultIcon = L.icon({
    iconUrl: '/marker-icon.png', // Ensure path is absolute or correct relative to public
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: '/marker-shadow.png'
});

const drawOptions = {color: '#00ff00', dashArray: '5, 5'};

interface MapEditorProps {
    mapName: string; // We can pass "Dam Battlegrounds" etc.
    onExit: () => void;
}

// Helper to capture clicks
const ClickHandler: React.FC<{ onClick: (latlng: L.LatLng) => void }> = ({onClick}) => {
    useMapEvents({
        click(e) {
            onClick(e.latlng);
        },
    });
    return null;
};

const MapEditor: React.FC<MapEditorProps> = ({mapName, onExit}) => {
    // Fixed bounds for our flat map
    const bounds: L.LatLngBoundsLiteral = [[-1000, -1000], [1000, 1000]];

    const [drawPoints, setDrawPoints] = useState<L.LatLng[]>([]);
    const [generatedCode, setGeneratedCode] = useState<string>('');

    const handleMapClick = (latlng: L.LatLng) => {
        setDrawPoints(prev => [...prev, latlng]);
    };

    const handleFinish = () => {
        if (drawPoints.length < 3) return alert("Need at least 3 points!");

        const formattedPoints = drawPoints
            .map(p => `    [${p.lat.toFixed(6)}, ${p.lng.toFixed(6)}]`)
            .join(',\n');

        setGeneratedCode(`const NewArea: L.LatLngExpression[] = [\n${formattedPoints}\n];`);
    };

    return (
        <div style={{padding: '20px', maxWidth: '1200px', margin: '0 auto'}}>
            <div style={{marginBottom: '15px', display: 'flex', justifyContent: 'space-between'}}>
                <h2>🛠️ Map Editor: {mapName}</h2>
                <button onClick={onExit} style={{backgroundColor: '#666'}}>Exit Editor</button>
            </div>

            <div style={{display: 'grid', gridTemplateColumns: '3fr 1fr', gap: '20px'}}>

                {/* LEFT: The Map */}
                <div style={{height: '700px', border: '2px solid #4CAF50'}}>
                    <MapContainer
                        center={[0, 0]}
                        zoom={1}
                        minZoom={-1}
                        maxZoom={2}
                        crs={L.CRS.Simple}
                        bounds={bounds}
                        style={{height: '100%', width: '100%'}}
                    >
                        <ClickHandler onClick={handleMapClick}/>

                        <ImageOverlay
                            url="/maps/dam_battlegrounds.png"
                            bounds={bounds}
                        />

                        {/* Drawing Preview */}
                        {drawPoints.length > 0 && (
                            <>
                                <Polyline positions={drawPoints} pathOptions={drawOptions}/>
                                {drawPoints.map((pos, i) => (
                                    <Marker key={i} position={pos} icon={defaultIcon} opacity={0.7}/>
                                ))}
                                {/* Closing line preview */}
                                {drawPoints.length > 2 && (
                                    <Polyline
                                        positions={[drawPoints[drawPoints.length - 1], drawPoints[0]]}
                                        pathOptions={{...drawOptions, opacity: 0.3}}
                                    />
                                )}
                            </>
                        )}
                    </MapContainer>
                </div>

                {/* RIGHT: Controls */}
                <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                    <div style={{padding: '15px', background: '#333', color: 'white', borderRadius: '8px'}}>
                        <h4>Controls</h4>
                        <p>Points placed: <strong>{drawPoints.length}</strong></p>
                        <button onClick={handleFinish} disabled={drawPoints.length < 3}
                                style={{width: '100%', marginBottom: '10px'}}>
                            Generate Code
                        </button>
                        <button onClick={() => {
                            setDrawPoints([]);
                            setGeneratedCode('')
                        }} style={{width: '100%', backgroundColor: '#d32f2f'}}>
                            Clear Points
                        </button>
                    </div>

                    {generatedCode && (
                        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1}}>
                            <textarea
                                value={generatedCode}
                                readOnly
                                style={{width: '100%', flexGrow: 1, fontFamily: 'monospace', padding: '10px'}}
                            />
                            <button
                                onClick={() => navigator.clipboard.writeText(generatedCode)}
                                style={{marginTop: '10px', backgroundColor: '#2196F3'}}
                            >
                                Copy to Clipboard
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MapEditor;