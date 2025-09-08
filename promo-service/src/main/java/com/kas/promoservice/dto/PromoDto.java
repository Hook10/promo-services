package com.kas.promoservice.dto;

import com.kas.promoservice.model.Status;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;


public record PromoDto(
        String id,

        @NotBlank(message = "Name is required")
        String name,
        String description,

        @Min(value = 1, message = "At least must be 1")
        @Max(value = 100, message = "Max is 100")
        Integer discountPercent,

        @NotNull
        @Size(min = 1, message = "At least must be one book")
        List<String> bookIds,

        @NotNull(message = "Status is required")
        Status status,

        @NotNull(message = "Start time is required")
        LocalDateTime startsAt,

        @NotNull(message = "End time is required")
        LocalDateTime endsAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
