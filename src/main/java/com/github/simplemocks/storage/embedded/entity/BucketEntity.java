package com.github.simplemocks.storage.embedded.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Bucket database entity
 *
 * @author sibmaks
 * @since 0.0.1
 */
@Entity(name = "storage_service_bucket")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "storage_service", name = "bucket")
public class BucketEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "modified_at", nullable = false)
    private ZonedDateTime modifiedAt;
    @Column(name = "readonly", nullable = false)
    private boolean readonly;
}
