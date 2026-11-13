package com.storeit.service;

import com.storeit.entity.FileMetadata;
import com.storeit.entity.User;
import com.storeit.exception.ForbiddenException;
import com.storeit.exception.ResourceNotFoundException;
import com.storeit.exception.StorageException;
import com.storeit.mapper.FileMapper;
import com.storeit.repository.FileRepository;
import com.storeit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileService fileService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();

    // ---- initUpload ----

    @Test
    void initUpload_shouldCreatePendingFile() {
        when(fileRepository.sumSizeByOwnerId(ownerId)).thenReturn(0L);
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata f = inv.getArgument(0);
            f.setId(fileId);
            return f;
        });

        FileMetadata result = fileService.initUpload(ownerId, "report.pdf", "application/pdf", 1024L);

        assertThat(result.getFilename()).isEqualTo("report.pdf");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getFileType()).isEqualTo("document");
        assertThat(result.getExtension()).isEqualTo("pdf");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getObjectKey()).contains(ownerId.toString());
        verify(fileRepository).save(any(FileMetadata.class));
    }

    @Test
    void initUpload_shouldRejectFilesOver50MB() {
        long tooLarge = 50L * 1024 * 1024 + 1;

        assertThatThrownBy(() -> fileService.initUpload(ownerId, "big.zip", "application/zip", tooLarge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 MB");
    }

    @Test
    void initUpload_shouldThrowWhenQuotaExceeded() {
        long almostFull = 2L * 1024 * 1024 * 1024 - 100; // 2GB - 100 bytes
        when(fileRepository.sumSizeByOwnerId(ownerId)).thenReturn(almostFull);

        assertThatThrownBy(() -> fileService.initUpload(ownerId, "big.zip", "application/zip", 200L))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Storage quota exceeded");
    }

    // ---- confirmUpload ----

    @Test
    void confirmUpload_shouldMarkActive() {
        FileMetadata file = buildPendingFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(storageService.headObject(file.getObjectKey())).thenReturn(1024L);
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        FileMetadata result = fileService.confirmUpload(fileId, ownerId);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSize()).isEqualTo(1024L);
    }

    @Test
    void confirmUpload_shouldRejectWrongOwner() {
        FileMetadata file = buildPendingFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        UUID otherOwner = UUID.randomUUID();
        assertThatThrownBy(() -> fileService.confirmUpload(fileId, otherOwner))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not own");
    }

    @Test
    void confirmUpload_shouldRejectNonPendingFile() {
        FileMetadata file = buildPendingFile();
        file.setStatus("ACTIVE");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.confirmUpload(fileId, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in PENDING");
    }

    @Test
    void confirmUpload_shouldMarkFailedWhenObjectMissing() {
        FileMetadata file = buildPendingFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(storageService.headObject(file.getObjectKey()))
                .thenThrow(new StorageException("Object not found"));
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> fileService.confirmUpload(fileId, ownerId))
                .isInstanceOf(StorageException.class);

        assertThat(file.getStatus()).isEqualTo("FAILED");
        verify(fileRepository).save(file);
    }

    // ---- listFiles ----

    @Test
    void listFiles_shouldReturnPage() {
        Page<FileMetadata> page = new PageImpl<>(List.of(buildActiveFile()));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<FileMetadata> result = fileService.listFiles(
                ownerId, "owner@example.com", null, null, 0, 20, "date", "desc");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listFiles_shouldFilterByType() {
        Page<FileMetadata> page = new PageImpl<>(List.of(buildActiveFile()));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<FileMetadata> result = fileService.listFiles(
                ownerId, "owner@example.com", List.of("video", "audio"), null, 0, 20, "date", "desc");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listFiles_shouldSearchByFilename() {
        Page<FileMetadata> page = new PageImpl<>(List.of(buildActiveFile()));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<FileMetadata> result = fileService.listFiles(
                ownerId, "owner@example.com", null, "report", 0, 20, "date", "desc");

        assertThat(result.getContent()).hasSize(1);
    }

    // ---- renameFile ----

    @Test
    void renameFile_shouldUpdateFilename() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        FileMetadata result = fileService.renameFile(fileId, ownerId, "new-name.pdf");

        assertThat(result.getFilename()).isEqualTo("new-name.pdf");
        assertThat(result.getExtension()).isEqualTo("pdf");
    }

    @Test
    void renameFile_shouldRejectWrongOwner() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.renameFile(fileId, UUID.randomUUID(), "x.txt"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- deleteFile ----

    @Test
    void deleteFile_shouldDeleteFromStorageAndDb() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        fileService.deleteFile(fileId, ownerId);

        verify(storageService).deleteObject(file.getObjectKey());
        verify(fileRepository).delete(file);
    }

    @Test
    void deleteFile_shouldThrowWhenNotFound() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile(fileId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getDownloadUrl ----

    @Test
    void getDownloadUrl_shouldReturnPresignedUrl() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(storageService.generatePresignedGetUrl(file.getObjectKey()))
                .thenReturn("http://localhost:9000/signed-get-url");

        String url = fileService.getDownloadUrl(fileId, buildUser(ownerId, "owner@example.com"));

        assertThat(url).isEqualTo("http://localhost:9000/signed-get-url");
    }

    @Test
    void getDownloadUrl_shouldAllowSharedUser() {
        FileMetadata file = buildActiveFile();
        file.setSharedWith(new ArrayList<>(List.of("friend@example.com")));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(storageService.generatePresignedGetUrl(file.getObjectKey()))
                .thenReturn("http://localhost:9000/signed-get-url");

        UUID friendId = UUID.randomUUID();
        String url = fileService.getDownloadUrl(fileId, buildUser(friendId, "friend@example.com"));

        assertThat(url).isEqualTo("http://localhost:9000/signed-get-url");
    }

    @Test
    void getDownloadUrl_shouldRejectUnrelatedUser() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() ->
                fileService.getDownloadUrl(fileId, buildUser(UUID.randomUUID(), "stranger@example.com")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getPresignedPutUrl_shouldRejectWrongOwner() {
        FileMetadata file = buildPendingFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.getPresignedPutUrl(fileId, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void toDto_shouldAttachOwnerAndPresignedUrl() {
        FileMetadata file = buildActiveFile();
        User owner = buildUser(ownerId, "owner@example.com");
        com.storeit.dto.FileDto dto = com.storeit.dto.FileDto.builder()
                .id(fileId)
                .ownerId(ownerId)
                .filename(file.getFilename())
                .build();

        when(fileMapper.toDto(file)).thenReturn(dto);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));
        when(storageService.generatePresignedGetUrl(file.getObjectKey()))
                .thenReturn("http://localhost:9000/signed-get-url");

        com.storeit.dto.FileDto result = fileService.toDto(file);

        assertThat(result.getOwnerName()).isEqualTo("Test User");
        assertThat(result.getOwnerEmail()).isEqualTo("owner@example.com");
        assertThat(result.getUrl()).isEqualTo("http://localhost:9000/signed-get-url");
        assertThat(result.getStorageBackend()).isEqualTo("minio");
    }

    // ---- shareFile ----

    @Test
    void shareFile_shouldUpdateSharedWith() {
        FileMetadata file = buildActiveFile();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        List<String> emails = List.of("user@example.com", "other@example.com");
        FileMetadata result = fileService.shareFile(fileId, ownerId, emails);

        assertThat(result.getSharedWith()).containsExactlyElementsOf(emails);
    }

    // ---- getSpaceUsed ----

    @Test
    void getSpaceUsed_shouldReturnUsageInfo() {
        when(fileRepository.sumSizeByOwnerId(ownerId)).thenReturn(1024L);

        FileService.SpaceInfo info = fileService.getSpaceUsed(ownerId);

        assertThat(info.usedBytes()).isEqualTo(1024L);
        assertThat(info.totalBytes()).isEqualTo(2L * 1024 * 1024 * 1024);
    }

    // ---- Helpers ----

    private User buildUser(UUID id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName("Test User")
                .passwordHash("hashed")
                .build();
    }

    private FileMetadata buildPendingFile() {
        return FileMetadata.builder()
                .id(fileId)
                .ownerId(ownerId)
                .filename("test.pdf")
                .objectKey(ownerId + "/uuid-test.pdf")
                .size(1024L)
                .contentType("application/pdf")
                .extension("pdf")
                .fileType("document")
                .status("PENDING")
                .sharedWith(new ArrayList<>())
                .build();
    }

    private FileMetadata buildActiveFile() {
        FileMetadata f = buildPendingFile();
        f.setStatus("ACTIVE");
        return f;
    }
}
