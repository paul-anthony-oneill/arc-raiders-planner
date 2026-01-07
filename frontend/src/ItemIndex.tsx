import React from 'react'
import { CatalogIndex } from './components/CatalogIndex'
import type { Item } from './types'
import { getRarityColors } from './utils/rarityColors'

interface ItemIndexProps {
    onItemSelected: (item: Item) => void
}

/**
 * Item search and selection interface.
 * WHY: Updated to Grid View and filtered logic for Planner 2.0
 */
const ItemIndex: React.FC<ItemIndexProps> = ({ onItemSelected }) => {
    return (
        <CatalogIndex<Item>
            apiUrl="/api/items"
            title="ARC Raiders Item Index"
            className="w-full"
            onSelect={onItemSelected}
            getItemKey={(item) => item.id}
            searchPlaceholder="Search items (e.g., circuit, gear)..."
            filterItems={(items) => items.filter((item) => {
                // Must have a loot type to be lootable
                if (item.lootType === null) return false;
                
                // Exclude Blueprints (random spawn, not targetable)
                if (item.itemType === 'Blueprint') return false;
                
                // Exclude High Tier items (II, III, IV, V) - Only target base items
                // Regex matches Roman numerals at the end of the name
                if (/\s(II|III|IV|V)$/.test(item.name)) return false;
                
                return true;
            })}
            renderItem={(item) => {
                const rarityColors = getRarityColors(item.rarity)
                return (
                    <div 
                        onClick={() => onItemSelected(item)}
                        className="
                            bg-retro-black/50 border border-retro-sand/20 hover:border-retro-orange
                            p-3 cursor-pointer transition-all hover:bg-retro-orange/5
                            flex flex-col gap-2 h-full relative group
                        "
                    >
                        {/* Header: Icon + Name */}
                        <div className="flex items-start gap-3">
                            <div className="w-10 h-10 bg-retro-dark border border-retro-sand/10 flex-shrink-0 flex items-center justify-center">
                                {item.iconUrl ? (
                                    <img src={item.iconUrl} alt={item.name} className="w-8 h-8 object-contain" />
                                ) : (
                                    <span className="text-retro-sand-dim text-xs">?</span>
                                )}
                            </div>
                            <div className="min-w-0 flex-1">
                                <strong className="text-retro-sand text-sm font-display tracking-wide block truncate">
                                    {item.name}
                                </strong>
                                <span 
                                    className="text-[10px] font-mono px-1.5 py-0.5 rounded"
                                    style={{
                                        background: rarityColors.background,
                                        color: rarityColors.text,
                                    }}
                                >
                                    {item.rarity}
                                </span>
                            </div>
                        </div>

                        {/* Description */}
                        <div className="text-xs text-retro-sand-dim line-clamp-2 min-h-[2.5em]">
                            {item.description}
                        </div>
                        
                        {/* Footer: Tags */}
                        <div className="flex items-center justify-between mt-auto pt-2 border-t border-retro-sand/5">
                            <span className="text-[10px] text-retro-sand-dim uppercase font-mono">
                                {item.lootType}
                            </span>
                            {item.hasRecipe && (
                                <span className="text-[10px] text-green-400 font-mono flex items-center gap-1">
                                    <span className="w-1.5 h-1.5 bg-green-400 rounded-full animate-pulse"></span>
                                    CRAFTABLE
                                </span>
                            )}
                        </div>
                    </div>
                )
            }}
        />
    )
}

export default ItemIndex
