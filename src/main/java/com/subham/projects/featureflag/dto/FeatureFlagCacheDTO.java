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
public class FeatureFlagCacheDTO {

    private boolean enabled;
    private int rolloutPercentage;
    private String salt;
    private Set<String> allowUsers;
    private Set<String> denyUsers;
}
