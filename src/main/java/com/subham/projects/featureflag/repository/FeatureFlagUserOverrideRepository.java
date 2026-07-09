package com.subham.projects.featureflag.repository;

import com.subham.projects.featureflag.entity.FeatureFlagUserOverrideEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FeatureFlagUserOverrideRepository extends JpaRepository<FeatureFlagUserOverrideEntity, Long> {

    @Query("SELECT o FROM FeatureFlagUserOverrideEntity o WHERE o.featureFlag.id = :featureFlagId AND o.isActive = true")
    List<FeatureFlagUserOverrideEntity> findActiveOverridesByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);
}
