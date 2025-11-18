import {useState} from 'react';
import ItemIndex from './ItemIndex';
import Planner from './Planner'; // We will create this component next
import type {Item} from './types';
import './App.css';

function App() {
    const [selectedItem, setSelectedItem] = useState<Item | null>(null);

    const handleBackToSearch = () => {
        setSelectedItem(null);
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>ARC Raiders Loot Planner</h1>
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