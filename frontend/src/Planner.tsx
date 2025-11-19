import React, {useEffect, useState} from 'react';
import type {Area, Item, MapRecommendation} from './types';
import MapComponent from './MapComponent';


const API_RECOMMENDATION_URL = '/api/items/recommendation';
const API_MAP_DATA_URL = '/api/maps'; // Base path for map data

interface PlannerProps {
    selectedItem: Item;
    onBack: () => void;
}

const Planner: React.FC<PlannerProps> = ({ selectedItem, onBack }) => {
    const [recommendations, setRecommendations] = useState<MapRecommendation[]>([]);
    const [mapData, setMapData] = useState<any | null>(null); // State to hold the full map data
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchPlannerData = async () => {
            setLoading(true);
            setError(null);
            setMapData(null); // Clear map data on new item selection

            if (!selectedItem.lootType) {
                setError(`${selectedItem.name} is only obtained via crafting or enemy drops.`);
                setLoading(false);
                return;
            }

            try {
                // 1. FETCH RECOMMENDATION
                const encodedName = encodeURIComponent(selectedItem.name);
                const recUrl = `${API_RECOMMENDATION_URL}?itemName=${encodedName}`;

                const recResponse = await fetch(recUrl);
                const recs: MapRecommendation[] = await recResponse.json();
                setRecommendations(recs);

                // 2. FETCH MAP COORDINATES (Only if recommendations exist)
                if (recs.length > 0) {
                    const recommendedMapName = recs[0].mapName;
                    const mapUrl = `${API_MAP_DATA_URL}/${encodeURIComponent(recommendedMapName)}/data`;

                    const mapResponse = await fetch(mapUrl);
                    const mapJson: { areas: Area[], name: string } = await mapResponse.json();
                    setMapData(mapJson);
                }

            } catch (err) {
                console.error('Planner fetch error:', err);
                setError(`Failed to process data: ${err instanceof Error ? err.message : 'Unknown error'}`);
            } finally {
                setLoading(false);
            }
        };

        fetchPlannerData();
    }, [selectedItem]);

    return (
        <div className="planner-container" style={{maxWidth: '1000px', margin: '0 auto', fontFamily: 'sans-serif'}}>
            <button onClick={onBack} style={{ marginBottom: '20px', padding: '10px', cursor: 'pointer' }}>
                &larr; Back to Item Search
            </button>

            <h2>Planning for: {selectedItem.name}</h2>
            <p>Required Loot Type: <strong>{selectedItem.lootType?.name || 'N/A'}</strong></p>

            <hr />

            {loading && <p>Calculating optimal maps and fetching coordinates...</p>}
            {error && <p style={{ color: 'red' }}>Error: {error}</p>}

            {!loading && !error && (
                <>
                    {recommendations.length > 0 && (
                        <div>
                            <h3>🗺️ Ranked Map Recommendations</h3>
                            <ol style={{paddingLeft: '20px'}}>
                                {recommendations.map((rec, index) => (
                                    <li key={rec.mapId} style={{
                                        marginBottom: '10px', padding: '10px',
                                        border: index === 0 ? '2px solid gold' : '1px solid #ccc',
                                        backgroundColor: index === 0 ? '#b5c26aff' : 'white',
                                        fontWeight: index === 0 ? 'bold' : 'normal'
                                    }}>
                                        # {index + 1}: **{rec.mapName}** (Found in {rec.matchingAreaCount} matching
                                        areas)
                                    </li>
                                ))}
                            </ol>
                        </div>
                    )}

                    {/* 2. RENDER THE MAP COMPONENT */}
                    {mapData && recommendations.length > 0 && (
                        <MapComponent
                            mapName={recommendations[0].mapName}
                            areas={mapData.areas}
                        />
                    )}
                </>
            )}
        </div>
    );
};

export default Planner;