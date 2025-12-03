import React, { useMemo } from 'react'
import MapComponent from '../MapComponent'
import type { PlannerResponse, Area } from '../types'

interface MaximizedMapViewProps {
  route: PlannerResponse | null
  onMinimize: () => void
  routingProfile?: string
}

/**
 * Maximized Map View - Planning Mode
 * WHY: Shows full route visualization with waypoints and extraction points
 */
export const MaximizedMapView: React.FC<MaximizedMapViewProps> = ({
  route,
  onMinimize,
  routingProfile,
}) => {
  // Convert waypoints to areas for MapComponent
  const areas = useMemo<Area[]>(() => {
    if (!route || !route.path) return []

    return route.path
      .filter(wp => wp.type === 'AREA')
      .map(wp => ({
        id: typeof wp.id === 'number' ? wp.id : parseInt(wp.id as string, 10),
        name: wp.name,
        mapX: wp.x,
        mapY: wp.y,
        coordinates: undefined, // Will be loaded by map if needed
        lootTypes: wp.lootTypes || [],
        lootAbundance: wp.lootAbundance,
        ongoingMatchItems: wp.ongoingMatchItems,
        targetMatchItems: wp.targetMatchItems,
      }))
  }, [route])

  if (!route) {
    return (
      <div className="flex items-center justify-center h-full bg-gray-800 rounded-lg">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4"></div>
          <p className="text-gray-400">Calculating route...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="relative h-full bg-gray-800 rounded-lg overflow-hidden">
      {/* Header with Minimize Button */}
      <div className="absolute top-0 left-0 right-0 z-20 p-4 bg-gradient-to-b from-gray-900 to-transparent">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-white">{route.mapName}</h2>
            <p className="text-sm text-gray-400">
              {route.path.length} waypoint{route.path.length !== 1 ? 's' : ''} • Score: {route.score.toFixed(0)}
            </p>
          </div>
          <button
            onClick={onMinimize}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors flex items-center gap-2"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
            Minimize
          </button>
        </div>
      </div>

      {/* Route Stats Panel */}
      <div className="absolute top-20 left-4 z-20 bg-gray-900/90 backdrop-blur-sm rounded-lg p-4 max-w-xs">
        <h3 className="text-sm font-semibold text-orange-500 mb-3 uppercase">Route Information</h3>

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-400">Waypoints:</span>
            <span className="text-white font-semibold">{route.path.length}</span>
          </div>

          <div className="flex justify-between">
            <span className="text-gray-400">Score:</span>
            <span className="text-white font-semibold">{route.score.toFixed(0)}</span>
          </div>

          {route.extractionPoint && (
            <div className="flex justify-between">
              <span className="text-gray-400">Extraction:</span>
              <span className="text-white font-semibold">{route.extractionPoint}</span>
            </div>
          )}

          {route.nearbyEnemySpawns && route.nearbyEnemySpawns.length > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-400">Enemy Spawns:</span>
              <span className="text-white font-semibold">{route.nearbyEnemySpawns.length}</span>
            </div>
          )}
        </div>
      </div>

      {/* Map Visualization Area */}
      <div className="absolute inset-0 bg-gray-900">
        <MapComponent
          mapName={route.mapName}
          areas={areas}
          routePath={route.path}
          extractionPoint={route.extractionPoint}
          extractionLat={route.extractionLat}
          extractionLng={route.extractionLng}
          enemySpawns={route.nearbyEnemySpawns || []}
          showRoutePath={true}
          routingProfile={routingProfile as any}
        />
      </div>
    </div>
  )
}
