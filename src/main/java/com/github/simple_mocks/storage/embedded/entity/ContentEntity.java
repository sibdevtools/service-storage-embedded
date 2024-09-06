package com.github.simple_mocks.storage.embedded.entity;

import com.github.simple_mocks.storage.embedded.dto.ContentStorageFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Content database entity
 *
 * @author sibmaks
 * @since 0.0.1
 */
@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "storage_service", name = "content")
public class ContentEntity {
    @Id
    @Column(name = "uid")
    private String uid;
    @Column(name = "name", nullable = false)
    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "bucket_id", nullable = false)
    private BucketEntity bucket;
    @Enumerated(value = EnumType.STRING)
    @Column(name = "storage_format", nullable = false)
    private ContentStorageFormat storageFormat;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "modified_at", nullable = false)
    private ZonedDateTime modifiedAt;
}
