package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.AreaDto;
import com.pauloneill.arcraidersplanner.model.*;
import com.pauloneill.arcraidersplanner.repository.GameMapRepository;
import com.pauloneill.arcraidersplanner.repository.ItemRepository;
import com.pauloneill.arcraidersplanner.repository.MapAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AreaService
 * WHY: Validates business logic for zone highlighting functionality
 */
@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private MapAreaRepository areaRepository;

    @Mock
    private GameMapRepository gameMapRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private DtoMapper dtoMapper;

    @InjectMocks
    private AreaService areaService;

    private GameMap testMap;
    private Item testItem;
    private LootType mechanicalLootType;
    private Area area1;
    private Area area2;

    @BeforeEach
    void setUp() {
        // Setup test map
        testMap = new GameMap();
        testMap.setId(1L);
        testMap.setName("Dam Battlegrounds");

        // Setup loot type
        mechanicalLootType = new LootType();
        mechanicalLootType.setId(1L);
        mechanicalLootType.setName("Mechanical");

        // Setup test item with loot type
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Steel Plate");
        testItem.setLootType(mechanicalLootType);

        // Setup test areas
        area1 = new Area();
        area1.setId(1L);
        area1.setName("Factory Floor");
        area1.setGameMap(testMap);
        area1.setMapX(100);
        area1.setMapY(200);

        area2 = new Area();
        area2.setId(2L);
        area2.setName("Assembly Line");
        area2.setGameMap(testMap);
        area2.setMapX(150);
        area2.setMapY(250);
    }

    @Test
    void shouldReturnAreasForValidMapAndItem() {
        // Given
        when(gameMapRepository.findByName("Dam Battlegrounds"))
                .thenReturn(Optional.of(testMap));
        when(itemRepository.findByName("Steel Plate"))
                .thenReturn(Optional.of(testItem));
        when(areaRepository.findByMapAndLootType(1L, "Mechanical"))
                .thenReturn(Arrays.asList(area1, area2));

        AreaDto dto1 = new AreaDto();
        dto1.setId(1L);
        dto1.setName("Factory Floor");

        AreaDto dto2 = new AreaDto();
        dto2.setId(2L);
        dto2.setName("Assembly Line");

        when(dtoMapper.toDto(area1)).thenReturn(dto1);
        when(dtoMapper.toDto(area2)).thenReturn(dto2);

        // When
        List<AreaDto> result = areaService.findAreasByMapAndItem("Dam Battlegrounds", "Steel Plate");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Factory Floor");
        assertThat(result.get(0).getMapName()).isEqualTo("Dam Battlegrounds");
        assertThat(result.get(1).getName()).isEqualTo("Assembly Line");

        verify(gameMapRepository).findByName("Dam Battlegrounds");
        verify(itemRepository).findByName("Steel Plate");
        verify(areaRepository).findByMapAndLootType(1L, "Mechanical");
    }

    @Test
    void shouldReturnEmptyListWhenItemHasNoLootType() {
        // Given - item without loot type (e.g., quest item)
        Item questItem = new Item();
        questItem.setId(2L);
        questItem.setName("Quest Item");
        questItem.setLootType(null); // No loot type

        when(gameMapRepository.findByName("Dam Battlegrounds"))
                .thenReturn(Optional.of(testMap));
        when(itemRepository.findByName("Quest Item"))
                .thenReturn(Optional.of(questItem));

        // When
        List<AreaDto> result = areaService.findAreasByMapAndItem("Dam Battlegrounds", "Quest Item");

        // Then
        assertThat(result).isEmpty();
        // Repository should NOT be called since item has no loot type
    }

    @Test
    void shouldThrow404WhenMapNotFound() {
        // Given
        when(gameMapRepository.findByName("Invalid Map"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                areaService.findAreasByMapAndItem("Invalid Map", "Steel Plate"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Map not found: Invalid Map")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldThrow404WhenItemNotFound() {
        // Given
        when(gameMapRepository.findByName("Dam Battlegrounds"))
                .thenReturn(Optional.of(testMap));
        when(itemRepository.findByName("Nonexistent Item"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                areaService.findAreasByMapAndItem("Dam Battlegrounds", "Nonexistent Item"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Item not found: Nonexistent Item")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleItemFoundInMultipleAreas() {
        // Given - common item found in many areas
        Area area3 = new Area();
        area3.setId(3L);
        area3.setName("Storage");
        area3.setGameMap(testMap);

        when(gameMapRepository.findByName("Dam Battlegrounds"))
                .thenReturn(Optional.of(testMap));
        when(itemRepository.findByName("Steel Plate"))
                .thenReturn(Optional.of(testItem));
        when(areaRepository.findByMapAndLootType(1L, "Mechanical"))
                .thenReturn(Arrays.asList(area1, area2, area3));

        when(dtoMapper.toDto(area1)).thenReturn(createDto(1L, "Factory Floor"));
        when(dtoMapper.toDto(area2)).thenReturn(createDto(2L, "Assembly Line"));
        when(dtoMapper.toDto(area3)).thenReturn(createDto(3L, "Storage"));

        // When
        List<AreaDto> result = areaService.findAreasByMapAndItem("Dam Battlegrounds", "Steel Plate");

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(AreaDto::getName)
                .containsExactly("Factory Floor", "Assembly Line", "Storage");
    }

    @Test
    void shouldSetMapNameOnReturnedDtos() {
        // Given
        when(gameMapRepository.findByName("Dam Battlegrounds"))
                .thenReturn(Optional.of(testMap));
        when(itemRepository.findByName("Steel Plate"))
                .thenReturn(Optional.of(testItem));
        when(areaRepository.findByMapAndLootType(1L, "Mechanical"))
                .thenReturn(Collections.singletonList(area1));

        AreaDto dto = new AreaDto();
        dto.setId(1L);
        dto.setName("Factory Floor");
        when(dtoMapper.toDto(area1)).thenReturn(dto);

        // When
        List<AreaDto> result = areaService.findAreasByMapAndItem("Dam Battlegrounds", "Steel Plate");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMapName()).isEqualTo("Dam Battlegrounds");
    }

    private AreaDto createDto(Long id, String name) {
        AreaDto dto = new AreaDto();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
