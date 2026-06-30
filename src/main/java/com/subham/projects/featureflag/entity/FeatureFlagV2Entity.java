package com.subham.projects.featureflag.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "t_v2_feature_flag")
public class FeatureFlagV2Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "feature_flag_name", nullable = false)
    private String featureFlagName;

    @Column(name = "feature_flag_desc")
    private String featureFlagDesc;

    @Column(name = "feature_flag_enabled", nullable = false)
    private Boolean featureFlagEnabled;

    @Column(name = "rollout_percentage", nullable = false)
    private Integer rolloutPercentage;

    @Column(name = "salt_value", nullable = false)
    private String saltValue;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy = "SYSTEM";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy = "SYSTEM";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}