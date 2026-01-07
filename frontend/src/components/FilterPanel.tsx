import React from 'react'

export interface FilterState {
  rarities: string[] // ["Common", "Rare", "Legendary"]
  itemTypes: string[] // ["Weapon", "Material", "Consumable"]
  lootTypes: string[] // ["Mechanical", "Industrial"]
  showOnlyTargeted: boolean // Show only selected targets
}

interface FilterPanelProps {
  filters: FilterState
  onFilterChange: (filters: FilterState) => void
  availableRarities: string[]
  availableItemTypes: string[]
  availableLootTypes: string[]
}

/**
 * Multi-criteria filter panel for items.
 * WHY: Users need to filter large item lists by rarity, type, and loot zone
 * to quickly find what they're looking for.
 */
export const FilterPanel: React.FC<FilterPanelProps> = ({
  filters,
  onFilterChange,
  availableRarities,
  availableItemTypes,
  availableLootTypes,
}) => {
  const toggleRarity = (rarity: string) => {
    const newRarities = filters.rarities.includes(rarity)
      ? filters.rarities.filter((r) => r !== rarity)
      : [...filters.rarities, rarity]

    onFilterChange({ ...filters, rarities: newRarities })
  }

  const toggleItemType = (type: string) => {
    const newTypes = filters.itemTypes.includes(type)
      ? filters.itemTypes.filter((t) => t !== type)
      : [...filters.itemTypes, type]

    onFilterChange({ ...filters, itemTypes: newTypes })
  }

  const toggleLootType = (type: string) => {
    const newTypes = filters.lootTypes.includes(type)
      ? filters.lootTypes.filter((t) => t !== type)
      : [...filters.lootTypes, type]

    onFilterChange({ ...filters, lootTypes: newTypes })
  }

  const clearFilters = () => {
    onFilterChange({
      rarities: [],
      itemTypes: [],
      lootTypes: [],
      showOnlyTargeted: false,
    })
  }

  const hasActiveFilters =
    filters.rarities.length > 0 ||
    filters.itemTypes.length > 0 ||
    filters.lootTypes.length > 0 ||
    filters.showOnlyTargeted

  return (
    <div className="bg-gray-700 rounded-lg p-3 mb-3">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-sm font-semibold text-white">Filters</h4>
        {hasActiveFilters && (
          <button
            onClick={clearFilters}
            className="text-xs text-orange-400 hover:text-orange-300 transition-colors"
          >
            Clear All
          </button>
        )}
      </div>

      {/* Rarity Filter */}
      {availableRarities.length > 0 && (
        <div className="mb-3">
          <div className="text-xs text-gray-400 mb-1 uppercase font-semibold">Rarity</div>
          <div className="flex flex-wrap gap-1">
            {availableRarities.map((rarity) => (
              <button
                key={rarity}
                onClick={() => toggleRarity(rarity)}
                className={`px-2 py-1 text-xs rounded transition-colors ${
                  filters.rarities.includes(rarity)
                    ? 'bg-orange-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                {rarity}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Item Type Filter */}
      {availableItemTypes.length > 0 && (
        <div className="mb-3">
          <div className="text-xs text-gray-400 mb-1 uppercase font-semibold">Item Type</div>
          <div className="flex flex-wrap gap-1">
            {availableItemTypes.map((type) => (
              <button
                key={type}
                onClick={() => toggleItemType(type)}
                className={`px-2 py-1 text-xs rounded transition-colors ${
                  filters.itemTypes.includes(type)
                    ? 'bg-orange-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Loot Type Filter */}
      {availableLootTypes.length > 0 && (
        <div className="mb-3">
          <div className="text-xs text-gray-400 mb-1 uppercase font-semibold">Loot Zone</div>
          <div className="flex flex-wrap gap-1">
            {availableLootTypes.map((type) => (
              <button
                key={type}
                onClick={() => toggleLootType(type)}
                className={`px-2 py-1 text-xs rounded transition-colors ${
                  filters.lootTypes.includes(type)
                    ? 'bg-orange-500 text-white'
                    : 'bg-gray-600 text-gray-300 hover:bg-gray-500'
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Show Only Targeted */}
      <label className="flex items-center text-sm text-gray-300 cursor-pointer hover:text-white transition-colors">
        <input
          type="checkbox"
          checked={filters.showOnlyTargeted}
          onChange={(e) => onFilterChange({ ...filters, showOnlyTargeted: e.target.checked })}
          className="mr-2 w-4 h-4"
        />
        Show only selected targets
      </label>
    </div>
  )
}
