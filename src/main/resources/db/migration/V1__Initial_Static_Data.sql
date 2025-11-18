-- Flyway Script V1: Initial Static Data Load

-- 1. INSERT LOOT TYPES
INSERT INTO loot_types (id, name) VALUES
                                      (1, 'Industrial'),
                                      (2, 'Mechanical'),
                                      (3, 'Residential'),
                                      (4, 'ARC'),
                                      (5, 'Nature'),
                                      (6, 'Technological'),
                                      (7, 'Electrical'),
                                      (8, 'Commercial'),
                                      (9, 'Medical'),
                                      (10, 'Security');
INSERT INTO maps (id, name, description) VALUES
    (1, 'Dam Battlegrounds', 'dam');

INSERT INTO areas (id, name, map_id, map_x, map_y) VALUES
                                                       (1, 'Scrap Yard', 1, 150, 600),
                                                       (2, 'Pale Apartments', 1, 400, 200),
                                                       (3, 'Electrical Substation', 1, 700, 300),
                                                       (4, 'Primary Facility', 1, 800, 600),
                                                       (5, 'Hydroponic Dome Complex', 1, 50, 50),
                                                       (6, 'Water Treatment Control', 1, 300, 500),
                                                       (7, 'Research and Administration', 1, 150, 600),
                                                       (8, 'Testing Annex', 1, 400, 200),
                                                       (9, 'Power Generation Complex', 1, 400, 200),
                                                       (10, 'Old Battleground', 1, 400, 200),
                                                       (11, 'Control Tower', 1, 400, 200);

-- This links Areas (Zones) to Loot Types (Categories).
-- Format: (area_id, loot_type_id)

INSERT INTO area_loot_type (area_id, loot_type_id) VALUES
-- Water Treatment Control (ID 6): Industrial (1) AND Mechanical (2)
(1, 1),
(1, 2),
(2, 3),
(3, 7),
(4, 1),
(4, 2),
(5, 5),
(5, 1),
(5, 10),
(6, 1),
(6, 2),
(7, 8),
(7, 6),
(8, 9),
(8, 8),
(9, 1),
(9, 7),
(10, 4),
(11, 10),
(11, 6);

-- 5. RESET SEQUENCES
SELECT setval(pg_get_serial_sequence('loot_types', 'id'), (SELECT MAX(id) FROM loot_types));
SELECT setval(pg_get_serial_sequence('maps', 'id'), (SELECT MAX(id) FROM maps));
SELECT setval(pg_get_serial_sequence('areas', 'id'), (SELECT MAX(id) FROM areas));