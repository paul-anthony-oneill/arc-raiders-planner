-- V013__fix_water_treatment_location.sql
-- Fixes the incorrect coordinates for 'Water Treatment Control' in Dam Battlegrounds
-- Previous values (300, 500) were placeholders.
-- New values (-155, -20) align with the centroid of the existing polygon zone.

UPDATE areas 
SET map_x = -155, 
    map_y = -20 
WHERE name = 'Water Treatment Control' 
  AND map_id = (SELECT id FROM maps WHERE name = 'Dam Battlegrounds');
