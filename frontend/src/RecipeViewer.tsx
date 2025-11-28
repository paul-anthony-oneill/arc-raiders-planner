import React, { useState, useEffect, useMemo } from 'react';
import { RecipeType } from './types';
import type { Recipe } from './types';
import { recipeApi } from './api/recipeApi';

interface RecipeViewerProps {
    onExit: () => void;
}

const RecipeViewer: React.FC<RecipeViewerProps> = ({ onExit }) => {
    const [recipes, setRecipes] = useState<Recipe[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [expandedRecipes, setExpandedRecipes] = useState<Set<number>>(new Set());

    useEffect(() => {
        loadRecipes();
    }, []);

    const loadRecipes = async () => {
        try {
            setLoading(true);
            const data = await recipeApi.getAllRecipes();
            setRecipes(data);
        } catch (e) {
            console.error("Failed to load recipes", e);
        } finally {
            setLoading(false);
        }
    };

    const toggleExpanded = (id: number) => {
        const newSet = new Set(expandedRecipes);
        if (newSet.has(id)) {
            newSet.delete(id);
        } else {
            newSet.add(id);
        }
        setExpandedRecipes(newSet);
    };

    const handleAccordionKeyDown = (e: React.KeyboardEvent, recipeId: number) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            toggleExpanded(recipeId);
        }
    };

    // Filter recipes by search term and group by type (memoized to prevent triple array iteration)
    const { filteredRecipes, craftingRecipes, workbenchRecipes } = useMemo(() => {
        const filtered = recipes.filter(r =>
            r.name.toLowerCase().includes(searchTerm.toLowerCase())
        );

        return {
            filteredRecipes: filtered,
            craftingRecipes: filtered.filter(r => r.type === RecipeType.CRAFTING),
            workbenchRecipes: filtered.filter(r => r.type === RecipeType.WORKBENCH_UPGRADE)
        };
    }, [recipes, searchTerm]);

    const RecipeCard = ({ recipe }: { recipe: Recipe }) => {
        const isExpanded = expandedRecipes.has(recipe.id!);

        return (
            <div className="border border-retro-sand/20 bg-retro-black/40 hover:border-retro-orange/50 transition-colors">
                {/* Recipe Header - Always Visible */}
                <div
                    role="button"
                    tabIndex={0}
                    className="p-3 cursor-pointer outline-none focus:ring-2 focus:ring-retro-orange flex justify-between items-center"
                    onClick={() => toggleExpanded(recipe.id!)}
                    onKeyDown={(e) => handleAccordionKeyDown(e, recipe.id!)}
                    aria-expanded={isExpanded}
                >
                    <div className="flex-1">
                        <div className="font-bold text-retro-orange">{recipe.name}</div>
                        <div className="text-[10px] text-retro-sand-dim mt-1">
                            {recipe.ingredients.length} ingredient{recipe.ingredients.length !== 1 ? 's' : ''}
                        </div>
                    </div>
                    <div className="text-retro-sand-dim text-xs ml-2">
                        {isExpanded ? '▼' : '►'}
                    </div>
                </div>

                {/* Recipe Details - Expandable */}
                {isExpanded && (
                    <div className="border-t border-retro-sand/20 p-3 bg-retro-black/60 space-y-2">
                        {/* Description */}
                        {recipe.description && (
                            <div className="text-xs text-retro-sand-dim italic mb-3">
                                {recipe.description}
                            </div>
                        )}

                        {/* Ingredients List */}
                        <div className="space-y-1">
                            <div className="text-xs uppercase font-bold text-retro-sand-dim mb-2">Required Materials:</div>
                            {recipe.ingredients.map((ing, idx) => (
                                <div key={idx} className="flex items-center gap-2 text-sm bg-retro-black/40 p-2 border border-retro-sand/10">
                                    <span className="text-retro-orange font-mono font-bold w-8 text-center">{ing.quantity}x</span>
                                    <span className="flex-1 text-retro-sand">{ing.itemName}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        );
    };

    return (
        <div className="fixed inset-0 z-50 bg-retro-bg flex flex-col text-retro-sand font-mono">
            {/* HEADER */}
            <div className="p-4 bg-retro-dark border-b border-retro-sand/20 flex justify-between items-center">
                <h3 className="text-xl font-display text-retro-orange uppercase">📖 Recipe Browser</h3>
                <button
                    onClick={onExit}
                    className="bg-retro-red hover:bg-retro-red/80 text-retro-black px-4 py-2 font-bold uppercase"
                >
                    Exit
                </button>
            </div>

            {/* SEARCH BAR */}
            <div className="p-4 bg-retro-black/30 border-b border-retro-sand/20">
                <label htmlFor="recipe-search" className="sr-only">Search recipes</label>
                <input
                    id="recipe-search"
                    type="text"
                    placeholder="🔍 Search recipes by name..."
                    className="w-full bg-retro-black border border-retro-sand/20 p-3 text-retro-sand focus:border-retro-orange outline-none"
                    value={searchTerm}
                    onChange={e => setSearchTerm(e.target.value)}
                />
                <div className="mt-2 text-xs text-retro-sand-dim">
                    Showing {filteredRecipes.length} of {recipes.length} recipes
                </div>
            </div>

            {/* RECIPE LIST */}
            <div className="flex-1 overflow-y-auto p-6">
                {loading ? (
                    <div className="text-center text-retro-sand-dim py-8">Loading recipes...</div>
                ) : (
                    <div className="max-w-4xl mx-auto space-y-8">
                        {/* CRAFTING RECIPES */}
                        {craftingRecipes.length > 0 && (
                            <div>
                                <h4 className="text-lg font-bold text-retro-sand mb-4 border-b border-retro-sand/20 pb-2 uppercase">
                                    🔨 Crafting Recipes ({craftingRecipes.length})
                                </h4>
                                <div className="space-y-2">
                                    {craftingRecipes.map(recipe => (
                                        <RecipeCard key={recipe.id} recipe={recipe} />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* WORKBENCH UPGRADES */}
                        {workbenchRecipes.length > 0 && (
                            <div>
                                <h4 className="text-lg font-bold text-retro-sand mb-4 border-b border-retro-sand/20 pb-2 uppercase">
                                    ⚙️ Workbench Upgrades ({workbenchRecipes.length})
                                </h4>
                                <div className="space-y-2">
                                    {workbenchRecipes.map(recipe => (
                                        <RecipeCard key={recipe.id} recipe={recipe} />
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* NO RESULTS */}
                        {filteredRecipes.length === 0 && !loading && (
                            <div className="text-center text-retro-sand-dim py-8 border border-retro-sand/20 bg-retro-black/20">
                                {searchTerm
                                    ? `No recipes found matching "${searchTerm}"`
                                    : "No recipes available. Recipes will sync from API on server startup."
                                }
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* FOOTER HINT */}
            <div className="p-3 bg-retro-black/30 border-t border-retro-sand/20 text-xs text-retro-sand-dim text-center">
                💡 Tip: Click any recipe to view ingredients and details
            </div>
        </div>
    );
};

export default RecipeViewer;
