package com.github.simple_mocks.storage.local.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
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
@Table(name = "content")
public class ContentEntity {
    @Id
    @Column(name = "uid")
    private String uid;
    @Column(name = "name", nullable = false)
    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "bucket_id")
    private BucketEntity bucket;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "modified_at", nullable = false)
    private ZonedDateTime modifiedAt;
}
