import { createRouter, createRoute, createRootRoute, RouterProvider } from '@tanstack/react-router'
import React from 'react'

// Import pages
import { TacticalPlannerPage } from './pages/TacticalPlannerPage'

// Create root route
const rootRoute = createRootRoute()

// Create tactical planner route (replaces /setup and /planner)
const tacticalPlannerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/planner',
  component: TacticalPlannerPage,
})

// Create index route (redirects to planner)
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => {
    const router = useRouter()
    React.useEffect(() => {
      router.navigate({ to: '/planner' })
    }, [router])
    return null
  },
})

// Create route tree
const routeTree = rootRoute.addChildren([
  indexRoute,
  tacticalPlannerRoute,
])

// Create router instance
const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

// Type declaration for router
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

// Export router provider component
export const AppRouter: React.FC = () => {
  return <RouterProvider router={router} />
}

// For development tools
function useRouter() {
  return router
}
