package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.dto.EnemySpawnDto;
import com.pauloneill.arcraidersplanner.dto.PlannerRequestDto;
import com.pauloneill.arcraidersplanner.dto.PlannerResponseDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
        Set<String> requiredLootTypes = resolveLootTypes(request.targetItemNames());

        // Get all spawns of target enemy types if specified
        List<MapMarker> allEnemySpawns = Collections.emptyList();
        if (request.targetEnemyTypes() != null && !request.targetEnemyTypes().isEmpty()) {
            allEnemySpawns = enemyService.getSpawnsByTypes(request.targetEnemyTypes());
        }

        // Require either items OR enemies to be specified
        if (requiredLootTypes.isEmpty() && allEnemySpawns.isEmpty()) {
            return Collections.emptyList();
        }

        List<GameMap> maps = gameMapRepository.findAllWithAreas();
        List<PlannerResponseDto> results = new ArrayList<>();

        for (GameMap map : maps) {
            // 1. Identify Relevant Areas
            List<Area> relevantAreas = map.getAreas().stream()
                    .filter(area -> area.getLootTypes().stream()
                            .anyMatch(lt -> requiredLootTypes.contains(lt.getName())))
                    .collect(Collectors.toList());

            // 2. Filter enemy spawns for this map
            List<MapMarker> enemySpawnsOnMap = allEnemySpawns.stream()
                    .filter(e -> e.getGameMap().getId().equals(map.getId()))
                    .toList();

            // Skip map if it has no relevant areas AND no target enemy spawns
            if (relevantAreas.isEmpty() && enemySpawnsOnMap.isEmpty())
                continue;

            // 3. Identify "Danger Zones" (High Tier Areas) for PvP modes
            List<Area> dangerZones = map.getAreas().stream()
                    .filter(a -> a.getLootAbundance() != null && a.getLootAbundance() == 1)
                    .toList();

            // 4. Identify Extraction Points
            // Premium extraction: Raider Hatches (requires key + specific profiles)
            // Generic extraction: Standard extraction markers (always available)
            List<MapMarker> extractionMarkers;
            if (request.hasRaiderKey() && (request.routingProfile() == PlannerRequestDto.RoutingProfile.EASY_EXFIL
                    || request.routingProfile() == PlannerRequestDto.RoutingProfile.SAFE_EXFIL)) {
                // Premium mode: Use Raider Hatches
                extractionMarkers = mapMarkerRepository.findByGameMapId(map.getId()).stream()
                        .filter(m -> "hatch".equalsIgnoreCase(m.getSubcategory()))
                        .toList();
            } else {
                // Standard mode: Use generic extraction markers
                extractionMarkers = mapMarkerRepository.findByGameMapId(map.getId()).stream()
                        .filter(m -> "extraction".equalsIgnoreCase(m.getSubcategory()))
                        .toList();
            }

            // 5. Calculate Score based on Profile (including enemy proximity)
            RouteResult route = calculateRouteAndScore(relevantAreas, requiredLootTypes, request.routingProfile(),
                    dangerZones, extractionMarkers, enemySpawnsOnMap);

            results.add(new PlannerResponseDto(
                    map.getId(),
                    map.getName(),
                    route.score,
                    route.path.stream().map(this::convertToAreaDto).toList(),
                    route.extractionPoint,
                    route.enemySpawns // Already converted to EnemySpawnDto in RouteResult
            ));
        }

        results.sort(Comparator.comparingDouble(PlannerResponseDto::score).reversed());
        return results;
    }

    private record RouteResult(double score, List<Area> path, String extractionPoint, List<EnemySpawnDto> enemySpawns) {
    }

    private RouteResult calculateRouteAndScore(
            List<Area> areas,
            Set<String> targets,
            PlannerRequestDto.RoutingProfile profile,
            List<Area> dangerZones,
            List<MapMarker> extractionMarkers,
            List<MapMarker> targetEnemies) {
        // --- MODE 1: PURE SCAVENGER ---
        // Logic: Simple count of matching areas. Distance is irrelevant.
        if (profile == PlannerRequestDto.RoutingProfile.PURE_SCAVENGER) {
            List<EnemySpawnDto> enemySpawnDtos = convertToEnemySpawnDtos(targetEnemies, areas);
            return new RouteResult(areas.size() * 100.0, areas, null, enemySpawnDtos);
        }

        // --- BASE SCORING (Used for all other modes) ---
        Map<Area, Double> areaScores = new HashMap<>();
        for (Area area : areas) {
            long matchCount = area.getLootTypes().stream().filter(lt -> targets.contains(lt.getName())).count();
            double score = (matchCount > 1) ? (matchCount * 100) : 10;

            // PvP Mode Adjustment: Boost areas near map edge (distance from 0,0)
            if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP
                    || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                double distFromCenter = Math.sqrt(Math.pow(area.getMapX(), 2) + Math.pow(area.getMapY(), 2));
                // Add 1 point for every 100 units away from center
                score += (distFromCenter / 100.0);

                // Penalize High Tier zones heavily in these modes
                if (area.getLootAbundance() != null && area.getLootAbundance() == 1) {
                    score -= 500;
                }
            }
            areaScores.put(area, score);
        }

        // Filter out areas with negative scores (too dangerous)
        List<Area> viableAreas = areas.stream().filter(a -> areaScores.get(a) > 0).toList();
        if (viableAreas.isEmpty()) {
            List<EnemySpawnDto> emptySpawns = convertToEnemySpawnDtos(targetEnemies, Collections.emptyList());
            return new RouteResult(-1000, Collections.emptyList(), null, emptySpawns);
        }

        // --- ROUTE GENERATION (Multi-Start Nearest Neighbor + 2-Opt) ---
        List<Area> path = findOptimalRoute(viableAreas);

        // Calculate score for the optimized path
        double totalScore = 0;
        for (int i = 0; i < path.size(); i++) {
            Area current = path.get(i);
            totalScore += areaScores.get(current);

            // Check Safety for PvP Modes when transitioning to next area
            if (i < path.size() - 1) {
                Area next = path.get(i + 1);
                if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP
                        || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                    if (isRouteDangerous(current, next, dangerZones)) {
                        totalScore -= 200; // Heavy penalty for crossing a High Tier zone
                    }
                }
            }
        }

        Area current = path.get(path.size() - 1); // Last area for exfil calculation

        // --- EXTRACTION LOGIC ---
        // Calculate nearest extraction point and apply scoring bonus for proximity
        String bestExit = null;
        if (!extractionMarkers.isEmpty()) {
            final Area finalLootZone = current;
            MapMarker nearestExtraction = extractionMarkers.stream()
                    .min(Comparator.comparingDouble(m -> distance(finalLootZone, m)))
                    .orElse(null);

            if (nearestExtraction != null) {
                double distToExit = distance(finalLootZone, nearestExtraction);

                // Apply distance-based scoring bonus for extraction proximity
                // Premium modes (EASY_EXFIL, SAFE_EXFIL) get stronger bonuses
                if (profile == PlannerRequestDto.RoutingProfile.EASY_EXFIL
                        || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                    // Stronger bonus for extraction-focused modes: +50 points max
                    totalScore += Math.max(0, 50 - (distToExit / 10.0));
                } else {
                    // Modest bonus for other modes: +25 points max
                    totalScore += Math.max(0, 25 - (distToExit / 20.0));
                }

                // Use marker name if available, otherwise use generic label
                bestExit = (nearestExtraction.getName() != null && !nearestExtraction.getName().isBlank())
                        ? nearestExtraction.getName()
                        : "Extraction Point";
            }
        }

        // --- ENEMY PROXIMITY SCORING ---
        // Bonus points if route naturally passes near target enemy spawn points
        if (!targetEnemies.isEmpty()) {
            double enemyScore = scoreEnemyProximity(path, targetEnemies);
            totalScore += enemyScore;
        }

        // Convert enemy spawns to DTOs with proximity info
        List<EnemySpawnDto> enemySpawnDtos = convertToEnemySpawnDtos(targetEnemies, path);

        return new RouteResult(totalScore, path, bestExit, enemySpawnDtos);
    }

    // --- ROUTE OPTIMIZATION HELPERS ---

    /**
     * Finds the optimal route using multi-start nearest-neighbor followed by 2-opt
     * improvement.
     * Tries starting from each area and picks the route with minimum total
     * distance.
     */
    private List<Area> findOptimalRoute(List<Area> areas) {
        if (areas.size() <= 2)
            return new ArrayList<>(areas);

        List<Area> bestRoute = null;
        double bestDistance = Double.MAX_VALUE;

        // Try starting from each area
        for (Area startArea : areas) {
            List<Area> route = nearestNeighborRoute(startArea, areas);
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
     * area.
     */
    private List<Area> nearestNeighborRoute(Area start, List<Area> allAreas) {
        List<Area> route = new ArrayList<>();
        Set<Area> unvisited = new HashSet<>(allAreas);

        Area current = start;
        route.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            final Area from = current;
            Area nearest = unvisited.stream()
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
    private double calculateTotalDistance(List<Area> route) {
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
    private List<Area> twoOptImprove(List<Area> route) {
        if (route.size() < 3)
            return route;

        List<Area> improved = new ArrayList<>(route);
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
                        List<Area> newRoute = new ArrayList<>(improved.subList(0, i + 1));
                        List<Area> reversed = new ArrayList<>(improved.subList(i + 1, j + 1));
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

    private Set<String> resolveLootTypes(List<String> itemNames) {
        Set<String> types = new HashSet<>();
        for (String name : itemNames) {
            itemRepository.findByName(name)
                    .map(Item::getLootType).map(LootType::getName)
                    .ifPresent(types::add);
        }
        return types;
    }

    private double distance(Area a, Area b) {
        return Math.sqrt(Math.pow(a.getMapX() - b.getMapX(), 2) + Math.pow(a.getMapY() - b.getMapY(), 2));
    }

    private double distance(Area a, MapMarker m) {
        return Math.sqrt(Math.pow(a.getMapX() - m.getLng(), 2) + Math.pow(a.getMapY() - m.getLat(), 2));
    }

    /**
     * Calculates the minimum distance from an enemy marker to the route path.
     * WHY: Enemy proximity should be measured to the actual path segments, not just
     * area centroids
     *
     * @param enemy Enemy spawn marker
     * @param path  Route path (list of areas)
     * @return Minimum distance from enemy to any point on the route path
     */
    private double distanceToRoutePath(MapMarker enemy, List<Area> path) {
        if (path.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // If only one area, use point-to-point distance
        if (path.size() == 1) {
            return distance(path.get(0), enemy);
        }

        // Calculate minimum distance to all route segments
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.size() - 1; i++) {
            Area start = path.get(i);
            Area end = path.get(i + 1);

            double segmentDist = pointToSegmentDistance(
                    enemy.getLng(), enemy.getLat(), // Point (enemy position)
                    start.getMapX(), start.getMapY(), // Segment start
                    end.getMapX(), end.getMapY() // Segment end
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
                    danger.getMapX(), danger.getMapY(),
                    start.getMapX(), start.getMapY(),
                    end.getMapX(), end.getMapY());

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
                // Leaflet [lat, lng] -> API [y, x]. Our DB stores [lat(y), lng(x)] usually
                // But based on V4 SQL, the JSON is [[x,y]...]
                // We assume standard euclidean distance from centroid
                double px = point.get(1); // SQL format often varies, check your migration!
                // Based on V4: '[[462,-438]...]' -> Looks like [x, y]
                double py = point.get(0);

                // Actually, Leaflet is usually [Lat, Lng] -> [Y, X]
                // The helper uses centroid mapX/mapY. Let's trust the max dist from centroid.
                double d = Math.sqrt(Math.pow(area.getMapX() - px, 2) + Math.pow(area.getMapY() - py, 2));
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
     * @param path    Route waypoints (loot areas)
     * @param enemies Target enemy spawn markers
     * @return Proximity score bonus
     */
    private double scoreEnemyProximity(List<Area> path, List<MapMarker> enemies) {
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
     * @param path    The optimized loot route
     * @return List of EnemySpawnDto with onRoute status and distances
     */
    private List<EnemySpawnDto> convertToEnemySpawnDtos(List<MapMarker> enemies, List<Area> path) {
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

    private AreaDto convertToAreaDto(Area area) {
        AreaDto dto = new AreaDto();
        dto.setId(area.getId());
        dto.setName(area.getName());
        dto.setMapX(area.getMapX());
        dto.setMapY(area.getMapY());
        dto.setCoordinates(area.getCoordinates());
        dto.setLootAbundance(area.getLootAbundance());
        if (area.getLootTypes() != null) {
            dto.setLootTypes(area.getLootTypes().stream().map(LootType::getName).collect(Collectors.toSet()));
        }
        return dto;
    }

}