import React, { useState, useCallback } from 'react'
import { useTargetSelection } from '../hooks/useTargetSelection'
import { LeftPanel } from '../components/LeftPanel'
import { CenterPanel } from '../components/CenterPanel'
import { MinimizedMapView } from '../components/MinimizedMapView'
import { MaximizedMapView } from '../components/MaximizedMapView'
import { itemApi } from '../api/itemApi'
import type { Item, PlannerResponse, Area } from '../types'

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
    // TODO: Build request and call planner API
    // const request: PlannerRequest = {
    //   targetItemNames: priorityTargets.filter(t => t.type === 'ITEM').map(t => t.name),
    //   targetEnemyTypes: priorityTargets.filter(t => t.type === 'ENEMY').map(t => t.name),
    //   targetRecipeIds: priorityTargets.filter(t => t.type === 'RECIPE').map(t => String(t.id)),
    //   targetContainerTypes: priorityTargets.filter(t => t.type === 'CONTAINER').map(t => t.name),
    //   ongoingItemNames: ongoingTargets.filter(t => t.type === 'ITEM').map(t => t.name),
    //   routingProfile,
    //   hasRaiderKey,
    // };
    // const route = await plannerApi.generateRoute(request);
    // setCalculatedRoute(route);
    setUiMode('PLANNING')
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
          />
        )}
      </div>
    </div>
  )
}
