import { useState, useEffect, useCallback } from 'react'
import Sidebar from './Sidebar'
import DataHUD from './DataHUD'
import MapComponent from './MapComponent'
import MapEditor from './MapEditor'
import RecipeViewer from './RecipeViewer'
import { Toast } from './components/Toast'
import { useToast } from './hooks/useToast'
import { RoutingProfile } from './types'
import type { Item, EnemyType, PlannerResponse, PlannerRequest, Recipe } from './types'
import type { RouteStats, MapDataResponse } from './types/stats'
import { recipeApi } from './api/recipeApi'
import './App.css'

const API_PLAN_URL = '/api/items/plan'

function App() {
    // Toast notifications
    const { toasts, showToast, hideToast } = useToast();

    // State
    const [loadout, setLoadout] = useState<Item[]>([])
    const [selectedEnemyTypes, setSelectedEnemyTypes] = useState<EnemyType[]>([])
    const [isCalculating, setIsCalculating] = useState(false)
    const [mapData, setMapData] = useState<MapDataResponse | null>(null)
    const [activeRoute, setActiveRoute] = useState<PlannerResponse | null>(null) // Store route separately
    const [stats, setStats] = useState<RouteStats | null>(null)
    const [activeEditor, setActiveEditor] = useState<"NONE" | "MAP" | "RECIPE">("NONE")
    const [accessibilityMode, setAccessibilityMode] = useState(false)

    // Routing configuration
    const [routingProfile, setRoutingProfile] = useState<RoutingProfile>(RoutingProfile.PURE_SCAVENGER)
    const [hasRaiderKey, setHasRaiderKey] = useState<boolean>(false)

    // Recipe State
    const [recipes, setRecipes] = useState<Recipe[]>([])
    const [recipeSelection, setRecipeSelection] = useState<Record<number, "PRIORITY" | "ONGOING" | undefined>>({})
    const [collectedIngredients, setCollectedIngredients] = useState<Record<number, Record<string, number>>>({})
    const [itemContextMap, setItemContextMap] = useState<Record<string, string[]>>({})

    // Mobile Sidebar State
    const [isSidebarOpen, setIsSidebarOpen] = useState(false)

    useEffect(() => {
        recipeApi.getAllRecipes().then(setRecipes).catch(console.error)
        // Load from localStorage
        const savedSelection = localStorage.getItem('recipeSelection');
        if (savedSelection) setRecipeSelection(JSON.parse(savedSelection));
        const savedCollected = localStorage.getItem('collectedIngredients');
        if (savedCollected) setCollectedIngredients(JSON.parse(savedCollected));
    }, [])

    useEffect(() => {
        localStorage.setItem('recipeSelection', JSON.stringify(recipeSelection));
    }, [recipeSelection])

    useEffect(() => {
        localStorage.setItem('collectedIngredients', JSON.stringify(collectedIngredients));
    }, [collectedIngredients])

    // Handlers
    const handleToggleRecipe = useCallback((recipeId: number) => {
        setRecipeSelection(prev => {
            const current = prev[recipeId];
            const next = current === undefined ? "PRIORITY" : current === "PRIORITY" ? "ONGOING" : undefined;
            return { ...prev, [recipeId]: next };
        });
    }, []);

    const handleIngredientUpdate = useCallback((recipeId: number, ingredientName: string, delta: number) => {
        setCollectedIngredients(prev => {
            const recipeCollected = prev[recipeId] || {};
            const currentCount = recipeCollected[ingredientName] || 0;
            const newCount = Math.max(0, currentCount + delta);
            return {
                ...prev,
                [recipeId]: {
                    ...recipeCollected,
                    [ingredientName]: newCount
                }
            };
        });
    }, []);

    const handleAddToLoadout = useCallback((item: Item) => {
        setLoadout(prev => {
            if (prev.length < 5) return [...prev, item];
            return prev;
        });
    }, []);

    const handleRemoveFromLoadout = useCallback((index: number) => {
        setLoadout(prev => {
            const updated = [...prev];
            updated.splice(index, 1);
            return updated;
        });
    }, []);

    const handleAddEnemyType = useCallback((enemyType: EnemyType) => {
        setSelectedEnemyTypes(prev => {
            if (prev.length < 5 && !prev.includes(enemyType)) {
                return [...prev, enemyType];
            }
            return prev;
        });
    }, []);

    const handleRemoveEnemyType = useCallback((index: number) => {
        setSelectedEnemyTypes(prev => {
            const updated = [...prev];
            updated.splice(index, 1);
            return updated;
        });
    }, []);

    const toggleAccessibility = () => {
        setAccessibilityMode(!accessibilityMode)
        document.body.classList.toggle('accessibility-mode')
    }

    const handleCalculateRoute = async () => {
        // Check if we have any items, enemies OR recipes selected
        const hasRecipes = Object.values(recipeSelection).some(v => v !== undefined);
        if (loadout.length === 0 && selectedEnemyTypes.length === 0 && !hasRecipes) return

        setIsCalculating(true)
        setStats(null)

        try {
            // Resolve Recipe Ingredients & Context
            const targetIngredients: string[] = [];
            const ongoingIngredients: string[] = [];
            const contextMap: Record<string, string[]> = {};

            Object.entries(recipeSelection).forEach(([idStr, status]) => {
                if (!status) return;
                const recipeId = Number(idStr);
                const recipe = recipes.find(r => r.id === recipeId);
                if (recipe) {
                    const collected = collectedIngredients[recipeId] || {};
                    
                    recipe.ingredients.forEach(ing => {
                        const collectedCount = collected[ing.itemName] || 0;
                        if (collectedCount < ing.quantity) {
                            // Item is still needed
                            if (status === "PRIORITY") {
                                targetIngredients.push(ing.itemName);
                            } else if (status === "ONGOING") {
                                ongoingIngredients.push(ing.itemName);
                            }
                            
                            // Add to context map
                            if (!contextMap[ing.itemName]) contextMap[ing.itemName] = [];
                            const label = `${recipe.name} (${status === 'PRIORITY' ? 'Priority' : 'Ongoing'})`;
                            if (!contextMap[ing.itemName].includes(label)) {
                                contextMap[ing.itemName].push(label);
                            }
                        }
                    });
                }
            });
            
            setItemContextMap(contextMap);

            // Construct the Request Object using STATE values
            const requestBody: PlannerRequest = {
                targetItemNames: [...loadout.map((i) => i.name), ...targetIngredients],
                targetEnemyTypes: selectedEnemyTypes,
                hasRaiderKey: hasRaiderKey,
                routingProfile: routingProfile,
                ongoingItemNames: ongoingIngredients,
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
                const mapJson: MapDataResponse = await mapResponse.json()
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
                showToast('No viable route found. Try adjusting your loadout or enemies.', 'error');
            }
        } catch (error) {
            console.error('Calculation failed:', error)
            showToast(`Failed to calculate route: ${error instanceof Error ? error.message : 'System Offline'}`, 'error');
        } finally {
            setIsCalculating(false)
        }
    }

    // Editor Mode
    if (activeEditor === "MAP") {
        return <MapEditor onExit={() => setActiveEditor("NONE")} showToast={showToast} />
    }
    if (activeEditor === "RECIPE") {
        return <RecipeViewer onExit={() => setActiveEditor("NONE")} />
    }

    return (
        <div className="fixed inset-0 w-full h-full bg-retro-bg overflow-hidden flex">
            {/* Global CRT Overlay */}
            <div className="absolute inset-0 crt-overlay pointer-events-none z-50"></div>

            {/* Mobile Sidebar Toggle */}
            <button
                className="md:hidden fixed top-4 left-4 z-50 p-2 bg-retro-dark border border-retro-sand/20 text-retro-sand"
                onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                aria-label="Toggle Navigation"
                aria-expanded={isSidebarOpen}
            >
                {isSidebarOpen ? '✕' : '☰'}
            </button>

            {/* Sidebar (Left) */}
            <aside className={`
                fixed md:relative z-40 h-full w-80 bg-retro-dark border-r border-retro-sand/10
                transition-transform duration-300 ease-in-out
                ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
            `}>
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
                    // Recipe props
                    recipes={recipes}
                    recipeSelection={recipeSelection}
                    onToggleRecipe={handleToggleRecipe}
                    collectedIngredients={collectedIngredients}
                    onIngredientUpdate={handleIngredientUpdate}
                />
            </aside>

            {/* Mobile Backdrop Overlay */}
            {isSidebarOpen && (
                <div
                    className="md:hidden fixed inset-0 bg-black/50 z-30"
                    onClick={() => setIsSidebarOpen(false)}
                    aria-hidden="true"
                />
            )}

            {/* Main Content (Right) */}
            <main className="flex-1 flex flex-col h-full relative">
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
                            onClick={() => setActiveEditor("RECIPE")}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-orange border border-retro-sand/20 px-2 py-1"
                        >
                            🔨 CRAFTING
                        </button>
                        <button
                            onClick={() => setActiveEditor("MAP")}
                            className="text-xs font-mono text-retro-sand-dim hover:text-retro-orange border border-retro-sand/20 px-2 py-1"
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
                                routePath={activeRoute?.path || []}
                                extractionPoint={activeRoute?.extractionPoint}
                                extractionLat={activeRoute?.extractionLat}
                                extractionLng={activeRoute?.extractionLng}
                                routingProfile={routingProfile}
                                showRoutePath={true}
                                enemySpawns={activeRoute?.nearbyEnemySpawns || []}
                                itemContextMap={itemContextMap}
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
            </main>

            {/* Toast Notifications */}
            {toasts.map(toast => (
                <Toast
                    key={toast.id}
                    message={toast.message}
                    type={toast.type}
                    onClose={() => hideToast(toast.id)}
                />
            ))}
        </div>
    )
}

export default App
