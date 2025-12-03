import React, { useState, useCallback } from 'react'
import { useTargetSelection } from '../hooks/useTargetSelection'
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
  const [, ] = useState<Area[]>([])
  const [, setCalculatedRoute] = useState<PlannerResponse | null>(null)

  const handleCalculateRoute = useCallback(async () => {
    // TODO: Build request and call planner API
    // const route = await plannerApi.generateRoute(request);
    // setCalculatedRoute(route);
    setUiMode('PLANNING')
  }, [priorityTargets, ongoingTargets, selectedMap, routingProfile, hasRaiderKey])

  const handleMinimize = useCallback(() => {
    setCalculatedRoute(null)
    setUiMode('SELECTION')
  }, [])

  const handleMapChange = useCallback(async (newMap: string) => {
    setSelectedMap(newMap)
    setCalculatedRoute(null) // Clear old route

    // TODO: Re-highlight zones for selected item on new map
    // if (selectedItem) {
    //   const zones = await mapApi.getZonesWithItem(newMap, selectedItem.name);
    //   setHighlightedZones(zones);
    // }
  }, [selectedItem])

  return (
    <div className="h-screen grid grid-cols-[300px_1fr_2fr] gap-4 p-4 bg-gray-900">
      {/* Left Panel - Always visible */}
      <div className="bg-gray-800 rounded-lg p-4 overflow-y-auto">
        <h2 className="text-xl font-bold text-white mb-4">Objectives</h2>
        <p className="text-gray-400 text-sm">Left Panel - Coming soon</p>
        {/* TODO: Add LeftPanel component */}
      </div>

      {/* Center Panel - Hidden in PLANNING mode */}
      {uiMode === 'SELECTION' && (
        <div className="bg-gray-800 rounded-lg p-4 overflow-y-auto">
          <h2 className="text-xl font-bold text-white mb-4">Item Details</h2>
          {selectedItem ? (
            <p className="text-gray-400 text-sm">Selected: {selectedItem.name}</p>
          ) : (
            <p className="text-gray-400 text-sm">Select an item to view details</p>
          )}
          {/* TODO: Add CenterPanel component */}
        </div>
      )}

      {/* Right Panel - Expands in PLANNING mode */}
      <div className={`bg-gray-800 rounded-lg overflow-hidden ${
        uiMode === 'PLANNING' ? 'col-span-2' : ''
      }`}>
        {uiMode === 'SELECTION' ? (
          <div className="h-full flex flex-col">
            <div className="p-4 border-b border-gray-700">
              <h2 className="text-xl font-bold text-white">Map Preview</h2>
              <select
                value={selectedMap}
                onChange={(e) => handleMapChange(e.target.value)}
                className="mt-2 bg-gray-700 text-white px-4 py-2 rounded border border-gray-600 w-full"
              >
                <option value="Dam Battlegrounds">Dam Battlegrounds</option>
                <option value="Ironwood Hydroponics">Ironwood Hydroponics</option>
                <option value="Scrapworks">Scrapworks</option>
              </select>
            </div>
            <div className="flex-1 relative">
              <div className="absolute inset-0 flex items-center justify-center text-gray-500">
                Map visualization - Coming soon
              </div>
              {/* TODO: Add MinimizedMapView component */}
              <button
                onClick={handleCalculateRoute}
                disabled={priorityTargets.length === 0}
                className={`absolute bottom-6 right-6 px-6 py-3 rounded-full font-semibold shadow-lg transition-all duration-200 ${
                  priorityTargets.length > 0
                    ? 'bg-orange-500 hover:bg-orange-600 text-white cursor-pointer'
                    : 'bg-gray-600 text-gray-400 cursor-not-allowed'
                }`}
              >
                Calculate Route
              </button>
            </div>
          </div>
        ) : (
          <div className="h-full relative">
            <button
              onClick={handleMinimize}
              className="absolute top-4 right-4 z-20 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg"
            >
              ✕ Minimize
            </button>
            <div className="h-full flex items-center justify-center text-gray-500">
              Route visualization - Coming soon
            </div>
            {/* TODO: Add MaximizedMapView component */}
          </div>
        )}
      </div>
    </div>
  )
}
