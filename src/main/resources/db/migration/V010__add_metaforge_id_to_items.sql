-- V010: Add Metaforge ID to Items
-- Purpose: Link local items to Metaforge API ID for reliable lookup and relationships
-- WHY: Needed to map workbench upgrade ingredients (which use API IDs) to local database items

ALTER TABLE items ADD COLUMN metaforge_id VARCHAR(100);

-- Create index for fast lookups
CREATE INDEX idx_items_metaforge_id ON items(metaforge_id);

-- Ensure uniqueness where it exists
CREATE UNIQUE INDEX idx_items_metaforge_id_unique ON items(metaforge_id) WHERE metaforge_id IS NOT NULL;
