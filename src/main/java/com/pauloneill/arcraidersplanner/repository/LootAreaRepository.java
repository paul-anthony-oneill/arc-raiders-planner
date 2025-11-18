package com.pauloneill.arcraidersplanner.repository;
import com.pauloneill.arcraidersplanner.model.LootArea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LootAreaRepository extends JpaRepository<LootArea, Long> {
    Optional<LootArea> findByName(String name);
}