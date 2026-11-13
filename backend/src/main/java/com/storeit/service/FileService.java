package com.storeit.service;

import com.storeit.dto.FileDto;
import com.storeit.entity.FileMetadata;
import com.storeit.entity.User;
import com.storeit.exception.ForbiddenException;
import com.storeit.exception.ResourceNotFoundException;
import com.storeit.exception.StorageException;
import com.storeit.mapper.FileMapper;
import com.storeit.repository.FileRepository;
import com.storeit.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileMapper fileMapper;

    private static final long MAX_QUOTA_BYTES = 2L * 1024 * 1024 * 1024; // 2 GB
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;   // 50 MB

    /**
     * Initiate a file upload: create a PENDING metadata record and return a presigned PUT URL.
     */
    @Transactional
    public FileMetadata initUpload(UUID ownerId, String filename, String contentType, long size) {
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File exceeds the 50 MB limit (" + size + " bytes)");
        }

        // Check quota
        long usedBytes = fileRepository.sumSizeByOwnerId(ownerId);
        if (usedBytes + size > MAX_QUOTA_BYTES) {
            throw new StorageException("Storage quota exceeded. Used: " + usedBytes +
                    " bytes, requested: " + size + " bytes, limit: " + MAX_QUOTA_BYTES + " bytes");
        }

        String extension = extractExtension(filename);
        String fileType = determineFileType(extension);
        String objectKey = ownerId + "/" + UUID.randomUUID() + "-" + filename;

        FileMetadata file = FileMetadata.builder()
                .ownerId(ownerId)
                .filename(filename)
                .objectKey(objectKey)
                .size(size)
                .contentType(contentType)
                .extension(extension)
                .fileType(fileType)
                .status("PENDING")
                .sharedWith(new ArrayList<>())
                .build();

        return fileRepository.save(file);
    }

    /**
     * Generate a presigned PUT URL for a PENDING file owned by the caller.
     */
    public String getPresignedPutUrl(UUID fileId, UUID ownerId) {
        FileMetadata file = findFile(fileId);
        requireOwner(file, ownerId);
        return storageService.generatePresignedPutUrl(file.getObjectKey(), file.getContentType());
    }

    /**
     * Confirm upload: verify the file exists in MinIO and mark as ACTIVE.
     */
    @Transactional
    public FileMetadata confirmUpload(UUID fileId, UUID ownerId) {
        FileMetadata file = findFile(fileId);
        requireOwner(file, ownerId);

        if (!"PENDING".equals(file.getStatus())) {
            throw new IllegalArgumentException("File is not in PENDING status");
        }

        // Verify the file actually exists in MinIO
        try {
            long actualSize = storageService.headObject(file.getObjectKey());
            file.setSize(actualSize); // update with actual size from storage
        } catch (StorageException e) {
            file.setStatus("FAILED");
            fileRepository.save(file);
            throw new StorageException("Upload verification failed — file not found in storage", e);
        }

        file.setStatus("ACTIVE");
        return fileRepository.save(file);
    }

    /**
     * List files the user can access (owned + shared with their email), optionally
     * filtered by one or more file types and a filename search, paginated and sorted.
     */
    public Page<FileMetadata> listFiles(UUID ownerId, String requesterEmail, List<String> types,
                                        String search, int page, int size,
                                        String sortField, String sortDir) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                mapSortField(sortField));
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<FileMetadata> spec =
                (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");

        spec = spec.and((root, query, cb) -> shareOrOwn(root, cb, ownerId, requesterEmail));

        List<String> normalizedTypes = normalizeTypes(types);
        if (!normalizedTypes.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("fileType").in(normalizedTypes));
        }

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("filename")), pattern));
        }

        return fileRepository.findAll(spec, pageable);
    }

    /**
     * Get a single file by ID, allowing the owner or a user the file was shared with.
     */
    public FileMetadata getFile(UUID fileId, User requester) {
        FileMetadata file = findFile(fileId);
        if (!canAccess(file, requester)) {
            throw new ForbiddenException("Access denied");
        }
        return file;
    }

    /**
     * Rename a file. Only the owner may rename.
     */
    @Transactional
    public FileMetadata renameFile(UUID fileId, UUID ownerId, String newName) {
        FileMetadata file = findFile(fileId);
        requireOwner(file, ownerId);

        file.setFilename(newName);
        String newExtension = extractExtension(newName);
        if (newExtension != null) {
            file.setExtension(newExtension);
        }
        return fileRepository.save(file);
    }

    /**
     * Delete a file from both MinIO and the database. Only the owner may delete.
     */
    @Transactional
    public void deleteFile(UUID fileId, UUID ownerId) {
        FileMetadata file = findFile(fileId);
        requireOwner(file, ownerId);

        storageService.deleteObject(file.getObjectKey());
        fileRepository.delete(file);
        log.info("Deleted file {} (owner: {})", fileId, ownerId);
    }

    /**
     * Get a presigned download URL for a file the caller may access.
     */
    public String getDownloadUrl(UUID fileId, User requester) {
        FileMetadata file = findFile(fileId);
        if (!canAccess(file, requester)) {
            throw new ForbiddenException("Access denied");
        }
        return storageService.generatePresignedGetUrl(file.getObjectKey());
    }

    /**
     * Share a file with a list of email addresses. Only the owner may share.
     */
    @Transactional
    public FileMetadata shareFile(UUID fileId, UUID ownerId, List<String> emails) {
        FileMetadata file = findFile(fileId);
        requireOwner(file, ownerId);

        List<String> normalized = emails == null
                ? new ArrayList<>()
                : emails.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(e -> !e.isEmpty())
                        .distinct()
                        .collect(Collectors.toCollection(ArrayList::new));

        file.setSharedWith(normalized);
        return fileRepository.save(file);
    }

    /**
     * Get the total space used by a user and the overall quota.
     */
    public SpaceInfo getSpaceUsed(UUID ownerId) {
        long usedBytes = fileRepository.sumSizeByOwnerId(ownerId);
        return new SpaceInfo(usedBytes, MAX_QUOTA_BYTES);
    }

    // ---- Response assembly (entity -> DTO with presigned URL + owner info) ----

    public FileDto toDto(FileMetadata file) {
        Map<UUID, User> owners = loadOwners(List.of(file));
        return toDto(file, owners);
    }

    public List<FileDto> toDtos(List<FileMetadata> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> owners = loadOwners(files);
        return files.stream().map(file -> toDto(file, owners)).toList();
    }

    public Page<FileDto> toDtoPage(Page<FileMetadata> page) {
        return new PageImpl<>(toDtos(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    private FileDto toDto(FileMetadata file, Map<UUID, User> owners) {
        FileDto dto = fileMapper.toDto(file);
        dto.setStorageBackend("minio");

        User owner = file.getOwnerId() == null ? null : owners.get(file.getOwnerId());
        if (owner != null) {
            dto.setOwnerName(owner.getFullName());
            dto.setOwnerEmail(owner.getEmail());
        }

        try {
            dto.setUrl(storageService.generatePresignedGetUrl(file.getObjectKey()));
        } catch (Exception e) {
            // Never fail a listing because MinIO is temporarily unreachable.
            log.warn("Could not generate presigned GET URL for {}: {}",
                    file.getObjectKey(), e.getMessage());
        }

        return dto;
    }

    private Map<UUID, User> loadOwners(List<FileMetadata> files) {
        Set<UUID> ownerIds = files.stream()
                .map(FileMetadata::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ownerIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, User> owners = new HashMap<>();
        userRepository.findAllById(ownerIds).forEach(user -> owners.put(user.getId(), user));
        return owners;
    }

    // ---- Helper types and methods ----

    public record SpaceInfo(long usedBytes, long totalBytes) {}

    private FileMetadata findFile(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    private void requireOwner(FileMetadata file, UUID ownerId) {
        if (!Objects.equals(file.getOwnerId(), ownerId)) {
            throw new ForbiddenException("You do not own this file");
        }
    }

    private boolean canAccess(FileMetadata file, User requester) {
        if (requester == null) {
            return false;
        }
        if (Objects.equals(file.getOwnerId(), requester.getId())) {
            return true;
        }
        return requester.getEmail() != null
                && file.getSharedWith() != null
                && file.getSharedWith().contains(requester.getEmail());
    }

    private Predicate shareOrOwn(Root<FileMetadata> root, CriteriaBuilder cb,
                                 UUID ownerId, String requesterEmail) {
        Predicate owned = cb.equal(root.get("ownerId"), ownerId);
        if (requesterEmail == null || requesterEmail.isBlank()) {
            return owned;
        }
        Predicate shared = cb.isTrue(
                cb.function("array_contains", Boolean.class,
                        root.get("sharedWith"),
                        cb.literal(requesterEmail)));
        return cb.or(owned, shared);
    }

    private List<String> normalizeTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return types.stream()
                .filter(Objects::nonNull)
                .flatMap(type -> List.of(type.split(",")).stream())
                .map(String::trim)
                .filter(type -> !type.isEmpty())
                .distinct()
                .toList();
    }

    private String extractExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    private String determineFileType(String extension) {
        if (extension == null) return "other";
        return switch (extension.toLowerCase()) {
            case "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx",
                 "odt", "ods", "odp", "csv", "rtf" -> "document";
            case "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico",
                 "tiff", "tif", "heic", "heif" -> "image";
            case "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
                 "m4v" -> "video";
            case "mp3", "wav", "ogg", "flac", "aac", "wma",
                 "m4a" -> "audio";
            default -> "other";
        };
    }

    private String mapSortField(String field) {
        if (field == null) return "createdAt";
        return switch (field) {
            case "name" -> "filename";
            case "size" -> "size";
            case "date", "created", "$createdAt" -> "createdAt";
            case "type" -> "fileType";
            default -> "createdAt";
        };
    }
}
