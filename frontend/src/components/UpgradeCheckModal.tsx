import React, { useRef, useEffect } from 'react'

interface UpgradeCheckModalProps {
  isOpen: boolean
  targetItemName: string
  prerequisiteItemName: string
  onConfirm: (hasPrerequisite: boolean) => void
  onCancel: () => void
}

/**
 * Modal to ask user if they already have the prerequisite item for a high-tier upgrade.
 */
export const UpgradeCheckModal: React.FC<UpgradeCheckModalProps> = ({
  isOpen,
  targetItemName,
  prerequisiteItemName,
  onConfirm,
  onCancel
}) => {
  const modalRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (isOpen) {
      modalRef.current?.focus()
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      <div className="absolute inset-0 bg-black/80 backdrop-blur-sm" onClick={onCancel} />
      
      <div
        ref={modalRef}
        tabIndex={-1}
        className="
          relative z-20 w-full max-w-lg bg-retro-dark border-2 border-retro-orange
          shadow-2xl shadow-retro-orange/50 p-6 text-center
        "
      >
        <div className="absolute inset-0 crt-overlay pointer-events-none z-10"></div>
        
        <h2 className="text-xl font-display text-retro-orange mb-4 uppercase tracking-widest relative z-20">
          Crafting Check
        </h2>
        
        <p className="text-retro-sand font-mono mb-6 relative z-20">
          You are planning to craft <span className="text-white font-bold">{targetItemName}</span>.
          <br /><br />
          Do you already have the required <span className="text-retro-blue font-bold">{prerequisiteItemName}</span>?
        </p>

        <div className="flex gap-4 justify-center relative z-20">
          <button
            onClick={() => onConfirm(true)}
            className="px-6 py-2 border-2 border-green-500 text-green-500 hover:bg-green-500 hover:text-retro-black font-display font-bold uppercase transition-all"
          >
            Yes, I have it
          </button>
          
          <button
            onClick={() => onConfirm(false)}
            className="px-6 py-2 border-2 border-retro-red text-retro-red hover:bg-retro-red hover:text-white font-display font-bold uppercase transition-all"
          >
            No, I need to craft it
          </button>
        </div>
        
        <button 
            onClick={onCancel}
            className="mt-4 text-xs text-retro-sand-dim hover:text-white underline relative z-20"
        >
            Cancel Selection
        </button>
      </div>
    </div>
  )
}
