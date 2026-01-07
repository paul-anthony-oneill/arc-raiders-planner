import React, { useState, useEffect } from 'react'
import MapComponent from '../MapComponent'
import type { Area } from '../types'
import type { GameMap } from '../utils/mapUtils'

interface MinimizedMapViewProps {
  selectedMap: string
  onMapChange: (mapName: string) => void
  highlightedZones: Area[]
  onCalculateRoute: () => void
  hasTargets: boolean
}

/**
 * Minimized Map View - Selection Mode
 * WHY: Shows map preview with zone highlights during objective selection
 */
export const MinimizedMapView: React.FC<MinimizedMapViewProps> = ({
  selectedMap,
  onMapChange,
  highlightedZones,
  onCalculateRoute,
  hasTargets,
}) => {
  const [maps, setMaps] = useState<GameMap[]>([])
  const [loading, setLoading] = useState(true)

  // Load maps from API
  useEffect(() => {
    fetch('/api/maps')
      .then(res => res.json())
      .then((data: GameMap[]) => {
        setMaps(data)
        setLoading(false)
      })
      .catch(err => {
        console.error('Failed to load maps:', err)
        setLoading(false)
      })
  }, [])

  return (
    <div className="relative h-full bg-retro-dark rounded-lg flex flex-col border border-retro-sand/20">
      {/* Map Switcher */}
      <div className="p-4 border-b border-retro-sand/20 z-10">
        <label className="block text-xs text-retro-sand-dim mb-2 font-mono font-semibold uppercase tracking-wider">SELECT MAP</label>
        <select
          value={selectedMap}
          onChange={(e) => onMapChange(e.target.value)}
          disabled={loading}
          className="w-full bg-retro-black text-retro-sand font-mono px-4 py-2 border border-retro-sand/20 focus:outline-none focus:border-retro-orange transition-colors"
        >
          {loading ? (
            <option>Loading maps...</option>
          ) : maps.length > 0 ? (
            maps.map(map => (
              <option key={map.id} value={map.name}>
                {map.name}
              </option>
            ))
          ) : (
            <option>No maps available</option>
          )}
        </select>

        {/* Zone Count Indicator */}
        {highlightedZones.length > 0 && (
          <div className="mt-2 text-xs font-mono text-retro-orange">
            {highlightedZones.length} zone{highlightedZones.length !== 1 ? 's' : ''} highlighted
          </div>
        )}
      </div>

      {/* Map Preview Area with Calculate Button */}
      <div className="flex-1 relative bg-retro-black overflow-hidden">
        <MapComponent
          mapName={selectedMap}
          areas={highlightedZones}
          showRoutePath={false}
          routePath={[]}
          enemySpawns={[]}
        />

        {/* Calculate Route FAB - positioned relative to map container */}
        <button
          onClick={onCalculateRoute}
          disabled={!hasTargets}
          className={`absolute bottom-6 right-6 z-[2000] px-6 py-3 font-display font-bold uppercase tracking-widest transition-all duration-200 border-2 shadow-lg ${
            hasTargets
              ? 'bg-retro-orange hover:bg-retro-orange-dim text-retro-black border-retro-orange hover:border-retro-orange-dim cursor-pointer transform hover:scale-105 text-glow box-glow'
              : 'bg-retro-dark text-retro-sand-dim border-retro-sand/20 cursor-not-allowed opacity-50'
          }`}
        >
          <span className="flex items-center gap-2">
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"
              />
            </svg>
            Calculate Route
          </span>
        </button>
      </div>
    </div>
  )
}
