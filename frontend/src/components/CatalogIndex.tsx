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

  // TODO: Add keyboard accessibility to item selection

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
        className="catalog-items grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
      >
        {displayedItems.length === 0 ? (
          <div className="col-span-full text-retro-sand-dim p-8 text-center">
            No items found
          </div>
        ) : (
          displayedItems.map(item => (
            <div key={getItemKey(item)}>
              {renderItem(item)}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
