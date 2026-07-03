package com.subham.projects.featureflag.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeatureFlagRequestDTO {
    @NotNull
    private String flagName;
    private String userId;
}
