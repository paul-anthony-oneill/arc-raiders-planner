-- Flyway Script V1: Initial Static Data Load
-- Uses CTEs (WITH... AS) and RETURNING to dynamically assign IDs and link foreign keys.

-- 1. INSERT LOOT TYPES and capture their generated IDs
WITH inserted_loot_types AS (
    INSERT INTO loot_types (name) VALUES
    ('Industrial'), ('Mechanical'), ('Residential'), ('ARC'), ('Nature'),
    ('Technological'), ('Electrical'), ('Commercial'), ('Medical'), ('Security')
    RETURNING id, name
),

-- 2. INSERT MAPS and capture the generated ID
inserted_maps AS (
    INSERT INTO maps (name, description) VALUES
    ('Dam Battlegrounds', 'dam')
    RETURNING id
),

-- 3. Define raw area data in a VALUES table (No IDs needed)
area_raw_data AS (
    SELECT name, map_x, map_y, coordinates, loot_abundance, map_id.id AS map_id FROM (
        VALUES
        ('Electrical Substation', -163, -277, '[[-330.62083435058594,-163.875],[-230.37083435058594,-106.625],[-199.37083435058594,-164.125],[-296.37083435058594,-218.375],[-329.62083435058594,-164.375]]', 3, 'Dam Battlegrounds'),
        ('Pale Apartments and Ruby Residence', -314, 508, '[[462.2583312988281,-438.75],[379.7583312988281,-398.75],[425.7583312988281,-303.25],[534.2583312988281,-131.75],[646.7583312988281,-179.25],[621.7583312988281,-228.75],[578.2583312988281,-266.75],[464.7583312988281,-436.25],[461.12916564941406,-438.125]]', 1, 'Dam Battlegrounds'),
        ('Generator Hall', 334, 474, '[[406.2583312988281,356.75],[504.2583312988281,408.75],[573.7583312988281,301.75],[476.7583312988281,247.75],[409.2583312988281,354.75]]', 2, 'Dam Battlegrounds'),
        ('Dam Power Core', 800, 600, NULL, 3, 'Dam Battlegrounds'),
        ('Hydroponic Dome Complex', 14, 299, '[[191.75833129882812,-35.25],[284.7583312988281,-96.75],[439.37916564941406,15.125],[418.62916564941406,106.375],[268.37916564941406,130.125],[191.87916564941406,-34.625]]', 1, 'Dam Battlegrounds'),
        ('Water Treatment Control', 300, 500,
         '[[-69.645828, -112.750000], [-89.145828, -179.500000], [3.854172, -203.000000], [15.354172, -171.250000], [29.854172, -172.750000], [38.854172, -141.000000], [-68.395828, -113.000000]]', 2, 'Dam Battlegrounds'),
        ('Research and Administration', 150, 600,
         '[[-168.516663, 101.499851], [-106.266663, 133.268824], [-152.516663, 206.312448], [-212.766663, 174.793624]]', 2, 'Dam Battlegrounds'),
        ('Testing Annex', 378, -363, '[[-432.12083435058594,393.375],[-402.12083435058594,348.125],[-360.12083435058594,338.375],[-338.37083435058594,349.125],[-333.12083435058594,363.125],[-284.87083435058594,388.875],[-324.62083435058594,448.125],[-431.87083435058594,394.125]]', 2, 'Dam Battlegrounds'),
        ('Power Generation Complex', 460, 390, '[[359.7583312988281,434.25],[371.7583312988281,475.25],[342.2583312988281,518.75],[411.7583312988281,554.75],[490.7583312988281,430.25],[396.7583312988281,374.25],[359.2583312988281,434.25]]', 2, 'Dam Battlegrounds'),
        ('Old Battleground', -408, 156, '[[144.25833129882812,-459.75],[103.75833129882812,-432.25],[107.75833129882812,-386.25],[147.25833129882812,-345.75],[201.75833129882812,-357.75],[217.25833129882812,-394.75],[182.75833129882812,-427.25],[143.25833129882812,-458.25]]', 3, 'Dam Battlegrounds'),
        ('Control Tower', 192, -107, '[[-151.37083435058594,207.125],[-105.62083435058594,133.875],[-40.12083435058594,168.375],[-84.87083435058594,242.125],[-150.62083435058594,207.625]]', 1, 'Dam Battlegrounds')
    ) AS area_data (name, map_x, map_y, coordinates, loot_abundance, map_name)
    INNER JOIN inserted_maps map_id ON area_data.map_name = 'Dam Battlegrounds'
),

-- 4. INSERT AREAS using the raw data and dynamically generated map_id
inserted_areas AS (
    INSERT INTO areas (name, map_id, map_x, map_y, coordinates, loot_abundance)
    SELECT
        name,
        map_id,
        map_x,
        map_y,
        coordinates,
        loot_abundance
    FROM area_raw_data
    RETURNING id, name
)

-- 5. INSERT MANY-TO-MANY LINKS (The final join operation)
INSERT INTO area_loot_type (area_id, loot_type_id)
SELECT
    a.id AS area_id,
    lt.id AS loot_type_id
FROM inserted_areas a
JOIN inserted_loot_types lt ON lt.name IN (
    -- Define the required LootTypes for each Area Name
    CASE a.name WHEN 'Electrical Substation' THEN 'Electrical' END,
    CASE a.name WHEN 'Pale Apartments and Ruby Residence' THEN 'Residential' END,
    CASE a.name WHEN 'Generator Hall' THEN 'Electrical' END,
    CASE a.name WHEN 'Hydroponic Dome Complex' THEN 'Nature' END,
    CASE a.name WHEN 'Hydroponic Dome Complex' THEN 'Industrial' END,
    CASE a.name WHEN 'Hydroponic Dome Complex' THEN 'Security' END,
    CASE a.name WHEN 'Water Treatment Control' THEN 'Industrial' END,
    CASE a.name WHEN 'Water Treatment Control' THEN 'Mechanical' END,
    CASE a.name WHEN 'Research and Administration' THEN 'Commercial' END,
    CASE a.name WHEN 'Research and Administration' THEN 'Technological' END,
    CASE a.name WHEN 'Testing Annex' THEN 'Medical' END,
    CASE a.name WHEN 'Testing Annex' THEN 'Commercial' END,
    CASE a.name WHEN 'Power Generation Complex' THEN 'Industrial' END,
    CASE a.name WHEN 'Power Generation Complex' THEN 'Electrical' END,
    CASE a.name WHEN 'Old Battleground' THEN 'ARC' END,
    CASE a.name WHEN 'Control Tower' THEN 'Security' END,
    CASE a.name WHEN 'Control Tower' THEN 'Technological' END
)
-- Ensure we only match on the rows defined in the CASE statement
WHERE lt.name IS NOT NULL;