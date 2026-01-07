import React from 'react'
import type { Recipe } from '../types'

interface RecipeCardProps {
  recipe: Recipe
  compact?: boolean
}

/**
 * Recipe Card Component
 * WHY: Displays crafting recipe details with ingredients list
 * Used in: ItemDetailView for crafting/usage sections
 */
export const RecipeCard: React.FC<RecipeCardProps> = ({ recipe, compact = false }) => {
  return (
    <div className="bg-gray-700 rounded-lg p-4">
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-semibold text-white">{recipe.name}</h4>
        <span className="px-2 py-1 bg-gray-600 rounded text-xs text-gray-300">
          {recipe.type}
        </span>
      </div>

      {!compact && recipe.description && (
        <p className="text-sm text-gray-400 mb-3">{recipe.description}</p>
      )}

      <div className="space-y-2">
        <div className="text-xs text-gray-400 uppercase font-semibold">Ingredients</div>
        {recipe.ingredients.length === 0 ? (
          <div className="text-sm text-gray-500 italic">No ingredients</div>
        ) : (
          recipe.ingredients.map((ingredient, index) => (
            <div key={index} className="flex items-center justify-between text-sm bg-gray-800 rounded px-3 py-2">
              <span className="text-gray-300">{ingredient.itemName}</span>
              <span className="text-white font-semibold">×{ingredient.quantity}</span>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
