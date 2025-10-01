package com.cartagenacorp.lm_integration.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationResponseDto implements Serializable {
    private UUID organizationId;
    private String organizationName;
    private LocalDateTime createdAt;
}