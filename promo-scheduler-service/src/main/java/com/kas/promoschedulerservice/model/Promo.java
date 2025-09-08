package com.kas.promoschedulerservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.ZoneOffset.UTC;

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
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private Long version;

}
