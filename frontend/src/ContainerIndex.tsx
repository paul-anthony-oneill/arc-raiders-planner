import React from 'react';
import { CatalogIndex } from './components/CatalogIndex';
import type { ContainerType } from './types';

interface ContainerIndexProps {
  onSelect: (container: ContainerType) => void;
  selectedContainers: string[]; // List of selected subcategories
}

export const ContainerIndex: React.FC<ContainerIndexProps> = ({ onSelect, selectedContainers }) => {
  return (
    <CatalogIndex<ContainerType>
      apiUrl="/api/containers"
      title="Target Containers"
      onSelect={onSelect}
      getItemKey={(container) => container.id}
      searchPlaceholder="Search containers (e.g., red locker, raider cache)..."
      renderItem={(container) => (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          width: '100%'
        }}>
          <div>
            <div className="font-medium">{container.name}</div>
            {container.description && (
              <div className="text-sm text-gray-600" style={{ fontSize: '0.85em', color: '#666' }}>
                {container.description}
              </div>
            )}
          </div>
          {selectedContainers.includes(container.subcategory) && (
            <span style={{ color: 'green', fontWeight: 'bold' }}>✓ Selected</span>
          )}
        </div>
      )}
    />
  );
};
