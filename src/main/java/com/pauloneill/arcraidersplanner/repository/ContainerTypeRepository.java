package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.ContainerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContainerTypeRepository extends JpaRepository<ContainerType, Long> {
    Optional<ContainerType> findBySubcategory(String subcategory);
    Optional<ContainerType> findByName(String name);
}
