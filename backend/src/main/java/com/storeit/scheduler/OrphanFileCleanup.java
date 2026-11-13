package com.storeit.scheduler;

import com.storeit.entity.FileMetadata;
import com.storeit.repository.FileRepository;
import com.storeit.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that cleans up orphaned PENDING files older than 1 hour.
 * These are files where the upload was initiated but never confirmed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanFileCleanup {

    private final FileRepository fileRepository;
    private final StorageService storageService;

    @Scheduled(cron = "0 0 * * * *") // every hour, at minute 0
    public void cleanupOrphanedFiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<FileMetadata> orphans = fileRepository.findByStatusAndCreatedAtBefore("PENDING", cutoff);

        if (orphans.isEmpty()) {
            log.debug("No orphaned PENDING files to clean up");
            return;
        }

        log.info("Cleaning up {} orphaned PENDING files older than {}", orphans.size(), cutoff);

        orphans.forEach(file -> {
            try {
                storageService.deleteObject(file.getObjectKey());
            } catch (Exception e) {
                log.warn("Failed to delete orphan object {}: {}", file.getObjectKey(), e.getMessage());
            }
            fileRepository.delete(file);
            log.debug("Cleaned up orphaned file: {} ({})", file.getId(), file.getFilename());
        });

        log.info("Orphan cleanup complete — removed {} files", orphans.size());
    }
}
