import React, { useState, useRef, useEffect } from 'react'

interface TooltipProps {
  content: string
  children: React.ReactElement
  position?: 'top' | 'bottom' | 'left' | 'right'
  delay?: number
}

/**
 * Tooltip Component
 * WHY: Provides contextual help and explanations for complex features
 * without cluttering the UI. Improves discoverability and reduces user confusion.
 */
export const Tooltip: React.FC<TooltipProps> = ({
  content,
  children,
  position = 'top',
  delay = 300
}) => {
  const [isVisible, setIsVisible] = useState(false)
  const [coords, setCoords] = useState({ x: 0, y: 0 })
  const timeoutRef = useRef<number | undefined>(undefined)
  const triggerRef = useRef<HTMLElement>(null)

  const showTooltip = () => {
    timeoutRef.current = window.setTimeout(() => {
      setIsVisible(true)
    }, delay)
  }

  const hideTooltip = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }
    setIsVisible(false)
  }

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  // Calculate tooltip position
  useEffect(() => {
    if (isVisible && triggerRef.current) {
      const rect = triggerRef.current.getBoundingClientRect()
      let x = 0
      let y = 0

      switch (position) {
        case 'top':
          x = rect.left + rect.width / 2
          y = rect.top - 8
          break
        case 'bottom':
          x = rect.left + rect.width / 2
          y = rect.bottom + 8
          break
        case 'left':
          x = rect.left - 8
          y = rect.top + rect.height / 2
          break
        case 'right':
          x = rect.right + 8
          y = rect.top + rect.height / 2
          break
      }

      setCoords({ x, y })
    }
  }, [isVisible, position])

  const positionClasses = {
    top: '-translate-x-1/2 -translate-y-full mb-2',
    bottom: '-translate-x-1/2 mt-2',
    left: '-translate-x-full -translate-y-1/2 mr-2',
    right: 'translate-x-0 -translate-y-1/2 ml-2'
  }

  const arrowClasses = {
    top: 'top-full left-1/2 -translate-x-1/2 border-l-transparent border-r-transparent border-b-transparent',
    bottom: 'bottom-full left-1/2 -translate-x-1/2 border-l-transparent border-r-transparent border-t-transparent',
    left: 'left-full top-1/2 -translate-y-1/2 border-t-transparent border-b-transparent border-r-transparent',
    right: 'right-full top-1/2 -translate-y-1/2 border-t-transparent border-b-transparent border-l-transparent'
  }

  const childElement = children as React.ReactElement<any>

  return (
    <>
      <span
        ref={triggerRef}
        onMouseEnter={showTooltip}
        onMouseLeave={hideTooltip}
        onFocus={showTooltip}
        onBlur={hideTooltip}
        style={{ display: 'inline-block' }}
      >
        {React.cloneElement(childElement, {
          'aria-describedby': isVisible ? 'tooltip' : undefined
        })}
      </span>

      {isVisible && (
        <div
          id="tooltip"
          role="tooltip"
          className={`fixed z-[100] px-3 py-2 bg-retro-dark border border-retro-orange text-retro-sand font-mono text-xs rounded shadow-lg max-w-xs ${positionClasses[position]}`}
          style={{
            left: `${coords.x}px`,
            top: `${coords.y}px`,
            pointerEvents: 'none'
          }}
        >
          {content}
          <div className={`absolute w-0 h-0 border-4 border-retro-orange ${arrowClasses[position]}`}></div>
        </div>
      )}
    </>
  )
}

/**
 * Keyboard Shortcut Hint Component
 * WHY: Helps users discover keyboard shortcuts, improving efficiency
 */
interface KeyboardHintProps {
  keys: string[]
  description: string
}

export const KeyboardHint: React.FC<KeyboardHintProps> = ({ keys, description }) => {
  return (
    <div className="flex items-center gap-2 text-xs font-mono">
      <div className="flex gap-1">
        {keys.map((key, i) => (
          <React.Fragment key={i}>
            {i > 0 && <span className="text-retro-sand-dim">+</span>}
            <kbd className="px-2 py-1 bg-retro-black border border-retro-sand/20 rounded text-retro-orange font-bold">
              {key}
            </kbd>
          </React.Fragment>
        ))}
      </div>
      <span className="text-retro-sand-dim">{description}</span>
    </div>
  )
}
