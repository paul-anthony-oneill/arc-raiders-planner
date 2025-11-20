import React, { useState, useEffect } from 'react';
import { ImageOverlay, MapContainer, Marker, Polygon, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Area } from './types';

// Define the component's props
interface MapProps {
    mapName: string;
    areas: Area[];
}

// Custom icon setup (Leaflet requires this for React/Webpack)
const defaultIcon = L.icon({
    iconUrl: 'marker-icon.png', // Placeholder icon
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: 'marker-shadow.png'
});

const MapComponent: React.FC<MapProps> = ({ mapName, areas }) => {
    const bounds: L.LatLngBoundsLiteral = [[-1000, -1000], [1000, 1000]];

    // Function to convert our simple mapX/mapY coordinates to Leaflet LatLng
    const coordsToLatLng = (x: number, y: number): L.LatLngTuple => {
        // Since we treat the map as a flat plane, we invert Y for common screen coordinates
        // and normalize the range.
        return [y, x] as L.LatLngTuple;
    };

    const getMapImageUrl = (name: string) => {
        const filename = name.toLowerCase().replace(/ /g, '_');
        return `/maps/${filename}.png`;
    };

    return (
        <div className="h-full w-full">
            <MapContainer
                center={[0, 0]}
                zoom={0}
                minZoom={-2}
                maxZoom={2}
                crs={L.CRS.Simple}
                bounds={bounds}
                style={{ height: '100%', width: '100%' }}
            >
                <ImageOverlay
                    url={getMapImageUrl(mapName)}
                    bounds={bounds}
                    attribution='&copy; Embark Studios'
                />

                {areas.map(area => {
                    // Logic to parse the coordinate string
                    let polygonPositions: L.LatLngExpression[] | null = null;

                    if (area.coordinates) {
                        try {
                            polygonPositions = JSON.parse(area.coordinates);
                        } catch (e) {
                            console.error(`Failed to parse coordinates for area ${area.name}`, e);
                        }
                    }

                    return (
                        <React.Fragment key={area.id}>
                            {/* 1. Draw the Polygon (Zone Shape) if coordinates exist */}
                            {polygonPositions && (
                                <Polygon
                                    positions={polygonPositions}
                                    pathOptions={{ color: 'red', fillColor: '#ff0000', fillOpacity: 0.2 }}
                                >
                                    {/* Popup for the Polygon click */}
                                    <Popup>
                                        <strong>{area.name}</strong><br />
                                        Types: {area.lootTypes.map(lt => lt.name).join(', ')}
                                    </Popup>
                                </Polygon>
                            )}

                            {/* 2. Draw the MapMarker (Icon) */}
                            <Marker
                                position={coordsToLatLng(area.mapX || 0, area.mapY || 0)}
                                icon={defaultIcon}
                            >
                                <Popup>
                                    <strong>{area.name}</strong><br />
                                    Types: {area.lootTypes.map(lt => lt.name).join(', ')}
                                </Popup>
                            </Marker>
                        </React.Fragment>
                    );
                })}
            </MapContainer>
        </div>
    );
};

export default MapComponent;