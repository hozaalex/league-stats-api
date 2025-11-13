package com.Alex.RiotTrackerApplication.model.dto;

import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MatchDto {


    private String matchId;
    private String gameMode;
    private long gameDuration;
    private long gameCreation;
    private List<ParticipantDto> participants;
    private Integer queueId;
}
