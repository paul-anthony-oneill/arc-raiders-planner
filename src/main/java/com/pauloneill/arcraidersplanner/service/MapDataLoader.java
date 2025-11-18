package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Area;
import com.pauloneill.arcraidersplanner.model.GameMap;
import com.pauloneill.arcraidersplanner.model.LootType;
import com.pauloneill.arcraidersplanner.repository.LootAreaRepository;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.MapAreaRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MapDataLoader implements CommandLineRunner {

    private final GameMapRepository mapRepository;
    private final LootAreaRepository lootAreaRepository;
    private final MapAreaRepository mapAreaRepository;

    public MapDataLoader(GameMapRepository mapRepository, LootAreaRepository lootAreaRepository, MapAreaRepository mapAreaRepository) {
        this.mapRepository = mapRepository;
        this.lootAreaRepository = lootAreaRepository;
        this.mapAreaRepository = mapAreaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (mapRepository.count() > 0 || mapAreaRepository.count() > 0) {
            System.out.println("Map data already loaded. Skipping static map setup.");
            return;
        }

        System.out.println("--- Loading Static Map and Area Data ---");

        LootType industrial = lootAreaRepository.findByName("Industrial").orElseGet(() -> createLootArea("Industrial"));
        LootType mechanical = lootAreaRepository.findByName("Mechanical").orElseGet(() -> createLootArea("Mechanical"));
        LootType arc = lootAreaRepository.findByName("ARC").orElseGet(() -> createLootArea("ARC"));
        LootType nature = lootAreaRepository.findByName("Nature").orElseGet(() -> createLootArea("Nature"));
        LootType technological = lootAreaRepository.findByName("Technological").orElseGet(() -> createLootArea("Technological"));
        LootType electrical = lootAreaRepository.findByName("Electrical").orElseGet(() -> createLootArea("Electrical"));
        LootType commercial = lootAreaRepository.findByName("Commercial").orElseGet(() -> createLootArea("Commercial"));
        LootType medical = lootAreaRepository.findByName("Medical").orElseGet(() -> createLootArea("Medical"));
        LootType security = lootAreaRepository.findByName("Security").orElseGet(() -> createLootArea("Security"));
        LootType residential = lootAreaRepository.findByName("Residential").orElseGet(() -> createLootArea("Residential"));

        GameMap damMap = createMap("Dam Battlegrounds", "dam");

        createArea(damMap, Set.of(industrial, mechanical), "Water Treatment Control", 300, 500); // Industrial AND Mechanical
        createArea(damMap, Set.of(commercial, technological), "Research and Administration", 150, 600); // Mechanical AND Residential
        createArea(damMap, Set.of(medical, commercial), "Testing Annex", 400, 200);

        System.out.println("Static map data loaded successfully.");
    }

    // Helper methods for clean code
    private GameMap createMap(String name, String code) {
        GameMap map = new GameMap();
        map.setName(name);
        map.setDescription(code);
        return mapRepository.save(map);
    }

    private LootType createLootArea(String name) {
        LootType area = new LootType();
        area.setName(name);
        return lootAreaRepository.save(area);
    }

    private Area createArea(GameMap map, Set<LootType> lootTypes, String areaName, Integer x, Integer y) {
        Area area = new Area();
        area.setName(areaName);
        area.setGameMap(map);
        area.setLootTypes(lootTypes); // <--- Set the collection
        area.setMapX(x);
        area.setMapY(y);
        return mapAreaRepository.save(area);
    }
}