package com.finanscore.query.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="user_request_read_model")
public class UserRequestReadModelEntity {
    @Id @Column(name="request_id",nullable=false) public UUID requestId;
    @Column(name="user_id",nullable=false) public Long userId;
    @Column(name="correlation_id",nullable=false,length=36) public String correlationId;
    @Column(name="product_code",length=40) public String productCode;
    @Column(precision=18,scale=2) public BigDecimal amount;
    @Column(length=8) public String currency;
    @Column(name="term_months") public Integer termMonths;
    @Column(length=150) public String purpose;
    @Column(name="workflow_status",nullable=false,length=40) public String workflowStatus;
    @Column(name="score_value") public Integer scoreValue;
    @Column(length=40) public String recommendation;
    @Column(name="model_version",length=20) public String modelVersion;
    @Column(name="report_id") public UUID reportId;
    @Column(name="report_available",nullable=false) public boolean reportAvailable;
    @Column(name="failure_reason",length=500) public String failureReason;
    @Column(name="failure_stage",length=30) public String failureStage;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
}
