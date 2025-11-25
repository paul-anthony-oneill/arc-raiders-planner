-- Rename existing columns to preserve data during migration
ALTER TABLE map_markers
    RENAME COLUMN lat TO raw_lat_temp;

ALTER TABLE map_markers
    RENAME COLUMN lng TO raw_lng_temp;

-- Add new calibrated coordinate columns
ALTER TABLE map_markers
    ADD COLUMN lat DOUBLE PRECISION,
    ADD COLUMN lng DOUBLE PRECISION;

-- Backfill with calibrated coordinates
UPDATE map_markers mm
SET
    lat = (mm.raw_lat_temp * gm.cal_scale_y) + gm.cal_offset_y,
    lng = (mm.raw_lng_temp * gm.cal_scale_x) + gm.cal_offset_x
FROM maps gm
WHERE mm.map_id = gm.id;

-- Make columns non-nullable
ALTER TABLE map_markers
    ALTER COLUMN lat SET NOT NULL,
    ALTER COLUMN lng SET NOT NULL;

-- Drop temporary columns
ALTER TABLE map_markers
    DROP COLUMN raw_lat_temp,
    DROP COLUMN raw_lng_temp;

-- Add index for spatial queries
CREATE INDEX idx_markers_coords ON map_markers(lat, lng);

-- Update column comments
COMMENT ON COLUMN map_markers.lat IS 'Leaflet-ready Y coordinate (calibrated, ready for [lat,lng] format)';
COMMENT ON COLUMN map_markers.lng IS 'Leaflet-ready X coordinate (calibrated, ready for [lat,lng] format)';
