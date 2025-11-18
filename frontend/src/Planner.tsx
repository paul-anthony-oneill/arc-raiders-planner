import React, { useState, useEffect } from 'react';
import type {Item, MapRecommendation} from './types';

const API_RECOMMENDATION_URL = '/api/items/recommendation';

interface PlannerProps {
    selectedItem: Item;
    onBack: () => void;
}

const Planner: React.FC<PlannerProps> = ({ selectedItem, onBack }) => {
    const [recommendations, setRecommendations] = useState<MapRecommendation[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchRecommendation = async () => {
            setLoading(true);
            setError(null);

            // Items that are only crafted or enemy drops cannot be planned
            if (!selectedItem.lootType) {
                setError(`${selectedItem.name} is only obtained via crafting or enemy drops and cannot be tracked to a map area.`);
                setLoading(false);
                return;
            }

            try {
                const encodedName = encodeURIComponent(selectedItem.name);
                const url = `${API_RECOMMENDATION_URL}?itemName=${encodedName}`;

                const response = await fetch(url);
                const data: MapRecommendation[] = await response.json();
                setRecommendations(data);
            } catch (err) {
                console.error('Planner fetch error:', err);
                setError(`Failed to get recommendation: ${err instanceof Error ? err.message : 'Unknown error'}`);
            } finally {
                setLoading(false);
            }
        };

        fetchRecommendation()
    }, [selectedItem]);

    return (
        <div className="planner-container" style={{ maxWidth: '800px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <button onClick={onBack} style={{ marginBottom: '20px', padding: '10px', cursor: 'pointer' }}>
                &larr; Back to Item Search
            </button>

            <h2>Planning for: {selectedItem.name}</h2>
            <p>Required Loot Type: <strong>{selectedItem.lootType?.name || 'N/A'}</strong></p>

            <hr />

            {loading && <p>Calculating optimal maps...</p>}
            {error && <p style={{ color: 'red' }}>Error: {error}</p>}

            {!loading && !error && (
                recommendations.length > 0 ? (
                    <div>
                        <h3>🗺️ Ranked Map Recommendations</h3>
                        <ol style={{ paddingLeft: '20px' }}>
                            {recommendations.map((rec, index) => (
                                <li key={rec.mapId} style={{
                                    marginBottom: '10px',
                                    padding: '10px',
                                    border: index === 0 ? '2px solid gold' : '1px solid #ccc',
                                    backgroundColor: index === 0 ? 'gray' : 'white',
                                    fontWeight: index === 0 ? 'bold' : 'normal'
                                }}>
                                    # {index + 1}: **{rec.mapName}** (Found in {rec.matchingAreaCount} matching areas)
                                </li>
                            ))}
                        </ol>
                    </div>
                ) : (
                    <p>No map areas defined for the "{selectedItem.lootType?.name}" loot type.</p>
                )
            )}
        </div>
    );
};

export default Planner;