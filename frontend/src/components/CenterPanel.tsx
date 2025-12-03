import React from 'react'
import { CollapsibleSection } from './CollapsibleSection'
import { RecipeCard } from './RecipeCard'
import { getRarityColors } from '../utils/rarityColors'
import type { Item } from '../types'

interface CenterPanelProps {
  item: Item | null
}

/**
 * Center Panel - Item Detail View
 * WHY: Displays comprehensive item information including crafting chains,
 * usage in recipes, and enemy drop sources
 */
export const CenterPanel: React.FC<CenterPanelProps> = ({ item }) => {
  if (!item) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-gray-400">
        <svg
          className="w-16 h-16 mb-4 text-gray-600"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          />
        </svg>
        <p className="text-sm">Select an item to view details</p>
      </div>
    )
  }

  const rarityColors = getRarityColors(item.rarity)

  return (
    <div className="flex flex-col h-full overflow-y-auto">
      {/* Header */}
      <div className="flex items-start gap-4 mb-6 pb-6 border-b border-gray-700">
        {item.iconUrl && (
          <img
            src={item.iconUrl}
            alt={item.name}
            className="w-16 h-16 rounded bg-gray-700 object-cover"
          />
        )}
        <div className="flex-1 min-w-0">
          <h2 className="text-2xl font-bold text-white mb-2 break-words">{item.name}</h2>
          <div className="flex items-center gap-2 mb-2 flex-wrap">
            <span
              className="px-2 py-1 rounded text-sm font-semibold"
              style={{
                backgroundColor: rarityColors.background,
                color: rarityColors.text,
              }}
            >
              {item.rarity}
            </span>
            {item.itemType && (
              <span className="px-2 py-1 bg-gray-700 rounded text-sm text-gray-300">
                {item.itemType}
              </span>
            )}
            {item.lootType && (
              <span className="px-2 py-1 bg-gray-700 rounded text-sm text-gray-300">
                {item.lootType}
              </span>
            )}
          </div>
          {item.description && (
            <p className="text-sm text-gray-300">{item.description}</p>
          )}
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6 p-4 bg-gray-700 rounded">
        <div>
          <div className="text-xs text-gray-400 mb-1">Value</div>
          <div className="text-lg font-semibold text-white">{item.value}</div>
        </div>
        <div>
          <div className="text-xs text-gray-400 mb-1">Weight</div>
          <div className="text-lg font-semibold text-white">{item.weight}kg</div>
        </div>
        <div>
          <div className="text-xs text-gray-400 mb-1">Stack Size</div>
          <div className="text-lg font-semibold text-white">{item.stackSize}</div>
        </div>
      </div>

      {/* Crafting Recipe (PRIMARY FOCUS) */}
      {item.craftingRecipe && (
        <div className="mb-4">
          <CollapsibleSection title="Crafting Recipe" defaultExpanded>
            <RecipeCard recipe={item.craftingRecipe} />
          </CollapsibleSection>
        </div>
      )}

      {/* Used In Recipes */}
      {item.usedInRecipes && item.usedInRecipes.length > 0 && (
        <div className="mb-4">
          <CollapsibleSection title={`Used In ${item.usedInRecipes.length} Recipe${item.usedInRecipes.length !== 1 ? 's' : ''}`} defaultExpanded>
            <div className="space-y-2">
              {item.usedInRecipes.map((recipe, index) => (
                <RecipeCard key={index} recipe={recipe} compact />
              ))}
            </div>
          </CollapsibleSection>
        </div>
      )}

      {/* Dropped By */}
      {item.droppedBy && item.droppedBy.length > 0 && (
        <div className="mb-4">
          <CollapsibleSection title={`Dropped By ${item.droppedBy.length} Enem${item.droppedBy.length !== 1 ? 'ies' : 'y'}`}>
            <div className="space-y-2">
              {item.droppedBy.map((enemyId, index) => (
                <div key={index} className="bg-gray-700 rounded-lg p-3">
                  <span className="text-gray-300 text-sm">{enemyId}</span>
                </div>
              ))}
            </div>
          </CollapsibleSection>
        </div>
      )}

      {/* Empty state when no extra info */}
      {!item.craftingRecipe && (!item.usedInRecipes || item.usedInRecipes.length === 0) && (!item.droppedBy || item.droppedBy.length === 0) && (
        <div className="flex-1 flex items-center justify-center text-gray-500 text-sm">
          <p>No additional crafting or drop information available</p>
        </div>
      )}
    </div>
  )
}
