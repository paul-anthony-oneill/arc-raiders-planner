CREATE TABLE loot_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255),
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE maps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255),
    name VARCHAR(255) UNIQUE NOT NULL,
    image_url VARCHAR(255)
);

CREATE TABLE areas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    map_x INTEGER,
    map_y INTEGER,
    coordinates VARCHAR(2000), -- Changed from TEXT to VARCHAR(2000) for H2 compatibility
    map_id BIGINT NOT NULL,
    loot_abundance INTEGER
);

CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255),
    icon_url VARCHAR(255),
    item_type VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    rarity VARCHAR(255),
    stack_size INTEGER,
    item_value INTEGER,
    weight DOUBLE PRECISION,
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

-- Recipe Schema
CREATE TABLE recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(2000), -- Changed from TEXT for H2 compatibility
    type VARCHAR(50) NOT NULL
);

CREATE TABLE recipe_ingredients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id),
    item_id BIGINT NOT NULL REFERENCES items(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT unique_recipe_ingredient UNIQUE (recipe_id, item_id)
);