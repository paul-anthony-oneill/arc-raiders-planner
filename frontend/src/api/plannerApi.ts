import type { PlannerRequest, PlannerResponse } from '../types'

const API_URL = '/api/items'

export const plannerApi = {
  /**
   * Generate optimized raid routes based on target selections
   * WHY: Calls backend route optimization algorithm with all target types
   *
   * @param request Planning parameters (items, enemies, containers, profile)
   * @returns List of ranked map routes with scores
   */
  generateRoute: async (request: PlannerRequest): Promise<PlannerResponse[]> => {
    const response = await fetch(`${API_URL}/plan`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`Failed to generate route: ${response.statusText}. ${errorText}`)
    }

    return response.json()
  },
}
