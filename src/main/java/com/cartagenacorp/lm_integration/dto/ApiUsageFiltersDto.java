package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiUsageFiltersDto {
    private List<String> features;
    private List<ProjectFilterDto> projectIds;
    private List<String> emails;
}
