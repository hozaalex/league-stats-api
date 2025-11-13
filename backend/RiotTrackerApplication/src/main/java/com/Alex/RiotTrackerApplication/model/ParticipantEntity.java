package com.Alex.RiotTrackerApplication.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name="participants",uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "puuid"}))

public class ParticipantEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String puuid;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="match_id")
    private MatchEntity match;

    private Integer championId;
    private Boolean win;


    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer totalMinionsKilled;
    private Integer goldEarned;

}
