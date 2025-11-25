package com.pauloneill.arcraidersplanner.repository;

import com.pauloneill.arcraidersplanner.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestRepository extends JpaRepository<Quest, String> {
}
