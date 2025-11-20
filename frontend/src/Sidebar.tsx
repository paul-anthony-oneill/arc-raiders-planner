import React from 'react';
import ItemIndex from './ItemIndex';
import type { Item } from './types';

interface SidebarProps {
    onAddToLoadout: (item: Item) => void;
    loadout: Item[];
    onRemoveFromLoadout: (index: number) => void;
    onCalculate: () => void;
    isCalculating: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({
    onAddToLoadout,
    loadout,
    onRemoveFromLoadout,
    onCalculate,
    isCalculating
}) => {
    return (
        <aside className="flex flex-col h-full border-r-2 border-retro-sand/20 bg-retro-dark/90 relative overflow-hidden">
            {/* CRT Overlay for Sidebar */}
            <div className="absolute inset-0 crt-overlay z-10 pointer-events-none"></div>

            {/* Header */}
            <div className="p-4 border-b border-retro-sand/20 z-20">
                <h2 className="text-xl font-display text-retro-orange text-glow uppercase tracking-widest">
                    Mission Control
                </h2>
                <div className="text-xs text-retro-sand-dim font-mono mt-1">
                    SYS.VER.2.0.4 // READY
                </div>
            </div>

            {/* Input Objectives (Search) */}
            <div className="flex-1 overflow-y-auto p-4 z-20 custom-scrollbar">
                <div className="mb-6">
                    <h3 className="text-sm text-retro-sand font-bold mb-2 uppercase tracking-wider">
                        {'>'} Input Objectives
                    </h3>
                    {/* Reusing ItemIndex but we might need to style it via CSS in index.css to fit better */}
                    <div className="opacity-90 transform scale-95 origin-top-left w-[105%]">
                        <ItemIndex onItemSelected={onAddToLoadout} />
                    </div>
                </div>
            </div>

            {/* Loadout List */}
            <div className="p-4 border-t border-retro-sand/20 bg-retro-black/50 z-20">
                <h3 className="text-sm text-retro-sand font-bold mb-3 uppercase tracking-wider flex justify-between items-center">
                    <span>{'>'} Loadout</span>
                    <span className="text-retro-orange">{loadout.length}/5</span>
                </h3>

                <div className="space-y-2 mb-4">
                    {loadout.length === 0 && (
                        <div className="text-xs text-retro-sand-dim italic text-center py-4 border border-dashed border-retro-sand-dim/30">
                            NO OBJECTIVES SELECTED
                        </div>
                    )}
                    {loadout.map((item, idx) => (
                        <div key={`${item.id}-${idx}`} className="flex items-center justify-between bg-retro-sand/10 border border-retro-sand/30 p-2 text-sm group hover:bg-retro-sand/20 transition-colors">
                            <span className="truncate text-retro-sand font-mono">{item.name}</span>
                            <button
                                onClick={() => onRemoveFromLoadout(idx)}
                                className="text-retro-red hover:text-retro-orange px-2 opacity-0 group-hover:opacity-100 transition-opacity"
                            >
                                [X]
                            </button>
                        </div>
                    ))}
                </div>

                {/* Calculate Button */}
                <button
                    onClick={onCalculate}
                    disabled={isCalculating || loadout.length === 0}
                    className={`
                        w-full py-4 font-display font-bold text-lg tracking-widest uppercase
                        border-2 transition-all duration-200 relative overflow-hidden group
                        ${isCalculating || loadout.length === 0
                            ? 'border-retro-sand-dim text-retro-sand-dim cursor-not-allowed opacity-50'
                            : 'border-retro-orange text-retro-black bg-retro-orange hover:bg-retro-orange-dim hover:border-retro-orange-dim box-glow'}
                    `}
                >
                    <span className="relative z-10">
                        {isCalculating ? 'CALCULATING...' : 'CALCULATE ROUTE'}
                    </span>
                </button>
            </div>
        </aside>
    );
};

export default Sidebar;
