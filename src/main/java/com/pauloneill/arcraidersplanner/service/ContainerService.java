package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.dto.ContainerTypeDto;
import com.pauloneill.arcraidersplanner.dto.MarkerGroupDto;
import com.pauloneill.arcraidersplanner.model.ContainerType;
import com.pauloneill.arcraidersplanner.model.MapMarker;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import com.pauloneill.arcraidersplanner.repository.ContainerTypeRepository;
import com.pauloneill.arcraidersplanner.repository.MapMarkerRepository;
import com.pauloneill.arcraidersplanner.repository.MarkerGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContainerService {

    private final ContainerTypeRepository containerTypeRepository;
    private final MarkerGroupRepository markerGroupRepository;
    private final MapMarkerRepository mapMarkerRepository;
    private final DtoMapper dtoMapper;

    public ContainerService(ContainerTypeRepository containerTypeRepository,
                            MarkerGroupRepository markerGroupRepository,
                            MapMarkerRepository mapMarkerRepository,
                            DtoMapper dtoMapper) {
        this.containerTypeRepository = containerTypeRepository;
        this.markerGroupRepository = markerGroupRepository;
        this.mapMarkerRepository = mapMarkerRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public List<ContainerTypeDto> getAllContainerTypes() {
        return containerTypeRepository.findAll().stream()
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContainerTypeDto> searchContainerTypes(String query) {
        // Simple case-insensitive contains search on name
        // Since we don't have a custom query method yet, we can filter in memory for now
        // or add a repository method. Given the small number of types, in-memory is fine.
        if (query == null || query.isBlank()) {
            return getAllContainerTypes();
        }
        String lowerQuery = query.toLowerCase();
        return containerTypeRepository.findAll().stream()
                .filter(ct -> ct.getName().toLowerCase().contains(lowerQuery) || 
                              ct.getSubcategory().toLowerCase().contains(lowerQuery))
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarkerGroupDto> getMarkerGroups(String mapId, String containerTypeSubcategory) {
        Long mapIdLong;
        try {
            mapIdLong = Long.parseLong(mapId);
        } catch (NumberFormatException e) {
            // Handle or log error, return empty list for now
            return List.of();
        }

        List<MarkerGroup> groups;
        if (containerTypeSubcategory != null && !containerTypeSubcategory.isBlank()) {
            var containerTypeOpt = containerTypeRepository.findBySubcategory(containerTypeSubcategory);
            if (containerTypeOpt.isPresent()) {
                groups = markerGroupRepository.findByGameMapIdAndContainerType(mapIdLong, containerTypeOpt.get());
            } else {
                return List.of();
            }
        } else {
            groups = markerGroupRepository.findByGameMapId(mapIdLong);
        }

        return groups.stream()
                .map(dtoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MapMarker> getGroupMarkers(Long groupId) {
        return markerGroupRepository.findById(groupId)
                .map(MarkerGroup::getMarkers)
                .orElse(List.of());
    }
}
