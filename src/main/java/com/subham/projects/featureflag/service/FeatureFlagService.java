package com.subham.projects.featureflag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subham.projects.featureflag.dto.CreateFeatureFlagRequestDTO;
import com.subham.projects.featureflag.dto.FeatureFlagRequestDTO;
import com.subham.projects.featureflag.dto.FeatureFlagResponseDTO;
import com.subham.projects.featureflag.entity.FeatureFlagEntity;
import com.subham.projects.featureflag.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void createFeatureFlag(CreateFeatureFlagRequestDTO requestDTO){
        try {
            FeatureFlagEntity entity = new FeatureFlagEntity();
            entity.setFeatureFlagName(requestDTO.getFlagName());
            entity.setFeatureFlagDesc(requestDTO.getFlagDesc());
            entity.setFeatureFlagEnabled(requestDTO.getFlagValue());
            featureFlagRepository.save(entity);
            redisTemplate.opsForValue().set(requestDTO.getFlagName(), entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public FeatureFlagResponseDTO getFeatureFlag(String flagName){
        try{
            if(Strings.isBlank(flagName)){
                throw new RuntimeException("Flag is not present");
            }
            Object redisCacheValue = redisTemplate.opsForValue().get(flagName);
            if(!Objects.isNull(redisCacheValue)){
                log.info("Fetching value from redis cache");
                FeatureFlagEntity entity =  objectMapper.convertValue(redisCacheValue,
                        FeatureFlagEntity.class);
                return prepareFeatureFlagResponse(entity);
            }
            Optional<FeatureFlagEntity> entity = featureFlagRepository.findByFeatureFlagName(flagName);
            FeatureFlagEntity featureFlag = entity.orElseThrow(()-> new RuntimeException("Flag is not present"));
            return prepareFeatureFlagResponse(featureFlag);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FeatureFlagResponseDTO prepareFeatureFlagResponse(FeatureFlagEntity featureFlag){
        return FeatureFlagResponseDTO.builder()
                .flagEnabled(featureFlag.getFeatureFlagEnabled())
                .build();
    }


}
