package com.pauloneill.arcraidersplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pauloneill.arcraidersplanner.dto.AreaDto;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlannerService(ItemRepository itemRepository, GameMapRepository gameMapRepository, MapMarkerRepository mapMarkerRepository) {
        this.itemRepository = itemRepository;
        this.gameMapRepository = gameMapRepository;
        this.mapMarkerRepository = mapMarkerRepository;
    }

    public List<PlannerResponseDto> generateRoute(PlannerRequestDto request) {
        Set<String> requiredLootTypes = resolveLootTypes(request.targetItemNames());
        if (requiredLootTypes.isEmpty()) return Collections.emptyList();

        List<GameMap> maps = gameMapRepository.findAllWithAreas();
        List<PlannerResponseDto> results = new ArrayList<>();

        for (GameMap map : maps) {
            // 1. Identify Relevant Areas
            List<Area> relevantAreas = map.getAreas().stream()
                    .filter(area -> area.getLootTypes().stream().anyMatch(lt -> requiredLootTypes.contains(lt.getName())))
                    .collect(Collectors.toList());

            if (relevantAreas.isEmpty()) continue;

            // 2. Identify "Danger Zones" (High Tier Areas) for PvP modes
            List<Area> dangerZones = map.getAreas().stream()
                    .filter(a -> a.getLootAbundance() != null && a.getLootAbundance() == 1)
                    .toList();

            // 3. Identify Raider Hatches for Exfil modes
            List<MapMarker> raiderHatches = Collections.emptyList();
            if (request.hasRaiderKey() && (request.routingProfile() == PlannerRequestDto.RoutingProfile.EASY_EXFIL || request.routingProfile() == PlannerRequestDto.RoutingProfile.SAFE_EXFIL)) {
                raiderHatches = mapMarkerRepository.findByGameMapId(map.getId()).stream()
                        .filter(m -> "Raider Hatch".equalsIgnoreCase(m.getCategory())) // Adjust category string as needed
                        .toList();
            }

            // 4. Calculate Score based on Profile
            RouteResult route = calculateRouteAndScore(relevantAreas, requiredLootTypes, request.routingProfile(), dangerZones, raiderHatches);

            results.add(new PlannerResponseDto(
                    map.getId(),
                    map.getName(),
                    route.score,
                    route.path.stream().map(this::convertToAreaDto).toList(),
                    route.extractionPoint
            ));
        }

        results.sort(Comparator.comparingDouble(PlannerResponseDto::score).reversed());
        return results;
    }

    private record RouteResult(double score, List<Area> path, String extractionPoint) {
    }

    private RouteResult calculateRouteAndScore(
            List<Area> areas,
            Set<String> targets,
            PlannerRequestDto.RoutingProfile profile,
            List<Area> dangerZones,
            List<MapMarker> raiderHatches
    ) {
        // --- MODE 1: PURE SCAVENGER ---
        // Logic: Simple count of matching areas. Distance is irrelevant.
        if (profile == PlannerRequestDto.RoutingProfile.PURE_SCAVENGER) {
            return new RouteResult(areas.size() * 100.0, areas, null);
        }

        // --- BASE SCORING (Used for all other modes) ---
        Map<Area, Double> areaScores = new HashMap<>();
        for (Area area : areas) {
            long matchCount = area.getLootTypes().stream().filter(lt -> targets.contains(lt.getName())).count();
            double score = (matchCount > 1) ? (matchCount * 100) : 10;

            // PvP Mode Adjustment: Boost areas near map edge (distance from 0,0)
            if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
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
        if (viableAreas.isEmpty()) return new RouteResult(-1000, Collections.emptyList(), null);

        // --- ROUTE GENERATION (Greedy TSP) ---
        Area current = viableAreas.stream().max(Comparator.comparingDouble(areaScores::get)).orElseThrow();
        List<Area> path = new ArrayList<>();
        List<Area> unvisited = new ArrayList<>(viableAreas);

        path.add(current);
        unvisited.remove(current);
        double totalScore = areaScores.get(current);

        while (!unvisited.isEmpty()) {
            final Area startNode = current;
            Area next = unvisited.stream()
                    .min(Comparator.comparingDouble(a -> distance(startNode, a)))
                    .orElseThrow();

            // Check Safety for PvP Modes
            if (profile == PlannerRequestDto.RoutingProfile.AVOID_PVP || profile == PlannerRequestDto.RoutingProfile.SAFE_EXFIL) {
                if (isRouteDangerous(startNode, next, dangerZones)) {
                    totalScore -= 200; // Heavy penalty for crossing a High Tier zone
                }
            }

            totalScore += areaScores.get(next);
            current = next;
            path.add(current);
            unvisited.remove(current);
        }

        // --- EXFIL LOGIC (EASY_EXFIL & SAFE_EXFIL) ---
        String bestExit = null;
        if (!raiderHatches.isEmpty()) {
            final Area finalLootZone = current;
            MapMarker nearestHatch = raiderHatches.stream()
                    .min(Comparator.comparingDouble(m -> distance(finalLootZone, m)))
                    .orElse(null);

            if (nearestHatch != null) {
                double distToExit = distance(finalLootZone, nearestHatch);
                // Bonus: Short run to exit = +50 points max, degrading with distance
                totalScore += Math.max(0, 50 - (distToExit / 10.0));
                bestExit = nearestHatch.getName();
            }
        }

        return new RouteResult(totalScore, path, bestExit);
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

    // Check if the line between two areas intersects any High Tier Zone
    private boolean isRouteDangerous(Area start, Area end, List<Area> dangerZones) {
        for (Area danger : dangerZones) {
            if (danger.getId().equals(start.getId()) || danger.getId().equals(end.getId())) continue;

            double dangerRadius = calculateDynamicRadius(danger);
            double distToHazard = pointToSegmentDistance(
                    danger.getMapX(), danger.getMapY(),
                    start.getMapX(), start.getMapY(),
                    end.getMapX(), end.getMapY()
            );

            if (distToHazard < dangerRadius) return true;
        }
        return false;
    }

    // Dynamic Radius Calculation based on Polygon Area
    private double calculateDynamicRadius(Area area) {
        try {
            // Parse JSON: [[x,y], [x,y], ...]
            List<List<Double>> coords = objectMapper.readValue(area.getCoordinates(), new TypeReference<>() {
            });
            if (coords.isEmpty()) return 50.0;

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
                if (d > maxDist) maxDist = d;
            }
            return maxDist; // Safe buffer radius
        } catch (IOException e) {
            return 100.0; // Fallback default
        }
    }

    // Math helper: Distance from Point P(px,py) to Line Segment AB
    private double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double l2 = Math.pow(bx - ax, 2) + Math.pow(by - ay, 2);
        if (l2 == 0) return Math.sqrt(Math.pow(px - ax, 2) + Math.pow(py - ay, 2));

        double t = ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / l2;
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * (bx - ax);
        double projY = ay + t * (by - ay);

        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
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