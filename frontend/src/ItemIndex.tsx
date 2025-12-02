import React from "react";
import { CatalogIndex } from "./components/CatalogIndex";
import type { Item } from "./types";

interface ItemIndexProps {
  onItemSelected: (item: Item) => void;
}

/**
 * Item search and selection interface using generic CatalogIndex.
 * WHY: Reuses CatalogIndex to eliminate 70+ lines of boilerplate
 */
const ItemIndex: React.FC<ItemIndexProps> = ({ onItemSelected }) => {
  return (
    <CatalogIndex<Item>
      apiUrl="/api/items"
      title="ARC Raiders Item Index"
      onSelect={onItemSelected}
      getItemKey={(item) => item.id}
      searchPlaceholder="Search items (e.g., circuit, gear)..."
      filterItems={(items) => items.filter((item) => item.lootType !== null)}
      renderItem={(item) => (
        <div>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "start",
            }}
          >
            <div style={{ flex: 1 }}>
              <strong style={{ fontSize: "16px" }}>{item.name}</strong>
              <div
                style={{
                  marginTop: "4px",
                  fontSize: "14px",
                  color: "#666",
                }}
              >
                {item.description}
              </div>
            </div>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "flex-end",
                gap: "4px",
                marginLeft: "16px",
              }}
            >
              <span
                style={{
                  padding: "2px 8px",
                  background: "#f0f0f0",
                  borderRadius: "4px",
                  fontSize: "12px",
                  fontWeight: "bold",
                }}
              >
                {item.rarity}
              </span>
              <span
                style={{
                  fontSize: "12px",
                  color: "#666",
                }}
              >
                {item.lootType}
              </span>
            </div>
          </div>
        </div>
      )}
    />
  );
};

export default ItemIndex;
