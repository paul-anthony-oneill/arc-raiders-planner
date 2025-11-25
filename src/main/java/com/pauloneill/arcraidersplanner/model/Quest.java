package com.pauloneill.arcraidersplanner.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quests")
public class Quest {
    @Id
    private String id;

    private String name;

    @ManyToMany(mappedBy = "quests")
    private Set<MapMarker> markers;
}
