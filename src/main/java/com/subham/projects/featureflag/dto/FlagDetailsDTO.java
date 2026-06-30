package com.subham.projects.featureflag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlagDetailsDTO {

    private Boolean flagValue;
    private String flagName;
    private Boolean isActive;
    private String flagDesc;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private Set<String> allowedUsers;
    private Set<String> deniedUsers;
    private Integer rolloutPercentage;
}
