package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.ContainerType;
import com.pauloneill.arcraidersplanner.model.MarkerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkerGroupRepository extends JpaRepository<MarkerGroup, Long> {
    List<MarkerGroup> findByGameMapId(Long mapId);
    List<MarkerGroup> findByGameMapIdAndContainerType(Long mapId, ContainerType containerType);
}
