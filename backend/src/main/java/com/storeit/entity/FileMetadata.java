package com.storeit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "shared_with", columnDefinition = "text[]")
    private List<String> sharedWith;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
