package com.subham.projects.featureflag.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class CreateFeatureFlagRequestDTO {

    @NonNull
    private String flagName;
    @NonNull
    private Boolean flagValue;
    @NonNull
    private String flagDesc;
}
