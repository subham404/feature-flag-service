package com.subham.projects.featureflag.repository;

import com.subham.projects.featureflag.entity.FeatureFlagUserOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagUserOverrideRepository extends JpaRepository<FeatureFlagUserOverrideEntity, Long> {
}
