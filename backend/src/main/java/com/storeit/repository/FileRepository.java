package com.storeit.repository;

import com.storeit.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileMetadata, UUID>,
        JpaSpecificationExecutor<FileMetadata> {

    /**
     * Find all active files owned by a user, paginated.
     */
    Page<FileMetadata> findByOwnerIdAndStatus(UUID ownerId, String status, Pageable pageable);

    /**
     * Find active files owned by a user filtered by file type.
     */
    Page<FileMetadata> findByOwnerIdAndStatusAndFileType(
            UUID ownerId, String status, String fileType, Pageable pageable);

    /**
     * Full-text search on filename for a specific owner's active files.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerId = :ownerId " +
           "AND f.status = 'ACTIVE' " +
           "AND LOWER(f.filename) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<FileMetadata> searchByFilename(
            @Param("ownerId") UUID ownerId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Full-text search on filename filtered by file type.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerId = :ownerId " +
           "AND f.status = 'ACTIVE' AND f.fileType = :fileType " +
           "AND LOWER(f.filename) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<FileMetadata> searchByFilenameAndType(
            @Param("ownerId") UUID ownerId,
            @Param("search") String search,
            @Param("fileType") String fileType,
            Pageable pageable);

    /**
     * Find orphaned PENDING files older than the given cutoff.
     */
    List<FileMetadata> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);

    /**
     * Sum total file size for a specific owner's active files.
     */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f " +
           "WHERE f.ownerId = :ownerId AND f.status = 'ACTIVE'")
    Long sumSizeByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Find active files shared with a specific email.
     */
    @Query(value = "SELECT * FROM files WHERE status = 'ACTIVE' AND :email = ANY(shared_with)",
           countQuery = "SELECT COUNT(*) FROM files WHERE status = 'ACTIVE' AND :email = ANY(shared_with)",
           nativeQuery = true)
    Page<FileMetadata> findSharedWithEmail(@Param("email") String email, Pageable pageable);
}
