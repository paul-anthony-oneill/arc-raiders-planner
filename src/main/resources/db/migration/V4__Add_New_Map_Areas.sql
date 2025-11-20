-- Flyway Script V4: Add Areas for New Maps
-- Template for adding areas to 'The Spaceport', 'Buried City', and 'Blue Gate'

-- 1. Define your new areas here in the VALUES clause.
--    Columns: Name, Map X, Map Y, Polygon Coordinates (JSON string), Loot Abundance (1-3), Map Name
WITH new_area_data (name, map_x, map_y, coordinates, loot_abundance, map_name) AS (
    VALUES
    -- === THE SPACEPORT ===
    ('Arrival Building', -222, 487, '[[536.25,-218.25],[490.75,-162.75],[411.75,-228],[459.5,-285],[536.5,-217.75]]', 1, 'The Spaceport'),
    ('Departure Building', -250, 186, '[[115,-269.25],[164.25,-228.25],[148,-207.5],[230,-138],[317.75,-244.5],[233.5,-310.75],[199.75,-268.5],[149,-310],[115.75,-269.75]]', 1, 'The Spaceport'),
    ('Launch Towers', -40, 267, '[[300.75,-113.5],[228.25,-97.25],[183.5,-42.75],[184.75,21.5],[278.75,101.5],[394,-36.75],[301,-113]]', 1, 'The Spaceport'),
    ('Control Tower A6', 55, 15, '[[-1.25,-2.25],[38.5,20],[74.25,145.75],[11.5,162.75],[-29.5,5],[-2,-2.25]]', 1, 'The Spaceport'),
    ('Container Storage', 245, -63, '[[-137,222.75],[-109.5,319.5],[47.5,277.5],[22,180.5],[-137.75,223]]', 1, 'The Spaceport'),
    ('Vehicle Maintenance', 72, -120, '[[-71.25,23],[-50,107],[-166.25,138.5],[-180.25,89.75],[-144.75,79.75],[-154,44.75],[-71.5,23.5]]', 2, 'The Spaceport'),
    ('Rocket Assembly', 319, 268, '[[353.75,308],[296.75,260.25],[273.75,285],[233.5,254.5],[184.25,312.75],[234,356.75],[219.5,373.5],[266.25,412.5],[353.75,308]]', 2, 'The Spaceport'),
    ('Trench Towers', 125, 600, '[[578.5,84.5],[611.75,112],[659.5,153.5],[627,190.75],[545.5,123],[578.5,84.75]]', 2, 'The Spaceport'),
    ('Fuel Control', 245, 357, '[[389.25,249.5],[366.25,231.25],[355.25,236.75],[331.25,215],[309.25,244.25],[357.5,290.5],[388.25,250]]', 3, 'The Spaceport'),

    -- === BURIED CITY ===
    ('Plaza Rosa', 28, -457, '[[-510.25,40.75],[-480,94.5],[-410.75,112.5],[-372.5,-43.75],[-446.25,-64],[-467.25,13],[-510.25,40.25]]', 2, 'Buried City'),
    ('Grandioso Apartments', -350, -290, '[[-356,-342.25],[-207.25,-300.25],[-191.75,-362],[-339.25,-402.25],[-357,-342]]', 2, 'Buried City'),
    ('Town Hall', 94, -97, '[[-113.75,-18.25],[-152,4.75],[-82.5,127.75],[-105,140.5],[-114.75,178.75],[-77.5,188],[-14.75,151],[-114.25,-18.5]]', 1, 'Buried City'),
    ('Research', 102, 88, '[[40,106.75],[65.25,151.75],[161.75,95.5],[135.5,50.25],[39.75,106.75]]', 2, 'Buried City'),
    ('Space Travel', 154, 118, '[[69.25,158.75],[96,203.5],[191.5,148.25],[165.75,102.5],[68.75,158]]', 1, 'Buried City'),
    ('Hospital', 29, 337, '[[265,45.25],[293,93],[444.5,4],[416.25,-44],[264,45.25]]', 1, 'Buried City'),
    ('Library', -120, 257, '[[263.75,-199.5],[312.75,-115],[276,-94.5],[275.25,-53.75],[237.25,-31.5],[171.75,-145.75],[263.5,-199.25]]', 2, 'Buried City')
    -- === BLUE GATE ===
    -- ('Gate Entrance', 0, 0, NULL, 1, 'Blue Gate')
    
),

-- 2. Insert the areas into the database and capture the generated IDs
inserted_areas AS (
    INSERT INTO areas (name, map_id, map_x, map_y, coordinates, loot_abundance)
    SELECT
        d.name,
        m.id,
        d.map_x,
        d.map_y,
        d.coordinates,
        d.loot_abundance
    FROM new_area_data d
    JOIN maps m ON m.name = d.map_name -- Links to the maps created in V3
    RETURNING id, name
)

-- 3. Link the new areas to their Loot Types
INSERT INTO area_loot_type (area_id, loot_type_id)
SELECT
    ia.id,
    lt.id
FROM inserted_areas ia
JOIN loot_types lt ON lt.name IN (
    -- Map Area Names to their Loot Types here
    -- Use one CASE per area to return the Loot Type name(s) it should have
    
    CASE ia.name 
        WHEN 'Example Area' THEN 'Industrial' 
        -- WHEN 'Spaceport Hangar' THEN 'Mechanical'
        -- WHEN 'Spaceport Hangar' THEN 'Security' -- Can have multiple types
    END
    
    -- Add more CASE statements or expand the logic as needed
)
WHERE lt.name IS NOT NULL;
