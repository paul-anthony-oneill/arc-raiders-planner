-- Flyway V2: Add Map Markers and Calibration Fields

-- 1. Add Calibration Columns to existing 'maps' table
alter table maps add column cal_scale_x DOUBLE PRECISION DEFAULT 1.0;
alter table maps add column cal_scale_y DOUBLE PRECISION DEFAULT 1.0;
alter table maps add column cal_offset_x DOUBLE PRECISION DEFAULT 0.0;
alter table maps add column cal_offset_y DOUBLE PRECISION DEFAULT 0.0;

-- 2. Create the new 'map_markers' table
create TABLE map_markers (
    id VARCHAR(255) PRIMARY KEY, -- UUID string
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    category VARCHAR(255),
    subcategory VARCHAR(255),
    name VARCHAR(255),
    map_id BIGINT NOT NULL
);

-- 3. Add Foreign Key Constraint
alter table map_markers
add CONSTRAINT fk_marker_map
FOREIGN KEY (map_id) REFERENCES maps (id);