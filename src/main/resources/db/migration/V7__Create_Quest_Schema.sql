-- V7__Create_Quest_Schema.sql

CREATE TABLE quests (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE quest_markers (
    quest_id VARCHAR(255) REFERENCES quests(id),
    marker_id VARCHAR(255) REFERENCES map_markers(id),
    PRIMARY KEY (quest_id, marker_id)
);

CREATE INDEX idx_quest_markers_quest_id ON quest_markers(quest_id);
CREATE INDEX idx_quest_markers_marker_id ON quest_markers(marker_id);

-- Rollback:
-- DROP TABLE quest_markers;
-- DROP TABLE quests;
