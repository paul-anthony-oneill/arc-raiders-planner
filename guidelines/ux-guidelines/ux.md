# UX Design Rules - ARC Raiders Planner

## User Goals and Core Tasks

### Primary User Objectives
1. **Discover optimal loot locations** - Players need to quickly identify which maps contain their target items
2. **Plan efficient raid routes** - Minimize time in danger zones while maximizing loot collection
3. **Understand risk vs. reward** - Make informed decisions about PvP exposure and enemy encounters
4. **Coordinate team strategy** - Share routes and priorities with squad members
5. **Adapt to playstyle** - Support different approaches (aggressive farming, safe exfil, PvP avoidance)

### Critical User Flows
- **Item Search → Map Recommendation → Route Generation → Route Refinement**
- **Browse Maps → Explore Loot Zones → Compare Areas → Commit to Strategy**
- **Select Enemy Targets → Optimize Proximity → Visualize Path → Execute Raid**
- **Quest Tracking → Component Collection → Crafting Planning → Priority Management**

## Information Architecture

### Content Hierarchy
1. **Primary Canvas**: Interactive map with route visualization (highest priority)
2. **Search/Input Layer**: Item/quest search driving recommendation engine
3. **Results/Options**: Map recommendations, area lists, routing profiles
4. **Context Details**: Item stats, area metadata, enemy spawn info
5. **Settings/Preferences**: Routing profiles, display options, filters

### Navigation Structure
```
Home (Dashboard)
├── Item Planner
│   ├── Item Search
│   ├── Map Recommendations
│   └── Route Visualization
├── Map Explorer
│   ├── All Maps
│   ├── Map Details
│   └── Area Catalog
├── Quest Tracker (Future)
│   ├── Active Quests
│   ├── Quest Items
│   └── Progress Tracking
└── Settings
    ├── Routing Preferences
    └── Display Options
```

### Mental Model Alignment
- Players think in terms of "what I need" → "where to find it" → "how to get there safely"
- Maps are mental anchors; areas are tactical waypoints
- Loot rarity creates natural prioritization without explicit ranking
- Exfil points serve as psychological safety anchors

## Progressive Disclosure

### Initial Experience (First-Time Users)
1. **Immediate Value**: Clear search prompt "What are you looking for?"
2. **Quick Win**: Single item search showing instant map recommendations
3. **Guided Discovery**: Tooltips on first interaction with routing profiles
4. **Contextual Education**: Inline explanations for technical concepts (TSP, proximity scoring)

### Advanced Features (Power Users)
- Multi-item planning revealed after single-item success
- Custom routing weights exposed in settings for fine-tuning
- Batch route comparison for optimization nerds
- Export functionality for sharing with communities

### Complexity Layering
- **Layer 1**: Simple search → See maps (beginners)
- **Layer 2**: Choose routing profile → Get optimized path (intermediate)
- **Layer 3**: Target specific enemies → Fine-tune route → Analyze metrics (advanced)
- **Layer 4**: API integration, bulk planning, statistical analysis (power users)

## Visual Hierarchy

### Attention Management
**Priority 1 (Immediate Focus):**
- Active search input
- Primary action button (Generate Route)
- Map canvas when route is displayed

**Priority 2 (Scanning):**
- Recommended maps list
- Current routing profile indicator
- Key metrics (area count, proximity score)

**Priority 3 (Reference):**
- Item details panel
- Area metadata
- Navigation menu

**Priority 4 (Ancillary):**
- Footer information
- Settings gear icon
- Help documentation links

### Visual Weight Distribution
- Map visualization: 60% of viewport on desktop
- Control panels: 30% (sidebar/drawer)
- Header/navigation: 10%
- Mobile: Full-screen map with slide-out controls

## Interaction Patterns

