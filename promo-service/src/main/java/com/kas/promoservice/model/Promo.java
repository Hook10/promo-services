package com.kas.promoservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promo {

    @BsonId
    private String id;
    private String name;
    private String description;
    private int discountPercent;
    private List<String> bookIds;
    private Status status;
    private LocalDateTime startsAt = LocalDateTime.now();
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
