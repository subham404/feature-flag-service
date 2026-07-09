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
import java.util.*;
import java.util.stream.Collectors;

import static com.subham.projects.featureflag.constants.OverrideType.ALLOW;
import static com.subham.projects.featureflag.constants.OverrideType.DENY;

@Slf4j
@Service
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
                boolean isEnabled = prepareFeatureFlagResponse(entity, requestDTO.getUserId());
                return FeatureFlagResponseDTO.builder()
                        .flagEnabled(isEnabled)
                        .build();
            }
            Optional<FeatureFlagV2Entity> entity = featureFlagRepository.findByFeatureFlagName(requestDTO.getFlagName());
            FeatureFlagV2Entity featureFlag = entity.orElseThrow(()-> new RuntimeException("Flag is not present"));
            List<FeatureFlagUserOverrideEntity> overrideUserList = featureFlagUserOverrideRepository.findActiveOverridesByFeatureFlagId(featureFlag.getId());
            Set<String> allowedList = overrideUserList.stream()
                    .filter( overrideUserEntity -> overrideUserEntity.getOverrideType() == ALLOW)
                    .map(FeatureFlagUserOverrideEntity::getUserId)
                    .collect(Collectors.toSet());
            Set<String> deniedList = overrideUserList.stream()
                    .filter( overrideUserEntity -> overrideUserEntity.getOverrideType() == ALLOW)
                    .map(FeatureFlagUserOverrideEntity::getUserId)
                    .collect(Collectors.toSet());
            FeatureFlagCacheDTO cacheDTO = FeatureFlagCacheDTO.builder()
                    .enabled(featureFlag.getFeatureFlagEnabled())
                    .salt(featureFlag.getSaltValue())
                    .allowUsers(allowedList)
                    .denyUsers(deniedList)
                    .rolloutPercentage(featureFlag.getRolloutPercentage())
                    .build();
            redisTemplate.opsForValue().set(requestDTO.getFlagName(), cacheDTO);
            Boolean flagEnabled =  prepareFeatureFlagResponse(cacheDTO, requestDTO.getUserId());
            return FeatureFlagResponseDTO.builder()
                    .flagEnabled(flagEnabled)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void updateFeatureFlag(UpdateFeatureFlagRequestDTO request) {
        try{
            Optional<FeatureFlagV2Entity> entity = featureFlagRepository.findByFeatureFlagName(request.getFlagName());
            FeatureFlagV2Entity featureFlag = entity.orElseThrow(() -> new RuntimeException("Flag is not present"));
            if (request.getFlagEnabled() != null) {
                featureFlag.setFeatureFlagEnabled(request.getFlagEnabled());
            }
            if (request.getRolloutPercentage() != null && isValidPercentage(request.getRolloutPercentage())) {
                featureFlag.setRolloutPercentage(request.getRolloutPercentage());
            }
            featureFlagRepository.save(featureFlag);
            if(request.getAllowUsers()!= null || request.getDenyUsers()!=null){
                updateUserOverrides(request, featureFlag);
            }
            updateCache(request, featureFlag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void updateCache(UpdateFeatureFlagRequestDTO request, FeatureFlagV2Entity featureFlag){
        FeatureFlagCacheDTO cacheDTO = FeatureFlagCacheDTO.builder()
                .enabled(featureFlag.getFeatureFlagEnabled())
                .salt(featureFlag.getSaltValue())
                .allowUsers(request.getAllowUsers())
                .denyUsers(request.getDenyUsers())
                .rolloutPercentage(featureFlag.getRolloutPercentage())
                .build();
        redisTemplate.opsForValue().set(featureFlag.getFeatureFlagName(), cacheDTO);
    }

    private boolean isValidPercentage(int percentage){
        return percentage>=0 && percentage<=100;
    }

    private void updateUserOverrides(UpdateFeatureFlagRequestDTO request, FeatureFlagV2Entity featureFlag){
        List<FeatureFlagUserOverrideEntity> overrideUserList = featureFlagUserOverrideRepository.findActiveOverridesByFeatureFlagId(featureFlag.getId());
        List<FeatureFlagUserOverrideEntity> updatedList = overrideUserList.stream()
                .filter(overrideUserEntity -> !(request.getDenyUsers().contains(overrideUserEntity.getUserId())
                        || request.getAllowUsers().contains(overrideUserEntity.getUserId())))
                .collect(Collectors.toCollection(ArrayList::new));
        updatedList.forEach(overrideUserEntity -> overrideUserEntity.setIsActive(false));
        updatedList.addAll(addNewOverrideUsers(request.getAllowUsers(),overrideUserList, ALLOW,featureFlag));
        updatedList.addAll(addNewOverrideUsers(request.getDenyUsers(),overrideUserList, DENY,featureFlag));
        featureFlagUserOverrideRepository.saveAll(updatedList);
    }

    private List<FeatureFlagUserOverrideEntity> addNewOverrideUsers(
            Set<String> users, List<FeatureFlagUserOverrideEntity> overrideEntities,
            OverrideType overrideType, FeatureFlagV2Entity featureFlag) {

        Set<String> alreadyActiveWithSameType = overrideEntities.stream()
                .filter(e -> e.getOverrideType() == overrideType)
                .map(FeatureFlagUserOverrideEntity::getUserId)
                .collect(Collectors.toSet());

        return users.stream()
                .filter(user -> !alreadyActiveWithSameType.contains(user))
                .map(user -> FeatureFlagUserOverrideEntity.builder()
                        .featureFlag(featureFlag)
                        .overrideType(overrideType)
                        .userId(user)
                        .isActive(true)
                        .build())
                .toList();
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
        overrideEntity.setIsActive(true);
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