### Input Mechanisms
- **Search**: Auto-complete with fuzzy matching, recent searches
- **Selection**: Radio buttons for single-choice (routing profile), checkboxes for multi-select (loot types)
- **Range**: Sliders for continuous values (risk tolerance, area count)
- **Toggle**: Switches for binary states (include PvP zones, show enemy spawns)

### Feedback Loops
- **Immediate**: Input validation, button state changes
- **Near-term**: Loading indicators during route calculation (2-5 seconds)
- **Delayed**: Toast notification for background sync operations
- **Persistent**: Error messages requiring user action

### State Transitions
```
Empty State → Searching → Results Loading → Results Display → Detail View
                                    ↓
                              No Results → Suggestions
```

### Keyboard Shortcuts (Power User Optimization)
- `Ctrl/Cmd + K`: Focus search
- `Ctrl/Cmd + Enter`: Generate route
- `Esc`: Close modals/clear search
- Arrow keys: Navigate results list
- `Tab`: Cycle through routing profiles

## Affordances and Signifiers

### Clickable Elements
- Primary buttons: Raised with shadow, color fill
- Secondary buttons: Outlined, no fill
- Icon buttons: Circular container on hover
- Links: Underline on hover, color change

### Interactive Maps
- Hover over area markers: Tooltip with area name and loot types
- Click marker: Zoom and show detailed panel
- Drag: Pan map canvas
- Scroll: Zoom in/out
- Route paths: Animated dash showing direction

### Visual States
- **Default**: Neutral colors, standard elevation
- **Hover**: Slight elevation increase, color shift
- **Active/Selected**: Accent color, higher contrast
- **Disabled**: Reduced opacity (38%), no hover effects
- **Loading**: Skeleton screens or spinner overlays
- **Error**: Red outline, error icon, helper text

## Accessibility Requirements

### WCAG 2.1 AA Compliance
- Minimum contrast ratio 4.5:1 for body text
- Minimum contrast ratio 3:1 for large text and UI components
- All interactive elements keyboard navigable
- Focus indicators visible on all focusable elements
- Form labels programmatically associated with inputs

### Screen Reader Support
- Semantic HTML structure (header, nav, main, section)
- ARIA labels for icon buttons and complex widgets
- Live regions for dynamic content updates
- Skip navigation links for quick access
- Alt text for decorative vs. informational images

### Motor Impairment Considerations
- Touch targets minimum 48x48px
- No required hover-only interactions
- Adequate spacing between clickable elements (8px minimum)
- No time-based interactions without extension options

### Cognitive Load Reduction
- Consistent navigation placement across views
- Clear visual grouping of related information
- Progressive complexity (simple → advanced)
- Undo/redo for destructive actions
- Confirmation dialogs for irreversible operations

## Feedback and Communication

### Success States
- **Route Generated**: "Optimal route calculated with X areas in Y path length"
- **Item Found**: "Found on X maps across Y areas"
- **Settings Saved**: "Routing preferences updated"

### Error States
- **Not Found**: "No maps contain this item. Try another search or browse all items."
- **API Failure**: "Unable to load map data. Retrying... [Retry Now]"
- **Invalid Input**: "Please enter at least 3 characters to search"

### Empty States
- **No Search Yet**: Hero image + "Search for any item to get started"
- **No Results**: Suggestion list + "Try searching for: [popular items]"
- **No Route Generated**: "Select a map and routing profile to generate your path"

### Loading States
- **Quick (<1s)**: Button loading spinner
- **Medium (1-3s)**: Skeleton screens with pulsing animation
- **Long (>3s)**: Progress bar with status messages
- **Background**: Subtle notification that doesn't block interaction

## Performance Perception

### Perceived Speed Optimization
- Optimistic UI updates (assume success, rollback on failure)
- Instant search results from local cache before API call
- Lazy load map tiles as user pans
- Prefetch likely next maps based on user behavior
- Show partial results during streaming data loads

