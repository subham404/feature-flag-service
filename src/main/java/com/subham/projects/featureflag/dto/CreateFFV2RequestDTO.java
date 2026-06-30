package com.subham.projects.featureflag.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class CreateFFV2RequestDTO {
    @NonNull
    private String flagName;
    @NonNull
    private Boolean flagEnabled;
    @NonNull
    private String flagDesc;

    Set<String> allowedUsers;
    Set<String> deniedUsers;
    Integer rolloutPercentage;
}
