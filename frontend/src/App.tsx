import {useState} from 'react';
import ItemIndex from './ItemIndex';
import Planner from './Planner';
import MapEditor from './MapEditor';
import type {Item} from './types';
import './App.css';

function App() {
    const [selectedItem, setSelectedItem] = useState<Item | null>(null);
    // New state for the editor
    const [showEditor, setShowEditor] = useState(false);

    const handleBackToSearch = () => {
        setSelectedItem(null);
    };

    // If Editor mode is active, render ONLY the editor
    if (showEditor) {
        return <MapEditor mapName="Dam Battlegrounds" onExit={() => setShowEditor(false)}/>;
    }

    return (
        <div className="App">
            <header className="App-header" style={{position: 'relative'}}>
                <h1>ARC Raiders Loot Planner</h1>

                {/* Secret/Dev Button in the top right corner */}
                <button
                    onClick={() => setShowEditor(true)}
                    style={{
                        position: 'absolute',
                        top: '20px',
                        right: '20px',
                        fontSize: '0.8rem',
                        padding: '5px 10px',
                        opacity: 0.6
                    }}
                >
                    🔧 Map Editor
                </button>
            </header>

            <main>
                {selectedItem ? (
                    <Planner
                        selectedItem={selectedItem}
                        onBack={handleBackToSearch}
                    />
                ) : (
                    <ItemIndex
                        onItemSelected={setSelectedItem}
                    />
                )}
            </main>
        </div>
    );
}

export default App;