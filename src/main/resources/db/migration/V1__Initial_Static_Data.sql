-- Flyway Script V1: Initial Static Data Load

-- 1. INSERT LOOT TYPES
insert into loot_types (id, name)
values (1, 'Industrial'),
       (2, 'Mechanical'),
       (3, 'Residential'),
       (4, 'ARC'),
       (5, 'Nature'),
       (6, 'Technological'),
       (7, 'Electrical'),
       (8, 'Commercial'),
       (9, 'Medical'),
       (10, 'Security');

insert into maps (id, name, description)
values (1, 'Dam Battlegrounds', 'dam');

insert into areas (id, name, map_id, map_x, map_y, coordinates)
values (1, 'Maintenance Tunnels', 1, 150, 600, null),
       (2, 'Housing Complex', 1, 400, 200, NULL),
       (3, 'North Factory', 1, 700, 300, NULL),
       (4, 'Dam Power Core', 1, 800, 600, NULL),
       (5, 'Fallen Satellite Site', 1, 50, 50, NULL),
       (6, 'Water Treatment Control', 1, 300, 500,
        '[
[-69.645828, -112.750000],
[-89.145828, -179.500000],
[3.854172, -203.000000],
[15.354172, -171.250000],
[29.854172, -172.750000],
[38.854172, -141.000000],
[-68.395828, -113.000000]
]'),

       (7, 'Research and Administration', 1, 150, 600,
        '[
    [-168.516663, 101.499851],
    [-106.266663, 133.268824],
    [-152.516663, 206.312448],
    [-212.766663, 174.793624],
]'),

       (8, 'Testing Annex', 1, 400, 200, NULL),
       (9, 'Power Generation Complex', 1, 400, 200, NULL),
       (10, 'Old Battleground', 1, 400, 200, NULL),
       (11, 'Control Tower', 1, 400, 200, NULL);

-- This links Areas (Zones) to Loot Types (Categories).
-- Format: (area_id, loot_type_id)

insert into area_loot_type (area_id, loot_type_id)
values
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
select setval(pg_get_serial_sequence('loot_types', 'id'), (select max(id) from loot_types));
select setval(pg_get_serial_sequence('maps', 'id'), (select max(id) from maps));
select setval(pg_get_serial_sequence('areas', 'id'), (select max(id) from areas));