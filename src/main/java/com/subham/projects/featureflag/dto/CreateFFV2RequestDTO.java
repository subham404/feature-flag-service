package com.subham.projects.featureflag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class CreateFFV2RequestDTO {
    @NotNull
    private String flagName;
    @NotNull
    private Boolean flagEnabled;
    @NotNull
    private String flagDesc;

    Set<String> allowedUsers;
    Set<String> deniedUsers;
    Integer rolloutPercentage;
}
