package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {
    private String title;
    private List<DescriptionDTO> descriptionsDTO;
    private Integer estimatedTime;
    private UUID projectId;
    private UUID sprintId;
    private Long priority;
    private Long status;
    private Long type;
    private UUID assignedId;
}
