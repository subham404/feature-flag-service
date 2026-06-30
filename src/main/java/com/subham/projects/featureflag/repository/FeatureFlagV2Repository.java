package com.subham.projects.featureflag.repository;

import com.subham.projects.featureflag.entity.FeatureFlagEntity;
import com.subham.projects.featureflag.entity.FeatureFlagV2Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureFlagV2Repository extends JpaRepository<FeatureFlagV2Entity,Long> {

    Optional<FeatureFlagV2Entity> findByFeatureFlagName(String featureFlagName);
}
