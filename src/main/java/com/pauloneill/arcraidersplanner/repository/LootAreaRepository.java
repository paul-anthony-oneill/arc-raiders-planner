package com.pauloneill.arcraidersplanner.repository;
import com.pauloneill.arcraidersplanner.model.LootType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LootAreaRepository extends JpaRepository<LootType, Long> {
    Optional<LootType> findByName(String name);
}