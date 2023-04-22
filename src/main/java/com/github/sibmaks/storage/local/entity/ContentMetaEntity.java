package com.github.sibmaks.storage.local.entity;

import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "STORAGE_CONTENT_META")
public class ContentMetaEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "key", nullable = false)
    private String key;
    @Column(name = "value", nullable = false)
    private String value;
}
