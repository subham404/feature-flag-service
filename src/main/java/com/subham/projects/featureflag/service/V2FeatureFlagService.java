package com.subham.projects.featureflag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subham.projects.featureflag.constants.OverrideType;
import com.subham.projects.featureflag.dto.*;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public FeatureFlagResponseDTO getFeatureFlag(FeatureFlagRequestDTO requestDTO){
        try{

            Object redisCacheValue = redisTemplate.opsForValue().get(requestDTO.getFlagName());
            if(!Objects.isNull(redisCacheValue)){
                log.info("Fetching value from redis cache");
                FeatureFlagCacheDTO entity =  objectMapper.convertValue(redisCacheValue,
                        FeatureFlagCacheDTO.class);
                return prepareFeatureFlagResponse(entity);
            }
            Optional<FeatureFlagV2Entity> entity = featureFlagRepository.findByFeatureFlagName(flagName);
            FeatureFlagV2Entity featureFlag = entity.orElseThrow(()-> new RuntimeException("Flag is not present"));
            FeatureFlagCacheDTO cacheDTO = Fe
            Boolean flagEnabled =  prepareFeatureFlagResponse(featureFlag, requestDTO.getUserId());
            return FeatureFlagResponseDTO.builder()
                    .flagEnabled(flagEnabled)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean prepareFeatureFlagResponse(FeatureFlagCacheDTO featureFlag, String userId){
        if (!featureFlag.isEnabled()) {
            return false;
        }

        // Explicit deny
        if (featureFlag.getDenyUsers() != null &&
                featureFlag.getDenyUsers().contains(userId)) {
            return false;
        }

        // Explicit allow
        if (featureFlag.getAllowUsers() != null &&
                featureFlag.getAllowUsers().contains(userId)) {
            return true;
        }

        // Full rollout
        if (featureFlag.getRolloutPercentage() >= 100) {
            return true;
        }

        // No rollout
        if (featureFlag.getRolloutPercentage() <= 0) {
            return false;
        }

        int bucket = getBucket(userId, featureFlag.getSalt());

        return bucket < featureFlag.getRolloutPercentage();
    }

    private int getBucket(String userId, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((userId + ":" + salt)
                    .getBytes(StandardCharsets.UTF_8));

            int value =
                    ((hash[0] & 0xFF) << 24)
                            | ((hash[1] & 0xFF) << 16)
                            | ((hash[2] & 0xFF) << 8)
                            | (hash[3] & 0xFF);

            return Math.floorMod(value, 100);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to compute feature bucket", e);
        }
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
