import React, { useState, useEffect } from 'react'
import { TargetCard } from './TargetCard'
import { useTargetSelection } from '../hooks/useTargetSelection'
import { itemApi } from '../api/itemApi'
import { recipeApi } from '../api/recipeApi'
import type { Item, Recipe, TargetSelection } from '../types'

type CategoryTab = 'ALL' | 'ITEMS' | 'ENEMIES' | 'CONTAINERS' | 'RECIPES'

interface LeftPanelProps {
  onItemSelect?: (item: Item) => void
}

/**
 * Left Panel - Objective Selection
 * WHY: Unified list of all selectable targets with category filtering
 */
export const LeftPanel: React.FC<LeftPanelProps> = ({ onItemSelect }) => {
  const {
    priorityTargets,
    ongoingTargets,
    addPriorityTarget,
    removePriorityTarget,
    addOngoingTarget,
    removeOngoingTarget,
    priorityCount,
    ongoingCount,
  } = useTargetSelection()

  const [activeTab, setActiveTab] = useState<CategoryTab>('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [items, setItems] = useState<Item[]>([])
  const [recipes, setRecipes] = useState<Recipe[]>([])
  const [loading, setLoading] = useState(true)

  // Load data on mount
  useEffect(() => {
    Promise.all([
      itemApi.getAllItems(),
      recipeApi.getAllRecipes(),
    ])
      .then(([itemsData, recipesData]) => {
        setItems(itemsData)
        setRecipes(recipesData)
        setLoading(false)
      })
      .catch(console.error)
  }, [])

  // Check if a target is selected
  const getSelectionState = (id: string | number, type: TargetSelection['type']): 'priority' | 'ongoing' | 'unselected' => {
    const isPriority = priorityTargets.some(t => t.id === id && t.type === type)
    const isOngoing = ongoingTargets.some(t => t.id === id && t.type === type)

    if (isPriority) return 'priority'
    if (isOngoing) return 'ongoing'
    return 'unselected'
  }

  // Handle target selection
  const handleTargetToggle = (target: TargetSelection) => {
    const currentState = getSelectionState(target.id, target.type)

    if (currentState === 'unselected') {
      if (priorityCount < 5) {
        addPriorityTarget(target)
      }
    } else if (currentState === 'priority') {
      if (target.type === 'RECIPE' && ongoingCount < 10) {
        removePriorityTarget(target.id, target.type)
        addOngoingTarget(target)
      } else {
        removePriorityTarget(target.id, target.type)
      }
    } else if (currentState === 'ongoing') {
      removeOngoingTarget(target.id, target.type)
    }

    // Notify parent when item is selected for detail view
    if (target.type === 'ITEM' && onItemSelect && target.data) {
      onItemSelect(target.data as Item)
    }
  }

  // Filter items based on active tab and search
  const filteredItems = items.filter(item => {
    if (activeTab !== 'ALL' && activeTab !== 'ITEMS') return false
    if (searchTerm && !item.name.toLowerCase().includes(searchTerm.toLowerCase())) return false
    return true
  })

  const filteredRecipes = recipes.filter(recipe => {
    if (activeTab !== 'ALL' && activeTab !== 'RECIPES') return false
    if (searchTerm && !recipe.name.toLowerCase().includes(searchTerm.toLowerCase())) return false
    return true
  })

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-gray-400">Loading objectives...</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="mb-4">
        <h2 className="text-xl font-bold text-white mb-2">Objectives</h2>
        <p className="text-xs text-gray-400">
          Priority: {priorityCount}/5 | Ongoing: {ongoingCount}/10
        </p>
      </div>

      {/* Category Tabs */}
      <div className="flex gap-2 mb-4 overflow-x-auto">
        {(['ALL', 'ITEMS', 'RECIPES'] as CategoryTab[]).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-3 py-1.5 rounded text-sm font-medium whitespace-nowrap transition-colors ${
              activeTab === tab
                ? 'bg-orange-500 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Search */}
      <input
        type="text"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        placeholder="Search objectives..."
        className="w-full px-3 py-2 mb-4 bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-orange-500"
      />

      {/* Objective List */}
      <div className="flex-1 overflow-y-auto space-y-2">
        {/* Items */}
        {filteredItems.map(item => {
          const target: TargetSelection = {
            id: item.id,
            type: 'ITEM',
            priority: 'PRIORITY',
            name: item.name,
            iconUrl: item.iconUrl,
            rarity: item.rarity,
            lootZone: item.lootType || undefined,
            data: item,
          }
          const state = getSelectionState(item.id, 'ITEM')

          return (
            <TargetCard
              key={`item-${item.id}`}
              id={item.id}
              type="ITEM"
              name={item.name}
              iconUrl={item.iconUrl}
              rarity={item.rarity}
              lootZone={item.lootType || undefined}
              selectionState={state}
              onSelect={() => handleTargetToggle(target)}
            />
          )
        })}

        {/* Recipes */}
        {filteredRecipes.map(recipe => {
          const target: TargetSelection = {
            id: recipe.metaforgeItemId,
            type: 'RECIPE',
            priority: 'PRIORITY',
            name: recipe.name,
            data: recipe,
          }
          const state = getSelectionState(recipe.metaforgeItemId, 'RECIPE')

          return (
            <TargetCard
              key={`recipe-${recipe.id}`}
              id={recipe.metaforgeItemId}
              type="RECIPE"
              name={recipe.name}
              description={recipe.description}
              selectionState={state}
              onSelect={() => handleTargetToggle(target)}
            />
          )
        })}

        {filteredItems.length === 0 && filteredRecipes.length === 0 && (
          <div className="text-center text-gray-400 py-8">
            No objectives found
          </div>
        )}
      </div>
    </div>
  )
}
