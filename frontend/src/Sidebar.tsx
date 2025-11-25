import React, { useState } from "react";
import ItemIndex from "./ItemIndex";
import EnemyIndex from "./EnemyIndex";
import { RoutingProfile } from "./types";
import type { Item, EnemyType, Quest } from "./types";

interface SidebarProps {
  onAddToLoadout: (item: Item) => void;
  loadout: Item[];
  onRemoveFromLoadout: (index: number) => void;
  onCalculate: () => void;
  isCalculating: boolean;

  // Enemy type support
  onAddEnemyType: (enemyType: EnemyType) => void;
  selectedEnemyTypes: EnemyType[];
  onRemoveEnemyType: (index: number) => void;

  // Quest support
  quests: Quest[];
  selectedQuestIds: string[];
  onSelectQuest: (questId: string) => void;
  onDeselectQuest: (questId: string) => void;

  // Routing Controls
  routingProfile: RoutingProfile;
  setRoutingProfile: (mode: RoutingProfile) => void;
  hasRaiderKey: boolean;
  setHasRaiderKey: (hasKey: boolean) => void;
}

const Sidebar: React.FC<SidebarProps> = ({
  onAddToLoadout,
  loadout,
  onRemoveFromLoadout,
  onCalculate,
  isCalculating,
  onAddEnemyType,
  selectedEnemyTypes,
  onRemoveEnemyType,
  quests,
  selectedQuestIds,
  onSelectQuest,
  onDeselectQuest,
  routingProfile,
  setRoutingProfile,
  hasRaiderKey,
  setHasRaiderKey,
}) => {
  const [targetType, setTargetType] = useState<"items" | "enemies" | "quests">("items");
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
          SYS.VER.2.1.0 // TARGETING
        </div>
      </div>

      {/* Input Objectives (Search) */}
      <div className="flex-1 overflow-y-auto p-4 z-20 custom-scrollbar">
        <div className="mb-6">
          <h3 className="text-sm text-retro-sand font-bold mb-2 uppercase tracking-wider">
            {">"} Input Objectives
          </h3>

          {/* Tab Switcher */}
          <div className="flex gap-2 mb-4">
            <button
              onClick={() => setTargetType("items")}
              className={`flex-1 py-2 px-3 text-xs font-mono uppercase tracking-wider transition-all ${
                targetType === "items"
                  ? "bg-retro-orange text-retro-black border border-retro-orange"
                  : "bg-retro-black/50 text-retro-sand-dim border border-retro-sand/20 hover:border-retro-orange/50"
              }`}
            >
              Loot Items
            </button>
            <button
              onClick={() => setTargetType("enemies")}
              className={`flex-1 py-2 px-3 text-xs font-mono uppercase tracking-wider transition-all ${
                targetType === "enemies"
                  ? "bg-retro-red text-retro-black border border-retro-red"
                  : "bg-retro-black/50 text-retro-sand-dim border border-retro-sand/20 hover:border-retro-red/50"
              }`}
            >
              ARC Enemies
            </button>
            <button
              onClick={() => setTargetType("quests")}
              className={`flex-1 py-2 px-3 text-xs font-mono uppercase tracking-wider transition-all ${
                targetType === "quests"
                  ? "bg-retro-blue text-retro-black border border-retro-blue"
                  : "bg-retro-black/50 text-retro-sand-dim border border-retro-sand/20 hover:border-retro-blue/50"
              }`}
            >
              Quests
            </button>
          </div>

          {/* Conditional Rendering */}
          <div className="opacity-90 transform scale-95 origin-top-left w-[105%]">
            {targetType === "items" ? (
              <ItemIndex onItemSelected={onAddToLoadout} />
            ) : targetType === "enemies" ? (
              <EnemyIndex
                onEnemyTypeSelected={onAddEnemyType}
                selectedEnemyTypes={selectedEnemyTypes}
              />
            ) : (
              <QuestSelector
                quests={quests}
                selectedQuestIds={selectedQuestIds}
                onSelectQuest={onSelectQuest}
                onDeselectQuest={onDeselectQuest}
              />
            )}
          </div>
        </div>
      </div>

      {/* Loadout & Controls */}
      <div className="p-4 border-t border-retro-sand/20 bg-retro-black/50 z-20">
        {/* Loadout List */}
        <h3 className="text-sm text-retro-sand font-bold mb-3 uppercase tracking-wider flex justify-between items-center">
          <span>{">"} Targets</span>
          <span className="text-retro-orange">
            {loadout.length + selectedEnemyTypes.length + selectedQuestIds.length}/15
          </span>
        </h3>

        <div className="space-y-2 mb-4 max-h-32 overflow-y-auto custom-scrollbar">
          {loadout.length === 0 && selectedEnemyTypes.length === 0 && selectedQuestIds.length === 0 && (
            <div className="text-xs text-retro-sand-dim italic text-center py-4 border border-dashed border-retro-sand-dim/30">
              NO OBJECTIVES SELECTED
            </div>
          )}
          {loadout.map((item, idx) => (
            <div
              key={`item-${item.id}-${idx}`}
              className="flex items-center justify-between bg-retro-orange/10 border border-retro-orange/30 p-2 text-sm group hover:bg-retro-orange/20 transition-colors"
            >
              <span className="truncate text-retro-sand font-mono">
                {item.name}
              </span>
              <button
                onClick={() => onRemoveFromLoadout(idx)}
                className="text-retro-red hover:text-retro-orange px-2 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                [X]
              </button>
            </div>
          ))}
          {selectedEnemyTypes.map((enemyType, idx) => (
            <div
              key={`enemy-${enemyType}-${idx}`}
              className="flex items-center justify-between bg-retro-red/10 border border-retro-red/30 p-2 text-sm group hover:bg-retro-red/20 transition-colors"
            >
              <span className="truncate text-retro-sand font-mono">
                ⚡ {enemyType.charAt(0).toUpperCase() + enemyType.slice(1)}
              </span>
              <button
                onClick={() => onRemoveEnemyType(idx)}
                className="text-retro-red hover:text-retro-orange px-2 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                [X]
              </button>
            </div>
          ))}
          {selectedQuestIds.map((questId, idx) => {
            const quest = quests.find(q => q.id === questId);
            return (
              <div
                key={`quest-${questId}-${idx}`}
                className="flex items-center justify-between bg-retro-blue/10 border border-retro-blue/30 p-2 text-sm group hover:bg-retro-blue/20 transition-colors"
              >
                <span className="truncate text-retro-sand font-mono">
                  ? {quest ? quest.name : questId}
                </span>
                <button
                  onClick={() => onDeselectQuest(questId)}
                  className="text-retro-red hover:text-retro-orange px-2 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  [X]
                </button>
              </div>
            )
          })}
        </div>

        {/* --- NEW: Routing Configuration --- */}
        <div className="mb-4 border-t border-retro-sand/20 pt-4">
          <h3 className="text-xs text-retro-sand font-bold mb-2 uppercase tracking-wider">
            {">"} Operational Mode
          </h3>

          <div className="space-y-2 text-sm font-mono text-retro-sand-dim">
            <label className="flex items-center gap-2 cursor-pointer hover:text-retro-sand">
              <input
                type="radio"
                name="routing"
                checked={routingProfile === RoutingProfile.PURE_SCAVENGER}
                onChange={() =>
                  setRoutingProfile(RoutingProfile.PURE_SCAVENGER)
                }
                className="accent-retro-orange"
              />
              Scavenger (Max Loot)
            </label>

            <label className="flex items-center gap-2 cursor-pointer hover:text-retro-sand">
              <input
                type="radio"
                name="routing"
                checked={routingProfile === RoutingProfile.AVOID_PVP}
                onChange={() => setRoutingProfile(RoutingProfile.AVOID_PVP)}
                className="accent-retro-orange"
              />
              Stealth (Avoid PvP)
            </label>

            <label className="flex items-center gap-2 cursor-pointer hover:text-retro-sand">
              <input
                type="radio"
                name="routing"
                checked={routingProfile === RoutingProfile.EASY_EXFIL}
                onChange={() => setRoutingProfile(RoutingProfile.EASY_EXFIL)}
                className="accent-retro-orange"
              />
              Exfil Priority
            </label>

            <label className="flex items-center gap-2 cursor-pointer hover:text-retro-sand">
              <input
                type="radio"
                name="routing"
                checked={routingProfile === RoutingProfile.SAFE_EXFIL}
                onChange={() => setRoutingProfile(RoutingProfile.SAFE_EXFIL)}
                className="accent-retro-orange"
              />
              Safe Extraction (Mixed)
            </label>
          </div>

          {/* Raider Key Toggle */}
          <div className="mt-3 pt-2 border-t border-dashed border-retro-sand/20">
            <label className="flex items-center gap-2 cursor-pointer text-xs text-retro-orange hover:text-retro-sand transition-colors">
              <input
                type="checkbox"
                checked={hasRaiderKey}
                onChange={(e) => setHasRaiderKey(e.target.checked)}
                className="accent-retro-orange"
              />
              [KEYCARD] Raider Hatch Access
            </label>
          </div>
        </div>

        {/* Calculate Button */}
        <button
          onClick={onCalculate}
          disabled={
            isCalculating ||
            (loadout.length === 0 && selectedEnemyTypes.length === 0)
          }
          className={`
                        w-full py-4 font-display font-bold text-lg tracking-widest uppercase
                        border-2 transition-all duration-200 relative overflow-hidden group
                        ${
                          isCalculating ||
                          (loadout.length === 0 &&
                            selectedEnemyTypes.length === 0)
                            ? "border-retro-sand-dim text-retro-sand-dim cursor-not-allowed opacity-50"
                            : "border-retro-orange text-retro-black bg-retro-orange hover:bg-retro-orange-dim hover:border-retro-orange-dim box-glow"
                        }
                    `}
        >
          <span className="relative z-10">
            {isCalculating ? "CALCULATING..." : "CALCULATE ROUTE"}
          </span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
