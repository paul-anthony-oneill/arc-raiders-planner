package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import org.springframework.stereotype.Service;

/**
 * Transforms raw Metaforge API coordinates to Leaflet CRS.Simple coordinates.
 *
 * WHY: All coordinates stored in database should be Leaflet-ready.
 * Raw coordinates available from Metaforge API if needed for debugging.
 *
 * COORDINATE SYSTEMS:
 * - Metaforge API: Returns [lng, lat] format (X, Y)
 * - Leaflet: Expects [lat, lng] format (Y, X)
 * - This service applies GameMap calibration parameters to transform coordinates
 */
@Service
public class CoordinateCalibrationService {

    /**
     * Calibrates raw API coordinates to Leaflet [lat, lng] format.
     *
     * @param rawLat Raw Y from Metaforge API
     * @param rawLng Raw X from Metaforge API
     * @param gameMap Map with calibration parameters
     * @return Array [calibratedLat, calibratedLng] ready for Leaflet
     */
    public double[] calibrateCoordinates(double rawLat, double rawLng, GameMap gameMap) {
        double scaleX = gameMap.getCalibrationScaleX() != null ? gameMap.getCalibrationScaleX() : 1.0;
        double scaleY = gameMap.getCalibrationScaleY() != null ? gameMap.getCalibrationScaleY() : 1.0;
        double offsetX = gameMap.getCalibrationOffsetX() != null ? gameMap.getCalibrationOffsetX() : 0.0;
        double offsetY = gameMap.getCalibrationOffsetY() != null ? gameMap.getCalibrationOffsetY() : 0.0;

        double calibratedLng = (rawLng * scaleX) + offsetX;
        double calibratedLat = (rawLat * scaleY) + offsetY;

        return new double[]{calibratedLat, calibratedLng};
    }
}
