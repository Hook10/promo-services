package com.kas.promoservice.dto.event;

import com.kas.promoservice.model.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromoEvent {

    private String eventId;
    private EventType eventType;
    private Instant occurredAt;
    private Payload payload;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payload {
        private String promoId;
        private String name;
        private String description;
        private Integer discountPercent;
        private List<String> bookIds;
        private Status status;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Instant timestamp;
    }

    public enum EventType {
        PROMO_CREATED,
        PROMO_UPDATED,
        PROMO_DELETED
    }
}
