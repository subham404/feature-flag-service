package com.subham.projects.featureflag.repository;

import com.subham.projects.featureflag.entity.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity,Long> {

    Optional<FeatureFlagEntity> findByFeatureFlagName(String featureFlagName);
}
