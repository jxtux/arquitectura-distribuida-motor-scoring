package com.finanscore.query.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="processed_projection_event")
public class ProcessedProjectionEventEntity {
    @Id @Column(name="projection_key",length=180) public String projectionKey;
    @Column(name="event_id",nullable=false,length=36) public String eventId;
    @Column(name="processed_at",nullable=false) public Instant processedAt;
}
