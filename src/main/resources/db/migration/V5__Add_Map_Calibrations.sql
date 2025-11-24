-- Flyway V5: Add Calibration Data for New Maps
-- WHY: Preserve calibration data that was entered via the Map Editor tool

-- Update The Spaceport with calibration data
UPDATE maps
SET
    cal_scale_x = 0.4955574349128728,
    cal_scale_y = -0.4989422717879409,
    cal_offset_x = -1918.4219386999089,
    cal_offset_y = 1286.2287827952161
WHERE name = 'The Spaceport';

-- Update Buried City with calibration data
UPDATE maps
SET
    cal_scale_x = 0.3007774135844742,
    cal_scale_y = -0.3017302247068329,
    cal_offset_x = -2140.563194640021,
    cal_offset_y = 1452.7178042046698
WHERE name = 'Buried City';

-- Update Blue Gate with calibration data
UPDATE maps
SET
    cal_scale_x = 0.33383493575190487,
    cal_scale_y = -0.33412335657727105,
    cal_offset_x = -2335.0181009765657,
    cal_offset_y = 1660.6714975965065
WHERE name = 'Blue Gate';
