package com.pauloneill.arcraidersplanner.controller;

import com.pauloneill.arcraidersplanner.dto.QuestDto;
import com.pauloneill.arcraidersplanner.model.Quest;
import com.pauloneill.arcraidersplanner.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quests")
@Tag(name = "Quests", description = "Quest related endpoints")
public class QuestController {
    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping
    @Operation(summary = "Get all quests")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Quests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QuestDto.class))
            )
    })
    public List<QuestDto> getAllQuests() {
        List<Quest> quests = questService.findAll();
        return quests.stream()
                .map(quest -> QuestDto.builder()
                        .id(quest.getId())
                        .name(quest.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
