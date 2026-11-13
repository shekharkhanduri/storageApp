package com.storeit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadInitResponse {

    private UUID fileId;
    private String presignedPutUrl;
}
