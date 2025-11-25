package com.pauloneill.arcraidersplanner.service;

import com.pauloneill.arcraidersplanner.model.Quest;
import com.pauloneill.arcraidersplanner.repository.QuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestService {
    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    public List<Quest> findAll() {
        return questRepository.findAll();
    }
}
