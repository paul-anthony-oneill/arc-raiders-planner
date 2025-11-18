-- Flyway Script V0: Create Initial Database Schema
CREATE TABLE loot_types (
                            id BIGSERIAL PRIMARY KEY,
                            description VARCHAR(255),
                            name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE maps (
                      id BIGSERIAL PRIMARY KEY,
                      description VARCHAR(255),
                      name VARCHAR(255) UNIQUE NOT NULL,
                      image_url VARCHAR(255)
);

CREATE TABLE areas (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) UNIQUE NOT NULL,
                       map_x INTEGER,
                       map_y INTEGER,
                       map_id BIGINT NOT NULL,
                       loot_type_id BIGINT
);

CREATE TABLE items (
                       id BIGSERIAL PRIMARY KEY,
                       description VARCHAR(255),
                       icon_url VARCHAR(255),
                       item_type VARCHAR(255),
                       name VARCHAR(255) NOT NULL,
                       rarity VARCHAR(255),
                       stack_size INTEGER,
                       value INTEGER,
                       weight NUMERIC(38,2),
                       loot_type_id BIGINT
);

CREATE TABLE area_loot_type (
                                area_id BIGINT NOT NULL,
                                loot_type_id BIGINT NOT NULL,
                                PRIMARY KEY (area_id, loot_type_id)
);

ALTER TABLE areas ADD CONSTRAINT fk_area_map FOREIGN KEY (map_id) REFERENCES maps (id);
ALTER TABLE area_loot_type ADD CONSTRAINT fk_alt_area FOREIGN KEY (area_id) REFERENCES areas (id);
ALTER TABLE area_loot_type ADD CONSTRAINT fk_alt_loot_type FOREIGN KEY (loot_type_id) REFERENCES loot_types (id);
ALTER TABLE items ADD CONSTRAINT fk_item_loot_type FOREIGN KEY (loot_type_id) REFERENCES loot_types (id);