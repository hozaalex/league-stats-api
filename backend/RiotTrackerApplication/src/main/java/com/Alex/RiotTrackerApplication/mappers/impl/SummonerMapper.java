package com.Alex.RiotTrackerApplication.mappers.impl;


import com.Alex.RiotTrackerApplication.model.SummonerEntity;
import com.Alex.RiotTrackerApplication.model.dto.SummonerDto;
import com.Alex.RiotTrackerApplication.mappers.Mapper;
import org.springframework.stereotype.Component;
import org.modelmapper.ModelMapper;


@Component
public class SummonerMapper implements Mapper<SummonerEntity, SummonerDto> {

    private ModelMapper modelMapper;

    public SummonerMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public SummonerDto mapTo(SummonerEntity summonerEntity) {
        return modelMapper.map(summonerEntity, SummonerDto.class);
    }

    @Override
    public SummonerEntity mapFrom(SummonerDto summonerDto) {
        return modelMapper.map(summonerDto, SummonerEntity.class);
    }
}
