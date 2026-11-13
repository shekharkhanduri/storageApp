package com.storeit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {

    private UUID id;
    private UUID ownerId;
    private String filename;
    private String objectKey;
    private Long size;
    private String contentType;
    private String extension;
    private String fileType;
    private String status;
    private List<String> sharedWith;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Fields assembled by FileService (not mapped directly from the entity).
    private String url;             // presigned GET URL for preview/download
    private String ownerName;
    private String ownerEmail;
    private String storageBackend;  // always "minio" for this backend
}
