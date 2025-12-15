import React, { useState, useEffect, useMemo } from 'react'
import { TargetCard } from './TargetCard'
import { FilterPanel, type FilterState } from './FilterPanel'
import { ItemListSkeleton } from './LoadingSkeleton'
import { itemApi } from '../api/itemApi'
import { recipeApi } from '../api/recipeApi'
import type { Item, Recipe, TargetSelection } from '../types'

type CategoryTab = 'ALL' | 'ITEMS' | 'ENEMIES' | 'CONTAINERS' | 'RECIPES'

interface LeftPanelProps {
  onItemSelect?: (item: Item) => void
  priorityTargets: TargetSelection[]
  ongoingTargets: TargetSelection[]
  addPriorityTarget: (target: TargetSelection) => void
  removePriorityTarget: (id: string | number, type: TargetSelection['type']) => void
  addOngoingTarget: (target: TargetSelection) => void
  removeOngoingTarget: (id: string | number, type: TargetSelection['type']) => void
  priorityCount: number
  ongoingCount: number
}

/**
 * Left Panel - Objective Selection
 * WHY: Unified list of all selectable targets with category filtering
 */
export const LeftPanel: React.FC<LeftPanelProps> = ({
  onItemSelect,
  priorityTargets,
  ongoingTargets,
  addPriorityTarget,
  removePriorityTarget,
  addOngoingTarget,
  removeOngoingTarget,
  priorityCount,
  ongoingCount,
}) => {
  const [activeTab, setActiveTab] = useState<CategoryTab>('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [items, setItems] = useState<Item[]>([])
  const [recipes, setRecipes] = useState<Recipe[]>([])
  const [loading, setLoading] = useState(true)
  const [filtersExpanded, setFiltersExpanded] = useState(false)
  const [filters, setFilters] = useState<FilterState>({
    rarities: [],
    itemTypes: [],
    lootTypes: [],
    showOnlyTargeted: false,
  })

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
      .catch(err => {
        console.error('Failed to load objectives:', err)
        setLoading(false) // Set loading to false even on error
      })
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

  // Calculate available filter options
  const availableRarities = useMemo(() =>
    [...new Set(items.map(i => i.rarity))].filter(Boolean).sort(),
    [items]
  )

  const availableItemTypes = useMemo(() =>
    [...new Set(items.map(i => i.itemType))].filter(Boolean).sort(),
    [items]
  )

  const availableLootTypes = useMemo(() =>
    [...new Set(items.map(i => i.lootType))].filter(Boolean).sort() as string[],
    [items]
  )

  // Filter items based on active tab, search, and filters
  const filteredItems = useMemo(() => {
    return items.filter(item => {
      // Tab filter
      if (activeTab !== 'ALL' && activeTab !== 'ITEMS') return false

      // Search filter
      if (searchTerm && !item.name.toLowerCase().includes(searchTerm.toLowerCase())) return false

      // Rarity filter
      if (filters.rarities.length > 0 && !filters.rarities.includes(item.rarity)) return false

      // Item type filter
      if (filters.itemTypes.length > 0 && !filters.itemTypes.includes(item.itemType)) return false

      // Loot type filter
      if (filters.lootTypes.length > 0 && item.lootType && !filters.lootTypes.includes(item.lootType)) return false

      // Show only targeted filter
      if (filters.showOnlyTargeted) {
        const isSelected = priorityTargets.some(t => t.id === item.id && t.type === 'ITEM') ||
                           ongoingTargets.some(t => t.id === item.id && t.type === 'ITEM')
        if (!isSelected) return false
      }

      return true
    })
  }, [items, activeTab, searchTerm, filters, priorityTargets, ongoingTargets])

  const filteredRecipes = useMemo(() => {
    return recipes.filter(recipe => {
      // Tab filter
      if (activeTab !== 'ALL' && activeTab !== 'RECIPES') return false

      // Search filter
      if (searchTerm && !recipe.name.toLowerCase().includes(searchTerm.toLowerCase())) return false

      // Show only targeted filter
      if (filters.showOnlyTargeted) {
        const isSelected = priorityTargets.some(t => t.id === recipe.metaforgeItemId && t.type === 'RECIPE') ||
                           ongoingTargets.some(t => t.id === recipe.metaforgeItemId && t.type === 'RECIPE')
        if (!isSelected) return false
      }

      return true
    })
  }, [recipes, activeTab, searchTerm, filters, priorityTargets, ongoingTargets])

  // Count active filters
  const activeFilterCount = filters.rarities.length + filters.itemTypes.length + filters.lootTypes.length + (filters.showOnlyTargeted ? 1 : 0)

  if (loading) {
    return (
      <div className="flex flex-col h-full">
        <div className="mb-4">
          <h2 className="text-retro-orange font-display text-sm uppercase tracking-wider">
            OBJECTIVES
          </h2>
        </div>
        <ItemListSkeleton />
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="mb-4 border-b border-retro-sand/20 pb-4">
        <h2 className="text-xl font-display font-bold text-retro-orange text-glow uppercase tracking-widest mb-2">Objectives</h2>
        <p className="text-xs text-retro-sand-dim font-mono">
          Priority: <span className="text-retro-orange">{priorityCount}/5</span> | Ongoing: <span className="text-retro-sand">{ongoingCount}/10</span>
        </p>
      </div>

      {/* Category Tabs */}
      <div className="flex gap-2 mb-4 overflow-x-auto">
        {(['ALL', 'ITEMS', 'RECIPES'] as CategoryTab[]).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-3 py-1.5 text-sm font-mono font-medium whitespace-nowrap transition-colors border ${
              activeTab === tab
                ? 'bg-retro-orange text-retro-black border-retro-orange'
                : 'bg-retro-black text-retro-sand-dim border-retro-sand/20 hover:border-retro-orange hover:text-retro-sand'
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
        className="w-full px-3 py-2 mb-3 bg-retro-black text-retro-sand font-mono border border-retro-sand/20 focus:outline-none focus:border-retro-orange transition-colors placeholder-retro-sand-dim"
      />

      {/* Filter Panel Toggle */}
      <button
        onClick={() => setFiltersExpanded(!filtersExpanded)}
        className="w-full flex items-center justify-between px-3 py-2 mb-3 bg-retro-black border border-retro-sand/20 hover:border-retro-orange transition-colors"
      >
        <span className="text-sm font-mono font-semibold text-retro-sand">
          Filters {activeFilterCount > 0 && <span className="text-retro-orange">({activeFilterCount})</span>}
        </span>
        <svg
          className={`w-4 h-4 transition-transform text-retro-sand ${filtersExpanded ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Filter Panel */}
      {filtersExpanded && (
        <FilterPanel
          filters={filters}
          onFilterChange={setFilters}
          availableRarities={availableRarities}
          availableItemTypes={availableItemTypes}
          availableLootTypes={availableLootTypes}
        />
      )}

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
          <div className="text-center text-retro-sand-dim font-mono py-8">
            No objectives found
          </div>
        )}
      </div>
    </div>
  )
}
