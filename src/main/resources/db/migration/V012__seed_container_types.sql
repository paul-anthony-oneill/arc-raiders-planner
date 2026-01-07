-- V012__seed_container_types.sql

INSERT INTO container_types (name, subcategory, description, icon_url) VALUES
('Red Locker', 'red-locker', 'High-value locker found in various locations.', NULL),
('Raider Cache', 'raider-cache', 'Hidden stash of high-tier loot.', NULL),
('Weapon Crate', 'weapon-crate', 'Container holding various weapons and weapon parts.', NULL),
('Toolbox', 'toolbox', 'Contains industrial and mechanical components.', NULL),
('Medical Cabinet', 'medical-cabinet', 'Stores medical supplies and consumables.', NULL),
('Ammo Box', 'ammo-box', 'Contains various types of ammunition.', NULL),
('Safe', 'safe', 'High security container, often requiring a key or code.', NULL)
ON CONFLICT (subcategory) DO NOTHING; -- Prevents re-insertion on re-run

-- Rollback:
-- DELETE FROM container_types WHERE subcategory IN ('red-locker', 'raider-cache', 'weapon-crate', 'toolbox', 'medical-cabinet', 'ammo-box', 'safe');