-- V009__add_item_dropped_by.sql

CREATE TABLE item_dropped_by (
    item_id BIGINT NOT NULL,
    enemy_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (item_id, enemy_id),
    CONSTRAINT fk_item_dropped_by_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Rollback: DROP TABLE item_dropped_by;