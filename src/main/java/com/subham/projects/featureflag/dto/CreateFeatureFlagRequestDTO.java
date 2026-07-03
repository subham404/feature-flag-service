package com.subham.projects.featureflag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;

@Data
public class CreateFeatureFlagRequestDTO {

    @NotNull
    private String flagName;
    @NotNull
    private Boolean flagValue;
    @NotNull
    private String flagDesc;
}
