package com.storeit.service;

import com.storeit.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        setField(storageService, "bucket", "storeit-files");
        setField(storageService, "presignedUrlExpiry", 900L);
        setField(storageService, "downloadUrlExpiry", 300L);
    }

    @Test
    void generatePresignedPutUrl_shouldReturnUrl() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("http://localhost:9000/storeit-files/test-key?signed").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generatePresignedPutUrl("test-key", "application/pdf");

        assertThat(url).contains("storeit-files").contains("test-key");
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void generatePresignedGetUrl_shouldReturnUrl() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("http://localhost:9000/storeit-files/test-key?signed-get").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generatePresignedGetUrl("test-key");

        assertThat(url).contains("storeit-files").contains("test-key");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void deleteObject_shouldCallS3Delete() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.deleteObject("some/key.txt");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteObject_shouldThrowStorageException_onFailure() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        assertThatThrownBy(() -> storageService.deleteObject("bad-key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to delete object");
    }

    @Test
    void headObject_shouldReturnContentLength() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(12345L)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        long size = storageService.headObject("some/key.txt");

        assertThat(size).isEqualTo(12345L);
        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void headObject_shouldThrowStorageException_whenObjectNotFound() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        assertThatThrownBy(() -> storageService.headObject("missing-key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Object not found");
    }

    // ---- Utility: set @Value fields via reflection ----
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
