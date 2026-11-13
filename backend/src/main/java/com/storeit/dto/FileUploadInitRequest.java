package com.storeit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadInitRequest {

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @Min(value = 1, message = "Size must be positive")
    private long size;
}
