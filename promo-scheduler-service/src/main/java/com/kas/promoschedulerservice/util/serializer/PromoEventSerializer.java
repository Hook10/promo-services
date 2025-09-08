package com.kas.promoschedulerservice.util.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kas.promoschedulerservice.dto.event.PromoEvent;
import org.apache.kafka.common.serialization.Serializer;

public class PromoEventSerializer implements Serializer<PromoEvent> {
    private final ObjectMapper objectMapper;

    public PromoEventSerializer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public byte[] serialize(String topic, PromoEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing BookEvent", e);
        }
    }
}
