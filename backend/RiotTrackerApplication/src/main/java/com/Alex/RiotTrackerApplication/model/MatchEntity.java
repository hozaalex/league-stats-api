package com.Alex.RiotTrackerApplication.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="matches")

public class MatchEntity {

    @Id
    private String matchId;

    private String gameMode;

    private long gameDuration;

    private Long gameCreation;

    private Integer queueId;

    @OneToMany(mappedBy = "match",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<ParticipantEntity> participants;






}
