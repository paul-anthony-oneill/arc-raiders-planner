-- V8: Add Metaforge API Recipe Support
-- Purpose: Enable automatic recipe sync from Metaforge API
-- WHY: Reduces manual entry burden, provides single source of truth for recipe data

-- Step 1: Clean slate - remove all existing manual recipes
-- WHY: All recipes will be synced from API, existing manual data is obsolete
DELETE FROM recipe_ingredients; -- Remove ingredients first (FK constraint)
DELETE FROM recipes;            -- Remove all recipes

-- Step 2: Add workbench tracking to items
-- WHY: Enables "upgrade Refiner to Level 3" as targetable objectives
ALTER TABLE items ADD COLUMN workbench VARCHAR(100);

-- Step 3: Add Metaforge API linkage to recipes
-- WHY: Links recipes to API items for idempotent sync (prevents duplicates)
ALTER TABLE recipes ADD COLUMN metaforge_item_id VARCHAR(100);
ALTER TABLE recipes ADD COLUMN is_recyclable BOOLEAN DEFAULT FALSE;

-- Step 4: Add indexes for performance
-- WHY: Optimize Metaforge ID lookups during sync operations
CREATE INDEX idx_recipes_metaforge_id ON recipes(metaforge_item_id);

-- Step 5: Add unique constraint for API linkage
-- WHY: Prevents duplicate recipes for same API item
CREATE UNIQUE INDEX idx_recipes_metaforge_unique ON recipes(metaforge_item_id)
    WHERE metaforge_item_id IS NOT NULL;

-- Note: is_recyclable field reserved for Phase 3 (recycle recipe support)
