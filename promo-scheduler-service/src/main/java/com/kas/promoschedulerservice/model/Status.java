package com.kas.promoschedulerservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    ENABLED,
    PAUSED,
    PENDING,
    ENDED,
    DELETED;

    @JsonCreator
    public static Status fromString(String status) {
        return Status.valueOf(status.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toLowerCase();
    }
}
