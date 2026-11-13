package com.storeit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeit.config.SecurityConfig;
import com.storeit.dto.*;
import com.storeit.entity.FileMetadata;
import com.storeit.entity.User;
import com.storeit.repository.UserRepository;
import com.storeit.security.JwtAuthFilter;
import com.storeit.security.JwtUtil;
import com.storeit.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileService fileService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID fileId = UUID.randomUUID();

    // ---- POST /api/files/upload-url ----

    @Test
    void uploadUrl_shouldReturnPresignedUrl() throws Exception {
        FileMetadata file = buildFile();
        when(fileService.initUpload(eq(userId), eq("test.pdf"), eq("application/pdf"), eq(1024L)))
                .thenReturn(file);
        when(fileService.getPresignedPutUrl(fileId, userId))
                .thenReturn("http://localhost:9000/presigned-put");

        mockMvc.perform(post("/api/files/upload-url")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                FileUploadInitRequest.builder()
                                        .filename("test.pdf")
                                        .contentType("application/pdf")
                                        .size(1024L)
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.presignedPutUrl").value("http://localhost:9000/presigned-put"));
    }

    // ---- POST /api/files/{id}/confirm ----

    @Test
    void confirmUpload_shouldReturnActiveFile() throws Exception {
        FileMetadata file = buildFile();
        file.setStatus("ACTIVE");
        FileDto dto = buildFileDto("ACTIVE");

        when(fileService.confirmUpload(fileId, userId)).thenReturn(file);
        when(fileService.toDto(file)).thenReturn(dto);

        mockMvc.perform(post("/api/files/{id}/confirm", fileId)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---- GET /api/files ----

    @Test
    void listFiles_shouldReturnPage() throws Exception {
        FileMetadata file = buildFile();
        file.setStatus("ACTIVE");
        FileDto dto = buildFileDto("ACTIVE");

        Page<FileMetadata> page = new PageImpl<>(List.of(file));
        Page<FileDto> dtoPage = new PageImpl<>(List.of(dto));
        when(fileService.listFiles(eq(userId), eq("test@example.com"), isNull(), isNull(),
                eq(0), eq(20), eq("date"), eq("desc")))
                .thenReturn(page);
        when(fileService.toDtoPage(any())).thenReturn(dtoPage);

        mockMvc.perform(get("/api/files")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filename").value("test.pdf"));
    }

    @Test
    void listFiles_shouldAcceptMultipleTypes() throws Exception {
        FileMetadata file = buildFile();
        file.setStatus("ACTIVE");
        FileDto dto = buildFileDto("ACTIVE");

        Page<FileMetadata> page = new PageImpl<>(List.of(file));
        Page<FileDto> dtoPage = new PageImpl<>(List.of(dto));
        when(fileService.listFiles(eq(userId), eq("test@example.com"), eq(List.of("video", "audio")),
                isNull(), eq(0), eq(20), eq("date"), eq("desc")))
                .thenReturn(page);
        when(fileService.toDtoPage(any())).thenReturn(dtoPage);

        mockMvc.perform(get("/api/files")
                        .param("type", "video,audio")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- GET /api/files/{id} ----

    @Test
    void getFile_shouldReturnFile() throws Exception {
        FileMetadata file = buildFile();
        FileDto dto = buildFileDto("ACTIVE");
        when(fileService.getFile(eq(fileId), any(User.class))).thenReturn(file);
        when(fileService.toDto(file)).thenReturn(dto);

        mockMvc.perform(get("/api/files/{id}", fileId)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("test.pdf"));
    }

    // ---- PATCH /api/files/{id}/rename ----

    @Test
    void renameFile_shouldReturnRenamedFile() throws Exception {
        FileMetadata file = buildFile();
        file.setFilename("renamed.pdf");
        FileDto dto = buildFileDto("ACTIVE");
        dto.setFilename("renamed.pdf");

        when(fileService.renameFile(fileId, userId, "renamed.pdf")).thenReturn(file);
        when(fileService.toDto(file)).thenReturn(dto);

        mockMvc.perform(patch("/api/files/{id}/rename", fileId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RenameRequest.builder().name("renamed.pdf").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("renamed.pdf"));
    }

    // ---- DELETE /api/files/{id} ----

    @Test
    void deleteFile_shouldReturn204() throws Exception {
        doNothing().when(fileService).deleteFile(fileId, userId);

        mockMvc.perform(delete("/api/files/{id}", fileId)
                        .with(authentication(auth())))
                .andExpect(status().isNoContent());
    }

    // ---- GET /api/files/{id}/download-url ----

    @Test
    void downloadUrl_shouldReturnPresignedGetUrl() throws Exception {
        when(fileService.getDownloadUrl(eq(fileId), any(User.class)))
                .thenReturn("http://localhost:9000/signed-download");

        mockMvc.perform(get("/api/files/{id}/download-url", fileId)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedGetUrl").value("http://localhost:9000/signed-download"));
    }

    // ---- PATCH /api/files/{id}/share ----

    @Test
    void shareFile_shouldReturnSharedFile() throws Exception {
        FileMetadata file = buildFile();
        file.setSharedWith(List.of("friend@example.com"));
        FileDto dto = buildFileDto("ACTIVE");
        dto.setSharedWith(List.of("friend@example.com"));

        when(fileService.shareFile(eq(fileId), eq(userId), eq(List.of("friend@example.com"))))
                .thenReturn(file);
        when(fileService.toDto(file)).thenReturn(dto);

        mockMvc.perform(patch("/api/files/{id}/share", fileId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ShareRequest.builder().emails(List.of("friend@example.com")).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedWith", hasSize(1)))
                .andExpect(jsonPath("$.sharedWith[0]").value("friend@example.com"));
    }

    // ---- GET /api/files/space ----

    @Test
    void getSpace_shouldReturnSpaceInfo() throws Exception {
        when(fileService.getSpaceUsed(userId))
                .thenReturn(new FileService.SpaceInfo(5000L, 2L * 1024 * 1024 * 1024));

        mockMvc.perform(get("/api/files/space")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedBytes").value(5000))
                .andExpect(jsonPath("$.totalBytes").value(2L * 1024 * 1024 * 1024));
    }

    // ---- Forbidden access should return 403 ----

    @Test
    void forbiddenAccess_shouldReturn403() throws Exception {
        when(fileService.getFile(eq(fileId), any(User.class)))
                .thenThrow(new com.storeit.exception.ForbiddenException("Access denied"));

        mockMvc.perform(get("/api/files/{id}", fileId)
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ---- Unauthenticated request should return 401 ----

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/files/space"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Helpers ----

    private UsernamePasswordAuthenticationToken auth() {
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .build();
        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }

    private FileMetadata buildFile() {
        return FileMetadata.builder()
                .id(fileId)
                .ownerId(userId)
                .filename("test.pdf")
                .objectKey(userId + "/uuid-test.pdf")
                .size(1024L)
                .contentType("application/pdf")
                .extension("pdf")
                .fileType("document")
                .status("ACTIVE")
                .sharedWith(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private FileDto buildFileDto(String status) {
        return FileDto.builder()
                .id(fileId)
                .ownerId(userId)
                .filename("test.pdf")
                .objectKey(userId + "/uuid-test.pdf")
                .size(1024L)
                .contentType("application/pdf")
                .extension("pdf")
                .fileType("document")
                .status(status)
                .sharedWith(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
