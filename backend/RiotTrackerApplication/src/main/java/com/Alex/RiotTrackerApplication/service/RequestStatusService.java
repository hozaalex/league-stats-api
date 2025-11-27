package com.Alex.RiotTrackerApplication.service;

import com.Alex.RiotTrackerApplication.model.dto.FrontendResponseWrapperDto;
import reactor.core.publisher.Mono;

public interface RequestStatusService {

    Mono<Void> saveStatus(String requestId, FrontendResponseWrapperDto status);
    Mono<FrontendResponseWrapperDto> getStatus(String requestId);


}
