package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// ...
public interface MapAreaRepository extends JpaRepository<Area, Long> {
    Optional<Area> findByName(String name); // <--- ADD THIS
}