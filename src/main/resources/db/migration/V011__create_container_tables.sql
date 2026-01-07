-- V011__create_container_tables.sql

-- Container type catalog (simple player-selectable types)
CREATE TABLE container_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,        -- "Red Locker", "Raider Cache"
    subcategory VARCHAR(100) UNIQUE NOT NULL, -- "red-locker", "raider-cache"
    description TEXT,
    icon_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Grouped marker zones (auto-generated clusters)
CREATE TABLE marker_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,               -- "Dam - Red Locker Zone 1"
    map_id BIGINT NOT NULL REFERENCES maps(id),
    container_type_id BIGINT NOT NULL REFERENCES container_types(id),

    -- Center point coordinates (average of all markers in group)
    center_lat DOUBLE PRECISION NOT NULL,
    center_lng DOUBLE PRECISION NOT NULL,

    -- Metadata
    marker_count INTEGER NOT NULL,            -- How many markers in this group
    radius DOUBLE PRECISION,                  -- Effective radius of the zone

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Junction: markers to groups (one marker = one group)
-- Marker ID is VARCHAR(255) in map_markers, so it should be VARCHAR(255) here too
CREATE TABLE marker_group_members (
    marker_id VARCHAR(255) PRIMARY KEY REFERENCES map_markers(id),
    group_id BIGINT NOT NULL REFERENCES marker_groups(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Track which markers are grouped vs standalone
ALTER TABLE map_markers
    ADD COLUMN is_grouped BOOLEAN DEFAULT FALSE,
    ADD COLUMN standalone_reason VARCHAR(100), -- "isolated", "unique_poi", "extraction_point"
    ADD COLUMN group_id BIGINT REFERENCES marker_groups(id); -- NEW: Foreign key to marker_groups

-- Add indexes for performance
CREATE INDEX idx_marker_groups_map_container ON marker_groups (map_id, container_type_id);
CREATE INDEX idx_map_markers_group_id ON map_markers (group_id);

-- Rollback:
-- DROP INDEX IF EXISTS idx_map_markers_group_id;
-- DROP INDEX IF EXISTS idx_marker_groups_map_container;
-- ALTER TABLE map_markers DROP COLUMN group_id, DROP COLUMN standalone_reason, DROP COLUMN is_grouped;
-- DROP TABLE IF EXISTS marker_group_members CASCADE;
-- DROP TABLE IF EXISTS marker_groups CASCADE;
-- DROP TABLE IF EXISTS container_types CASCADE;