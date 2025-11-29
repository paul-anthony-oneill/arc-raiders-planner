import React, { useState, useEffect } from "react";
import type { EnemyType } from "./types";

const API_BASE_URL = "/api/enemies/types";

interface EnemyIndexProps {
  onEnemyTypeSelected: (enemyType: EnemyType) => void;
  selectedEnemyTypes: EnemyType[];
}

/**
 * Enemy type selection interface.
 * WHY: Allows players to select enemy types (not specific spawns) for raid planning
 */
const EnemyIndex: React.FC<EnemyIndexProps> = ({
  onEnemyTypeSelected,
  selectedEnemyTypes,
}) => {
  const [enemyTypes, setEnemyTypes] = useState<EnemyType[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEnemyTypes = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(API_BASE_URL);
      const data: EnemyType[] = await response.json();
      setEnemyTypes(data);
    } catch (err) {
      console.error("Fetch error:", err);
      setError(
        `Failed to fetch enemy types. Check console for details. (Error: ${err instanceof Error ? err.message : "Unknown"})`,
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEnemyTypes();
  }, []);

  const isEnemyTypeSelected = (enemyType: string) => {
    return selectedEnemyTypes.includes(enemyType);
  };

  const capitalize = (str: string) => {
    return str.charAt(0).toUpperCase() + str.slice(1);
  };

  const handleKeyDown = (e: React.KeyboardEvent, type: EnemyType) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onEnemyTypeSelected(type);
    }
  };

  return (
    <div
      className="enemy-index-container"
      style={{ maxWidth: "800px", margin: "0 auto", fontFamily: "sans-serif" }}
    >
      <h2>ARC Enemy Types ({enemyTypes.length} Available)</h2>
      <p style={{ fontSize: "14px", color: "#666", marginBottom: "20px" }}>
        Select enemy types to target. The planner will route towards spawns of
        these enemies near your loot areas.
      </p>

      {error && <p style={{ color: "red" }}>Error: {error}</p>}

      {loading && enemyTypes.length === 0 && !error && (
        <p>Loading enemy types from backend...</p>
      )}

      <div
        className="enemy-list"
        style={{
          maxHeight: "60vh",
          overflowY: "auto",
          border: "1px solid #ccc",
          padding: "10px",
        }}
      >
        {enemyTypes.length > 0
          ? enemyTypes.map((enemyType) => (
              <div
                key={enemyType}
                role="button"
                tabIndex={0}
                className="enemy-card outline-none focus:ring-2 focus:ring-retro-orange"
                onClick={() => onEnemyTypeSelected(enemyType)}
                onKeyDown={(e) => handleKeyDown(e, enemyType)}
                style={{
                  border: "1px solid #eee",
                  padding: "15px",
                  marginBottom: "10px",
                  borderRadius: "4px",
                  display: "flex",
                  alignItems: "center",
                  cursor: "pointer",
                  backgroundColor: isEnemyTypeSelected(enemyType)
                    ? "#e3f2fd"
                    : "white",
                  transition: "background-color 0.2s",
                }}
              >
                <div
                  style={{
                    width: "40px",
                    height: "40px",
                    borderRadius: "50%",
                    backgroundColor: "#f44336",
                    marginRight: "15px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "white",
                    fontWeight: "bold",
                    fontSize: "12px",
                  }}
                >
                  ⚡
                </div>
                <div>
                  <h4 style={{ margin: 0, fontSize: "18px" }}>
                    {capitalize(enemyType)}
                  </h4>
                </div>
              </div>
            ))
          : !loading && <p>No enemy types available.</p>}
      </div>
    </div>
  );
};

export default EnemyIndex;
