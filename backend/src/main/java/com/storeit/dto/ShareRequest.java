package com.storeit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequest {

    /** Empty list is allowed — it removes all existing shares. */
    @NotNull(message = "Emails list is required (use [] to remove all shares)")
    private List<String> emails;
}
