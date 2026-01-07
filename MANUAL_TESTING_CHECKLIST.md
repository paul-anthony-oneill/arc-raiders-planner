# Manual Testing Checklist - ARC Raiders Tactical Planner

**Branch:** `feat/UI-redesign`
**Date:** 2025-12-15
**Features to Test:** 8 new features + UX improvements

---

## 🔧 **Test Environment Setup**

- [ ] Backend running: `./mvnw spring-boot:run`
- [ ] Frontend running: `cd frontend && npm run dev`
- [ ] PostgreSQL container up (auto-started by Spring Boot)
- [ ] Database migrated successfully (check logs for Flyway V013)
- [ ] Browser: Chrome/Firefox (test on both if possible)
- [ ] Mobile device OR browser DevTools responsive mode

---

## 1️⃣ **Zone Highlighting Feature**

**Goal:** Verify items highlight their spawn zones on maps

### Desktop Testing
- [ ] Navigate to `/planner`
- [ ] Select an item from the Objectives panel (left)
- [ ] **Expected:** Item details appear in center panel
- [ ] **Expected:** Zones appear highlighted on minimap (right)
- [ ] Change map dropdown (select different map)
- [ ] **Expected:** Zone highlighting updates for new map
- [ ] Select item with NO loot type (e.g., quest item)
- [ ] **Expected:** No zones highlighted (empty state)

### Mobile Testing (< 768px)
- [ ] Resize browser to mobile width OR use phone
- [ ] Select Objectives tab → pick an item
- [ ] Switch to Map tab
- [ ] **Expected:** Zones highlighted correctly

### Edge Cases
- [ ] Select item found in 5+ zones
- [ ] **Expected:** All zones highlighted (no duplicates)
- [ ] Rapidly switch between items
- [ ] **Expected:** Zones update smoothly, no lag

---

## 2️⃣ **Recipe Chain & Prerequisites**

**Goal:** Verify prerequisite detection for crafting upgrades

### Test Crafting Items
- [ ] Find an upgrade item (e.g., Anvil IV, Workbench III)
- [ ] **Expected:** Item detail shows recipe with ingredients
- [ ] Check for prerequisite items (same type, lower tier)
- [ ] **Expected:** Anvil III marked as prerequisite for Anvil IV
- [ ] **Expected:** Materials (steel, etc.) NOT marked as prerequisites

### Test Non-Craftable Items
- [ ] Select a material item (e.g., Steel Plate)
- [ ] **Expected:** No recipe shown OR shows "used in" recipes

### Edge Cases
- [ ] Item with complex chain (Workbench IV → III → II)
- [ ] **Expected:** Chain displayed correctly
- [ ] Item without Metaforge ID
- [ ] **Expected:** No recipe data (graceful handling)

---

## 3️⃣ **Advanced Filtering System**

**Goal:** Verify multi-criteria filtering works correctly

### Basic Filtering
- [ ] Open FilterPanel in Objectives (left panel)
- [ ] Apply rarity filter (select "Rare")
- [ ] **Expected:** Only rare items shown
- [ ] Apply item type filter (select "Weapon")
- [ ] **Expected:** Only rare weapons shown
- [ ] Apply loot zone filter (select "Mechanical")
- [ ] **Expected:** Only rare weapons from mechanical zones

### Filter Combinations
- [ ] Select 3 rarities + 2 types + 1 zone
- [ ] **Expected:** Items match ALL criteria
- [ ] Click "Clear All"
- [ ] **Expected:** All filters removed, full list returns

### Show Only Targeted
- [ ] Select 2-3 items as targets
- [ ] Enable "Show only targeted" toggle
- [ ] **Expected:** Only selected items visible
- [ ] Disable toggle
- [ ] **Expected:** Full list returns

---

## 4️⃣ **Route Export & Sharing**

**Goal:** Verify route persistence and sharing