### Progress Indicators
- Determinate progress bars when duration is known (route calculation)
- Indeterminate spinners for unknown duration (external API calls)
- Skeleton screens maintaining layout structure
- Inline validation without blocking form submission

## Mobile vs. Desktop Considerations

### Mobile-Specific Patterns
- Bottom sheet for controls over map (thumb-friendly)
- Swipe gestures for navigation between views
- Collapsible sections conserving vertical space
- Tap-to-zoom on map with pinch-to-zoom support
- Floating action button for primary action (Generate Route)

### Desktop Enhancements
- Persistent sidebar for controls
- Keyboard shortcuts for power users
- Multi-column layouts for comparison views
- Hover tooltips with rich information
- Right-click context menus on map elements

### Responsive Breakpoints Strategy
- **xs (mobile)**: Single column, full-screen map, drawer navigation
- **sm (tablet)**: Two-column when landscape, map + sidebar
- **md+ (desktop)**: Three-column possible, persistent panels
- **xl (large desktop)**: Enhanced visualizations, more data density

## Platform Conventions

### React 19 Patterns
- Suspense boundaries for async components
- Transitions for non-urgent updates
- Use server components for static content (future)
- Error boundaries for graceful failure handling

### Material UI v7 Conventions
- Follow Material Design 3.0 specifications
- Use theme system for consistent styling
- Leverage built-in accessibility features
- Component composition over customization
- sx prop for style overrides, styled() for reusable styles

### TanStack Router Integration
- Shareable URLs for specific routes/queries
- Search params for filter states
- Loading states tied to route transitions
- Type-safe navigation with autocomplete

## Aesthetic and Emotional Design

### Tone and Voice
- **Confident**: "We found the best route for you"
- **Helpful**: "Try these popular items" vs. "No results"
- **Efficient**: Direct language, no fluff
- **Gaming-Appropriate**: Industry terminology (exfil, PvP, farming)

### Visual Polish
- Subtle animations enhancing feedback (not decoration)
- Consistent timing functions (ease-in-out for most transitions)
- Attention to detail in micro-interactions
- Professional yet edgy aesthetic matching game universe

### Trust Building
- Transparent algorithms (show why a route is "optimal")
- Consistent behavior (same input = same output)
- Data accuracy (source attribution for game data)
- Privacy respect (no tracking beyond essential analytics)

## Mobile vs. Desktop Trade-offs

### Favor Mobile When:
- Primary use case is planning during gameplay (second screen)
- Quick lookups between raids
- On-the-go strategy discussion with team

### Favor Desktop When:
- Detailed route comparison and analysis
- Multi-item batch planning
- Community content creation (screenshots, sharing)

### Universal Principles:
- Core functionality available on all devices
- No desktop-only critical features
- Performance targets: <3s initial load, <1s interactions
- Offline-first architecture where possible

## Animations and Transitions

### Animation Purpose Taxonomy
- **Feedback**: Button press, toggle switch
- **Spatial**: Route path drawing, map marker clustering
- **Clarification**: Expand/collapse panels, tooltips
- **Delight**: Confetti on optimal route discovery (sparingly!)

### Timing Standards
- **Instant**: <100ms (perceived as immediate)
- **Quick**: 100-300ms (standard transitions)
- **Moderate**: 300-500ms (complex state changes)
- **Slow**: 500ms+ (page transitions, rare use)

### Respect User Preferences
- Honor `prefers-reduced-motion` media query
- Provide settings toggle for animations
- Ensure functionality works without animations

## Content-First Design

### Prioritize User Objectives
- Item search above decorative hero images
- Map recommendations before marketing content
- Clear CTAs without hunting

### Content Legibility
- Line length: 50-75 characters for optimal readability
- Paragraph spacing for breathability
- Headings sized for clear hierarchy
- Bullet points for scannable lists

### Contextual Help
- Inline documentation where confusion likely
- Tooltip icons for optional explanations
- Link to comprehensive guide for deep dives
- No modal tutorials blocking initial use
