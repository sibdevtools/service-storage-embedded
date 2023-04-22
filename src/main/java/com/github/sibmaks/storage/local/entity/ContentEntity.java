package com.github.sibmaks.storage.local.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author sibmaks
 * @since 2023-04-22
 */
@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "STORAGE_CONTENT")
public class ContentEntity {
    @Id
    @Column(name = "uid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uid;
    @Column(name = "name", nullable = false)
    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "bucket_id")
    private BucketEntity bucket;
    @OneToMany(mappedBy = "content_id", cascade = CascadeType.ALL)
    private List<ContentMetaEntity> meta;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "modified_at", nullable = false)
    private ZonedDateTime modifiedAt;
}
