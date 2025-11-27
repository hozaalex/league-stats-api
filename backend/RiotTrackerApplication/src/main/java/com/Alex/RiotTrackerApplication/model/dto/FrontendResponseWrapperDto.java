package com.Alex.RiotTrackerApplication.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FrontendResponseWrapperDto {

    private String status;
    private String requestId;
    private FrontEndResponseDto data;
    private String error;
}
