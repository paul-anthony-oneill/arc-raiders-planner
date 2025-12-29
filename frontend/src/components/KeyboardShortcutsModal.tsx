import React, { useRef, useEffect } from 'react'
import { useFocusTrap } from '../hooks/useKeyboardShortcuts'

interface KeyboardShortcutsModalProps {
  isOpen: boolean
  onClose: () => void
}

/**
 * Keyboard Shortcuts Help Modal
 * WHY: Discoverability - users need to know what keyboard shortcuts exist
 */
export const KeyboardShortcutsModal: React.FC<KeyboardShortcutsModalProps> = ({ isOpen, onClose }) => {
  const modalRef = useRef<HTMLDivElement>(null)

  // Trap focus inside modal when open
  useFocusTrap(modalRef, isOpen)

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-retro-black/80 backdrop-blur-sm">
      <div
        ref={modalRef}
        className="bg-retro-dark border-2 border-retro-orange rounded-lg max-w-2xl w-full max-h-[80vh] overflow-auto shadow-2xl"
        role="dialog"
        aria-labelledby="shortcuts-title"
        aria-modal="true"
      >
        {/* Header */}
        <div className="sticky top-0 bg-retro-dark border-b border-retro-sand/20 p-4 flex items-center justify-between">
          <h2 id="shortcuts-title" className="text-xl font-display font-bold text-retro-orange text-glow uppercase tracking-widest">
            Keyboard Shortcuts
          </h2>
          <button
            onClick={onClose}
            className="text-retro-sand hover:text-retro-orange transition-colors p-2"
            aria-label="Close shortcuts help"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Global Shortcuts */}
          <section>
            <h3 className="text-sm font-display font-bold text-retro-sand uppercase tracking-wider mb-3">
              Global
            </h3>
            <div className="space-y-2">
              <ShortcutRow keys={['?']} description="Show this help dialog" />
              <ShortcutRow keys={['Esc']} description="Close route view or dismiss error" />
              <ShortcutRow keys={['Ctrl', 'A']} description="Toggle accessibility mode (removes CRT effects)" />
            </div>
          </section>

          {/* Route Planning */}
          <section>
            <h3 className="text-sm font-display font-bold text-retro-sand uppercase tracking-wider mb-3">
              Route Planning
            </h3>
            <div className="space-y-2">
              <ShortcutRow keys={['Ctrl', 'Enter']} description="Calculate route from selected targets" />
              <ShortcutRow keys={['Esc']} description="Minimize route view and return to selection" />
            </div>
          </section>

          {/* Tips */}
          <section className="pt-4 border-t border-retro-sand/20">
            <div className="text-xs text-retro-sand-dim font-mono space-y-1">
              <p>• Most interactive elements support Tab navigation</p>
              <p>• Press Space or Enter to activate focused buttons</p>
              <p>• Focus indicators show current keyboard position</p>
            </div>
          </section>
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-retro-dark border-t border-retro-sand/20 p-4 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-retro-orange hover:bg-retro-orange/80 text-retro-dark font-mono font-bold transition-colors"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Single shortcut row display
 */
const ShortcutRow: React.FC<{ keys: string[]; description: string }> = ({ keys, description }) => {
  return (
    <div className="flex items-center justify-between py-2 px-3 rounded bg-retro-black/30">
      <span className="text-sm text-retro-sand font-mono">{description}</span>
      <div className="flex gap-1">
        {keys.map((key, i) => (
          <React.Fragment key={i}>
            {i > 0 && <span className="text-retro-sand-dim mx-1">+</span>}
            <kbd className="px-2 py-1 text-xs font-mono font-bold bg-retro-dark border border-retro-sand/30 rounded text-retro-orange shadow-sm">
              {key}
            </kbd>
          </React.Fragment>
        ))}
      </div>
    </div>
  )
}
