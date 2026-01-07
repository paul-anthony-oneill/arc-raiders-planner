import type { Item } from '../types'

const API_URL = '/api/items'

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

  /**
   * Get recipe chain for an item (prerequisites and recursive crafting info).
   *
   * @param itemId The item ID
   * @returns RecipeChain object
   */
  getRecipeChain: async (itemId: number): Promise<import('../types').RecipeChain | null> => {
    const response = await fetch(`${API_URL}/${itemId}/recipe-chain`)
    if (!response.ok) {
        // Return null for 404 (no recipe) or throw?
        // Let's assume 200 with empty body or null if not found
        // Backend returns null if no recipe.
        if (response.status === 204 || response.status === 404) {
            return null as any;
        }
        throw new Error(`Failed to fetch recipe chain: ${response.statusText}`)
    }
    // Handle null response content
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }
}
