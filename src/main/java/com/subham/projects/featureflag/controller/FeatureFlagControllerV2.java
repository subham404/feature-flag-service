package com.subham.projects.featureflag.controller;

import com.subham.projects.featureflag.dto.CreateFFV2RequestDTO;
import com.subham.projects.featureflag.dto.CreateFeatureFlagRequestDTO;
import com.subham.projects.featureflag.dto.FeatureFlagResponseDTO;
import com.subham.projects.featureflag.service.FeatureFlagService;
import com.subham.projects.featureflag.service.V2FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/feature-flag")
public class FeatureFlagControllerV2 {

    private final V2FeatureFlagService featureFlagService;

    @PostMapping("/save")
    public void saveFeatureFlag(@RequestBody CreateFFV2RequestDTO requestDTO){
        log.info("Saving feature flag- name:{}, desc: {}", requestDTO.getFlagName(), requestDTO.getFlagDesc());
        featureFlagService.createFeatureFlag(requestDTO);
    }

    @GetMapping("/fetch")
    public FeatureFlagResponseDTO getFeatureFlag(@RequestParam("flagName") String flagName){
        log.info("Fetching flag value for feature flag: {}", flagName);
        return featureFlagService.getFeatureFlag(flagName);
    }

}
