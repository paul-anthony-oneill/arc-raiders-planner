import React, { useMemo } from 'react'
import MapComponent from '../MapComponent'
import { exportRouteToJSON, shareRoute, printRoute } from '../utils/routeExport'
import { useTargetSelection } from '../hooks/useTargetSelection'
import type { PlannerResponse, Area } from '../types'

interface MaximizedMapViewProps {
  route: PlannerResponse | null
  onMinimize: () => void
  routingProfile?: string
  onRecalculate: () => void
}

/**
 * Maximized Map View - Planning Mode
 * WHY: Shows full route visualization with waypoints and extraction points
 */
export const MaximizedMapView: React.FC<MaximizedMapViewProps> = ({
  route,
  onMinimize,
  routingProfile,
  onRecalculate,
}) => {
  const { priorityTargets, ongoingTargets, hasRaiderKey } = useTargetSelection()

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
      <div className="flex items-center justify-center h-full bg-retro-dark rounded-lg border border-retro-sand/20">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-retro-orange mx-auto mb-4"></div>
          <p className="text-retro-sand-dim font-mono">Calculating route...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="relative h-full bg-retro-dark rounded-lg overflow-hidden border border-retro-sand/20">
      {/* Header with Minimize Button */}
      <div className="absolute top-0 left-0 right-0 z-20 p-4 bg-gradient-to-b from-retro-black/90 to-transparent">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-display font-bold text-retro-orange text-glow uppercase tracking-widest">{route.mapName}</h2>
            <p className="text-sm text-retro-sand-dim font-mono">
              {route.path.length} waypoint{route.path.length !== 1 ? 's' : ''} • Score: {route.score.toFixed(0)}
            </p>
          </div>
          <button
            onClick={onMinimize}
            className="px-4 py-2 bg-retro-dark hover:bg-retro-black text-retro-sand border border-retro-sand/20 hover:border-retro-orange transition-colors flex items-center gap-2 font-mono"
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
      <div className="absolute top-20 left-4 z-20 bg-retro-dark/90 backdrop-blur-sm border border-retro-sand/20 rounded-lg p-4 max-w-xs">
        <h3 className="text-sm font-display font-semibold text-retro-orange text-glow mb-3 uppercase tracking-wider">Route Information</h3>

        <div className="space-y-2 text-sm font-mono">
          <div className="flex justify-between">
            <span className="text-retro-sand-dim">Waypoints:</span>
            <span className="text-retro-orange font-semibold">{route.path.length}</span>
          </div>

          <div className="flex justify-between">
            <span className="text-retro-sand-dim">Score:</span>
            <span className="text-retro-orange font-semibold">{route.score.toFixed(0)}</span>
          </div>

          {route.extractionPoint && (
            <div className="flex justify-between">
              <span className="text-retro-sand-dim">Extraction:</span>
              <span className="text-retro-sand font-semibold">{route.extractionPoint}</span>
            </div>
          )}

          {route.nearbyEnemySpawns && route.nearbyEnemySpawns.length > 0 && (
            <div className="flex justify-between">
              <span className="text-retro-sand-dim">Enemy Spawns:</span>
              <span className="text-retro-orange font-semibold">{route.nearbyEnemySpawns.length}</span>
            </div>
          )}
        </div>

        {/* Export/Share Actions */}
        <div className="mt-4 pt-4 border-t border-retro-sand/20 space-y-2">
            <button
                onClick={onRecalculate}
                className="w-full px-3 py-2 bg-retro-orange hover:bg-retro-orange-dark text-retro-black font-bold border border-retro-orange shadow-glow-orange transition-all font-mono text-sm flex items-center justify-center gap-2 mb-2"
            >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Recalculate Route
            </button>

          <button
            onClick={() => exportRouteToJSON(route)}
            className="w-full px-3 py-2 bg-retro-black hover:bg-retro-dark text-retro-sand border border-retro-sand/20 hover:border-retro-orange transition-colors font-mono text-sm flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Export JSON
          </button>

          <button
            onClick={() => {
              shareRoute(priorityTargets, ongoingTargets, route.mapName, routingProfile || 'PURE_SCAVENGER', hasRaiderKey)
              alert('Route link copied to clipboard!')
            }}
            className="w-full px-3 py-2 bg-retro-black hover:bg-retro-dark text-retro-sand border border-retro-sand/20 hover:border-retro-orange transition-colors font-mono text-sm flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            Share Link
          </button>

          <button
            onClick={printRoute}
            className="w-full px-3 py-2 bg-retro-black hover:bg-retro-dark text-retro-sand border border-retro-sand/20 hover:border-retro-orange transition-colors font-mono text-sm flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
            </svg>
            Print Route
          </button>
        </div>
      </div>

      {/* Map Visualization Area */}
      <div className="absolute inset-0 bg-retro-black z-0">
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
