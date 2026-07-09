package com.subham.projects.featureflag.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateFeatureFlagRequestDTO {

    private Boolean flagEnabled;
    private String flagName;
    private Integer rolloutPercentage;
    private Set<String> allowUsers;
    private Set<String> denyUsers;
}
