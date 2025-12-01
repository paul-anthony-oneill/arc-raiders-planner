import React, { useEffect, useState } from 'react';
import type { Recipe } from './types';

interface Props {
  selectedRecipeIds: string[];
  onSelectionChange: (recipeIds: string[]) => void;
}

export const RecipeSelector: React.FC<Props> = ({
  selectedRecipeIds,
  onSelectionChange
}) => {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/recipes')
      .then(r => {
        if (!r.ok) throw new Error('Failed to fetch recipes');
        return r.json();
      })
      .then((data: Recipe[]) => {
        setRecipes(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Failed to load recipes:', err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const handleToggle = (metaforgeItemId: string) => {
    if (selectedRecipeIds.includes(metaforgeItemId)) {
      onSelectionChange(selectedRecipeIds.filter(id => id !== metaforgeItemId));
    } else {
      onSelectionChange([...selectedRecipeIds, metaforgeItemId]);
    }
  };

  // Filter recipes by type
  const craftingRecipes = recipes.filter(r => r.type === 'CRAFTING');
  const workbenchUpgrades = recipes.filter(r => r.type === 'WORKBENCH_UPGRADE');

  if (loading) return <div className="text-gray-500 text-sm p-2">Loading recipes...</div>;
  if (error) return <div className="text-red-500 text-sm p-2">Error: {error}</div>;
  if (recipes.length === 0) return <div className="text-gray-500 text-sm p-2">No recipes available</div>;

  const renderRecipeSection = (title: string, recipeList: Recipe[]) => {
    if (recipeList.length === 0) return null;

    return (
      <div className="mb-4">
        <h4 className="text-md font-semibold mb-2">{title}</h4>
        <div className="space-y-2">
          {recipeList.map(recipe => (
            <label key={recipe.metaforgeItemId} className="flex items-start space-x-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 p-2 rounded">
              <input
                type="checkbox"
                checked={selectedRecipeIds.includes(recipe.metaforgeItemId)}
                onChange={() => handleToggle(recipe.metaforgeItemId)}
                className="mt-1"
              />
              <div className="flex-1">
                <span className="font-medium block">{recipe.name}</span>
                <span className="text-xs text-gray-600 dark:text-gray-400 block">
                  {recipe.ingredients.map(ing =>
                    `${ing.itemName} (${ing.quantity})`
                  ).join(', ')}
                </span>
              </div>
            </label>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="recipe-selector">
      <h3 className="text-lg font-semibold mb-3">Target Recipes</h3>
      {renderRecipeSection('Workbench Upgrades', workbenchUpgrades)}
      {renderRecipeSection('Crafting Recipes', craftingRecipes)}
    </div>
  );
};
