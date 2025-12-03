import type { Item } from '../types'

const API_URL = 'http://localhost:8080/api/items'

export const itemApi = {
  /**
   * Get item with full crafting/usage context.
   * WHY: Provides data for tactical planner center panel detail view
   *
   * @param itemId The item ID
   * @returns Item with crafting recipe, usage recipes, and drop sources
   */
  getItemDetails: async (itemId: number): Promise<Item> => {
    const response = await fetch(`${API_URL}/${itemId}/details`)
    if (!response.ok) {
      throw new Error(`Failed to fetch item details: ${response.statusText}`)
    }
    return response.json()
  },

  /**
   * Search items by name
   *
   * @param search Search term
   * @returns List of matching items
   */
  searchItems: async (search: string): Promise<Item[]> => {
    const response = await fetch(`${API_URL}?search=${encodeURIComponent(search)}`)
    if (!response.ok) {
      throw new Error(`Failed to search items: ${response.statusText}`)
    }
    return response.json()
  },

  /**
   * Get all items (limited to top 50)
   *
   * @returns List of items
   */
  getAllItems: async (): Promise<Item[]> => {
    const response = await fetch(API_URL)
    if (!response.ok) {
      throw new Error(`Failed to fetch items: ${response.statusText}`)
    }
    return response.json()
  },
}