### JSON Export
- [ ] Select 3+ target items
- [ ] Calculate route (use Calculate Route button)
- [ ] Look for export button (if implemented in UI)
- [ ] Export route to JSON
- [ ] **Expected:** File downloads with route data
- [ ] Open JSON file
- [ ] **Expected:** Valid JSON with waypoints, map, items

### URL Sharing (if implemented)
- [ ] Calculate a route
- [ ] Click "Share Route" button
- [ ] **Expected:** URL copied to clipboard
- [ ] Paste URL in new browser tab
- [ ] **Expected:** Route loads correctly

### Print (if implemented)
- [ ] Calculate route
- [ ] Trigger print dialog
- [ ] **Expected:** Print preview shows route details

---

## 5️⃣ **Mobile Responsiveness**

**Goal:** Verify app works on all screen sizes

### Tablet (768px - 1024px)
- [ ] Resize browser to tablet width
- [ ] **Expected:** 2-column layout (Objectives + Map OR Details)
- [ ] Navigate all tabs
- [ ] **Expected:** All features accessible

### Mobile (< 768px)
- [ ] Resize to mobile width (320px - 767px)
- [ ] **Expected:** Tab navigation appears at top
- [ ] **Expected:** Single column layout
- [ ] Tap each tab: Objectives, Details, Map
- [ ] **Expected:** Panels switch correctly
- [ ] Select item in Objectives tab
- [ ] **Expected:** Auto-switch to Details tab (if implemented)
- [ ] Calculate route
- [ ] **Expected:** Auto-switch to Map tab

### Landscape Mobile
- [ ] Rotate device to landscape OR resize to 667x375
- [ ] **Expected:** Layout adapts, no horizontal scroll
- [ ] **Expected:** All content visible

---

## 6️⃣ **Loading Skeletons**

**Goal:** Verify loading states show skeleton UI

### Initial Load
- [ ] Clear browser cache
- [ ] Refresh page
- [ ] **Expected:** Skeleton loader in Objectives panel
- [ ] **Expected:** Smooth transition to actual items

### Item Detail Load
- [ ] Select item (if slow network)
- [ ] **Expected:** Skeleton in Details panel (if detectable)

### Map Load
- [ ] Calculate route
- [ ] **Expected:** Spinning loader while calculating
- [ ] **Expected:** Smooth transition to map view

---

## 7️⃣ **Keyboard Shortcuts**

**Goal:** Verify keyboard navigation works

### Shortcut Tests
- [ ] Press `ESC` with route open
- [ ] **Expected:** Route closes, returns to selection mode
- [ ] Press `ESC` with error notification
- [ ] **Expected:** Error dismisses
- [ ] Select targets, press `Ctrl+Enter`
- [ ] **Expected:** Route calculation starts
- [ ] Press `Ctrl+A`
- [ ] **Expected:** Accessibility mode toggles

### Focus Management
- [ ] Tab through interface
- [ ] **Expected:** Focus visible on all interactive elements
- [ ] Press `Shift+Tab` to reverse
- [ ] **Expected:** Focus moves backwards

---

## 8️⃣ **Tooltips & Contextual Help**

**Goal:** Verify tooltips provide helpful information

### Tooltip Tests
- [ ] Hover over Accessibility toggle button
- [ ] **Expected:** Tooltip appears with explanation + shortcut
- [ ] **Expected:** Tooltip positioned correctly (not off-screen)
- [ ] Move mouse away
- [ ] **Expected:** Tooltip disappears after delay
- [ ] Focus element with keyboard (Tab)
- [ ] **Expected:** Tooltip appears on focus
- [ ] Press `Tab` to next element
- [ ] **Expected:** Tooltip disappears

---

## 9️⃣ **Smooth Transitions & Animations**

**Goal:** Verify animations are smooth and polished

### Panel Transitions
- [ ] Switch between tabs on mobile
- [ ] **Expected:** Smooth 300ms transitions
- [ ] Toggle filters open/closed
- [ ] **Expected:** Smooth expansion/collapse
- [ ] Calculate route (mode switch)
- [ ] **Expected:** Smooth transition to planning mode

