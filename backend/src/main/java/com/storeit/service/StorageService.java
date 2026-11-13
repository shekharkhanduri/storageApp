package com.storeit.service;

import com.storeit.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.presigned-url-expiry}")
    private long presignedUrlExpiry;

    @Value("${app.minio.download-url-expiry}")
    private long downloadUrlExpiry;

    /**
     * Generate a presigned PUT URL for uploading a file directly to MinIO.
     */
    public String generatePresignedPutUrl(String objectKey, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpiry))
                    .putObjectRequest(putRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned PUT URL", e);
        }
    }

    /**
     * Generate a presigned GET URL for downloading a file from MinIO.
     */
    public String generatePresignedGetUrl(String objectKey) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(downloadUrlExpiry))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned GET URL", e);
        }
    }

    /**
     * Delete an object from MinIO.
     */
    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            log.info("Deleted object: {}", objectKey);
        } catch (Exception e) {
            throw new StorageException("Failed to delete object: " + objectKey, e);
        }
    }

    /**
     * Check if an object exists in MinIO and return its size.
     * Returns the content length if the object exists.
     */
    public long headObject(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
            return response.contentLength();
        } catch (NoSuchKeyException e) {
            throw new StorageException("Object not found: " + objectKey);
        } catch (Exception e) {
            throw new StorageException("Failed to head object: " + objectKey, e);
        }
    }
}
