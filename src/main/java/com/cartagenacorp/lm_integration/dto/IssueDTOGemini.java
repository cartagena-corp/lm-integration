package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTOGemini implements Serializable {
    private String title;
    private List<DescriptionDTO> descriptionsDTO;
    private UUID projectId;
    private String assignedId;
}