### Loading Animations
- [ ] Observe skeleton pulse animation
- [ ] **Expected:** Smooth, not jerky
- [ ] Watch route calculation spinner
- [ ] **Expected:** Smooth rotation

### No Motion Sickness
- [ ] **Expected:** No excessive motion
- [ ] **Expected:** Transitions complete in < 500ms

---

## 🔟 **Lazy Loading Performance**

**Goal:** Verify code splitting improves performance

### Bundle Analysis
- [ ] Open browser DevTools → Network tab
- [ ] Refresh page
- [ ] **Expected:** Main bundle loads (~470KB)
- [ ] **Expected:** MaximizedMapView chunk NOT loaded initially
- [ ] Calculate first route
- [ ] **Expected:** MaximizedMapView chunk loads (~7KB)
- [ ] Calculate second route
- [ ] **Expected:** Chunk cached, instant load

---

## 🐛 **Error Handling & Edge Cases**

### API Errors
- [ ] Stop backend server
- [ ] Select an item
- [ ] **Expected:** Error notification appears
- [ ] **Expected:** Error message is clear
- [ ] Restart backend
- [ ] Retry operation
- [ ] **Expected:** Works correctly

### Empty States
- [ ] Search for non-existent item
- [ ] **Expected:** "No results" message
- [ ] Apply filters with no matches
- [ ] **Expected:** Empty state shown

### Invalid Data
- [ ] Calculate route with 0 targets
- [ ] **Expected:** Button disabled OR error shown
- [ ] Select map without zones
- [ ] **Expected:** Graceful handling

---

## ✅ **Final Checks**

### Cross-Browser (if time permits)
- [ ] Test in Chrome
- [ ] Test in Firefox
- [ ] Test in Safari (if on Mac)
- [ ] Test in Edge

### Accessibility
- [ ] Enable screen reader (if possible)
- [ ] Navigate with keyboard only
- [ ] Toggle accessibility mode
- [ ] **Expected:** CRT effects removed
- [ ] **Expected:** System fonts used

### Performance
- [ ] Check Console for errors
- [ ] **Expected:** Zero console errors
- [ ] Check Network tab for failed requests
- [ ] **Expected:** All API calls succeed
- [ ] Monitor memory usage (DevTools)
- [ ] **Expected:** No memory leaks after 5+ routes

---

## 📊 **Testing Summary**

**Total Test Scenarios:** ~80+ test cases
**Estimated Time:** 30-45 minutes
**Critical Path:** Features 1, 2, 3, 5 (zone highlighting, recipes, filtering, mobile)

### Priority Levels
- 🔴 **P0 (Must Pass):** Features 1, 5, 8 (zone highlighting, mobile, keyboard)
- 🟡 **P1 (Should Pass):** Features 2, 3, 6 (recipes, filtering, skeletons)
- 🟢 **P2 (Nice to Pass):** Features 4, 7, 9, 10 (export, tooltips, animations, lazy loading)

---

## 📝 **Reporting Issues**

If you find bugs during testing:

1. **Note the scenario** (which test case)
2. **Record steps to reproduce**
3. **Take screenshot/video** if visual issue
4. **Check browser console** for errors
5. **Document expected vs actual** behavior

**Example:**
```
Feature: Zone Highlighting
Scenario: shouldReturnEmptyListWhenItemHasNoLootType
Steps: Selected "Quest Item" from objectives
Expected: No zones highlighted
Actual: App crashed with null pointer
Console: TypeError: Cannot read property 'name' of null
Browser: Chrome 120
```

---

## 🎉 **When All Tests Pass**

Congratulations! The branch is ready to merge to master.

**Next steps:**
1. Commit any fixes discovered during testing
2. Run full backend test suite: `./mvnw test`
3. Run full frontend build: `cd frontend && npm run build`
4. Merge to master
5. Deploy and celebrate! 🚀
