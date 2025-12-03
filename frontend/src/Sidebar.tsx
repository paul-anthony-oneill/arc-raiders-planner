import React, { useState, useMemo } from "react";
import ItemIndex from "./ItemIndex";
import EnemyIndex from "./EnemyIndex";
import RecipeIndex from "./RecipeIndex";
import { RoutingProfile } from "./types";
import type { Item, EnemyType, Recipe } from "./types";

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

  // Routing Controls
  routingProfile: RoutingProfile;
  setRoutingProfile: (mode: RoutingProfile) => void;
  hasRaiderKey: boolean;
  setHasRaiderKey: (hasKey: boolean) => void;

  // Recipe Support
  recipes: Recipe[];
  recipeSelection: Record<number, "PRIORITY" | "ONGOING" | undefined>;
  onToggleRecipe: (recipeId: number) => void;
  collectedIngredients: Record<number, Record<string, number>>;
  onIngredientUpdate: (recipeId: number, ingredientName: string, delta: number) => void;
}

const RecipeTracker: React.FC<{
  recipe: Recipe;
  status: "PRIORITY" | "ONGOING";
  collected: Record<string, number>;
  onUpdate: (name: string, delta: number) => void;
}> = ({ recipe, status, collected, onUpdate }) => {
  const isPriority = status === "PRIORITY";
  const borderColor = isPriority ? "border-retro-orange" : "border-retro-blue";
  const textColor = isPriority ? "text-retro-orange" : "text-retro-blue";
  const bgColor = isPriority ? "bg-retro-orange/10" : "bg-retro-blue/10";

  return (
    <div className={`border ${borderColor} ${bgColor} mb-2 p-2`}>
      <div className={`text-xs font-bold mb-1 ${textColor} flex justify-between`}>
        <span className="truncate">{recipe.name}</span>
        <span className="text-[10px] uppercase">{status}</span>
      </div>
      <div className="space-y-1">
        {recipe.ingredients.map((ing) => {
          const current = collected[ing.itemName] || 0;
          const needed = ing.quantity;
          const isComplete = current >= needed;

          return (
            <div key={ing.itemName} className="flex items-center justify-between text-xs font-mono">
              <span className={`truncate flex-1 ${isComplete ? "text-retro-sand-dim line-through opacity-50" : "text-retro-sand"}`}>
                {ing.itemName}
              </span>
              <div className="flex items-center gap-1 bg-retro-black/50 px-1 border border-retro-sand/10">
                 <button 
                    onClick={() => onUpdate(ing.itemName, -1)}
                    disabled={current <= 0}
                    className="text-retro-red hover:text-white disabled:opacity-30 px-1 font-bold"
                 >-</button>
                 <span className={`w-6 text-center ${isComplete ? "text-green-400" : "text-retro-sand"}`}>
                    {current}/{needed}
                 </span>
                 <button 
                    onClick={() => onUpdate(ing.itemName, 1)}
                    disabled={current >= needed}
                    className="text-green-400 hover:text-white disabled:opacity-30 px-1 font-bold"
                 >+</button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const Sidebar: React.FC<SidebarProps> = ({
  onAddToLoadout,
  loadout,
  onRemoveFromLoadout,
  onCalculate,
  isCalculating,
  onAddEnemyType,
  selectedEnemyTypes,
  onRemoveEnemyType,
  routingProfile,
  setRoutingProfile,
  hasRaiderKey,
  setHasRaiderKey,
  recipes,
  recipeSelection,
  onToggleRecipe,
  collectedIngredients,
  onIngredientUpdate,
}) => {
  const [targetType, setTargetType] = useState<"items" | "enemies" | "recipes">("items");

  // Filter active recipes
  const activeRecipes = useMemo(() =>
    recipes.filter(r => recipeSelection[r.id!] !== undefined),
    [recipes, recipeSelection]
  );

  return (
    <aside className="flex flex-col h-full border-r-2 border-retro-sand/20 bg-retro-dark/90 relative overflow-hidden">
      {/* CRT Overlay for Sidebar */}
      <div className="absolute inset-0 crt-overlay z-10 pointer-events-none"></div>

      {/* Header */}
      <div className="p-4 border-b border-retro-sand/20 z-20">
        <div className="flex items-start justify-between mb-2">
          <h2 className="text-xl font-display text-retro-orange text-glow uppercase tracking-widest">
            Mission Control
          </h2>
          <a
            href="/setup"
            className="
              text-xs
              font-mono
              text-retro-sand-dim
              hover:text-retro-orange
              transition-colors
              border
              border-retro-sand/20
              hover:border-retro-orange/50
              px-2
              py-1
              whitespace-nowrap
            "
            onClick={(e) => {
              e.preventDefault()
              window.location.href = '/setup'
            }}
          >
            ← Setup
          </a>
        </div>
        <div className="text-xs text-retro-sand-dim font-mono">
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
              onClick={() => setTargetType("recipes")}
              className={`flex-1 py-2 px-3 text-xs font-mono uppercase tracking-wider transition-all ${
                targetType === "recipes"
                  ? "bg-retro-blue text-retro-black border border-retro-blue"
                  : "bg-retro-black/50 text-retro-sand-dim border border-retro-sand/20 hover:border-retro-blue/50"
              }`}
            >
              Recipes
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
              <RecipeIndex 
                recipes={recipes}
                selectionState={recipeSelection}
                onToggleRecipe={onToggleRecipe}
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
            {loadout.length + selectedEnemyTypes.length + activeRecipes.length}/10
          </span>
        </h3>

        <div className="space-y-2 mb-4 max-h-32 overflow-y-auto custom-scrollbar">
          {loadout.length === 0 && selectedEnemyTypes.length === 0 && activeRecipes.length === 0 && (
            <div className="text-xs text-retro-sand-dim italic text-center py-4 border border-dashed border-retro-sand-dim/30">
              NO OBJECTIVES SELECTED
            </div>
          )}
          
          {/* Recipe Trackers */}
          {activeRecipes.map(recipe => (
            <RecipeTracker 
                key={`recipe-${recipe.id}`}
                recipe={recipe}
                status={recipeSelection[recipe.id!]!}
                collected={collectedIngredients[recipe.id!] || {}}
                onUpdate={(name, delta) => onIngredientUpdate(recipe.id!, name, delta)}
            />
          ))}

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
                aria-label={`Remove ${item.name} from loadout`}
                className="text-retro-red hover:text-retro-orange px-2"
              >
                <span aria-hidden="true">✕</span>
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
                aria-label={`Remove ${enemyType} from targets`}
                className="text-retro-red hover:text-retro-orange px-2"
              >
                <span aria-hidden="true">✕</span>
              </button>
            </div>
          ))}
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
            (loadout.length === 0 && selectedEnemyTypes.length === 0 && activeRecipes.length === 0)
          }
          aria-label="Calculate optimal route"
          aria-busy={isCalculating}
          className={`
                        w-full py-4 font-display font-bold text-lg tracking-widest uppercase
                        border-2 transition-all duration-200 relative overflow-hidden group
                        ${
                          isCalculating ||
                          (loadout.length === 0 &&
                            selectedEnemyTypes.length === 0 &&
                            activeRecipes.length === 0)
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

export default React.memo(Sidebar);
