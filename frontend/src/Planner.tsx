import React, { useEffect, useState } from "react";
import { RoutingProfile } from "./types";
import type { Area, Item, PlannerResponse } from "./types";
import MapComponent from "./MapComponent";

const API_PLANNER_URL = "/api/planner";
const API_MAP_DATA_URL = "/api/maps";
const API_ENEMY_TYPES_URL = "/api/enemies/types";

interface PlannerProps {
  selectedItem: Item;
  onBack: () => void;
}

const Planner: React.FC<PlannerProps> = ({ selectedItem, onBack }) => {
  const [recommendations, setRecommendations] = useState<PlannerResponse[]>([]);
  const [mapData, setMapData] = useState<any | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // New state for routing controls
  const [hasRaiderKey, setHasRaiderKey] = useState<boolean>(false);
  const [routingProfile, setRoutingProfile] =
    useState<RoutingProfile>("PURE_SCAVENGER");
  const [showRoutePath, setShowRoutePath] = useState<boolean>(true);

  // New state for enemy targeting
  const [availableEnemies, setAvailableEnemies] = useState<string[]>([]);
  const [targetEnemies, setTargetEnemies] = useState<string[]>([]);

  // Fetch available enemy types on mount
  useEffect(() => {
    const fetchEnemyTypes = async () => {
      try {
        const response = await fetch(API_ENEMY_TYPES_URL);
        const enemyTypes: string[] = await response.json();
        setAvailableEnemies(enemyTypes);
      } catch (err) {
        console.error("Failed to fetch enemy types:", err);
        // Non-critical, so we don't set a user-facing error
      }
    };
    fetchEnemyTypes();
  }, []);

  // Smart defaults: Update routing profile based on Raider Key status
  useEffect(() => {
    if (
      !hasRaiderKey &&
      (routingProfile === "EASY_EXFIL" || routingProfile === "SAFE_EXFIL")
    ) {
      setRoutingProfile("PURE_SCAVENGER");
    }
  }, [hasRaiderKey, routingProfile]);

  useEffect(() => {
    const fetchPlannerData = async () => {
      setLoading(true);
      setError(null);
      setMapData(null);

      if (!selectedItem.lootType && targetEnemies.length === 0) {
        setError(
          `${selectedItem.name} is only obtained via crafting or enemy drops, and no enemy target is selected.`,
        );
        setLoading(false);
        setRecommendations([]);
        return;
      }

      try {
        // POST to new planner API
        const response = await fetch(API_PLANNER_URL, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            targetItemNames: selectedItem.lootType ? [selectedItem.name] : [],
            targetEnemyTypes: targetEnemies,
            hasRaiderKey,
            routingProfile,
          }),
        });

        const results: PlannerResponse[] = await response.json();
        setRecommendations(results);

        // Fetch map coordinates for top result
        if (results.length > 0) {
          const recommendedMapName = results[0].mapName;
          const mapUrl = `${API_MAP_DATA_URL}/${encodeURIComponent(recommendedMapName)}/data`;

          const mapResponse = await fetch(mapUrl);
          const mapJson: { areas: Area[]; name: string } =
            await mapResponse.json();
          setMapData(mapJson);
        }
      } catch (err) {
        console.error("Planner fetch error:", err);
        setError(
          `Failed to process data: ${err instanceof Error ? err.message : "Unknown error"}`,
        );
      } finally {
        setLoading(false);
      }
    };

    fetchPlannerData();
  }, [selectedItem, hasRaiderKey, routingProfile, targetEnemies]);

  // Profile descriptions for help text
  const profileDescriptions: Record<RoutingProfile, string> = {
    PURE_SCAVENGER: "Maximum loot zones - fastest collection",
    EASY_EXFIL: "Routes near Raider Hatches for quick extraction",
    AVOID_PVP: "Safe routes avoiding high-traffic danger zones",
    SAFE_EXFIL: "Combined: Safe route + close Raider Hatch exit",
  };

  return (
    <div
      className="planner-container"
      style={{
        maxWidth: "1200px",
        margin: "0 auto",
        fontFamily: "sans-serif",
        padding: "20px",
      }}
    >
      <button
        onClick={onBack}
        style={{
          marginBottom: "20px",
          padding: "10px 20px",
          cursor: "pointer",
          fontSize: "16px",
        }}
      >
        &larr; Back to Item Search
      </button>

      <h2>Planning for: {selectedItem.name}</h2>
      <p>
        Required Loot Type:{" "}
        <strong>{selectedItem.lootType?.name || "N/A"}</strong>
      </p>

      {/* Routing Profile Controls */}
      <div
        style={{
          backgroundColor: "#f5f5f5",
          padding: "20px",
          borderRadius: "8px",
          marginBottom: "20px",
          border: "1px solid #ddd",
        }}
      >
        <h3 style={{ marginTop: 0 }}>⚙️ Routing Options</h3>

        <div style={{ marginBottom: "15px" }}>
          <label
            style={{
              display: "flex",
              alignItems: "center",
              fontSize: "16px",
              cursor: "pointer",
            }}
          >
            <input
              type="checkbox"
              checked={hasRaiderKey}
              onChange={(e) => setHasRaiderKey(e.target.checked)}
              style={{
                marginRight: "10px",
                width: "18px",
                height: "18px",
                cursor: "pointer",
              }}
            />
            <strong>I have a Raider Key</strong>
          </label>
          <p
            style={{
              marginLeft: "28px",
              marginTop: "5px",
              fontSize: "14px",
              color: "#666",
            }}
          >
            Enables premium extraction options (EASY_EXFIL, SAFE_EXFIL)
          </p>
        </div>

        <div style={{ marginBottom: "15px" }}>
          <label
            style={{
              display: "block",
              marginBottom: "8px",
              fontSize: "16px",
              fontWeight: "bold",
            }}
          >
            Routing Strategy:
          </label>
          <select
            value={routingProfile}
            onChange={(e) =>
              setRoutingProfile(e.target.value as RoutingProfile)
            }
            style={{
              padding: "10px",
              fontSize: "16px",
              width: "100%",
              maxWidth: "400px",
              borderRadius: "4px",
              border: "1px solid #ccc",
              cursor: "pointer",
            }}
          >
            <option value="PURE_SCAVENGER">
              🔍 Pure Scavenger - Most Loot Zones
            </option>
            <option value="EASY_EXFIL" disabled={!hasRaiderKey}>
              🚪 Easy Exfil - Near Raider Hatches{" "}
              {!hasRaiderKey && "(Requires Key)"}
            </option>
            <option value="AVOID_PVP">🛡️ Avoid PvP - Safe from Players</option>
            <option value="SAFE_EXFIL" disabled={!hasRaiderKey}>
              ✅ Safe Exfil - Safe Route + Close Exit{" "}
              {!hasRaiderKey && "(Requires Key)"}
            </option>
          </select>
          <p
            style={{
              marginTop: "8px",
              fontSize: "14px",
              color: "#666",
              fontStyle: "italic",
            }}
          >
            {profileDescriptions[routingProfile]}
          </p>
        </div>

        <div style={{ marginBottom: "15px" }}>
          <label
            style={{
              display: "block",
              marginBottom: "8px",
              fontSize: "16px",
              fontWeight: "bold",
            }}
          >
            🎯 Targeted Enemies:
          </label>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "10px" }}>
            {availableEnemies.map((enemy) => (
              <label
                key={enemy}
                style={{
                  display: "flex",
                  alignItems: "center",
                  cursor: "pointer",
                  padding: "5px 10px",
                  backgroundColor: targetEnemies.includes(enemy)
                    ? "#e0f7fa"
                    : "#fff",
                  border: "1px solid #ccc",
                  borderRadius: "16px",
                }}
              >
                <input
                  type="checkbox"
                  checked={targetEnemies.includes(enemy)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setTargetEnemies([...targetEnemies, enemy]);
                    } else {
                      setTargetEnemies(
                        targetEnemies.filter((t) => t !== enemy),
                      );
                    }
                  }}
                  style={{ marginRight: "8px" }}
                />
                {enemy.charAt(0).toUpperCase() + enemy.slice(1)}
              </label>
            ))}
          </div>
          <p
            style={{
              marginTop: "8px",
              fontSize: "14px",
              color: "#666",
              fontStyle: "italic",
            }}
          >
            Select high-value enemies to include in route planning. The route
            will be scored based on proximity to these targets.
          </p>
        </div>

        <div>
          <label
            style={{
              display: "flex",
              alignItems: "center",
              fontSize: "14px",
              cursor: "pointer",
            }}
          >
            <input
              type="checkbox"
              checked={showRoutePath}
              onChange={(e) => setShowRoutePath(e.target.checked)}
              style={{
                marginRight: "8px",
                width: "16px",
                height: "16px",
                cursor: "pointer",
              }}
            />
            Show route path on map
          </label>
        </div>
      </div>

      <hr />

      {loading && <p>Calculating optimal routes...</p>}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}

      {!loading && !error && (
        <>
          {recommendations.length > 0 && (
            <div>
              <h3>🗺️ Ranked Map Recommendations</h3>
              <ol style={{ paddingLeft: "20px" }}>
                {recommendations.map((rec, index) => (
                  <li
                    key={rec.mapId}
                    style={{
                      marginBottom: "15px",
                      padding: "15px",
                      border: index === 0 ? "2px solid gold" : "1px solid #ccc",
                      backgroundColor: index === 0 ? "#b5c26aff" : "white",
                      fontWeight: index === 0 ? "bold" : "normal",
                      borderRadius: "6px",
                    }}
                  >
                    <div>
                      #{index + 1}: <strong>{rec.mapName}</strong>
                      <span style={{ marginLeft: "10px", color: "#666" }}>
                        (Score: {rec.score.toFixed(1)})
                      </span>
                    </div>
                    <div
                      style={{
                        fontSize: "14px",
                        marginTop: "5px",
                        color: "#555",
                      }}
                    >
                      Route: {rec.routePath.length} areas
                      {rec.extractionPoint && ` → Exit: ${rec.extractionPoint}`}
                    </div>
                  </li>
                ))}
              </ol>
            </div>
          )}

          {/* Render the map with enhanced features */}
          {mapData && recommendations.length > 0 && (
            <MapComponent
              mapName={recommendations[0].mapName}
              areas={mapData.areas}
              routePath={recommendations[0].routePath}
              extractionPoint={recommendations[0].extractionPoint}
              routingProfile={routingProfile}
              showRoutePath={showRoutePath}
              enemySpawns={recommendations[0].nearbyEnemySpawns}
            />
          )}
        </>
      )}
    </div>
  );
};

export default Planner;
