import React, { useState, useCallback, useEffect } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { CollapsibleSection } from '../components/CollapsibleSection'
import { TargetCard } from '../components/TargetCard'
import { ShoppingCart } from '../components/ShoppingCart'
import { PriorityLimitModal } from '../components/PriorityLimitModal'
import { CatalogIndex } from '../components/CatalogIndex'
import { useTargetSelection } from '../hooks/useTargetSelection'
import { recipeApi } from '../api/recipeApi'
import type { Item, TargetSelection, Recipe, EnemyType, ContainerType } from '../types'

/**
 * Setup Page - Initial landing page for target selection
 * WHY: Provides spacious interface for browsing and selecting objectives before route planning
 */
const SetupPage: React.FC = () => {
  const navigate = useNavigate()
  const {
    priorityTargets,
    ongoingTargets,
    routingProfile,
    hasRaiderKey,
    addPriorityTarget,
    addOngoingTarget,
    removePriorityTarget,
    removeOngoingTarget,
    movePriorityToOngoing,
    setRoutingProfile,
    setHasRaiderKey,
    markRouteCalculated,
    priorityCount,
    ongoingCount,
  } = useTargetSelection()

  const [showLimitModal, setShowLimitModal] = useState(false)
  const [recipes, setRecipes] = useState<Recipe[]>([])

  // Load recipes on mount
  useEffect(() => {
    recipeApi.getAllRecipes().then(setRecipes).catch(console.error)
  }, [])

  // Check if a target is selected and in which state
  const getSelectionState = useCallback(
    (id: string | number, type: TargetSelection['type']) => {
      const isPriority = priorityTargets.some(t => t.id === id && t.type === type)
      const isOngoing = ongoingTargets.some(t => t.id === id && t.type === type)

      if (isPriority) return 'priority' as const
      if (isOngoing) return 'ongoing' as const
      return 'unselected' as const
    },
    [priorityTargets, ongoingTargets]
  )

  // Handle target selection toggle
  const handleTargetToggle = useCallback(
    (target: TargetSelection) => {
      const currentState = getSelectionState(target.id, target.type)

      if (currentState === 'unselected') {
        // Try to add as priority
        if (priorityCount < 5) {
          addPriorityTarget(target)
        } else {
          // Already at limit, can't add more priority
          console.warn('Priority limit reached')
        }
      } else if (currentState === 'priority') {
        // Move priority -> ongoing (recipes only) or remove
        if (target.type === 'RECIPE' && ongoingCount < 10) {
          removePriorityTarget(target.id, target.type)
          addOngoingTarget(target)
        } else {
          // Remove from priority
          removePriorityTarget(target.id, target.type)
        }
      } else if (currentState === 'ongoing') {
        // Remove from ongoing
        removeOngoingTarget(target.id, target.type)
      }
    },
    [
      getSelectionState,
      priorityCount,
      ongoingCount,
      addPriorityTarget,
      addOngoingTarget,
      removePriorityTarget,
      removeOngoingTarget,
    ]
  )

  // Handle Calculate Route click
  const handleCalculateRoute = useCallback(() => {
    if (priorityCount > 5) {
      // Show modal to reduce to 5
      setShowLimitModal(true)
    } else if (priorityCount > 0 || ongoingCount > 0) {
      // Valid, proceed to planner
      markRouteCalculated()
      navigate({ to: '/planner' })
    }
  }, [priorityCount, ongoingCount, markRouteCalculated, navigate])

  // Handle modal acceptance
  const handleModalAccept = useCallback(
    (removedIds: Set<string>, movedToOngoingIds: Set<string>) => {
      // Remove targets
      removedIds.forEach(key => {
        const [type, ...idParts] = key.split('-')
        const id = idParts.join('-')
        removePriorityTarget(id, type as TargetSelection['type'])
      })

      // Move to ongoing
      movedToOngoingIds.forEach(key => {
        const [type, ...idParts] = key.split('-')
        const id = idParts.join('-')
        const target = priorityTargets.find(t => `${t.type}-${t.id}` === key)
        if (target) {
          movePriorityToOngoing(id, type as TargetSelection['type'])
        }
      })

      setShowLimitModal(false)

      // After adjustments, navigate to planner
      markRouteCalculated()
      navigate({ to: '/planner' })
    },
    [removePriorityTarget, movePriorityToOngoing, priorityTargets, markRouteCalculated, navigate]
  )

  return (
    <div className="fixed inset-0 w-full h-full bg-retro-bg overflow-hidden flex flex-col">
      {/* Global CRT Overlay */}
      <div className="absolute inset-0 crt-overlay pointer-events-none z-50"></div>

      {/* Header */}
      <header className="h-16 border-b border-retro-sand/20 bg-retro-dark flex items-center justify-between px-6 z-30 flex-shrink-0">
        <div>
          <h1 className="text-retro-sand font-display text-2xl tracking-widest">
            MISSION SETUP //{' '}
            <span className="text-retro-orange">TARGET SELECTION</span>
          </h1>
          <p className="text-xs text-retro-sand-dim font-mono mt-1">
            Select items, recipes, enemies, or containers to optimize your raid
          </p>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto pb-64 custom-scrollbar">
        <div className="max-w-7xl mx-auto p-6 space-y-4">
          {/* Help Panel */}
          <div className="
            bg-retro-dark/50
            border
            border-retro-sand/20
            p-4
            rounded
          ">
            <h2 className="
              text-sm
              font-display
              text-retro-orange
              uppercase
              tracking-wider
              mb-2
            ">
              💡 Quick Guide
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-retro-sand-dim font-mono">
              <div>
                <strong className="text-retro-sand">Target Limits:</strong>
                <ul className="mt-1 space-y-1 ml-4">
                  <li>• Priority: 5 max (affects routing)</li>
                  <li>• Ongoing: 10 max (tracked only)</li>
                </ul>
              </div>
              <div>
                <strong className="text-retro-sand">Routing Modes:</strong>
                <ul className="mt-1 space-y-1 ml-4">
                  <li>• 🔍 Scavenger: Max loot zones</li>
                  <li>• 🛡️ Avoid PvP: Safe routes</li>
                  <li>• 🚪 Easy Exfil: Near hatches (key req.)</li>
                  <li>• ✅ Safe Exfil: Safe + exit (key req.)</li>
                </ul>
              </div>
            </div>
            <p className="mt-3 text-xs text-retro-sand-dim italic">
              Tip: Click a target once to add as Priority. Click again to move to Ongoing (recipes only) or remove.
            </p>
          </div>
          {/* Items Section */}
          <CollapsibleSection title="Loot Items" icon="📦" defaultExpanded={true}>
            <p className="text-sm text-retro-sand-dim mb-4">
              Select items you want to find. Items count toward your 5 priority target limit.
            </p>
            <CatalogIndex<Item>
              apiUrl="/api/items"
              title=""
              onSelect={(item) => {
                const target: TargetSelection = {
                  id: item.id,
                  type: 'ITEM',
                  priority: 'PRIORITY',
                  name: item.name,
                  iconUrl: item.iconUrl,
                  rarity: item.rarity,
                  lootZone: item.lootType || undefined,
                  data: item,
                }
                handleTargetToggle(target)
              }}
              getItemKey={(item) => item.id}
              searchPlaceholder="Search items (e.g., circuit, gear)..."
              filterItems={(items) => items.filter((item) => item.lootType !== null)}
              renderItem={(item) => {
                const selectionState = getSelectionState(item.id, 'ITEM')
                return (
                  <TargetCard
                    id={item.id}
                    type="ITEM"
                    name={item.name}
                    iconUrl={item.iconUrl}
                    rarity={item.rarity}
                    lootZone={item.lootType || undefined}
                    description={item.description}
                    selectionState={selectionState}
                    onSelect={() => {
                      const target: TargetSelection = {
                        id: item.id,
                        type: 'ITEM',
                        priority: 'PRIORITY',
                        name: item.name,
                        iconUrl: item.iconUrl,
                        rarity: item.rarity,
                        lootZone: item.lootType || undefined,
                        data: item,
                      }
                      handleTargetToggle(target)
                    }}
                  />
                )
              }}
            />
          </CollapsibleSection>

          {/* Recipes Section */}
          <CollapsibleSection title="Crafting Recipes" icon="🔨">
            <p className="text-sm text-retro-sand-dim mb-4">
              Select recipes to track ingredients. Recipes can be Priority (5 limit) or Ongoing (10 limit).
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {recipes.map(recipe => {
                const selectionState = getSelectionState(recipe.id!, 'RECIPE')
                return (
                  <TargetCard
                    key={recipe.id}
                    id={recipe.id!}
                    type="RECIPE"
                    name={recipe.name}
                    description={recipe.description}
                    selectionState={selectionState}
                    onSelect={() => {
                      const target: TargetSelection = {
                        id: recipe.id!,
                        type: 'RECIPE',
                        priority: 'PRIORITY',
                        name: recipe.name,
                        data: recipe,
                      }
                      handleTargetToggle(target)
                    }}
                  />
                )
              })}
            </div>
          </CollapsibleSection>

          {/* ARC Enemies Section */}
          <CollapsibleSection title="ARC Enemies" icon="⚡">
            <p className="text-sm text-retro-sand-dim mb-4">
              Target specific enemy types. Routes will be optimized to pass near these spawns.
            </p>
            <CatalogIndex<EnemyType>
              apiUrl="/api/enemies/types"
              title=""
              onSelect={(enemyType) => {
                const target: TargetSelection = {
                  id: enemyType,
                  type: 'ENEMY',
                  priority: 'PRIORITY',
                  name: enemyType.charAt(0).toUpperCase() + enemyType.slice(1),
                  data: enemyType,
                }
                handleTargetToggle(target)
              }}
              getItemKey={(enemyType) => enemyType}
              searchable={false}
              renderItem={(enemyType) => {
                const selectionState = getSelectionState(enemyType, 'ENEMY')
                return (
                  <TargetCard
                    id={enemyType}
                    type="ENEMY"
                    name={enemyType.charAt(0).toUpperCase() + enemyType.slice(1)}
                    selectionState={selectionState}
                    onSelect={() => {
                      const target: TargetSelection = {
                        id: enemyType,
                        type: 'ENEMY',
                        priority: 'PRIORITY',
                        name: enemyType.charAt(0).toUpperCase() + enemyType.slice(1),
                        data: enemyType,
                      }
                      handleTargetToggle(target)
                    }}
                  />
                )
              }}
            />
          </CollapsibleSection>

          {/* Containers Section */}
          <CollapsibleSection title="Container Zones" icon="🎁">
            <p className="text-sm text-retro-sand-dim mb-4">
              Select container types to include in your route. Containers are grouped by proximity.
            </p>
            <CatalogIndex<ContainerType>
              apiUrl="/api/containers"
              title=""
              onSelect={(container) => {
                const target: TargetSelection = {
                  id: container.id,
                  type: 'CONTAINER',
                  priority: 'PRIORITY',
                  name: container.name,
                  iconUrl: container.iconUrl,
                  data: container,
                }
                handleTargetToggle(target)
              }}
              getItemKey={(container) => container.id}
              searchPlaceholder="Search containers (e.g., red locker, raider cache)..."
              renderItem={(container) => {
                const selectionState = getSelectionState(container.id, 'CONTAINER')
                return (
                  <TargetCard
                    id={container.id}
                    type="CONTAINER"
                    name={container.name}
                    iconUrl={container.iconUrl}
                    description={container.description}
                    selectionState={selectionState}
                    onSelect={() => {
                      const target: TargetSelection = {
                        id: container.id,
                        type: 'CONTAINER',
                        priority: 'PRIORITY',
                        name: container.name,
                        iconUrl: container.iconUrl,
                        data: container,
                      }
                      handleTargetToggle(target)
                    }}
                  />
                )
              }}
            />
          </CollapsibleSection>
        </div>
      </main>

      {/* Shopping Cart (Fixed Bottom) */}
      <ShoppingCart
        priorityTargets={priorityTargets}
        ongoingTargets={ongoingTargets}
        onRemovePriority={removePriorityTarget}
        onRemoveOngoing={removeOngoingTarget}
        routingProfile={routingProfile}
        onChangeRoutingProfile={setRoutingProfile}
        hasRaiderKey={hasRaiderKey}
        onChangeRaiderKey={setHasRaiderKey}
        onCalculateRoute={handleCalculateRoute}
      />

      {/* Priority Limit Modal */}
      <PriorityLimitModal
        isOpen={showLimitModal}
        priorityTargets={priorityTargets}
        onAccept={handleModalAccept}
        onCancel={() => setShowLimitModal(false)}
      />
    </div>
  )
}

export default SetupPage
