package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.dto.EnemySpawnDto;
import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.dto.WaypointDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlannerService {

    private final ItemRepository itemRepository;
    private final GameMapRepository gameMapRepository;
    private final MapMarkerRepository mapMarkerRepository;
    private final EnemyService enemyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlannerService(ItemRepository itemRepository, GameMapRepository gameMapRepository,
            MapMarkerRepository mapMarkerRepository, EnemyService enemyService) {
        this.itemRepository = itemRepository;
        this.gameMapRepository = gameMapRepository;
        this.mapMarkerRepository = mapMarkerRepository;
        this.enemyService = enemyService;
    }

    public List<PlannerResponseDto> generateRoute(PlannerRequestDto request) {
        // Step 1: Resolve target item information (loot types and dropped-by enemies)
        TargetItemInfo targetItemInfo = resolveTargetItemInformation(request.targetItemNames());

        Set<String> requiredLootTypes = targetItemInfo.targetLootTypes();
        Set<String> targetDroppedByEnemies = targetItemInfo.targetDroppedByEnemies();
        Map<String, List<String>> lootTypeToItemNames = targetItemInfo.lootTypeToItemNames();
        Map<String, List<String>> enemyTypeToItemNames = targetItemInfo.enemyTypeToItemNames();

        // Step 2: Combine explicitly requested enemy types with those derived from item drops
        Set<String> allTargetEnemyTypes = new HashSet<>();
        if (request.targetEnemyTypes() != null) {
            allTargetEnemyTypes.addAll(request.targetEnemyTypes());
        }
        allTargetEnemyTypes.addAll(targetDroppedByEnemies);

        // Step 3: Get all spawns of target enemy types
        List<MapMarker> allEnemySpawns = Collections.emptyList();
        if (!allTargetEnemyTypes.isEmpty()) {
            allEnemySpawns = enemyService.getSpawnsByTypes(new ArrayList<>(allTargetEnemyTypes));
        }

        // Require either items OR enemies to be specified
        if (requiredLootTypes.isEmpty() && allTargetEnemyTypes.isEmpty()) {
            log.warn("No loot types or enemy types specified for route generation.");
            return Collections.emptyList();
        }

        List<GameMap> maps = gameMapRepository.findAllWithAreas();
        List<PlannerResponseDto> results = new ArrayList<>();

        for (GameMap map : maps) {
            // Filter enemy spawns for this map
            List<MapMarker> enemySpawnsOnMap = allEnemySpawns.stream()
                    .filter(e -> e.getGameMap().getId().equals(map.getId()))
                    .toList();

            // 1. Identify Relevant Areas (from requested loot types)
            List<Area> relevantLootAreas = map.getAreas().stream()
                    .filter(area -> area.getLootTypes().stream()
                            .anyMatch(lt -> requiredLootTypes.contains(lt.getName())))
                    .toList();

            // 2. Identify Exclusive Enemy Drop Markers (items ONLY dropped by enemy, no loot area)
            List<MapMarker> exclusiveEnemyMarkers = enemySpawnsOnMap.stream()
                    .filter(marker -> targetItemInfo.exclusiveDroppedByEnemies().contains(marker.getSubcategory()))
                    .toList();

            // Combine relevant areas and exclusive enemy markers into the list of viable points for routing
            List<RoutablePoint> viablePoints = new ArrayList<>();
            viablePoints.addAll(relevantLootAreas);
            viablePoints.addAll(exclusiveEnemyMarkers);

            // Skip map if it has no viable points
            if (viablePoints.isEmpty()) {
                log.debug("Map {} has no relevant areas or exclusive enemy markers for target items.", map.getName());
                continue;
            }

            // 3. Identify "Danger Zones" (High Tier Areas) for PvP modes
            List<Area> dangerZones = map.getAreas().stream()
                    .filter(a -> a.getLootAbundance() != null && a.getLootAbundance() == 1)
                    .toList();

            // 4. Identify Extraction Points
            List<MapMarker> extractionMarkers;
            if (request.hasRaiderKey() && (request.routingProfile() == PlannerRequestDto.RoutingProfile.EASY_EXFIL
                    || request.routingProfile() == PlannerRequestDto.RoutingProfile.SAFE_EXFIL)) {
                extractionMarkers = mapMarkerRepository.findByGameMapId(map.getId()).stream()
                        .filter(m -> "hatch".equalsIgnoreCase(m.getSubcategory()))
                        .toList();
                log.debug("Using Raider Hatches: found {} hatches for map {}", extractionMarkers.size(), map.getName());
            } else {
                extractionMarkers = mapMarkerRepository.findByGameMapId(map.getId()).stream()
                        .filter(m -> "extraction".equalsIgnoreCase(m.getSubcategory()))
                        .toList();
                log.debug("Using extraction markers: found {} extraction points for map {}", extractionMarkers.size(),
                        map.getName());
            }

            // 5. Calculate Score based on Profile (including enemy proximity)
            RouteResult route = calculateRouteAndScore(
                    viablePoints, // Use combined list of routable points
                    relevantLootAreas, // For score calculation for areas
                    requiredLootTypes,
                    request.routingProfile(),
                    dangerZones,
                    extractionMarkers,
                    enemySpawnsOnMap, // All enemies for proximity scoring
                    map);

            // Resolve Ongoing Items Map: LootType Name -> List of Item Names
            Map<String, List<String>> ongoingLootMap = resolveOngoingItems(request.ongoingItemNames());

            PlannerResponseDto response = new PlannerResponseDto(
                    map.getId(),
                    map.getName(),
                    route.score(),
                    route.path().stream().map(point -> convertToWaypointDto(point, ongoingLootMap, lootTypeToItemNames, enemyTypeToItemNames)).toList(),
                    route.extractionPoint(),
                    route.extractionLat(),
                    route.extractionLng(),
                    route.enemySpawns()
            );
            log.debug("Route for {}: extraction={}, coords=[{}, {}]", map.getName(), route.extractionPoint(),
                    route.extractionLat(), route.extractionLng());
            results.add(response);
        }

        results.sort(Comparator.comparingDouble(PlannerResponseDto::score).reversed());
        return results;
    }

    private record TargetItemInfo(
            Set<String> targetLootTypes,
            Set<String> targetDroppedByEnemies,
            Set<String> exclusiveDroppedByEnemies, // Enemies that ONLY drop item, no loot area
            Map<String, List<String>> lootTypeToItemNames,
            Map<String, List<String>> enemyTypeToItemNames
    ) {}

    private TargetItemInfo resolveTargetItemInformation(List<String> itemNames) {
        Set<String> targetLootTypes = new HashSet<>();
        Set<String> targetDroppedByEnemies = new HashSet<>();
        Set<String> exclusiveDroppedByEnemies = new HashSet<>();
        Map<String, List<String>> lootTypeToItemNames = new HashMap<>();
        Map<String, List<String>> enemyTypeToItemNames = new HashMap<>();

        if (itemNames != null && !itemNames.isEmpty()) {
            for (String name : itemNames) {
                itemRepository.findByName(name)
                        .ifPresent(item -> {
                            boolean hasLootType = item.getLootType() != null;
                            boolean hasDroppedBy = item.getDroppedBy() != null && !item.getDroppedBy().isEmpty();

                            if (hasLootType) {
                                targetLootTypes.add(item.getLootType().getName());
                                lootTypeToItemNames.computeIfAbsent(item.getLootType().getName(), k -> new ArrayList<>()).add(name);
                            }
                            if (hasDroppedBy) {
                                targetDroppedByEnemies.addAll(item.getDroppedBy());
                                item.getDroppedBy().forEach(enemyType ->
                                        enemyTypeToItemNames.computeIfAbsent(enemyType, k -> new ArrayList<>()).add(name));

                                if (!hasLootType) { // If item ONLY drops from enemy
                                    exclusiveDroppedByEnemies.addAll(item.getDroppedBy());
                                }
                            }
                        });
            }
        }
        return new TargetItemInfo(targetLootTypes, targetDroppedByEnemies, exclusiveDroppedByEnemies, lootTypeToItemNames, enemyTypeToItemNames);
    }

    private record RouteResult(double score, List<? extends RoutablePoint> path, String extractionPoint, Double extractionLat,
            Double extractionLng, List<EnemySpawnDto> enemySpawns) {
    }

    private RouteResult calculateRouteAndScore(
            List<? extends RoutablePoint> viablePoints, // The actual points to route between (Areas and Exclusive Enemy Markers)
            List<Area> relevantLootAreas, // Only for score calculation for areas
            Set<String> targets,
            PlannerRequestDto.RoutingProfile profile,
            List<Area> dangerZones,
            List<MapMarker> extractionMarkers,
            List<MapMarker> allTargetEnemiesOnMap, // All enemies for proximity scoring
            GameMap map) {

        // --- MODE 1: PURE SCAVENGER ---
        // Logic: Simple count of matching areas. Distance is irrelevant.
        if (profile == PlannerRequestDto.RoutingProfile.PURE_SCAVENGER) {
            List<EnemySpawnDto> enemySpawnDtos = convertToEnemySpawnDtos(allTargetEnemiesOnMap, viablePoints);
            // In PURE_SCAVENGER, we return all viablePoints (Area or Marker)
            return new RouteResult(viablePoints.size() * 100.0, viablePoints, null, null, null, enemySpawnDtos);
        }

        // --- BASE SCORING (Used for all other modes) ---
        Map<RoutablePoint, Double> pointScores = new HashMap<>();
        for (RoutablePoint point : viablePoints) {
            double score = 0.0;
            if (point instanceof Area area) {
                long matchCount = area.getLootTypes().stream().filter(lt -> targets.contains(lt.getName())).count();
                score = (matchCount > 1) ? (matchCount * 100) : 10;

                // PvP Mode Adjustment: Boost areas near map edge (distance from 0,0)
                if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP
                        || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                    double distFromCenter = Math.sqrt(Math.pow(area.getX(), 2) + Math.pow(area.getY(), 2));
                    // Add 1 point for every 100 units away from center
                    score += (distFromCenter / 100.0);

                    // Penalize High Tier zones heavily in these modes
                    if (area.getLootAbundance() != null && area.getLootAbundance() == 1) {
                        score -= 500;
                    }
                }
            } else if (point instanceof MapMarker marker) {
                // Scoring for exclusive enemy markers: give a base score for visiting
                score = 75.0; // Base score for reaching an enemy target
            }
            pointScores.put(point, score);
        }

        // Filter out points with negative scores (too dangerous)
        List<RoutablePoint> routablePointsForTSP = viablePoints.stream().filter(p -> pointScores.get(p) > 0).collect(Collectors.toList());

        if (routablePointsForTSP.isEmpty()) {
            log.debug("No viable points found - returning fallback extraction point if available");
            List<EnemySpawnDto> emptySpawns = convertToEnemySpawnDtos(allTargetEnemiesOnMap, Collections.emptyList());

            // Still calculate extraction point even with no route
            String bestExit = null;
            Double extractionLat = null;
            Double extractionLng = null;
            if (!extractionMarkers.isEmpty()) {
                // Use ANY area from the map as reference
                Set<Area> allMapAreas = map.getAreas();
                if (!allMapAreas.isEmpty()) {
                    Area referenceArea = allMapAreas.iterator().next(); // Use an actual Area as reference
                    MapMarker nearestExtraction = extractionMarkers.stream()
                            .min(Comparator.comparingDouble(m -> distance(referenceArea, m)))
                            .orElse(null);
                    if (nearestExtraction != null) {
                        bestExit = (nearestExtraction.getName() != null && !nearestExtraction.getName().isBlank())
                                ? nearestExtraction.getName()
                                : "Extraction Point";
                        extractionLat = nearestExtraction.getLat();
                        extractionLng = nearestExtraction.getLng();
                        log.debug("Fallback extraction: {} at [{}, {}]", bestExit, extractionLat, extractionLng);
                    }
                } else {
                    log.warn("Map {} has no areas to use as reference for extraction calculation", map.getName());
                }
            }
            return new RouteResult(-1000, Collections.emptyList(), bestExit, extractionLat, extractionLng, emptySpawns);
        }

        // --- ROUTE GENERATION (Multi-Start Nearest Neighbor + 2-Opt) ---
        List<? extends RoutablePoint> path = findOptimalRoute(routablePointsForTSP);
        log.debug("Generated route with {} points", path.size());

        // Calculate score for the optimized path
        double totalScore = 0;
        for (int i = 0; i < path.size(); i++) {
            RoutablePoint current = path.get(i);
            totalScore += pointScores.getOrDefault(current, 0.0);

            // Check Safety for PvP Modes when transitioning to next point
            if (i < path.size() - 1) {
                RoutablePoint next = path.get(i + 1);
                // Only check danger zones if both points are Areas
                if (current instanceof Area areaCurrent && next instanceof Area areaNext) {
                    if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP
                            || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                        if (isRouteDangerous(areaCurrent, areaNext, dangerZones)) {
                            totalScore -= 200; // Heavy penalty for crossing a High Tier zone
                        }
                    }
                }
            }
        }

        // --- EXTRACTION LOGIC ---
        String bestExit = null;
        Double extractionLat = null;
        Double extractionLng = null;

        if (path.isEmpty()) {
            log.warn("Cannot calculate extraction point: route path is empty");
        } else if (extractionMarkers.isEmpty()) {
            log.warn("Cannot calculate extraction point: no extraction markers available");
        } else {
            RoutablePoint finalRoutablePoint = path.get(path.size() - 1); // Last point for exfil calculation
            MapMarker nearestExtraction = extractionMarkers.stream()
                    .min(Comparator.comparingDouble(m -> distance(finalRoutablePoint, m)))
                    .orElse(null);

            if (nearestExtraction != null) {
                double distToExit = distance(finalRoutablePoint, nearestExtraction);

                // Apply distance-based scoring bonus for extraction proximity
                if (profile == PlannerRequestDto.RoutingProfile.EASY_EXFIL
                        || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                    totalScore += Math.max(0, 50 - (distToExit / 10.0));
                } else {
                    totalScore += Math.max(0, 25 - (distToExit / 20.0));
                }

                bestExit = (nearestExtraction.getName() != null && !nearestExtraction.getName().isBlank())
                        ? nearestExtraction.getName()
                        : "Extraction Point";
                extractionLat = nearestExtraction.getLat();
                extractionLng = nearestExtraction.getLng();
                log.debug("Selected extraction: {} at [{}, {}], distance: {}", bestExit, extractionLat, extractionLng,
                        distToExit);
            }
        }

        // --- ENEMY PROXIMITY SCORING ---
        // Bonus points if route naturally passes near target enemy spawn points
        if (!allTargetEnemiesOnMap.isEmpty()) {
            double enemyScore = scoreEnemyProximity(path, allTargetEnemiesOnMap);
            totalScore += enemyScore;
        }

        // Convert enemy spawns to DTOs with proximity info
        List<EnemySpawnDto> enemySpawnDtos = convertToEnemySpawnDtos(allTargetEnemiesOnMap, path);

        return new RouteResult(totalScore, path, bestExit, extractionLat, extractionLng, enemySpawnDtos);
    }

    // --- ROUTE OPTIMIZATION HELPERS ---

    /**
     * Finds the optimal route using multi-start nearest-neighbor followed by 2-opt
     * improvement.
     * Tries starting from each point and picks the route with minimum total
     * distance.
     */
    private List<? extends RoutablePoint> findOptimalRoute(List<? extends RoutablePoint> points) {
        if (points.size() <= 2)
            return new ArrayList<>(points);

        List<? extends RoutablePoint> bestRoute = null;
        double bestDistance = Double.MAX_VALUE;

        // Try starting from each point
        for (RoutablePoint startPoint : points) {
            List<? extends RoutablePoint> route = nearestNeighborRoute(startPoint, points);
            double totalDistance = calculateTotalDistance(route);

            if (totalDistance < bestDistance) {
                bestDistance = totalDistance;
                bestRoute = route;
            }
        }

        // Apply 2-opt optimization to eliminate crossings
        return twoOptImprove(bestRoute);
    }

    /**
     * Constructs a route using nearest-neighbor heuristic starting from a specific
     * point.
     */
    private List<? extends RoutablePoint> nearestNeighborRoute(RoutablePoint start, List<? extends RoutablePoint> allPoints) {
        List<RoutablePoint> route = new ArrayList<>();
        Set<RoutablePoint> unvisited = new HashSet<>(allPoints);

        RoutablePoint current = start;
        route.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            final RoutablePoint from = current;
            RoutablePoint nearest = unvisited.stream()
                    .min(Comparator.comparingDouble(a -> distance(from, a)))
                    .orElseThrow();

            route.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        return route;
    }

    /**
     * Calculates total Euclidean distance for a route.
     */
    private double calculateTotalDistance(List<? extends RoutablePoint> route) {
        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            total += distance(route.get(i), route.get(i + 1));
        }
        return total;
    }

    /**
     * Improves route using 2-opt algorithm to eliminate edge crossings.
     * Iteratively swaps edge pairs if it reduces total distance.
     * WHY: Ensures routes don't zigzag unnecessarily (e.g., A→B→C when A→C→B is
     * shorter)
     */
    private List<? extends RoutablePoint> twoOptImprove(List<? extends RoutablePoint> route) {
        if (route.size() < 3)
            return route;

        List<RoutablePoint> improved = new ArrayList<>(route);
        boolean foundImprovement = true;

        while (foundImprovement) {
            foundImprovement = false;

            for (int i = 0; i < improved.size() - 2; i++) {
                for (int j = i + 2; j < improved.size(); j++) {
                    // Calculate current distance: i→(i+1) and j→(j+1)
                    double currentDist = distance(improved.get(i), improved.get(i + 1));
                    if (j < improved.size() - 1) {
                        currentDist += distance(improved.get(j), improved.get(j + 1));
                    }

                    // Calculate swapped distance: i→j and (i+1)→(j+1)
                    double swappedDist = distance(improved.get(i), improved.get(j));
                    if (j < improved.size() - 1) {
                        swappedDist += distance(improved.get(i + 1), improved.get(j + 1));
                    }

                    // If swap reduces distance, reverse the segment
                    if (swappedDist < currentDist) {
                        // Reverse segment from (i+1) to j
                        List<RoutablePoint> newRoute = new ArrayList<>(improved.subList(0, i + 1));
                        List<RoutablePoint> reversed = new ArrayList<>(improved.subList(i + 1, j + 1));
                        Collections.reverse(reversed);
                        newRoute.addAll(reversed);
                        if (j + 1 < improved.size()) {
                            newRoute.addAll(improved.subList(j + 1, improved.size()));
                        }

                        improved = newRoute;
                        foundImprovement = true;
                        break;
                    }
                }
                if (foundImprovement)
                    break;
            }
        }

        return improved;
    }

    // --- HELPERS ---

    /**
     * Calculates the Euclidean distance between two RoutablePoints.
     */
    private double distance(RoutablePoint p1, RoutablePoint p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    /**
     * Calculates the minimum distance from an enemy marker to the route path.
     * WHY: Enemy proximity should be measured to the actual path segments, not just
     * area centroids
     *
     * @param enemy Enemy spawn marker
     * @param path  Route path (list of RoutablePoints)
     * @return Minimum distance from enemy to any point on the route path
     */
    private double distanceToRoutePath(MapMarker enemy, List<? extends RoutablePoint> path) {
        if (path.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // If only one point, use point-to-point distance
        if (path.size() == 1) {
            return distance(path.get(0), enemy);
        }

        // Calculate minimum distance to all route segments
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.size() - 1; i++) {
            RoutablePoint start = path.get(i);
            RoutablePoint end = path.get(i + 1);

            double segmentDist = pointToSegmentDistance(
                    enemy.getX(), enemy.getY(), // Point (enemy position)
                    start.getX(), start.getY(), // Segment start
                    end.getX(), end.getY() // Segment end
            );

            minDistance = Math.min(minDistance, segmentDist);
        }

        return minDistance;
    }

    // Check if the line between two areas intersects any High Tier Zone
    private boolean isRouteDangerous(Area start, Area end, List<Area> dangerZones) {
        for (Area danger : dangerZones) {
            if (danger.getId().equals(start.getId()) || danger.getId().equals(end.getId()))
                continue;

            double dangerRadius = calculateDynamicRadius(danger);
            double distToHazard = pointToSegmentDistance(
                    danger.getX(), danger.getY(),
                    start.getX(), start.getY(),
                    end.getX(), end.getY());

            if (distToHazard < dangerRadius)
                return true;
        }
        return false;
    }

    // Dynamic Radius Calculation based on Polygon Area
    private double calculateDynamicRadius(Area area) {
        try {
            // Parse JSON: [[x,y], [x,y], ...]
            List<List<Double>> coords = objectMapper.readValue(area.getCoordinates(), new TypeReference<>() {
            });
            if (coords.isEmpty())
                return 50.0;

            // Find max distance from center to any vertex
            double maxDist = 0;
            for (List<Double> point : coords) {
                // Based on V4: '[[462,-438]...]' -> Looks like [x, y]
                double px = point.get(0);
                double py = point.get(1);

                double d = Math.sqrt(Math.pow(area.getX() - px, 2) + Math.pow(area.getY() - py, 2));
                if (d > maxDist)
                    maxDist = d;
            }
            return maxDist; // Safe buffer radius
        } catch (IOException e) {
            return 100.0; // Fallback default
        }
    }

    // Math helper: Distance from Point P(px,py) to Line Segment AB
    private double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double l2 = Math.pow(bx - ax, 2) + Math.pow(by - ay, 2);
        if (l2 == 0)
            return Math.sqrt(Math.pow(px - ax, 2) + Math.pow(py - ay, 2));

        double t = ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / l2;
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * (bx - ax);
        double projY = ay + t * (by - ay);

        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }

    /**
     * Scores how well a route passes near target enemy spawn points.
     * WHY: Routes that naturally pass enemies are more efficient for combined
     * loot+hunt missions
     *
     * @param path    Route waypoints (RoutablePoints)
     * @param enemies Target enemy spawn markers
     * @return Proximity score bonus
     */
    private double scoreEnemyProximity(List<? extends RoutablePoint> path, List<MapMarker> enemies) {
        double score = 0;
        final double PROXIMITY_THRESHOLD = 400.0; // Match the threshold used for onRoute detection

        for (MapMarker enemy : enemies) {
            // Calculate minimum distance to route path segments
            double minDist = distanceToRoutePath(enemy, path);

            // Within proximity threshold = full points, drops off linearly
            if (minDist < PROXIMITY_THRESHOLD) {
                score += PROXIMITY_THRESHOLD - minDist;
            }
        }
        return score;
    }

    /**
     * Converts enemy spawn markers to DTOs with route proximity information.
     * WHY: Frontend needs to display all spawns with highlighting for those near
     * the route
     *
     * @param enemies All enemy spawns of selected types on this map
     * @param path    The optimized loot route (List of RoutablePoints)
     * @return List of EnemySpawnDto with onRoute status and distances
     */
    private List<EnemySpawnDto> convertToEnemySpawnDtos(List<MapMarker> enemies, List<? extends RoutablePoint> path) {
        if (enemies.isEmpty()) {
            return Collections.emptyList();
        }

        final double PROXIMITY_THRESHOLD = 400.0; // Units for considering a spawn "on route"

        return enemies.stream()
                .map(enemy -> {
                    Double distanceToRoute = null;
                    Boolean onRoute = false;

                    if (!path.isEmpty()) {
                        // Calculate minimum distance from this spawn to the route path segments
                        distanceToRoute = distanceToRoutePath(enemy, path);

                        onRoute = distanceToRoute < PROXIMITY_THRESHOLD;
                    }

                    return new EnemySpawnDto(
                            enemy.getId(),
                            enemy.getSubcategory(), // Enemy type (e.g., "sentinel")
                            enemy.getGameMap().getName(),
                            enemy.getLat(),
                            enemy.getLng(),
                            onRoute,
                            distanceToRoute);
                })
                .toList();
    }

    /**
     * Converts a RoutablePoint to a WaypointDto for API response.
     * WHY: Provides a unified DTO for both Area and MapMarker points in the route.
     */
    private WaypointDto convertToWaypointDto(
            RoutablePoint point,
            Map<String, List<String>> ongoingLootMap,
            Map<String, List<String>> targetLootTypeToItemNames,
            Map<String, List<String>> targetEnemyTypeToItemNames) {

        String type;
        Set<String> lootTypes = Collections.emptySet();
        Integer lootAbundance = null;
        List<String> ongoingMatchItems = Collections.emptyList();
        List<String> targetMatchItems = new ArrayList<>();

        if (point instanceof Area area) {
            type = "AREA";
            if (area.getLootTypes() != null) {
                lootTypes = area.getLootTypes().stream().map(LootType::getName).collect(Collectors.toSet());
            }
            lootAbundance = area.getLootAbundance();

            // Populate ongoingMatchItems for areas
            if (ongoingLootMap != null && !ongoingLootMap.isEmpty()) {
                List<String> matches = new ArrayList<>();
                for (String lootType : lootTypes) {
                    if (ongoingLootMap.containsKey(lootType)) {
                        matches.addAll(ongoingLootMap.get(lootType));
                    }
                }
                ongoingMatchItems = matches.stream().distinct().sorted().toList();
            }

            // Populate targetMatchItems for areas
            if (targetLootTypeToItemNames != null && !targetLootTypeToItemNames.isEmpty()) {
                for (String lootType : lootTypes) {
                    if (targetLootTypeToItemNames.containsKey(lootType)) {
                        targetMatchItems.addAll(targetLootTypeToItemNames.get(lootType));
                    }
                }
            }

        } else if (point instanceof MapMarker marker) {
            type = "MARKER";
            // Populate targetMatchItems for markers (enemies)
            if (targetEnemyTypeToItemNames != null && !targetEnemyTypeToItemNames.isEmpty()) {
                if (marker.getSubcategory() != null && targetEnemyTypeToItemNames.containsKey(marker.getSubcategory())) {
                    targetMatchItems.addAll(targetEnemyTypeToItemNames.get(marker.getSubcategory()));
                }
            }
        } else {
            type = "UNKNOWN"; // Should not happen with proper type checking
        }

        return new WaypointDto(
                point.getId(),
                point.getName(),
                point.getX(),
                point.getY(),
                type,
                lootTypes,
                lootAbundance,
                ongoingMatchItems,
                targetMatchItems.stream().distinct().sorted().toList()
        );
    }

    private Map<String, List<String>> resolveOngoingItems(List<String> itemNames) {
        if (itemNames == null || itemNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> map = new HashMap<>();
        for (String name : itemNames) {
            itemRepository.findByName(name)
                    .ifPresent(item -> {
                        if (item.getLootType() != null) {
                            map.computeIfAbsent(item.getLootType().getName(), k -> new ArrayList<>()).add(name);
                        }
                    });
        }
        return map;
    }

}