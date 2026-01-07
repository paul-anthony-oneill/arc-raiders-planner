import React, { memo } from 'react'
import { getRarityColors } from '../utils/rarityColors'

export type TargetType = 'ITEM' | 'RECIPE' | 'ENEMY' | 'CONTAINER'
export type SelectionState = 'unselected' | 'priority' | 'ongoing'

interface TargetCardProps {
  id: string | number
  type: TargetType
  name: string
  iconUrl?: string
  rarity?: string
  lootZone?: string
  description?: string
  selectionState: SelectionState
  onSelect: () => void
  disabled?: boolean
  className?: string
}

/**
 * Generic selectable card for targets (Items, Recipes, Enemies, Containers)
 * WHY: Reusable component for all target types with consistent selection UX
 * Memoized to prevent unnecessary re-renders in lists
 */
const TargetCardComponent: React.FC<TargetCardProps> = ({
  id: _id, // Kept for future use (e.g., analytics, debugging)
  type,
  name,
  iconUrl,
  rarity,
  lootZone,
  description,
  selectionState,
  onSelect,
  disabled = false,
  className = '',
}) => {
  // Get rarity colors if applicable
  const rarityColors = rarity ? getRarityColors(rarity) : null

  // Selection state styling
  const getSelectionStyles = () => {
    if (disabled) {
      return {
        border: 'border-retro-sand-dim/20',
        bg: 'bg-retro-dark/30',
        shadow: '',
        badge: null,
        cursor: 'cursor-not-allowed opacity-50',
      }
    }

    switch (selectionState) {
      case 'priority':
        return {
          border: 'border-retro-orange',
          bg: 'bg-retro-orange/10',
          shadow: 'shadow-lg shadow-retro-orange/50',
          badge: { text: 'PRIORITY', color: 'bg-retro-orange text-retro-black' },
          cursor: 'cursor-pointer',
        }
      case 'ongoing':
        return {
          border: 'border-retro-blue',
          bg: 'bg-retro-blue/10',
          shadow: 'shadow-lg shadow-retro-blue/50',
          badge: { text: 'ONGOING', color: 'bg-retro-blue text-retro-black' },
          cursor: 'cursor-pointer',
        }
      default:
        return {
          border: 'border-retro-sand/20',
          bg: 'bg-retro-dark/50',
          shadow: '',
          badge: null,
          cursor: 'cursor-pointer hover:border-retro-orange/50',
        }
    }
  }

  const styles = getSelectionStyles()

  // Get type-specific icon/emoji if no iconUrl
  const getTypeIcon = () => {
    if (iconUrl) return null
    switch (type) {
      case 'ITEM':
        return '📦'
      case 'RECIPE':
        return '🔨'
      case 'ENEMY':
        return '⚡'
      case 'CONTAINER':
        return '🎁'
      default:
        return '❓'
    }
  }

  return (
    <button
      type="button"
      onClick={disabled ? undefined : onSelect}
      disabled={disabled}
      aria-label={`${selectionState === 'unselected' ? 'Select' : 'Deselect'} ${name}`}
      aria-pressed={selectionState !== 'unselected'}
      className={`
        relative
        flex flex-col
        p-3
        h-[160px]
        border-2
        ${styles.border}
        ${styles.bg}
        ${styles.shadow}
        ${styles.cursor}
        transition-all
        duration-200
        group
        text-left
        ${className}
      `}
    >
      {/* Selection Badge */}
      {styles.badge && (
        <div className={`
          absolute
          top-2
          right-2
          px-2
          py-1
          text-[10px]
          font-bold
          font-mono
          tracking-wider
          ${styles.badge.color}
          z-10
        `}>
          {styles.badge.text}
        </div>
      )}

      {/* Icon */}
      <div className="flex items-center gap-3 mb-2">
        {iconUrl ? (
          <img
            src={iconUrl}
            alt=""
            className="w-12 h-12 object-contain"
            loading="lazy"
          />
        ) : (
          <span className="text-3xl" aria-hidden="true">
            {getTypeIcon()}
          </span>
        )}

        {/* Name and Rarity */}
        <div className="flex-1 min-w-0">
          <h4 className="
            text-retro-sand
            font-bold
            text-sm
            truncate
            group-hover:text-retro-orange
            transition-colors
          ">
            {name}
          </h4>
          {rarity && rarityColors && (
            <span
              className="
                inline-block
                mt-1
                px-2
                py-0.5
                text-[10px]
                font-bold
                rounded
              "
              style={{
                background: rarityColors.background,
                color: rarityColors.text,
              }}
            >
              {rarity}
            </span>
          )}
        </div>
      </div>

      {/* Loot Zone / Additional Info */}
      {lootZone && (
        <div className="text-xs text-retro-sand-dim font-mono mb-1">
          <span className="text-retro-orange">Zone:</span> {lootZone}
        </div>
      )}

      {/* Description */}
      {description && (
        <p className="
          text-xs
          text-retro-sand-dim
          line-clamp-2
          mt-auto
        ">
          {description}
        </p>
      )}

      {/* Type Indicator */}
      <div className="
        mt-2
        pt-2
        border-t
        border-retro-sand/10
        text-[10px]
        font-mono
        text-retro-sand-dim
        uppercase
      ">
        {type}
      </div>
    </button>
  )
}

export const TargetCard = memo(TargetCardComponent)
