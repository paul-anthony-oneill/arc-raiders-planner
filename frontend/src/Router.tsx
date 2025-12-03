import { createRouter, createRoute, createRootRoute, RouterProvider } from '@tanstack/react-router'
import React from 'react'

// Import pages
import SetupPage from './pages/SetupPage'
import PlannerPage from './pages/PlannerPage'

// Create root route
const rootRoute = createRootRoute()

// Create setup route
const setupRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/setup',
  component: SetupPage,
})

// Create planner route
const plannerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/planner',
  component: PlannerPage,
})

// Create index route (redirects to setup)
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => {
    const router = useRouter()
    React.useEffect(() => {
      router.navigate({ to: '/setup' })
    }, [router])
    return null
  },
})

// Create route tree
const routeTree = rootRoute.addChildren([
  indexRoute,
  setupRoute,
  plannerRoute,
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
