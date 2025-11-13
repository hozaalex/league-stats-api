package com.Alex.RiotTrackerApplication.mappers.impl;

import com.Alex.RiotTrackerApplication.mappers.Mapper;
import com.Alex.RiotTrackerApplication.model.RankedStatsEntity;
import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.RankedStatsDto;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RankedStatsMapper implements Mapper<RankedStatsEntity, RankedStatsDto> {

    private final ModelMapper modelMapper;

    public RankedStatsMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public RankedStatsDto mapTo(RankedStatsEntity rankedStatsEntity) {
        return modelMapper.map(rankedStatsEntity, RankedStatsDto.class);
    }

    @Override
    public RankedStatsEntity mapFrom(RankedStatsDto dto) {
        return modelMapper.map(dto, RankedStatsEntity.class);
    }
}
