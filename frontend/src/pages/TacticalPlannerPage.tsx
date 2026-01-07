import React, { useState, useCallback, useEffect, lazy, Suspense } from 'react'
import { useTargetSelection } from '../hooks/useTargetSelection'
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts'
import { LeftPanel } from '../components/LeftPanel'
import { CenterPanel } from '../components/CenterPanel'
import { MinimizedMapView } from '../components/MinimizedMapView'
import { MapSkeleton } from '../components/LoadingSkeleton'
import { Tooltip } from '../components/Tooltip'
import { KeyboardShortcutsModal } from '../components/KeyboardShortcutsModal'
import { itemApi } from '../api/itemApi'
import { mapApi } from '../api/mapApi'
import { plannerApi } from '../api/plannerApi'
import type { Item, PlannerResponse, Area, PlannerRequest } from '../types'

// Lazy load heavy map visualization component
const MaximizedMapView = lazy(() => import('../components/MaximizedMapView').then(m => ({ default: m.MaximizedMapView })))

/**
 * Unified Tactical Planner Interface
 * WHY: Single-page design with Selection → Planning → Back workflow
 *
 * Three states:
 * 1. SELECTION: Left (objectives) + Center (details) + Right (minimized map)
 * 2. PLANNING: Left (objectives) + Right (maximized map with route)
 * 3. Minimize: Clear route, keep selections, return to SELECTION
 */
