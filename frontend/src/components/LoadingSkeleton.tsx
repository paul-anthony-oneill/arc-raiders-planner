import React from 'react'

/**
 * Loading Skeleton Component
 * WHY: Provides visual feedback during data loading, improving perceived performance
 * and reducing user anxiety during API calls.
 */

interface SkeletonProps {
  className?: string
  count?: number
  type?: 'text' | 'card' | 'image' | 'button'
}

export const Skeleton: React.FC<SkeletonProps> = ({
  className = '',
  count = 1,
  type = 'text'
}) => {
  const baseClass = 'animate-pulse bg-retro-sand/10 rounded'

  const typeClasses = {
    text: 'h-4 w-full',
    card: 'h-24 w-full',
    image: 'h-48 w-full',
    button: 'h-10 w-32'
  }

  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className={`${baseClass} ${typeClasses[type]} ${className}`}
          aria-hidden="true"
        />
      ))}
    </>
  )
}

/**
 * Item List Loading Skeleton
 * WHY: Specific skeleton for item list in LeftPanel
 */
export const ItemListSkeleton: React.FC = () => {
  return (
    <div className="space-y-3" role="status" aria-label="Loading items">
      <span className="sr-only">Loading items...</span>
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="bg-retro-sand/5 rounded-lg p-3 space-y-2">
          <div className="flex items-center justify-between">
            <Skeleton className="w-32" />
            <Skeleton className="w-16" />
          </div>
          <Skeleton className="w-full h-3" />
          <div className="flex gap-2">
            <Skeleton className="w-20 h-6" />
            <Skeleton className="w-20 h-6" />
          </div>
        </div>
      ))}
    </div>
  )
}

/**
 * Map Loading Skeleton
 * WHY: Feedback while map is loading
 */
export const MapSkeleton: React.FC = () => {
  return (
    <div className="w-full h-full bg-retro-black/50 flex items-center justify-center" role="status">
      <div className="text-center space-y-4">
        <div className="animate-pulse">
          <div className="w-16 h-16 border-4 border-retro-orange border-t-transparent rounded-full animate-spin mx-auto" />
        </div>
        <p className="text-retro-sand-dim font-mono text-sm">
          Loading map data...
          <span className="sr-only">Please wait</span>
        </p>
      </div>
    </div>
  )
}

/**
 * Item Detail Loading Skeleton
 * WHY: Feedback while item details are being fetched
 */
export const ItemDetailSkeleton: React.FC = () => {
  return (
    <div className="space-y-6" role="status" aria-label="Loading item details">
      <span className="sr-only">Loading item details...</span>

      {/* Header */}
      <div className="space-y-2">
        <Skeleton className="w-48 h-8" />
        <div className="flex gap-2">
          <Skeleton className="w-24 h-6" />
          <Skeleton className="w-24 h-6" />
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4">
        <Skeleton className="h-16" />
        <Skeleton className="h-16" />
        <Skeleton className="h-16" />
        <Skeleton className="h-16" />
      </div>

      {/* Description */}
      <div className="space-y-2">
        <Skeleton className="w-32 h-5" />
        <Skeleton className="w-full h-4" />
        <Skeleton className="w-full h-4" />
        <Skeleton className="w-3/4 h-4" />
      </div>

      {/* Recipes section */}
      <div className="space-y-3">
        <Skeleton className="w-40 h-5" />
        <Skeleton type="card" count={2} className="mb-2" />
      </div>
    </div>
  )
}
