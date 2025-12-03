import React from 'react'
import MapComponent from '../MapComponent'
import type { Area } from '../types'

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
  return (
    <div className="relative h-full bg-gray-800 rounded-lg overflow-hidden flex flex-col">
      {/* Map Switcher */}
      <div className="p-4 border-b border-gray-700">
        <label className="block text-xs text-gray-400 mb-2 font-semibold">SELECT MAP</label>
        <select
          value={selectedMap}
          onChange={(e) => onMapChange(e.target.value)}
          className="w-full bg-gray-700 text-white px-4 py-2 rounded border border-gray-600 focus:outline-none focus:border-orange-500"
        >
          <option value="Dam Battlegrounds">Dam Battlegrounds</option>
          <option value="Ironwood Hydroponics">Ironwood Hydroponics</option>
          <option value="Scrapworks">Scrapworks</option>
        </select>
      </div>

      {/* Map Preview Area */}
      <div className="flex-1 relative bg-gray-900">
        <MapComponent
          mapName={selectedMap}
          areas={highlightedZones}
          showRoutePath={false}
          routePath={[]}
          enemySpawns={[]}
        />
      </div>

      {/* Calculate Route FAB */}
      <button
        onClick={onCalculateRoute}
        disabled={!hasTargets}
        className={`absolute bottom-6 right-6 z-20 px-6 py-3 rounded-full font-semibold shadow-lg transition-all duration-200 ${
          hasTargets
            ? 'bg-orange-500 hover:bg-orange-600 text-white cursor-pointer transform hover:scale-105'
            : 'bg-gray-600 text-gray-400 cursor-not-allowed'
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
  )
}
