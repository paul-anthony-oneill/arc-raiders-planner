import { useState } from 'react'
import Sidebar from './Sidebar'
import DataHUD from './DataHUD'
import MapComponent from './MapComponent'
import MapEditor from './MapEditor'
import { RoutingProfile } from './types'
import type { Item, EnemyType, Area, PlannerResponse, PlannerRequest } from './types'
import './App.css'

const API_PLAN_URL = '/api/items/plan'

function App() {
    // State
    const [loadout, setLoadout] = useState<Item[]>([])
    const [selectedEnemyTypes, setSelectedEnemyTypes] = useState<EnemyType[]>([])
    const [isCalculating, setIsCalculating] = useState(false)
    const [mapData, setMapData] = useState<{
        areas: Area[]
        name: string
    } | null>(null)
    const [activeRoute, setActiveRoute] = useState<PlannerResponse | null>(null) // Store route separately
    const [stats, setStats] = useState<any | null>(null)
    const [showEditor, setShowEditor] = useState(false)
    const [accessibilityMode, setAccessibilityMode] = useState(false)

    // Routing configuration
    const [routingProfile, setRoutingProfile] = useState<RoutingProfile>(RoutingProfile.PURE_SCAVENGER)
    const [hasRaiderKey, setHasRaiderKey] = useState<boolean>(false)

    // Handlers
    const handleAddToLoadout = (item: Item) => {
        if (loadout.length < 5) {
            setLoadout([...loadout, item])
        }
    }

    const handleRemoveFromLoadout = (index: number) => {
        const newLoadout = [...loadout]
        newLoadout.splice(index, 1)
        setLoadout(newLoadout)
    }

    const handleAddEnemyType = (enemyType: EnemyType) => {
        if (selectedEnemyTypes.length < 5 && !selectedEnemyTypes.includes(enemyType)) {
            setSelectedEnemyTypes([...selectedEnemyTypes, enemyType])
        }
    }

    const handleRemoveEnemyType = (index: number) => {
        const newEnemyTypes = [...selectedEnemyTypes]
        newEnemyTypes.splice(index, 1)
        setSelectedEnemyTypes(newEnemyTypes)
    }

    const toggleAccessibility = () => {
        setAccessibilityMode(!accessibilityMode)
        document.body.classList.toggle('accessibility-mode')
    }

    const handleCalculateRoute = async () => {
        if (loadout.length === 0 && selectedEnemyTypes.length === 0) return

        setIsCalculating(true)
        setStats(null)

        try {
            // Construct the Request Object using STATE values
            const requestBody: PlannerRequest = {
                targetItemNames: loadout.map((i) => i.name),
                targetEnemyTypes: selectedEnemyTypes,
                hasRaiderKey: hasRaiderKey,
                routingProfile: routingProfile,
            }

            const planRes = await fetch(API_PLAN_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody),
            })

            if (!planRes.ok) {
                throw new Error(`Planning failed: ${planRes.status} ${planRes.statusText}`)
            }

            const plans: PlannerResponse[] = await planRes.json()

            if (plans.length > 0) {
                const bestPlan = plans[0]

                // 2. Store the active route
                setActiveRoute(bestPlan)

                // 3. Fetch FULL map data (all areas, not just route)
                const mapUrl = `/api/maps/${encodeURIComponent(bestPlan.mapName)}/data`
                const mapResponse = await fetch(mapUrl)
                if (!mapResponse.ok) {
                    throw new Error(`Failed to fetch map data: ${mapResponse.status}`)
                }
                const mapJson: { areas: Area[]; name: string } = await mapResponse.json()
                setMapData(mapJson)

                // 5. Generate Stats
                setStats({
                    sectorName: bestPlan.mapName,
                    environment: 'LIVE FIRE ZONE',
                    lootProbability: {
                        rare: Math.min(100, Math.max(0, Math.floor(bestPlan.score))),
                        epic: 100,
                    },
                    threatLevel: bestPlan.score < 0 ? 'EXTREME' : 'MEDIUM',
                })
            } else {
                alert('No viable route found for this objective.')
            }
        } catch (error) {
            console.error('Calculation failed:', error)
            alert('Failed to calculate route. System Offline.')
        } finally {
            setIsCalculating(false)
        }
    }

    // Editor Mode
    if (showEditor) {
        return <MapEditor onExit={() => setShowEditor(false)} />
    }

    return (
        <div className="fixed inset-0 w-full h-full bg-retro-bg overflow-hidden flex">
            {/* Global CRT Overlay */}
            <div className="absolute inset-0 crt-overlay pointer-events-none z-50"></div>

            {/* Sidebar (Left) */}
            <div className="w-80 flex-shrink-0 h-full z-40">
                <Sidebar
                    loadout={loadout}
                    onAddToLoadout={handleAddToLoadout}
                    onRemoveFromLoadout={handleRemoveFromLoadout}
                    onCalculate={handleCalculateRoute}
                    isCalculating={isCalculating}
                    // Enemy type support
                    selectedEnemyTypes={selectedEnemyTypes}
                    onAddEnemyType={handleAddEnemyType}
                    onRemoveEnemyType={handleRemoveEnemyType}
                    // Routing props
                    routingProfile={routingProfile}
                    setRoutingProfile={setRoutingProfile}
                    hasRaiderKey={hasRaiderKey}
                    setHasRaiderKey={setHasRaiderKey}
                />
            </div>

            {/* Main Content (Right) */}
            <div className="flex-1 flex flex-col h-full relative">
                {/* Top Bar / Header */}
                <header className="h-12 border-b border-retro-sand/20 bg-retro-dark flex items-center justify-between px-4 z-30">
                    <h1 className="text-retro-sand font-display text-lg tracking-widest">
                        TACTICAL MAP //{' '}
                        <span className="text-retro-orange">{mapData ? mapData.name : 'NO SIGNAL'}</span>
                    </h1>
                    <div className="flex items-center gap-4">
                        <button
                            onClick={toggleAccessibility}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-sand border border-retro-sand/20 px-2 py-1"
                        >
                            {accessibilityMode ? '[A11Y: ON]' : '[A11Y: OFF]'}
                        </button>
                        <button
                            onClick={() => setShowEditor(true)}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-orange"
                        >
                            🔧 EDITOR
                        </button>
                    </div>
                </header>

                {/* Map Viewport */}
                <div className="flex-1 relative bg-retro-black overflow-hidden">
                    {mapData ? (
                        <div className="w-full h-full">
                            <MapComponent
                                mapName={mapData.name}
                                areas={mapData.areas}
                                routePath={activeRoute?.routePath || []}
                                extractionPoint={activeRoute?.extractionPoint}
                                extractionLat={activeRoute?.extractionLat}
                                extractionLng={activeRoute?.extractionLng}
                                routingProfile={routingProfile}
                                showRoutePath={true}
                                enemySpawns={activeRoute?.nearbyEnemySpawns || []}
                            />
                        </div>
                    ) : (
                        <div className="absolute inset-0 flex items-center justify-center text-retro-sand-dim/20 font-display text-4xl uppercase tracking-widest select-none">
                            Awaiting Coordinates
                        </div>
                    )}
                </div>

                {/* Bottom HUD */}
                <div className="h-32 flex-shrink-0 z-40">
                    <DataHUD stats={stats} activeProfile={routingProfile} hoveredProfile={null} />
                </div>
            </div>
        </div>
    )
}

export default App
