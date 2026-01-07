import React from "react";
import { CatalogIndex } from "./components/CatalogIndex";
import type { EnemyType } from "./types";

interface EnemyIndexProps {
  onEnemyTypeSelected: (enemyType: EnemyType) => void;
  selectedEnemyTypes: EnemyType[];
}

/**
 * Enemy type selection interface using generic CatalogIndex.
 * WHY: Reuses CatalogIndex to eliminate 70+ lines of boilerplate
 */
const EnemyIndex: React.FC<EnemyIndexProps> = ({
  onEnemyTypeSelected,
  selectedEnemyTypes,
}) => {
  const isEnemyTypeSelected = (enemyType: string) => {
    return selectedEnemyTypes.includes(enemyType);
  };

  const capitalize = (str: string) => {
    return str.charAt(0).toUpperCase() + str.slice(1);
  };

  return (
    <CatalogIndex<EnemyType>
      apiUrl="/api/enemies/types"
      title="ARC Enemy Types"
      onSelect={onEnemyTypeSelected}
      getItemKey={(enemyType) => enemyType}
      searchable={false}
      subtitle="Select enemy types to target. The planner will route towards spawns of these enemies near your loot areas."
      renderItem={(enemyType) => (
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
            <strong style={{ fontSize: "16px" }}>
              {capitalize(enemyType)}
            </strong>
          </div>
          <div
            style={{
              padding: "4px 12px",
              background: isEnemyTypeSelected(enemyType)
                ? "#4CAF50"
                : "#ddd",
              color: isEnemyTypeSelected(enemyType) ? "white" : "#333",
              borderRadius: "4px",
              fontSize: "12px",
              fontWeight: "bold",
            }}
          >
            {isEnemyTypeSelected(enemyType) ? "SELECTED" : "SELECT"}
          </div>
        </div>
      )}
    />
  );
};

export default EnemyIndex;
