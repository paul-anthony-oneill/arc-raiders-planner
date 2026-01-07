import React, { useState, useEffect, useRef, useCallback } from 'react'
import type { TargetSelection } from '../types'

interface PriorityLimitModalProps {
  isOpen: boolean
  priorityTargets: TargetSelection[]
  onAccept: (removedIds: Set<string>, movedToOngoingIds: Set<string>) => void
  onCancel: () => void
}

/**
 * Modal for managing priority target limits when exceeding 5 items
 * WHY: Enforces routing quality by requiring users to prioritize selections
 */
export const PriorityLimitModal: React.FC<PriorityLimitModalProps> = ({
  isOpen,
  priorityTargets,
  onAccept,
  onCancel,
}) => {
  const [removedIds, setRemovedIds] = useState<Set<string>>(new Set())
  const [movedToOngoingIds, setMovedToOngoingIds] = useState<Set<string>>(new Set())
  const modalRef = useRef<HTMLDivElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  // Calculate remaining priority count
  const remainingCount = priorityTargets.length - removedIds.size - movedToOngoingIds.size
  const isValid = remainingCount === 5
  const isOverLimit = remainingCount > 5
  const isUnderLimit = remainingCount < 5

  // Store previous focus and trap focus in modal
  useEffect(() => {
    if (isOpen) {
      previousFocusRef.current = document.activeElement as HTMLElement
      modalRef.current?.focus()
    } else if (previousFocusRef.current) {
      previousFocusRef.current.focus()
    }
  }, [isOpen])

  // Handle Escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onCancel()
      }
    }
    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [isOpen, onCancel])

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setRemovedIds(new Set())
      setMovedToOngoingIds(new Set())
    }
  }, [isOpen])

  const getTargetKey = (target: TargetSelection) => `${target.type}-${target.id}`

  const toggleRemove = useCallback((target: TargetSelection) => {
    const key = getTargetKey(target)
    setRemovedIds(prev => {
      const next = new Set(prev)
      if (next.has(key)) {
        next.delete(key)
      } else {
        next.add(key)
        // Can't be both removed and moved
        setMovedToOngoingIds(prev => {
          const updated = new Set(prev)
          updated.delete(key)
          return updated
        })
      }
      return next
    })
  }, [])

  const toggleMoveToOngoing = useCallback((target: TargetSelection) => {
    const key = getTargetKey(target)
    setMovedToOngoingIds(prev => {
      const next = new Set(prev)
      if (next.has(key)) {
        next.delete(key)
      } else {
        next.add(key)
        // Can't be both removed and moved
        setRemovedIds(prev => {
          const updated = new Set(prev)
          updated.delete(key)
          return updated
        })
      }
      return next
    })
  }, [])

  const handleAccept = () => {
    if (isValid) {
      onAccept(removedIds, movedToOngoingIds)
    }
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="priority-limit-title"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/80 backdrop-blur-sm"
        onClick={onCancel}
        aria-hidden="true"
      />

      {/* Modal Content */}
      <div
        ref={modalRef}
        tabIndex={-1}
        className="
          relative
          w-full
          max-w-2xl
          max-h-[80vh]
          bg-retro-dark
          border-2
          border-retro-orange
          shadow-2xl
          shadow-retro-orange/50
          overflow-hidden
        "
      >
        {/* CRT Overlay */}
        <div className="absolute inset-0 crt-overlay pointer-events-none z-10"></div>

        {/* Header */}
        <div className="relative z-20 p-6 border-b border-retro-sand/20">
          <h2
            id="priority-limit-title"
            className="
              text-2xl
              font-display
              text-retro-orange
              uppercase
              tracking-widest
              mb-2
            "
          >
            Priority Limit Exceeded
          </h2>
          <p className="text-sm text-retro-sand-dim font-mono">
            Routes with more than 5 priority targets have degraded quality.
            Please select exactly 5 items to keep as priority.
          </p>

          {/* Counter */}
          <div className="mt-4 flex items-center gap-2">
            <span className="text-sm font-mono text-retro-sand">
              Priority Targets:
            </span>
            <span
              className={`
                text-2xl
                font-bold
                font-mono
                ${isValid ? 'text-green-400' : isOverLimit ? 'text-retro-red' : 'text-yellow-400'}
              `}
            >
              {remainingCount}/5
            </span>
            {isValid && <span className="text-green-400">✓</span>}
            {isOverLimit && <span className="text-retro-red">Remove {remainingCount - 5} more</span>}
            {isUnderLimit && <span className="text-yellow-400">Add {5 - remainingCount} back</span>}
          </div>
        </div>

        {/* Target List */}
        <div className="relative z-20 p-6 overflow-y-auto max-h-[50vh] custom-scrollbar">
          <div className="space-y-2">
            {priorityTargets.map(target => {
              const key = getTargetKey(target)
              const isRemoved = removedIds.has(key)
              const isMovedToOngoing = movedToOngoingIds.has(key)
              const isRecipe = target.type === 'RECIPE'

              return (
                <div
                  key={key}
                  className={`
                    flex
                    items-center
                    justify-between
                    p-3
                    border
                    transition-all
                    ${isRemoved || isMovedToOngoing
                      ? 'border-retro-sand-dim/20 bg-retro-black/30 opacity-60'
                      : 'border-retro-orange/30 bg-retro-orange/10'
                    }
                  `}
                >
                  {/* Target Info */}
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    {target.iconUrl && (
                      <img
                        src={target.iconUrl}
                        alt=""
                        className="w-8 h-8 object-contain flex-shrink-0"
                      />
                    )}
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-bold text-retro-sand truncate">
                        {target.name}
                      </div>
                      <div className="text-xs text-retro-sand-dim font-mono">
                        {target.type}
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-4 flex-shrink-0">
                    {/* Remove Checkbox */}
                    <label className="
                      flex
                      items-center
                      gap-2
                      cursor-pointer
                      text-xs
                      font-mono
                      text-retro-red
                      hover:text-white
                      transition-colors
                    ">
                      <input
                        type="checkbox"
                        checked={isRemoved}
                        onChange={() => toggleRemove(target)}
                        className="accent-retro-red"
                      />
                      Remove
                    </label>

                    {/* Move to Ongoing (Recipes Only) */}
                    {isRecipe && (
                      <label className="
                        flex
                        items-center
                        gap-2
                        cursor-pointer
                        text-xs
                        font-mono
                        text-retro-blue
                        hover:text-white
                        transition-colors
                      ">
                        <input
                          type="checkbox"
                          checked={isMovedToOngoing}
                          onChange={() => toggleMoveToOngoing(target)}
                          className="accent-retro-blue"
                        />
                        Move to Ongoing
                      </label>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="relative z-20 p-6 border-t border-retro-sand/20 flex justify-end gap-4">
          <button
            onClick={onCancel}
            className="
              px-6
              py-3
              font-display
              font-bold
              uppercase
              tracking-wider
              border-2
              border-retro-sand/50
              text-retro-sand
              hover:border-retro-sand
              hover:text-white
              transition-all
            "
          >
            Cancel
          </button>
          <button
            onClick={handleAccept}
            disabled={!isValid}
            className={`
              px-6
              py-3
              font-display
              font-bold
              uppercase
              tracking-wider
              border-2
              transition-all
              ${
                isValid
                  ? 'border-green-400 bg-green-400 text-retro-black hover:bg-green-500 hover:border-green-500'
                  : 'border-retro-sand-dim text-retro-sand-dim cursor-not-allowed opacity-50'
              }
            `}
          >
            Accept Changes
          </button>
        </div>
      </div>
    </div>
  )
}
