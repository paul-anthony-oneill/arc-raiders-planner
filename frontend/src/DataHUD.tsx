import React from "react";
import { RoutingProfile } from "./types";

interface DataHUDProps {
  stats: {
    sectorName?: string;
    environment?: string;
    lootProbability?: { rare: number; epic: number };
    threatLevel?: "LOW" | "MEDIUM" | "HIGH" | "EXTREME";
  } | null;
  // New Props
  activeProfile: RoutingProfile;
  hoveredProfile: RoutingProfile | null;
}

const MODE_DESCRIPTIONS: Record<RoutingProfile, string> = {
  [RoutingProfile.PURE_SCAVENGER]:
    "PROTOCOL: MAX_YIELD. Ignores distance constraints. Targets maximum loot density across all sectors.",
  [RoutingProfile.EASY_EXFIL]:
    "PROTOCOL: FAST_EXIT. Prioritizes loot clusters in close proximity to operational Raider Hatches.",
  [RoutingProfile.AVOID_PVP]:
    "PROTOCOL: STEALTH. Routes through low-traffic sectors. Avoids High-Tier zones. Penalizes open ground.",
  [RoutingProfile.SAFE_EXFIL]:
    "PROTOCOL: HYBRID. Balances stealth routing with hatch proximity. The recommended setting for solo operatives.",
};

const DataHUD: React.FC<DataHUDProps> = ({
  stats,
  activeProfile,
  hoveredProfile,
}) => {
  // Logic: Show hovered description if available, otherwise show active description
  const currentDescription = MODE_DESCRIPTIONS[hoveredProfile || activeProfile];
  const currentTitle = hoveredProfile ? "PREVIEWING MODE" : "ACTIVE MODE";

  return (
    <footer className="h-32 border-t-2 border-retro-sand/20 bg-retro-dark/95 relative overflow-hidden grid grid-cols-3 gap-4 p-4 z-30">
      <div className="absolute inset-0 crt-overlay pointer-events-none"></div>

      {/* Col 1: Sector Analysis (Standard) */}
      <div className="border-r border-retro-sand/10 pr-4 flex flex-col justify-center">
        <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-1">
          Sector Analysis
        </h4>
        <div className="text-xl font-display text-retro-sand text-glow">
          {stats?.sectorName || "AWAITING DATA..."}
        </div>
        <div className="text-sm text-retro-sand-dim font-mono">
          ENV: {stats?.environment || "---"}
        </div>
      </div>

      {/* Col 2: Mission Parameters (The New Dynamic Area) */}
      <div className="border-r border-retro-sand/10 px-4 flex flex-col justify-center relative">
        <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-2">
          {currentTitle} // {hoveredProfile || activeProfile}
        </h4>

        {/* The Description Text */}
        <div className="text-xs font-mono text-retro-sand leading-relaxed h-full flex items-center">
          <span className="typing-effect">{currentDescription}</span>
        </div>

        {/* Optional: Keep Loot Probability as a smaller element or hide it when hovering? 
                    For now, let's replace it with this text because explaining the mode is critical. 
                    Once a route is calculated (stats exist), we could toggle back.
                */}
      </div>

      {/* Col 3: Threat Level */}
      <div className="pl-4 flex flex-col justify-center">
        <h4 className="text-xs text-retro-orange uppercase tracking-widest mb-1">
          Threat Level
        </h4>
        <div
          className={`text-3xl font-display font-bold ${
            stats?.threatLevel === "HIGH" || stats?.threatLevel === "EXTREME"
              ? "text-retro-red animate-pulse text-glow"
              : "text-retro-green"
          }`}
        >
          {stats?.threatLevel || "---"}
        </div>
      </div>
    </footer>
  );
};

export default React.memo(DataHUD);
