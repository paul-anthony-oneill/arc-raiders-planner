# UI Design Rules - ARC Raiders Planner

## Core Design Philosophy

Tactical precision meets intuitive navigation creating frictionless raid planning experiences for looters and raiders.

## Visual Design Principles

### 1. Gaming-First Aesthetic
Dark-themed interface optimized for long planning sessions with cyberpunk/tactical undertones
High-contrast accent colors for critical information (loot rarity, danger zones, exfil points)
Subtle sci-fi elements that respect the ARC Raiders universe without overwhelming functionality

### 2. Information Hierarchy
Primary actions (search items, generate routes) positioned prominently with visual weight
Strategic use of color gradients for data importance (legendary > epic > rare > common)
Typography scaling utilizing MUI's theme system for clear content stratification
Map visualization as hero element with supporting panels that don't compete for attention

### 3. Spatial Organization
Strategic whitespace calibrated for information density without cognitive overload
Card-based layouts for discrete data clusters (items, areas, routes)
Grid systems following Material Design 3.0 specifications for consistency
Breathable padding complemented by deliberate use of dividers for content segmentation

### 4. Motion and Feedback
Purposeful micro-interactions signaling system state changes with minimal latency
Route animation revealing path optimization without distracting from decision-making
Hover states providing contextual information (item stats, area dangers)
Loading states using skeleton screens maintaining layout stability
Smooth transitions between routing profiles without jarring layout shifts

### 5. Color Theory and Accessibility
Systematic palette derived from ARC Raiders brand with WCAG 2.1 AA compliance minimum
Rarity color coding (orange=legendary, purple=epic, blue=rare, green=uncommon)
Semantic colors for status communication (success, warning, danger, info)
Strategic accent placement guiding user attention to optimal farming routes
Dark mode optimized for reduced eye strain during extended planning sessions

### 6. Data Visualization
Map markers using intuitive iconography (crosshairs for enemies, chest for loot, exit for hatches)
Route paths color-coded by routing profile (blue=scavenger, green=safe, yellow=pvp-avoid)
Proximity scoring visualized through gradient intensity or numeric badges
Clear legend systems for symbol disambiguation
Tooltip overlays preventing information overload on primary canvas

### 7. Component Design Standards (Material UI v7)
Consistent elevation system: 0=flat, 1=cards, 2=modals, 3=dropdowns
Button hierarchy: contained (primary), outlined (secondary), text (tertiary)
Input fields with clear labels, helper text, and validation states
Chips for tags (loot types, enemy types) with dismissible variants
Tables with sticky headers, sortable columns, and row hover states
Use MUI's sx prop for one-off styling, styled() for reusable components

### 8. Responsive Design Patterns
Mobile-first approach with progressive enhancement for tablet/desktop
Breakpoints: xs (0px), sm (600px), md (900px), lg (1200px), xl (1536px)
Collapsible navigation drawer on mobile, persistent sidebar on desktop
Map viewport scaling maintaining aspect ratio across devices
Touch-friendly tap targets (minimum 48x48px) for mobile interactions

### 9. Performance Optimization
Lazy loading for route visualizations and map markers
Virtualization for long lists (item search results, area lists)
Debounced search inputs preventing excessive API calls
Memoized components for expensive calculations (route optimization)
Code splitting by feature (planner, items, maps, quests)

### 10. Iconography and Symbols
Material Icons as primary icon set for UI controls
Custom SVG icons for game-specific elements (ARC enemies, loot types)
Consistent sizing: 16px (small), 24px (default), 32px (large)
Icon buttons with aria-labels for accessibility
Badge overlays for counts and notifications

### 11. Typography System
MUI Typography component enforcing scale consistency
Font families: 'Roboto' for body, 'Orbitron' or 'Exo 2' for headers (gaming feel)
Weight variance: 300 (light), 400 (regular), 500 (medium), 700 (bold)
Line-height optimization: 1.2 for headers, 1.6 for body text
Limited use of uppercase for emphasis (section headers, buttons)

### 12. Form Design
Single-column forms with clear visual flow
Inline validation with immediate feedback
Auto-complete for item search with fuzzy matching
Grouped controls for related settings (routing preferences)
Submit buttons aligned right, cancel/reset aligned left

### 13. Error States and Empty States
Friendly error messages with actionable solutions
Illustrations for empty states encouraging first action
Retry mechanisms for failed API calls
Fallback UI for missing data (placeholder cards)
Toast notifications for background operations

### 14. Gamification Elements
Visual reward feedback for optimal route discovery
Progress indicators for route generation algorithms
Achievement-style badges for discovering rare loot locations
Comparative metrics (your route vs. average efficiency)

## Anti-Patterns to Avoid

❌ Inconsistent spacing between similar UI elements
❌ Mixing gaming aesthetic with overly corporate/formal design language
❌ Information overload on initial view (progressive disclosure required)
❌ Animations without purpose or that delay critical interactions
❌ Ignoring loading states leading to perceived performance issues
❌ Poor contrast ratios failing accessibility standards
❌ Nested dropdowns more than 2 levels deep
❌ Auto-playing animations that can't be paused
❌ Hidden functionality without visual affordances
❌ Inconsistent button styles across similar actions

## Component Checklist

Before committing a component, verify:
- [ ] Follows MUI v7 theming conventions
- [ ] Responsive across all breakpoints
- [ ] Accessible (ARIA labels, keyboard navigation)
- [ ] Loading and error states implemented
- [ ] Consistent with established design patterns
- [ ] Performance optimized (memoization where needed)
- [ ] Type-safe props with proper TypeScript definitions
- [ ] Dark theme optimized colors
- [ ] Gaming aesthetic maintained without sacrificing usability
