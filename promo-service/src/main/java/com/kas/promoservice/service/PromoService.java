package com.kas.promoservice.service;

import com.kas.promoservice.dto.PromoDto;
import com.kas.promoservice.dto.event.PromoEvent;
import com.kas.promoservice.exception.PromoNotFoundException;
import com.kas.promoservice.model.Promo;
import com.kas.promoservice.repository.PromoDao;
import com.kas.promoservice.util.mapper.PromoMapper;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PromoService {

  private final PromoDao promoDao;
  private final PromoMapper promoMapper;
  private final KafkaSender<String, PromoEvent> kafkaSender;

  public Mono<PromoDto> savePromo(PromoDto promoDto) {
    log.info("Save promo: {}", promoDto);

    Promo entity = promoMapper.toEntity(promoDto);
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    if (entity.getVersion() == null) {
      entity.setVersion(0L);
    }
    entity.setCreatedAt(LocalDateTime.now());
    entity.setUpdatedAt(LocalDateTime.now());

    return promoDao.save(entity)
        .doOnSuccess(saved -> sendKafkaEvent(entity, PromoEvent.EventType.PROMO_CREATED))
        .map(promoMapper::toDto);
  }

  public Mono<PromoDto> updatePromo(PromoDto promoDto, String id) {
    log.info("Update promo with id: {}", id);

    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .flatMap(existing -> {
          Promo updated = promoMapper.toEntity(promoDto);
          updated.setId(id);
          updated.setCreatedAt(existing.getCreatedAt());
          updated.setUpdatedAt(LocalDateTime.now());
          updated.setVersion(existing.getVersion() + 1);

          // Just reuse save for simplicity (no explicit optimistic lock here)
          return promoDao.save(updated)
              .doOnSuccess(saved -> sendKafkaEvent(saved, PromoEvent.EventType.PROMO_UPDATED))
              .map(promoMapper::toDto);
        });
  }

  public Mono<Void> deletePromo(String id) {
    log.info("Delete promo with id: {}", id);

    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .flatMap(existing ->
            Mono.from(promoDao.getCollection().deleteOne(Filters.eq("_id", id)))
                .doOnSuccess(result -> {
                  if (result.getDeletedCount() == 1) {
                    sendKafkaEvent(existing, PromoEvent.EventType.PROMO_DELETED);
                  }
                })
        )
        .then();
  }

  public Flux<PromoDto> getPaginatedPromos(int page, int size) {
    return promoDao.pagination(page, size)
        .map(promoMapper::toDto);
  }

  public Mono<PromoDto> getPromoById(String id) {
    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .map(promoMapper::toDto);
  }

  private void sendKafkaEvent(Promo promo, PromoEvent.EventType type) {
    PromoEvent event = buildPromoEvent(promo, type);
    SenderRecord<String, PromoEvent, String> record =
        SenderRecord.create("promo-topic", null, System.currentTimeMillis(), promo.getId(), event, null);

    kafkaSender.send(Mono.just(record))
        .doOnError(e -> log.error("Failed to send Kafka event: {}", e.getMessage()))
        .subscribe();
  }

  private PromoEvent buildPromoEvent(Promo promo, PromoEvent.EventType type) {
    PromoEvent event = new PromoEvent();
    event.setEventId(promo.getId());
    event.setEventType(type);
    event.setOccurredAt(Instant.now());

    PromoEvent.Payload payload = new PromoEvent.Payload();
    payload.setPromoId(promo.getId());
    payload.setName(promo.getName());
    payload.setDescription(promo.getDescription());
    payload.setDiscountPercent(promo.getDiscountPercent());
    payload.setBookIds(promo.getBookIds());
    payload.setStatus(promo.getStatus());
    payload.setStartsAt(promo.getStartsAt());
    payload.setEndsAt(promo.getEndsAt());
    payload.setCreatedAt(promo.getCreatedAt());
    payload.setUpdatedAt(promo.getUpdatedAt());
    payload.setTimestamp(Instant.now());

    event.setPayload(payload);
    return event;
  }
}
