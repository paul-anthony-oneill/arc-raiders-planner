import React, { useState, useCallback, useRef, useEffect } from 'react'

interface CollapsibleSectionProps {
  title: string
  icon?: string
  defaultExpanded?: boolean
  children: React.ReactNode
  className?: string
}

/**
 * Collapsible accordion section with smooth animations and full keyboard accessibility
 * WHY: Reduces vertical space usage on Setup page while maintaining clear information hierarchy
 */
export const CollapsibleSection: React.FC<CollapsibleSectionProps> = ({
  title,
  icon,
  defaultExpanded = false,
  children,
  className = '',
}) => {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded)
  const contentRef = useRef<HTMLDivElement>(null)
  const [contentHeight, setContentHeight] = useState<number | 'auto'>('auto')
  const buttonId = useRef(`collapsible-${Math.random().toString(36).substr(2, 9)}`)
  const contentId = useRef(`content-${Math.random().toString(36).substr(2, 9)}`)

  // Toggle expanded state
  const handleToggle = useCallback(() => {
    setIsExpanded(prev => !prev)
  }, [])

  // Keyboard handler for Enter and Space
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      handleToggle()
    }
  }, [handleToggle])

  // Calculate content height for smooth animation
  useEffect(() => {
    if (contentRef.current) {
      if (isExpanded) {
        const height = contentRef.current.scrollHeight
        setContentHeight(height)
      } else {
        setContentHeight(0)
      }
    }
  }, [isExpanded, children])

  return (
    <div className={`collapsible-section ${className}`}>
      {/* Header Button */}
      <button
        id={buttonId.current}
        type="button"
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        aria-expanded={isExpanded}
        aria-controls={contentId.current}
        className="
          w-full flex items-center justify-between
          p-4
          bg-retro-dark/50
          border border-retro-sand/20
          hover:border-retro-orange/50
          transition-all duration-200
          cursor-pointer
          group
          min-h-[48px]
        "
      >
        {/* Left: Icon + Title */}
        <div className="flex items-center gap-3">
          {icon && (
            <span
              className="text-2xl group-hover:scale-110 transition-transform duration-200"
              aria-hidden="true"
            >
              {icon}
            </span>
          )}
          <h3 className="
            text-retro-sand
            font-display
            text-lg
            uppercase
            tracking-wider
            group-hover:text-retro-orange
            transition-colors
            text-left
          ">
            {title}
          </h3>
        </div>

        {/* Right: Expand/Collapse Indicator */}
        <div className="flex items-center gap-2">
          <span className="
            text-xs
            font-mono
            text-retro-sand-dim
            group-hover:text-retro-orange
            transition-colors
            hidden sm:inline
          ">
            {isExpanded ? 'COLLAPSE' : 'EXPAND'}
          </span>
          <svg
            className={`
              w-6 h-6
              text-retro-orange
              transition-transform
              duration-300
              ${isExpanded ? 'rotate-180' : 'rotate-0'}
            `}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </div>
      </button>

      {/* Collapsible Content */}
      <div
        id={contentId.current}
        ref={contentRef}
        role="region"
        aria-labelledby={buttonId.current}
        className="
          overflow-hidden
          transition-all
          duration-300
          ease-in-out
        "
        style={{
          height: contentHeight === 'auto' ? 'auto' : `${contentHeight}px`,
        }}
      >
        <div className="
          p-4
          bg-retro-black/30
          border-x border-b border-retro-sand/10
        ">
          {children}
        </div>
      </div>
    </div>
  )
}
