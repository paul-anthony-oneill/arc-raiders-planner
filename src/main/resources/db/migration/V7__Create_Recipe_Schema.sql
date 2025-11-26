CREATE TABLE recipes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(50) NOT NULL -- 'CRAFTING' or 'WORKBENCH_UPGRADE'
);

CREATE TABLE recipe_ingredients (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id), -- Assumes 'items' table exists from Item.java
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT unique_recipe_ingredient UNIQUE (recipe_id, item_id)
);