export const TacticalPlannerPage: React.FC = () => {
  // Session state from hook
  const {
    priorityTargets,
    ongoingTargets,
    routingProfile,
    hasRaiderKey,
    addPriorityTarget,
    removePriorityTarget,
    addOngoingTarget,
    removeOngoingTarget,
    priorityCount,
    ongoingCount,
  } = useTargetSelection()

  // UI state (local to page)
  const [uiMode, setUiMode] = useState<'SELECTION' | 'PLANNING'>('SELECTION')
  const [selectedMap, setSelectedMap] = useState<string>('Dam Battlegrounds')
  const [selectedItem, setSelectedItem] = useState<Item | null>(null)
  const [highlightedZones, setHighlightedZones] = useState<Area[]>([])
  const [calculatedRoute, setCalculatedRoute] = useState<PlannerResponse | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showShortcutsModal, setShowShortcutsModal] = useState(false)

  // Handle item selection from LeftPanel
  const handleItemSelect = useCallback(async (item: Item) => {
    try {
      // Fetch enhanced item details
      const details = await itemApi.getItemDetails(item.id)
      setSelectedItem(details)

      // Highlight zones on current map
      try {
        const zones = await mapApi.getZonesWithItem(selectedMap, item.name)
        setHighlightedZones(zones)
      } catch (error) {
        console.error('Failed to fetch zones:', error)
        // Don't block item selection if zones fail to load
        setHighlightedZones([])
      }
    } catch (error) {
      console.error('Failed to fetch item details:', error)
      setSelectedItem(item) // Fallback to basic item data
    }
  }, [selectedMap])

  const handleCalculateRoute = useCallback(async () => {
    setIsCalculating(true)
    setError(null)

    try {
      // Build planner request from selected targets
      const request: PlannerRequest = {
        targetItemNames: priorityTargets.filter(t => t.type === 'ITEM').map(t => t.name),
        targetEnemyTypes: priorityTargets.filter(t => t.type === 'ENEMY').map(t => t.name),
        targetRecipeIds: priorityTargets.filter(t => t.type === 'RECIPE').map(t => String(t.id)),
        targetContainerTypes: priorityTargets.filter(t => t.type === 'CONTAINER').map(t => t.name),
        ongoingItemNames: ongoingTargets.filter(t => t.type === 'ITEM').map(t => t.name),
        routingProfile,
        hasRaiderKey,
      }

      // Call planner API (returns array of routes ranked by score)
      const routes = await plannerApi.generateRoute(request)

      if (routes && routes.length > 0) {
        // Use the top-ranked route
        setCalculatedRoute(routes[0])
        setUiMode('PLANNING')
      } else {
        setError('No routes found for selected targets')
      }
    } catch (err) {
      console.error('Failed to calculate route:', err)
      setError(err instanceof Error ? err.message : 'Failed to calculate route')
    } finally {
      setIsCalculating(false)
    }
  }, [priorityTargets, ongoingTargets, routingProfile, hasRaiderKey])

  const handleMinimize = useCallback(() => {
    setCalculatedRoute(null)
    setUiMode('SELECTION')
  }, [])

  const handleMapChange = useCallback(async (newMap: string) => {
    setSelectedMap(newMap)
    setCalculatedRoute(null) // Clear old route

    // Re-highlight zones for selected item on new map
    if (selectedItem) {
      try {
        const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name)
        setHighlightedZones(zones)
      } catch (error) {
        console.error('Failed to fetch zones:', error)
        setHighlightedZones([])
      }
    }
  }, [selectedItem])

  // A11Y state
  const [accessibilityMode, setAccessibilityMode] = useState(false)

  // Mobile panel state
  const [activeMobilePanel, setActiveMobilePanel] = useState<'objectives' | 'details' | 'map'>('objectives')

  const toggleAccessibility = () => {
    setAccessibilityMode(!accessibilityMode)
    document.body.classList.toggle('accessibility-mode')
  }

  // Keyboard shortcuts
  useKeyboardShortcuts([
    {
      key: '?',
      shift: true,
      action: () => {
        setShowShortcutsModal(true)
      },
      description: 'Show keyboard shortcuts help'
    },
    {
      key: 'Escape',
      action: () => {
        if (showShortcutsModal) {
          setShowShortcutsModal(false)
        } else if (uiMode === 'PLANNING') {
          handleMinimize()
        } else if (error) {
          setError(null)
        }
      },
      description: 'Close route or dismiss error'
    },
    {
      key: 'Enter',
      ctrl: true,
      action: () => {
        if (priorityTargets.length > 0 && uiMode === 'SELECTION') {
          handleCalculateRoute()
        }
      },
      description: 'Calculate route'
    },
    {
      key: 'a',
      ctrl: true,
      action: () => {
        toggleAccessibility()
      },
      description: 'Toggle accessibility mode'
    }
  ], !showShortcutsModal)

  // Auto-switch to map tab on mobile when route is calculated
  useEffect(() => {
    if (uiMode === 'PLANNING') {
      setActiveMobilePanel('map')
    }
  }, [uiMode])

  return (
    <div className="fixed inset-0 w-full h-full bg-retro-bg overflow-hidden flex flex-col">
      {/* Global CRT Overlay */}
      <div className="absolute inset-0 crt-overlay pointer-events-none z-50"></div>

      {/* Top Header */}
      <header className="h-12 border-b border-retro-sand/20 bg-retro-dark flex items-center justify-between px-4 z-30">
        <h1 className="text-retro-sand font-display text-sm md:text-lg tracking-widest uppercase truncate">
          <span className="hidden md:inline">TACTICAL PLANNER // </span>
          <span className="text-retro-orange text-glow">{selectedMap || 'NO SIGNAL'}</span>
        </h1>
        <div className="flex items-center gap-2">
          <Tooltip content="Keyboard shortcuts (Press ? to open)" position="bottom">
            <button
              onClick={() => setShowShortcutsModal(true)}
              className="text-xs font-mono text-retro-sand-dim hover:text-retro-sand border border-retro-sand/20 hover:border-retro-orange px-2 py-1 transition-colors"
              aria-label="Show keyboard shortcuts"
            >
              [?]
            </button>
          </Tooltip>
          <Tooltip
            content={`Accessibility mode ${accessibilityMode ? 'enabled' : 'disabled'}. Removes CRT effects and uses system fonts. Shortcut: Ctrl+A`}
            position="bottom"
          >
            <button
              onClick={toggleAccessibility}
              className="text-xs font-mono text-retro-sand-dim hover:text-retro-sand border border-retro-sand/20 hover:border-retro-orange px-2 py-1 transition-colors"
              aria-label={accessibilityMode ? 'Disable accessibility mode' : 'Enable accessibility mode'}
            >
              {accessibilityMode ? '[A11Y: ON]' : '[A11Y: OFF]'}
            </button>
          </Tooltip>
        </div>
      </header>

      {/* Mobile Navigation Tabs (visible only on mobile) */}
      <nav className="md:hidden flex border-b border-retro-sand/20 bg-retro-dark z-20" role="tablist">
        <button
          role="tab"
          aria-selected={activeMobilePanel === 'objectives'}
          onClick={() => setActiveMobilePanel('objectives')}
          className={`flex-1 py-2 font-mono text-xs uppercase transition-colors ${
            activeMobilePanel === 'objectives'
              ? 'bg-retro-orange text-retro-dark border-b-2 border-retro-orange'
              : 'text-retro-sand-dim hover:text-retro-sand'
          }`}
        >
          Objectives
        </button>
        <button
          role="tab"
          aria-selected={activeMobilePanel === 'details'}
          onClick={() => setActiveMobilePanel('details')}
          className={`flex-1 py-2 font-mono text-xs uppercase transition-colors ${
            activeMobilePanel === 'details'
              ? 'bg-retro-orange text-retro-dark border-b-2 border-retro-orange'
              : 'text-retro-sand-dim hover:text-retro-sand'
          }`}
        >
          Details
        </button>
        <button
          role="tab"
          aria-selected={activeMobilePanel === 'map'}
          onClick={() => setActiveMobilePanel('map')}
          className={`flex-1 py-2 font-mono text-xs uppercase transition-colors ${
            activeMobilePanel === 'map'
              ? 'bg-retro-orange text-retro-dark border-b-2 border-retro-orange'
              : 'text-retro-sand-dim hover:text-retro-sand'
          }`}
        >
          Map
        </button>
      </nav>

      {/* Main Content Grid - Responsive Layout */}
      <div className="flex-1 grid grid-cols-1 md:grid-cols-[300px_1fr] lg:grid-cols-[300px_1fr_2fr] gap-4 p-2 md:p-4 overflow-hidden">
        {/* Left Panel - Objectives (Mobile: tab-controlled, Desktop: always visible) */}
        <div className={`bg-retro-dark rounded-lg p-4 overflow-y-auto border border-retro-sand/20 relative ${
          activeMobilePanel === 'objectives' ? 'block' : 'hidden md:block'
        }`} role="tabpanel" aria-labelledby="objectives-tab">
          <div className="absolute inset-0 crt-overlay pointer-events-none"></div>
          <div className="relative z-10">
            <LeftPanel
              onItemSelect={handleItemSelect}
              priorityTargets={priorityTargets}
              ongoingTargets={ongoingTargets}
              addPriorityTarget={addPriorityTarget}
              removePriorityTarget={removePriorityTarget}
              addOngoingTarget={addOngoingTarget}
              removeOngoingTarget={removeOngoingTarget}
              priorityCount={priorityCount}
              ongoingCount={ongoingCount}
            />
          </div>
        </div>

        {/* Center Panel - Details (Mobile: tab-controlled, Desktop: hidden in PLANNING mode) */}
        {uiMode === 'SELECTION' && (
          <div className={`bg-retro-dark rounded-lg p-4 overflow-y-auto border border-retro-sand/20 relative ${
            activeMobilePanel === 'details' ? 'block' : 'hidden lg:block'
          }`} role="tabpanel" aria-labelledby="details-tab">
            <div className="absolute inset-0 crt-overlay pointer-events-none"></div>
            <div className="relative z-10">
              <CenterPanel item={selectedItem} />
            </div>
          </div>
        )}

        {/* Right Panel - Map (Mobile: tab-controlled, Desktop: expands in PLANNING mode) */}
        <div className={`bg-retro-dark rounded-lg overflow-hidden relative ${
          uiMode === 'PLANNING' ? 'md:col-span-2 lg:col-span-2' : ''
        } ${
          activeMobilePanel === 'map' ? 'block' : 'hidden md:block'
        }`} role="tabpanel" aria-labelledby="map-tab">
          {/* Error notification */}
          {error && (
            <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-30 bg-retro-red text-retro-sand font-mono px-6 py-3 border-2 border-retro-red shadow-lg">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {error}
                <button onClick={() => setError(null)} className="ml-2 hover:text-white transition-colors">✕</button>
              </div>
            </div>
          )}

          {/* Loading overlay */}
          {isCalculating && (
            <div className="absolute inset-0 z-20 bg-retro-black/80 flex items-center justify-center backdrop-blur-sm">
              <div className="bg-retro-dark border-2 border-retro-orange rounded-lg p-6 flex flex-col items-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-retro-orange mb-4"></div>
                <p className="text-retro-orange font-display font-bold text-glow uppercase tracking-wider">Calculating optimal route...</p>
                <p className="text-retro-sand-dim font-mono text-sm mt-2">Analyzing {priorityTargets.length} target{priorityTargets.length !== 1 ? 's' : ''}</p>
              </div>
            </div>
          )}

          {uiMode === 'SELECTION' ? (
            <MinimizedMapView
              selectedMap={selectedMap}
              onMapChange={handleMapChange}
              highlightedZones={highlightedZones}
              onCalculateRoute={handleCalculateRoute}
              hasTargets={priorityTargets.length > 0}
            />
          ) : (
            <Suspense fallback={<MapSkeleton />}>
              <MaximizedMapView
                route={calculatedRoute}
                onMinimize={handleMinimize}
                routingProfile={routingProfile}
                onRecalculate={handleCalculateRoute}
              />
            </Suspense>
          )}
        </div>
      </div>

      {/* Keyboard Shortcuts Help Modal */}
      <KeyboardShortcutsModal
        isOpen={showShortcutsModal}
        onClose={() => setShowShortcutsModal(false)}
      />
    </div>
  )
}
