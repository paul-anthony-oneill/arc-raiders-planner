import { useState } from 'react';
import Sidebar from './Sidebar';
import DataHUD from './DataHUD';
import MapComponent from './MapComponent';
import MapEditor from './MapEditor';
import type { Item, Area, MapRecommendation } from './types';
import './App.css';

const API_RECOMMENDATION_URL = '/api/items/recommendation';
const API_MAP_DATA_URL = '/api/maps';

function App() {
    // State
    const [loadout, setLoadout] = useState<Item[]>([]);
    const [isCalculating, setIsCalculating] = useState(false);
    const [mapData, setMapData] = useState<{ areas: Area[], name: string } | null>(null);
    const [stats, setStats] = useState<any | null>(null);
    const [showEditor, setShowEditor] = useState(false);
    const [accessibilityMode, setAccessibilityMode] = useState(false);

    // Handlers
    const handleAddToLoadout = (item: Item) => {
        if (loadout.length < 5) {
            setLoadout([...loadout, item]);
        }
    };

    const handleRemoveFromLoadout = (index: number) => {
        const newLoadout = [...loadout];
        newLoadout.splice(index, 1);
        setLoadout(newLoadout);
    };

    const toggleAccessibility = () => {
        setAccessibilityMode(!accessibilityMode);
        document.body.classList.toggle('accessibility-mode');
    };

    const handleCalculateRoute = async () => {
        if (loadout.length === 0) return;

        setIsCalculating(true);
        setStats(null); // Reset stats

        try {
            // For this demo, we use the first item to determine the map
            // In a real app, this would be a complex multi-objective optimization
            const primaryObjective = loadout[0];

            if (!primaryObjective.lootType) {
                // Fallback if no loot type (e.g. crafted item)
                // Just mock some data or show error
                console.warn("Item has no loot type");
            }

            // 1. Fetch Recommendation
            const encodedName = encodeURIComponent(primaryObjective.name);
            const recResponse = await fetch(`${API_RECOMMENDATION_URL}?itemName=${encodedName}`);
            if (!recResponse.ok) {
                throw new Error(`Failed to fetch recommendations: ${recResponse.status} ${recResponse.statusText}`);
            }
            const recs: MapRecommendation[] = await recResponse.json();

            if (recs.length > 0) {
                const recommendedMapName = recs[0].mapName;

                // 2. Fetch Map Data
                const mapResponse = await fetch(`${API_MAP_DATA_URL}/${encodeURIComponent(recommendedMapName)}/data`);
                const mapJson = await mapResponse.json();

                setMapData(mapJson);

                // 3. Generate Mock Stats based on result
                setStats({
                    sectorName: recommendedMapName,
                    environment: 'ARID CANYON', // Mock
                    lootProbability: {
                        rare: Math.floor(Math.random() * 30) + 20,
                        epic: Math.floor(Math.random() * 15) + 5
                    },
                    threatLevel: ['LOW', 'MEDIUM', 'HIGH', 'EXTREME'][Math.floor(Math.random() * 4)]
                });
            } else {
                alert("No suitable map found for this objective.");
            }

        } catch (error) {
            console.error("Calculation failed:", error);
            alert("Failed to calculate route. System Offline.");
        } finally {
            setIsCalculating(false);
        }
    };

    // Editor Mode
    if (showEditor) {
        return <MapEditor onExit={() => setShowEditor(false)} />;
    }

    return (
        <div className="fixed inset-0 w-full h-full bg-retro-bg overflow-hidden flex">
            {/* Global CRT Overlay (if not in accessibility mode) */}
            <div className="absolute inset-0 crt-overlay pointer-events-none z-50"></div>

            {/* Sidebar (Left) */}
            <div className="w-80 flex-shrink-0 h-full z-40">
                <Sidebar
                    loadout={loadout}
                    onAddToLoadout={handleAddToLoadout}
                    onRemoveFromLoadout={handleRemoveFromLoadout}
                    onCalculate={handleCalculateRoute}
                    isCalculating={isCalculating}
                />
            </div>

            {/* Main Content (Right) */}
            <div className="flex-1 flex flex-col h-full relative">
                {/* Top Bar / Header */}
                <header className="h-12 border-b border-retro-sand/20 bg-retro-dark flex items-center justify-between px-4 z-30">
                    <h1 className="text-retro-sand font-display text-lg tracking-widest">
                        TACTICAL MAP // <span className="text-retro-orange">{mapData ? mapData.name : 'NO SIGNAL'}</span>
                    </h1>
                    <div className="flex items-center gap-4">
                        <button
                            onClick={toggleAccessibility}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-sand border border-retro-sand/20 px-2 py-1"
                        >
                            {accessibilityMode ? '[A11Y: ON]' : '[A11Y: OFF]'}
                        </button>
                        <button
                            onClick={() => setShowEditor(true)}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-orange"
                        >
                            🔧 EDITOR
                        </button>
                    </div>
                </header>

                {/* Map Viewport */}
                <div className="flex-1 relative bg-retro-black overflow-hidden">
                    {mapData ? (
                        <div className="w-full h-full">
                            <MapComponent
                                mapName={mapData.name}
                                areas={mapData.areas}
                            />
                        </div>
                    ) : (
                        <div className="absolute inset-0 flex items-center justify-center text-retro-sand-dim/20 font-display text-4xl uppercase tracking-widest select-none">
                            Awaiting Coordinates
                        </div>
                    )}
                </div>

                {/* Bottom HUD */}
                <div className="h-32 flex-shrink-0 z-40">
                    <DataHUD stats={stats} />
                </div>
            </div>
        </div>
    );
}

export default App;