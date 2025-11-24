import React, { useState, useEffect } from "react";
import type { Item } from "./types";

const API_BASE_URL = "/api/items";
interface ItemIndexProps {
  onItemSelected: (item: Item) => void;
}

const ItemIndex: React.FC<ItemIndexProps> = ({ onItemSelected }) => {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const fetchItems = async (search: string = "") => {
    setLoading(true);
    setError(null);

    try {
      const url = `${API_BASE_URL}${search ? `?search=${search}` : ""}`;
      const response = await fetch(url);
      const data: Item[] = await response.json();
      setItems(data);
    } catch (err) {
      console.error("Fetch error:", err);
      setError(
        `Failed to fetch items. Check console for details. (Error: ${err instanceof Error ? err.message : "Unknown"})`,
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const delaySearch = setTimeout(() => {
      fetchItems(searchTerm);
    }, 300);

    return () => clearTimeout(delaySearch);
  }, [searchTerm]);

  const visibleItems = items.filter((item) => item.lootType !== null);

  return (
    <div
      className="item-index-container"
      style={{ maxWidth: "800px", margin: "0 auto", fontFamily: "sans-serif" }}
    >
      <h2>ARC Raiders Item Index ({visibleItems.length} Total)</h2>
      <input
        type="text"
        placeholder="Search items (e.g., circuit, gear)..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        style={{
          width: "100%",
          padding: "10px",
          marginBottom: "20px",
          fontSize: "16px",
        }}
      />

      {error && <p style={{ color: "red" }}>Error: {error}</p>}

      {loading && visibleItems.length === 0 && !error && (
        <p>Loading items from backend...</p>
      )}

      <div
        className="item-list"
        style={{
          maxHeight: "60vh",
          overflowY: "auto",
          border: "1px solid #ccc",
          padding: "10px",
        }}
      >
        {visibleItems.length > 0
          ? visibleItems.map((item) => (
              <div
                key={item.id}
                className="item-card"
                onClick={() => onItemSelected(item)}
                style={{
                  border: "1px solid #eee",
                  padding: "10px",
                  marginBottom: "10px",
                  borderRadius: "4px",
                  display: "flex",
                  alignItems: "center",
                  cursor: "pointer",
                }}
              >
                <div className="item-icon-container">
                  {" "}
                  {}
                  <img
                    src={item.iconUrl || "placeholder.webp"}
                    alt={item.name}
                  />
                </div>
                <div>
                  <h4>
                    {item.name} ({item.rarity})
                  </h4>
                  <p style={{ margin: 0, fontSize: "14px" }}>
                    Type: <strong>{item.itemType}</strong> | Loot Zone:
                    <strong>
                      {item.lootType
                        ? item.lootType.name
                        : " (Crafted/Enemy Drop)"}
                    </strong>
                  </p>
                </div>
              </div>
            ))
          : !loading && <p>No items found matching "{searchTerm}".</p>}
      </div>
    </div>
  );
};

export default ItemIndex;
