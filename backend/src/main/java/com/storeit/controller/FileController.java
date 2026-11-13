package com.storeit.controller;

import com.storeit.dto.*;
import com.storeit.entity.FileMetadata;
import com.storeit.entity.User;
import com.storeit.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, download, management, and sharing")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-url")
    @Operation(summary = "Initiate file upload — returns presigned PUT URL")
    public ResponseEntity<FileUploadInitResponse> uploadUrl(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FileUploadInitRequest request) {

        FileMetadata file = fileService.initUpload(
                user.getId(), request.getFilename(),
                request.getContentType(), request.getSize());

        String presignedUrl = fileService.getPresignedPutUrl(file.getId(), user.getId());

        return ResponseEntity.ok(FileUploadInitResponse.builder()
                .fileId(file.getId())
                .presignedPutUrl(presignedUrl)
                .build());
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm file upload — verifies object in MinIO and marks ACTIVE")
    public ResponseEntity<FileDto> confirmUpload(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id) {

        FileMetadata file = fileService.confirmUpload(id, user.getId());
        return ResponseEntity.ok(fileService.toDto(file));
    }

    @GetMapping
    @Operation(summary = "List files — paginated, filterable by type(s) and search")
    public ResponseEntity<Page<FileDto>> listFiles(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "type", required = false) List<String> type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false, defaultValue = "date") String sortField,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String sortDir,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        Page<FileMetadata> files = fileService.listFiles(
                user.getId(), user.getEmail(), type, search, page, size, sortField, sortDir);
        return ResponseEntity.ok(fileService.toDtoPage(files));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single file by ID")
    public ResponseEntity<FileDto> getFile(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id) {

        FileMetadata file = fileService.getFile(id, user);
        return ResponseEntity.ok(fileService.toDto(file));
    }

    @PatchMapping("/{id}/rename")
    @Operation(summary = "Rename a file")
    public ResponseEntity<FileDto> renameFile(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id,
            @Valid @RequestBody RenameRequest request) {

        FileMetadata file = fileService.renameFile(id, user.getId(), request.getName());
        return ResponseEntity.ok(fileService.toDto(file));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file from MinIO and database")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id) {

        fileService.deleteFile(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get presigned download URL for a file")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id) {

        String url = fileService.getDownloadUrl(id, user);
        return ResponseEntity.ok(DownloadUrlResponse.builder()
                .presignedGetUrl(url)
                .build());
    }

    @PatchMapping("/{id}/share")
    @Operation(summary = "Share a file with email addresses")
    public ResponseEntity<FileDto> shareFile(
            @AuthenticationPrincipal User user,
            @PathVariable("id") java.util.UUID id,
            @Valid @RequestBody ShareRequest request) {

        FileMetadata file = fileService.shareFile(id, user.getId(), request.getEmails());
        return ResponseEntity.ok(fileService.toDto(file));
    }

    @GetMapping("/space")
    @Operation(summary = "Get storage space usage for the current user")
    public ResponseEntity<SpaceResponse> getSpace(
            @AuthenticationPrincipal User user) {

        FileService.SpaceInfo info = fileService.getSpaceUsed(user.getId());
        return ResponseEntity.ok(SpaceResponse.builder()
                .usedBytes(info.usedBytes())
                .totalBytes(info.totalBytes())
                .build());
    }
}
