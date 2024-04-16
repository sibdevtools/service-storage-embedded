package com.github.simple_mocks.storage.local.entity;

import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "content_meta")
public class ContentMetaEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "meta_key", nullable = false)
    private String key;
    @Column(name = "meta_value", nullable = false)
    private String value;
    @Column(name = "content_uid", nullable = false)
    private String contentUid;
}
