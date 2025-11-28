import React from "react";
import { RecipeType } from "./types";
import type { Recipe } from "./types";


interface RecipeIndexProps {
  recipes: Recipe[];
  selectionState: Record<number, "PRIORITY" | "ONGOING" | undefined>;
  onToggleRecipe: (recipeId: number) => void;
}

const RecipeIndex: React.FC<RecipeIndexProps> = ({
  recipes,
  selectionState,
  onToggleRecipe,
}) => {
  const getStatusColor = (status: "PRIORITY" | "ONGOING" | undefined) => {
    switch (status) {
      case "PRIORITY":
        return "bg-retro-orange/20 border-retro-orange text-retro-sand";
      case "ONGOING":
        return "bg-retro-blue/20 border-retro-blue text-retro-sand";
      default:
        return "bg-retro-black/50 border-retro-sand/20 text-retro-sand-dim hover:border-retro-sand/50";
    }
  };

  const getStatusLabel = (status: "PRIORITY" | "ONGOING" | undefined) => {
    switch (status) {
      case "PRIORITY":
        return "[!] PRIORITY";
      case "ONGOING":
        return "[~] ONGOING";
      default:
        return "";
    }
  };

  // Group by type
  const craftingRecipes = recipes.filter(r => r.type === RecipeType.CRAFTING);
  const upgrades = recipes.filter(r => r.type === RecipeType.WORKBENCH_UPGRADE);

  const renderList = (list: Recipe[], title: string) => (
    <div className="mb-4">
      <h4 className="text-xs text-retro-sand font-bold mb-2 uppercase tracking-wider opacity-80 border-b border-retro-sand/10 pb-1">
        {title}
      </h4>
      <div className="space-y-2">
        {list.map((recipe) => {
          const status = selectionState[recipe.id!];
          return (
            <div
              key={recipe.id}
              onClick={() => onToggleRecipe(recipe.id!)}
              className={`
                p-2 border text-sm cursor-pointer transition-all group relative
                ${getStatusColor(status)}
              `}
            >
              <div className="flex justify-between items-start">
                <span className="font-mono font-bold truncate pr-2">
                  {recipe.name}
                </span>
                <span className={`text-[10px] font-mono uppercase tracking-tight ${status === 'ONGOING' ? 'text-retro-blue' : 'text-retro-orange'}`}>
                  {getStatusLabel(status)}
                </span>
              </div>
              
              <div className="mt-1 text-[10px] text-retro-sand-dim font-mono opacity-80">
                 Ingredients: {recipe.ingredients.map(i => `${i.quantity}x ${i.itemName}`).join(", ")}
              </div>
              
              {/* Hover Overlay for interaction hint */}
              <div className="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
            </div>
          );
        })}
        {list.length === 0 && (
            <div className="text-xs text-retro-sand-dim italic p-2">No {title.toLowerCase()} found.</div>
        )}
      </div>
    </div>
  );

  return (
    <div className="mt-2">
      {renderList(craftingRecipes, "Crafting Recipes")}
      {renderList(upgrades, "Workbench Upgrades")}
    </div>
  );
};

export default RecipeIndex;
