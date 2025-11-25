package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.GameMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CoordinateCalibrationServiceTest {

    @Autowired
    private CoordinateCalibrationService service;

    @Test
    void testCalibrationWithScaleAndOffset() {
        GameMap map = new GameMap();
        map.setCalibrationScaleX(2.0);
        map.setCalibrationScaleY(1.5);
        map.setCalibrationOffsetX(100.0);
        map.setCalibrationOffsetY(50.0);

        double[] result = service.calibrateCoordinates(10.0, 20.0, map);

        // (10 * 1.5) + 50 = 65 (lat/Y)
        // (20 * 2.0) + 100 = 140 (lng/X)
        assertEquals(65.0, result[0], 0.01);   // calibratedLat
        assertEquals(140.0, result[1], 0.01);  // calibratedLng
    }

    @Test
    void testDefaultCalibration() {
        GameMap map = new GameMap();
        // No calibration parameters set (nulls)

        double[] result = service.calibrateCoordinates(100.0, 200.0, map);

        // Should use defaults (scale=1.0, offset=0.0)
        assertEquals(100.0, result[0], 0.01);
        assertEquals(200.0, result[1], 0.01);
    }

    @Test
    void testNegativeCoordinates() {
        GameMap map = new GameMap();
        map.setCalibrationScaleX(1.0);
        map.setCalibrationScaleY(1.0);
        map.setCalibrationOffsetX(0.0);
        map.setCalibrationOffsetY(0.0);

        double[] result = service.calibrateCoordinates(-50.0, -100.0, map);

        assertEquals(-50.0, result[0], 0.01);
        assertEquals(-100.0, result[1], 0.01);
    }
}
