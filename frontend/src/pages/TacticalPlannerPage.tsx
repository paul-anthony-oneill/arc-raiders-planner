import React, { useState, useCallback } from 'react'
import { useTargetSelection } from '../hooks/useTargetSelection'
import { LeftPanel } from '../components/LeftPanel'
import { CenterPanel } from '../components/CenterPanel'
import { MinimizedMapView } from '../components/MinimizedMapView'
import { MaximizedMapView } from '../components/MaximizedMapView'
import { itemApi } from '../api/itemApi'
import { plannerApi } from '../api/plannerApi'
import type { Item, PlannerResponse, Area, PlannerRequest } from '../types'

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
  } = useTargetSelection()

  // UI state (local to page)
  const [uiMode, setUiMode] = useState<'SELECTION' | 'PLANNING'>('SELECTION')
  const [selectedMap, setSelectedMap] = useState<string>('Dam Battlegrounds')
  const [selectedItem, setSelectedItem] = useState<Item | null>(null)
  const [highlightedZones] = useState<Area[]>([])
  const [calculatedRoute, setCalculatedRoute] = useState<PlannerResponse | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Handle item selection from LeftPanel
  const handleItemSelect = useCallback(async (item: Item) => {
    try {
      // Fetch enhanced item details
      const details = await itemApi.getItemDetails(item.id)
      setSelectedItem(details)

      // TODO: Highlight zones on current map
      // const zones = await mapApi.getZonesWithItem(selectedMap, item.name);
      // setHighlightedZones(zones);
    } catch (error) {
      console.error('Failed to fetch item details:', error)
      setSelectedItem(item) // Fallback to basic item data
    }
  }, [])

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

  const handleMapChange = useCallback((newMap: string) => {
    setSelectedMap(newMap)
    setCalculatedRoute(null) // Clear old route

    // TODO: Re-highlight zones for selected item on new map
    // if (selectedItem) {
    //   const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name);
    //   setHighlightedZones(zones);
    // }
  }, [])

  return (
    <div className="h-screen grid grid-cols-[300px_1fr_2fr] gap-4 p-4 bg-gray-900">
      {/* Left Panel - Always visible */}
      <div className="bg-gray-800 rounded-lg p-4 overflow-y-auto">
        <LeftPanel onItemSelect={handleItemSelect} />
      </div>

      {/* Center Panel - Hidden in PLANNING mode */}
      {uiMode === 'SELECTION' && (
        <div className="bg-gray-800 rounded-lg p-4 overflow-y-auto">
          <CenterPanel item={selectedItem} />
        </div>
      )}

      {/* Right Panel - Expands in PLANNING mode */}
      <div className={`bg-gray-800 rounded-lg overflow-hidden ${
        uiMode === 'PLANNING' ? 'col-span-2' : ''
      }`}>
        {/* Error notification */}
        {error && (
          <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-30 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg">
            <div className="flex items-center gap-2">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {error}
              <button onClick={() => setError(null)} className="ml-2 hover:text-gray-200">✕</button>
            </div>
          </div>
        )}

        {/* Loading overlay */}
        {isCalculating && (
          <div className="absolute inset-0 z-20 bg-black/50 flex items-center justify-center">
            <div className="bg-gray-800 rounded-lg p-6 flex flex-col items-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mb-4"></div>
              <p className="text-white font-semibold">Calculating optimal route...</p>
              <p className="text-gray-400 text-sm mt-2">Analyzing {priorityTargets.length} target{priorityTargets.length !== 1 ? 's' : ''}</p>
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
          <MaximizedMapView
            route={calculatedRoute}
            onMinimize={handleMinimize}
            routingProfile={routingProfile}
          />
        )}
      </div>
    </div>
  )
}
