import type { PlannerResponse } from '../types'

/**
 * Export route to JSON file.
 * WHY: Allows users to save calculated routes for later reference or sharing
 *
 * @param route The route to export
 */
export const exportRouteToJSON = (route: PlannerResponse): void => {
  const data = JSON.stringify(route, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `route-${route.mapName}-${Date.now()}.json`
  a.click()

  URL.revokeObjectURL(url)
}

/**
 * Share route via URL.
 * WHY: Enables users to share their planned routes with teammates
 *
 * @param priorityTargets Priority targets (items/enemies/containers)
 * @param ongoingTargets Ongoing targets
 * @param selectedMap Map name
 * @param routingProfile Routing profile
 * @param hasRaiderKey Whether user has raider key
 * @returns Share URL
 */
export const shareRoute = (
  priorityTargets: Array<{ id: number | string; type: string; name: string }>,
  ongoingTargets: Array<{ id: number | string; type: string; name: string }>,
  selectedMap: string,
  routingProfile: string,
  hasRaiderKey: boolean
): string => {
  const routeData = {
    priorityTargets,
    ongoingTargets,
    map: selectedMap,
    profile: routingProfile,
    hasRaiderKey,
  }

  const encoded = encodeURIComponent(JSON.stringify(routeData))
  const shareUrl = `${window.location.origin}/planner?route=${encoded}`

  // Copy to clipboard
  navigator.clipboard.writeText(shareUrl).then(
    () => {
      console.log('Route link copied to clipboard!')
    },
    (err) => {
      console.error('Failed to copy route link:', err)
    }
  )

  return shareUrl
}

/**
 * Parse route data from URL parameters.
 * WHY: Loads shared routes from URL
 *
 * @returns Parsed route data or null if no route in URL
 */
export const loadRouteFromURL = ():
  | {
      priorityTargets: Array<{ id: number | string; type: string; name: string }>
      ongoingTargets: Array<{ id: number | string; type: string; name: string }>
      map: string
      profile: string
      hasRaiderKey: boolean
    }
  | null => {
  const params = new URLSearchParams(window.location.search)
  const routeData = params.get('route')

  if (!routeData) {
    return null
  }

  try {
    const decoded = JSON.parse(decodeURIComponent(routeData))
    return decoded
  } catch (error) {
    console.error('Invalid route data in URL:', error)
    return null
  }
}

/**
 * Trigger browser print dialog for current route.
 * WHY: Allows users to print routes for offline reference during raids
 */
export const printRoute = (): void => {
  window.print()
}
