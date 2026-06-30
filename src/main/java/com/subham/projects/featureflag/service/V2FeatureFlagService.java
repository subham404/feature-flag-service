package com.subham.projects.featureflag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subham.projects.featureflag.constants.OverrideType;
import com.subham.projects.featureflag.dto.CreateFFV2RequestDTO;
import com.subham.projects.featureflag.dto.CreateFeatureFlagRequestDTO;
import com.subham.projects.featureflag.dto.FeatureFlagCacheDTO;
import com.subham.projects.featureflag.dto.FeatureFlagResponseDTO;
import com.subham.projects.featureflag.entity.FeatureFlagEntity;
import com.subham.projects.featureflag.entity.FeatureFlagUserOverrideEntity;
import com.subham.projects.featureflag.entity.FeatureFlagV2Entity;
import com.subham.projects.featureflag.repository.FeatureFlagRepository;
import com.subham.projects.featureflag.repository.FeatureFlagUserOverrideRepository;
import com.subham.projects.featureflag.repository.FeatureFlagV2Repository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;

import static com.subham.projects.featureflag.constants.OverrideType.ALLOW;
import static com.subham.projects.featureflag.constants.OverrideType.DENY;

@Service
@Slf4j
@RequiredArgsConstructor
public class V2FeatureFlagService {

    private final FeatureFlagV2Repository featureFlagRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final FeatureFlagUserOverrideRepository featureFlagUserOverrideRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Integer LENGTH = 7;

    @Transactional
    public void createFeatureFlag(CreateFFV2RequestDTO requestDTO){
        try {
            String saltValue = generateSalt();
            FeatureFlagV2Entity entity = new FeatureFlagV2Entity();
            entity.setFeatureFlagName(requestDTO.getFlagName());
            entity.setFeatureFlagDesc(requestDTO.getFlagDesc());
            entity.setFeatureFlagEnabled(requestDTO.getFlagEnabled());
            entity.setRolloutPercentage(requestDTO.getRolloutPercentage());
            entity.setSaltValue(saltValue);
            featureFlagRepository.save(entity);
            if(requestDTO.getAllowedUsers() != null && !requestDTO.getAllowedUsers().isEmpty()){
                FeatureFlagUserOverrideEntity overrideEntity = new FeatureFlagUserOverrideEntity();
                requestDTO.getAllowedUsers().forEach(allowedUsers ->insertOverrideUser(allowedUsers,ALLOW,entity));

            }
            if(requestDTO.getDeniedUsers() != null && !requestDTO.getDeniedUsers().isEmpty()){
                FeatureFlagUserOverrideEntity overrideEntity = new FeatureFlagUserOverrideEntity();
                requestDTO.getDeniedUsers().forEach(deniedUsers ->insertOverrideUser(deniedUsers,DENY,entity));

            }
            FeatureFlagCacheDTO featureFlagCache = prepareFeatureFlagCache(requestDTO, saltValue);
            redisTemplate.opsForValue().set(requestDTO.getFlagName(), featureFlagCache);
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
                FeatureFlagV2Entity entity =  objectMapper.convertValue(redisCacheValue,
                        FeatureFlagV2Entity.class);
                return prepareFeatureFlagResponse(entity);
            }
            Optional<FeatureFlagV2Entity> entity = featureFlagRepository.findByFeatureFlagName(flagName);
            FeatureFlagV2Entity featureFlag = entity.orElseThrow(()-> new RuntimeException("Flag is not present"));
            return prepareFeatureFlagResponse(featureFlag);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FeatureFlagResponseDTO prepareFeatureFlagResponse(FeatureFlagV2Entity featureFlag){
        return FeatureFlagResponseDTO.builder()
                .flagValue(featureFlag.getFeatureFlagEnabled())
                .build();
    }

    private void insertOverrideUser(String userId, OverrideType overrideType, FeatureFlagV2Entity entity){
        FeatureFlagUserOverrideEntity overrideEntity = new FeatureFlagUserOverrideEntity();
        overrideEntity.setFeatureFlag(entity);
        overrideEntity.setOverrideType(overrideType);
        overrideEntity.setUserId(userId);

        featureFlagUserOverrideRepository.save(overrideEntity);
    }

    private FeatureFlagCacheDTO prepareFeatureFlagCache(CreateFFV2RequestDTO requestDTO, String saltValue){
        return FeatureFlagCacheDTO.builder()
                .enabled(requestDTO.getFlagEnabled())
                .salt(saltValue)
                .rolloutPercentage(requestDTO.getRolloutPercentage())
                .allowUsers(requestDTO.getAllowedUsers())
                .denyUsers(requestDTO.getDeniedUsers())
                .build();
    }
    public static String generateSalt() {
        StringBuilder salt = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            salt.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return salt.toString();
    }
}
