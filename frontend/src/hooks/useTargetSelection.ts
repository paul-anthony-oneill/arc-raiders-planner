import { useState, useEffect, useCallback } from 'react'
import type { SessionState, TargetSelection, RoutingProfile } from '../types'

const SESSION_STORAGE_KEY = 'arc_raiders_planner_session'

/**
 * Initial/default state for new sessions
 */
const getInitialState = (): SessionState => ({
  priorityTargets: [],
  ongoingTargets: [],
  routingProfile: 'PURE_SCAVENGER',
  hasRaiderKey: false,
  lastCalculatedHash: null,
})

/**
 * Load state from session storage, or return initial state
 */
const loadSessionState = (): SessionState => {
  try {
    const stored = sessionStorage.getItem(SESSION_STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored) as SessionState
      return parsed
    }
  } catch (error) {
    console.error('Failed to load session state:', error)
  }
  return getInitialState()
}

/**
 * Save state to session storage
 */
const saveSessionState = (state: SessionState): void => {
  try {
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(state))
  } catch (error) {
    console.error('Failed to save session state:', error)
  }
}

/**
 * Compute hash of current selections for detecting changes
 */
const computeSelectionHash = (state: SessionState): string => {
  const { priorityTargets, ongoingTargets, routingProfile, hasRaiderKey } = state
  const data = {
    priority: priorityTargets.map(t => ({ id: t.id, type: t.type })).sort((a, b) => String(a.id).localeCompare(String(b.id))),
    ongoing: ongoingTargets.map(t => ({ id: t.id, type: t.type })).sort((a, b) => String(a.id).localeCompare(String(b.id))),
    profile: routingProfile,
    key: hasRaiderKey,
  }
  return JSON.stringify(data)
}

/**
 * Custom hook for managing target selection state with session persistence
 * WHY: Persists user selections between Setup and Planner pages within a browser session
 */
export const useTargetSelection = () => {
  const [state, setState] = useState<SessionState>(loadSessionState)

  // Save to session storage whenever state changes
  useEffect(() => {
    saveSessionState(state)
  }, [state])

  // Persist on browser close/refresh (beforeunload)
  useEffect(() => {
    const handleBeforeUnload = () => {
      saveSessionState(state)
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [state])

  // Add a priority target
  const addPriorityTarget = useCallback((target: TargetSelection) => {
    setState(prev => {
      // Check if already exists
      if (prev.priorityTargets.some(t => t.id === target.id && t.type === target.type)) {
        return prev
      }
      // Enforce 5 priority limit (items + enemies + containers combined)
      if (prev.priorityTargets.length >= 5) {
        console.warn('Cannot add more than 5 priority targets')
        return prev
      }
      return {
        ...prev,
        priorityTargets: [...prev.priorityTargets, { ...target, priority: 'PRIORITY' }],
      }
    })
  }, [])

  // Add an ongoing target (recipes only, up to 10)
  const addOngoingTarget = useCallback((target: TargetSelection) => {
    setState(prev => {
      // Check if already exists
      if (prev.ongoingTargets.some(t => t.id === target.id && t.type === target.type)) {
        return prev
      }
      // Enforce 10 ongoing limit
      if (prev.ongoingTargets.length >= 10) {
        console.warn('Cannot add more than 10 ongoing targets')
        return prev
      }
      return {
        ...prev,
        ongoingTargets: [...prev.ongoingTargets, { ...target, priority: 'ONGOING' }],
      }
    })
  }, [])

  // Remove a priority target
  const removePriorityTarget = useCallback((id: string | number, type: TargetSelection['type']) => {
    setState(prev => ({
      ...prev,
      priorityTargets: prev.priorityTargets.filter(t => !(t.id === id && t.type === type)),
    }))
  }, [])

  // Remove an ongoing target
  const removeOngoingTarget = useCallback((id: string | number, type: TargetSelection['type']) => {
    setState(prev => ({
      ...prev,
      ongoingTargets: prev.ongoingTargets.filter(t => !(t.id === id && t.type === type)),
    }))
  }, [])

  // Move priority target to ongoing (for modal adjustment)
  const movePriorityToOngoing = useCallback((id: string | number, type: TargetSelection['type']) => {
    setState(prev => {
      const target = prev.priorityTargets.find(t => t.id === id && t.type === type)
      if (!target) return prev

      // Only allow recipes to be moved to ongoing
      if (target.type !== 'RECIPE') {
        console.warn('Only recipes can be moved to ongoing')
        return prev
      }

      return {
        ...prev,
        priorityTargets: prev.priorityTargets.filter(t => !(t.id === id && t.type === type)),
        ongoingTargets: [...prev.ongoingTargets, { ...target, priority: 'ONGOING' }],
      }
    })
  }, [])

  // Set routing profile
  const setRoutingProfile = useCallback((profile: RoutingProfile) => {
    setState(prev => ({ ...prev, routingProfile: profile }))
  }, [])

  // Set raider key status
  const setHasRaiderKey = useCallback((hasKey: boolean) => {
    setState(prev => ({ ...prev, hasRaiderKey: hasKey }))
  }, [])

  // Mark route as calculated (store current selection hash)
  const markRouteCalculated = useCallback(() => {
    setState(prev => ({
      ...prev,
      lastCalculatedHash: computeSelectionHash(prev),
    }))
  }, [])

  // Check if route is outdated (selections changed since last calculation)
  const isRouteOutdated = useCallback((): boolean => {
    if (!state.lastCalculatedHash) return false
    const currentHash = computeSelectionHash(state)
    return currentHash !== state.lastCalculatedHash
  }, [state])

  // Clear all selections
  const clearAllTargets = useCallback(() => {
    setState(prev => ({
      ...prev,
      priorityTargets: [],
      ongoingTargets: [],
      lastCalculatedHash: null,
    }))
  }, [])

  // Reset session to initial state
  const resetSession = useCallback(() => {
    setState(getInitialState())
  }, [])

  return {
    // State
    priorityTargets: state.priorityTargets,
    ongoingTargets: state.ongoingTargets,
    routingProfile: state.routingProfile,
    hasRaiderKey: state.hasRaiderKey,

    // Actions
    addPriorityTarget,
    addOngoingTarget,
    removePriorityTarget,
    removeOngoingTarget,
    movePriorityToOngoing,
    setRoutingProfile,
    setHasRaiderKey,
    markRouteCalculated,
    clearAllTargets,
    resetSession,

    // Computed
    isRouteOutdated: isRouteOutdated(),
    priorityCount: state.priorityTargets.length,
    ongoingCount: state.ongoingTargets.length,
    canAddPriority: state.priorityTargets.length < 5,
    canAddOngoing: state.ongoingTargets.length < 10,
  }
}
