package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDescriptionsDto {
    private Long id;
    private String name;
    private String color;
    private Integer orderIndex;
}
