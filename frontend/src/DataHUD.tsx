import React from 'react';

interface DataHUDProps {
    stats: {
        sectorName?: string;
        environment?: string;
        lootProbability?: { rare: number; epic: number };
        threatLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME';
    } | null;
}

const DataHUD: React.FC<DataHUDProps> = ({ stats }) => {
    return (
        <footer className="h-32 border-t-2 border-retro-sand/20 bg-retro-dark/95 relative overflow-hidden grid grid-cols-3 gap-4 p-4 z-30">
            {/* CRT Overlay */}
            <div className="absolute inset-0 crt-overlay pointer-events-none"></div>

            {/* Sector Analysis */}
            <div className="border-r border-retro-sand/10 pr-4 flex flex-col justify-center">
                <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-1">Sector Analysis</h4>
                <div className="text-xl font-display text-retro-sand text-glow">
                    {stats?.sectorName || 'AWAITING DATA...'}
                </div>
                <div className="text-sm text-retro-sand-dim font-mono">
                    ENV: {stats?.environment || '---'}
                </div>
            </div>

            {/* Loot Probability */}
            <div className="border-r border-retro-sand/10 px-4 flex flex-col justify-center">
                <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-2">Loot Probability</h4>
                <div className="flex items-center gap-4 text-sm font-mono">
                    <div className="flex-1">
                        <div className="flex justify-between mb-1">
                            <span className="text-retro-sand">RARE</span>
                            <span className="text-retro-sand">{stats?.lootProbability?.rare || 0}%</span>
                        </div>
                        <div className="h-2 bg-retro-black border border-retro-sand/30">
                            <div
                                className="h-full bg-retro-sand/60"
                                style={{ width: `${stats?.lootProbability?.rare || 0}%` }}
                            ></div>
                        </div>
                    </div>
                    <div className="flex-1">
                        <div className="flex justify-between mb-1">
                            <span className="text-retro-sand">EPIC</span>
                            <span className="text-retro-sand">{stats?.lootProbability?.epic || 0}%</span>
                        </div>
                        <div className="h-2 bg-retro-black border border-retro-sand/30">
                            <div
                                className="h-full bg-retro-orange"
                                style={{ width: `${stats?.lootProbability?.epic || 0}%` }}
                            ></div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Threat Level */}
            <div className="pl-4 flex flex-col justify-center">
                <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-1">Threat Level</h4>
                <div className={`text-3xl font-display font-bold ${stats?.threatLevel === 'HIGH' || stats?.threatLevel === 'EXTREME'
                        ? 'text-retro-red animate-pulse text-glow'
                        : 'text-retro-green'
                    }`}>
                    {stats?.threatLevel || '---'}
                </div>
            </div>
        </footer>
    );
};

export default DataHUD;
