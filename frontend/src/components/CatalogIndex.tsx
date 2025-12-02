import React, { useEffect, useState } from 'react';

/**
 * Props for the generic CatalogIndex component.
 * WHY: Allows reuse across Items, Enemies, Containers without code duplication
 */
export interface CatalogIndexProps<T> {
  apiUrl: string;
  title: string;
  onSelect: (item: T) => void;
  renderItem: (item: T) => React.ReactNode;
  searchable?: boolean;
  searchPlaceholder?: string;
  getItemKey: (item: T) => string | number;
  subtitle?: string;
  filterItems?: (items: T[]) => T[];
}

/**
 * Generic catalog index component for displaying searchable item lists.
 * WHY: Eliminates ~150 lines of duplicated code across ItemIndex, EnemyIndex, and future container/recipe indexes.
 *
 * @template T The type of items being displayed (Item, Enemy, Container, etc.)
 */
export function CatalogIndex<T>({
  apiUrl,
  title,
  onSelect,
  renderItem,
  searchable = true,
  searchPlaceholder = "Search...",
  getItemKey,
  subtitle,
  filterItems
}: CatalogIndexProps<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const delaySearch = setTimeout(() => {
      setLoading(true);
      setError(null);

      const url = searchable && searchTerm
        ? `${apiUrl}?search=${encodeURIComponent(searchTerm)}`
        : apiUrl;

      fetch(url)
        .then(r => {
          if (!r.ok) throw new Error(`HTTP ${r.status}: ${r.statusText}`);
          return r.json();
        })
        .then(data => {
          setItems(data);
          setLoading(false);
        })
        .catch(err => {
          console.error("Fetch error:", err);
          setError(err instanceof Error ? err.message : "Unknown error");
          setLoading(false);
        });
    }, 300);

    return () => clearTimeout(delaySearch);
  }, [searchTerm, apiUrl, searchable]);

  const displayedItems = filterItems ? filterItems(items) : items;

  const handleKeyDown = (e: React.KeyboardEvent, item: T) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelect(item);
    }
  };

  if (loading && displayedItems.length === 0 && !error) {
    return (
      <div style={{ maxWidth: "800px", margin: "0 auto", fontFamily: "sans-serif" }}>
        <h2>{title}</h2>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ maxWidth: "800px", margin: "0 auto", fontFamily: "sans-serif" }}>
        <h2>{title}</h2>
        <p style={{ color: "red" }}>Error: {error}</p>
      </div>
    );
  }

  return (
    <div
      className="catalog-index-container"
      style={{ maxWidth: "800px", margin: "0 auto", fontFamily: "sans-serif" }}
    >
      <h2>{title} ({displayedItems.length} {displayedItems.length === 1 ? 'item' : 'items'})</h2>

      {subtitle && (
        <p style={{ fontSize: "14px", color: "#666", marginBottom: "20px" }}>
          {subtitle}
        </p>
      )}

      {searchable && (
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder={searchPlaceholder}
          style={{
            width: "100%",
            padding: "10px",
            marginBottom: "20px",
            fontSize: "16px",
          }}
        />
      )}

      <div
        className="catalog-items"
        style={{
          maxHeight: "60vh",
          overflowY: "auto",
          border: "1px solid #ccc",
          padding: "10px",
        }}
      >
        {displayedItems.length === 0 ? (
          <div style={{ color: "#666", padding: "20px", textAlign: "center" }}>
            No items found
          </div>
        ) : (
          displayedItems.map(item => (
            <div
              key={getItemKey(item)}
              role="button"
              tabIndex={0}
              className="catalog-item cursor-pointer outline-none focus:ring-2 focus:ring-retro-orange"
              onClick={() => onSelect(item)}
              onKeyDown={(e) => handleKeyDown(e, item)}
              style={{
                border: "1px solid #eee",
                padding: "10px",
                marginBottom: "10px",
                borderRadius: "4px",
                cursor: "pointer",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = "#f5f5f5";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = "white";
              }}
            >
              {renderItem(item)}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
