import React from 'react'
import type { TargetSelection, RoutingProfile } from '../types'

interface ShoppingCartProps {
  priorityTargets: TargetSelection[]
  ongoingTargets: TargetSelection[]
  onRemovePriority: (id: string | number, type: TargetSelection['type']) => void
  onRemoveOngoing: (id: string | number, type: TargetSelection['type']) => void
  routingProfile: RoutingProfile
  onChangeRoutingProfile: (profile: RoutingProfile) => void
  hasRaiderKey: boolean
  onChangeRaiderKey: (hasKey: boolean) => void
  onCalculateRoute: () => void
  isCalculating?: boolean
  className?: string
}

/**
 * Fixed bottom cart panel showing selected targets and routing options
 * WHY: Always-visible summary of selections with quick access to calculate action
 */
export const ShoppingCart: React.FC<ShoppingCartProps> = ({
  priorityTargets,
  ongoingTargets,
  onRemovePriority,
  onRemoveOngoing,
  routingProfile,
  onChangeRoutingProfile,
  hasRaiderKey,
  onChangeRaiderKey,
  onCalculateRoute,
  isCalculating = false,
  className = '',
}) => {
  const totalTargets = priorityTargets.length + ongoingTargets.length
  const canCalculate = totalTargets > 0 && !isCalculating

  // Routing profile descriptions
  const profileDescriptions: Record<RoutingProfile, string> = {
    PURE_SCAVENGER: 'Maximum loot zones',
    EASY_EXFIL: 'Near Raider Hatches',
    AVOID_PVP: 'Safe from players',
    SAFE_EXFIL: 'Safe + close exit',
  }

  return (
    <div
      className={`
        fixed
        bottom-0
        left-0
        right-0
        bg-retro-dark/95
        border-t-2
        border-retro-orange/50
        shadow-2xl
        shadow-retro-orange/20
        z-40
        ${className}
      `}
      role="complementary"
      aria-label="Target selection cart"
    >
      {/* CRT Overlay */}
      <div className="absolute inset-0 crt-overlay pointer-events-none z-10"></div>

      <div className="relative z-20 max-w-7xl mx-auto p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Left: Priority Targets */}
          <div>
            <h3 className="
              text-xs
              font-mono
              text-retro-orange
              uppercase
              tracking-wider
              mb-2
              flex items-center justify-between
            ">
              <span>Priority Targets</span>
              <span className={priorityTargets.length >= 5 ? 'text-retro-red' : ''}>
                {priorityTargets.length}/5
              </span>
            </h3>
            <div className="
              max-h-32
              overflow-y-auto
              space-y-1
              custom-scrollbar
            ">
              {priorityTargets.length === 0 ? (
                <div className="
                  text-xs
                  text-retro-sand-dim
                  italic
                  text-center
                  py-2
                  border
                  border-dashed
                  border-retro-sand-dim/30
                ">
                  No priority targets
                </div>
              ) : (
                priorityTargets.map(target => (
                  <div
                    key={`${target.type}-${target.id}`}
                    className="
                      flex
                      items-center
                      justify-between
                      bg-retro-orange/10
                      border
                      border-retro-orange/30
                      p-2
                      group
                      hover:bg-retro-orange/20
                      transition-colors
                    "
                  >
                    <div className="flex items-center gap-2 min-w-0 flex-1">
                      {target.iconUrl && (
                        <img
                          src={target.iconUrl}
                          alt=""
                          className="w-6 h-6 object-contain flex-shrink-0"
                        />
                      )}
                      <span className="
                        text-xs
                        text-retro-sand
                        font-mono
                        truncate
                      ">
                        {target.name}
                      </span>
                    </div>
                    <button
                      onClick={() => onRemovePriority(target.id, target.type)}
                      aria-label={`Remove ${target.name} from priority targets`}
                      className="
                        text-retro-red
                        hover:text-white
                        px-2
                        flex-shrink-0
                        min-w-[48px]
                        min-h-[48px]
                        flex
                        items-center
                        justify-center
                      "
                    >
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Middle: Ongoing Targets & Routing */}
          <div className="space-y-3">
            {/* Ongoing Targets */}
            <div>
              <h3 className="
                text-xs
                font-mono
                text-retro-blue
                uppercase
                tracking-wider
                mb-2
                flex items-center justify-between
              ">
                <span>Ongoing Targets</span>
                <span className={ongoingTargets.length >= 10 ? 'text-retro-red' : ''}>
                  {ongoingTargets.length}/10
                </span>
              </h3>
              <div className="
                max-h-20
                overflow-y-auto
                space-y-1
                custom-scrollbar
              ">
                {ongoingTargets.length === 0 ? (
                  <div className="
                    text-xs
                    text-retro-sand-dim
                    italic
                    text-center
                    py-1
                    border
                    border-dashed
                    border-retro-sand-dim/30
                  ">
                    No ongoing targets
                  </div>
                ) : (
                  ongoingTargets.map(target => (
                    <div
                      key={`${target.type}-${target.id}`}
                      className="
                        flex
                        items-center
                        justify-between
                        bg-retro-blue/10
                        border
                        border-retro-blue/30
                        p-1
                        text-xs
                      "
                    >
                      <span className="text-retro-sand font-mono truncate flex-1">
                        {target.name}
                      </span>
                      <button
                        onClick={() => onRemoveOngoing(target.id, target.type)}
                        aria-label={`Remove ${target.name} from ongoing targets`}
                        className="text-retro-red hover:text-white px-2"
                      >
                        ✕
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Routing Profile Selector */}
            <div>
              <label
                htmlFor="routing-profile"
                className="
                  text-xs
                  font-mono
                  text-retro-sand
                  uppercase
                  tracking-wider
                  block
                  mb-1
                "
              >
                Routing Mode
              </label>
              <select
                id="routing-profile"
                value={routingProfile}
                onChange={(e) => onChangeRoutingProfile(e.target.value as RoutingProfile)}
                aria-describedby="routing-profile-description"
                className="
                  w-full
                  p-2
                  bg-retro-black/50
                  border
                  border-retro-sand/20
                  text-retro-sand
                  text-xs
                  font-mono
                  hover:border-retro-orange/50
                  focus:border-retro-orange
                  focus:outline-none
                  transition-colors
                "
              >
                <option value="PURE_SCAVENGER">🔍 Scavenger</option>
                <option value="AVOID_PVP">🛡️ Avoid PvP</option>
                <option value="EASY_EXFIL" disabled={!hasRaiderKey}>
                  🚪 Easy Exfil {!hasRaiderKey && '(Need Key)'}
                </option>
                <option value="SAFE_EXFIL" disabled={!hasRaiderKey}>
                  ✅ Safe Exfil {!hasRaiderKey && '(Need Key)'}
                </option>
              </select>
              <p id="routing-profile-description" className="text-[10px] text-retro-sand-dim mt-1">
                {profileDescriptions[routingProfile]}
              </p>
            </div>
          </div>

          {/* Right: Raider Key & Calculate Button */}
          <div className="flex flex-col justify-between">
            {/* Raider Key Toggle */}
            <label className="
              flex
              items-center
              gap-2
              cursor-pointer
              text-xs
              text-retro-sand
              hover:text-retro-orange
              transition-colors
              p-2
              border
              border-retro-sand/20
              hover:border-retro-orange/50
            ">
              <input
                type="checkbox"
                checked={hasRaiderKey}
                onChange={(e) => onChangeRaiderKey(e.target.checked)}
                className="accent-retro-orange"
              />
              <span className="font-mono uppercase tracking-wider">
                🔑 Raider Key
              </span>
            </label>

            {/* Calculate Route Button */}
            <button
              onClick={onCalculateRoute}
              disabled={!canCalculate}
              aria-label="Calculate optimal route"
              aria-busy={isCalculating}
              className={`
                w-full
                py-4
                font-display
                font-bold
                text-lg
                tracking-widest
                uppercase
                border-2
                transition-all
                duration-200
                relative
                overflow-hidden
                ${
                  canCalculate
                    ? 'border-retro-orange text-retro-black bg-retro-orange hover:bg-retro-orange-dim hover:border-retro-orange-dim'
                    : 'border-retro-sand-dim text-retro-sand-dim cursor-not-allowed opacity-50'
                }
              `}
            >
              <span className="relative z-10">
                {isCalculating ? 'CALCULATING...' : 'CALCULATE ROUTE'}
              </span>
            </button>

            {/* Target Count Summary */}
            <div className="
              text-center
              text-[10px]
              font-mono
              text-retro-sand-dim
              mt-2
            ">
              {totalTargets} {totalTargets === 1 ? 'target' : 'targets'} selected
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
