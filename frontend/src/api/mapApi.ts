import type { Area } from '../types'

const API_URL = '/api/areas'

export const mapApi = {
  /**
   * Get all zones on a map that contain a specific item.
   * WHY: Enables zone highlighting when user selects an item in tactical planner,
   * providing immediate visual feedback on where the item can be found.
   *
   * @param mapName The map name (e.g., "Dam Battlegrounds")
   * @param itemName The item name to search for
   * @returns List of areas that contain this item's loot type
   */
  getZonesWithItem: async (mapName: string, itemName: string): Promise<Area[]> => {
    const params = new URLSearchParams({
      mapName,
      itemName,
    })

    const response = await fetch(`${API_URL}/by-map-and-item?${params}`)

    if (!response.ok) {
      throw new Error(`Failed to fetch zones: ${response.statusText}`)
    }

    return response.json()
  },
}
