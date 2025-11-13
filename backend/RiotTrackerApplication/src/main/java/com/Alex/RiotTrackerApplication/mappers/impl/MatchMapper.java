package com.Alex.RiotTrackerApplication.mappers.impl;

import com.Alex.RiotTrackerApplication.mappers.Mapper;
import com.Alex.RiotTrackerApplication.model.MatchEntity;
import com.Alex.RiotTrackerApplication.model.ParticipantEntity;
import com.Alex.RiotTrackerApplication.model.dto.MatchDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper implements Mapper<MatchEntity, MatchDto> {

    private ModelMapper modelMapper;

    public MatchMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;


        this.modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);


        this.modelMapper.typeMap(MatchDto.class, MatchEntity.class)
                .addMappings(mapper -> {
                    mapper.using(ctx -> {
                        return null;
                    });
                });
    }

    @Override
    public MatchDto mapTo(MatchEntity matchEntity) {
        return modelMapper.map(matchEntity, MatchDto.class);
    }

    @Override
    public MatchEntity mapFrom(MatchDto matchDto) {
        MatchEntity entity = modelMapper.map(matchDto, MatchEntity.class);


        if (entity.getParticipants() != null) {
            entity.getParticipants().forEach(p -> p.setId(0));
        }

        return entity;
    }
}