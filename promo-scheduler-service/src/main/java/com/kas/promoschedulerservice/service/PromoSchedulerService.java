package com.kas.promoschedulerservice.service;

import com.kas.promoschedulerservice.dto.event.PromoEvent;
import com.kas.promoschedulerservice.model.Promo;
import com.kas.promoschedulerservice.model.Status;
import com.kas.promoschedulerservice.repository.PromoDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoSchedulerService {

  private final PromoDao promoDao;
  private final KafkaSender<String, PromoEvent> kafkaSender;

  @Scheduled(cron = "${scheduler.promo.cron:0 * * * * *}",
      initialDelayString = "${scheduler.promo.initial-delay:10000}")
  public void checkAndUpdatePromoStatuses() {
    log.info("Starting promo status check scheduler...");

    checkAndStartPromos()
        .then(checkAndEndPromos()) // Check for promos that should end
        .doOnSuccess(v -> log.info("Promo status check completed successfully"))
        .doOnError(error -> log.error("Promo status check failed: {}", error.getMessage()))
        .subscribe();
  }

  private Mono<Void> checkAndStartPromos() {
    log.debug("Checking for promos that should start...");
    LocalDateTime now = LocalDateTime.now();

    return promoDao.findPromosToStart(now)
        .flatMap(promo -> {
          if (promo.getStatus() == Status.ENABLED && promo.getStartsAt().isBefore(now)) {
            return activatePromo(promo);
          } else if (promo.getStatus() == Status.ENABLED && promo.getStartsAt().isAfter(now)) {
            return setPromoToPending(promo);
          }
          return Mono.just(promo);
        })
        .then();
  }

  private Mono<Void> checkAndEndPromos() {
    log.debug("Checking for promos that should end...");
    LocalDateTime now = LocalDateTime.now();

    return promoDao.findPromosToEnd(now)
        .flatMap(promo -> {
          if ((promo.getStatus() == Status.ENABLED || promo.getStatus() == Status.PENDING) &&
              promo.getEndsAt() != null && promo.getEndsAt().isBefore(now)) {
            // Promo should be ended
            return endPromo(promo);
          }
          return Mono.just(promo);
        })
        .then();
  }

  private Mono<Promo> activatePromo(Promo promo) {
    log.info("Activating promo: {}", promo.getId());
    promo.setStatus(Status.PENDING); // Or whatever status represents active
    promo.setUpdatedAt(LocalDateTime.now());

    return promoDao.update(promo.getId(), promo.getVersion(), promo)
        .flatMap(updatedPromo ->
            sendPromoEvent(updatedPromo, PromoEvent.EventType.PROMO_UPDATED, "Promo activated")
        )
        .doOnSuccess(updated -> log.info("Promo {} activated successfully", promo.getId()))
        .doOnError(error -> log.error("Failed to activate promo {}: {}", promo.getId(), error.getMessage()));
  }

  private Mono<Promo> setPromoToPending(Promo promo) {
    log.info("Setting promo to pending: {}", promo.getId());
    promo.setStatus(Status.PENDING);
    promo.setUpdatedAt(LocalDateTime.now());

    return promoDao.update(promo.getId(), promo.getVersion(), promo)
        .flatMap(updatedPromo ->
            sendPromoEvent(updatedPromo, PromoEvent.EventType.PROMO_UPDATED, "Promo set to pending")
        );
  }

  private Mono<Promo> endPromo(Promo promo) {
    log.info("Ending promo: {}", promo.getId());
    promo.setStatus(Status.ENDED);
    promo.setUpdatedAt(LocalDateTime.now());

    return promoDao.update(promo.getId(), promo.getVersion(), promo)
        .flatMap(updatedPromo ->
            sendPromoEvent(updatedPromo, PromoEvent.EventType.PROMO_UPDATED, "Promo ended")
        )
        .doOnSuccess(updated -> log.info("Promo {} ended successfully", promo.getId()))
        .doOnError(error -> log.error("Failed to end promo {}: {}", promo.getId(), error.getMessage()));
  }

  private Mono<Promo> sendPromoEvent(Promo promo, PromoEvent.EventType eventType, String description) {
    PromoEvent event = buildPromoEvent(promo, eventType, description);
    SenderRecord<String, PromoEvent, String> senderRecord = SenderRecord.create(
        "promo-topic",
        null,
        System.currentTimeMillis(),
        promo.getId(),
        event,
        null
    );

    return Mono.from(kafkaSender.send(Mono.just(senderRecord)))
        .thenReturn(promo)
        .doOnNext(result -> log.debug("Sent {} event for promo: {}", eventType, promo.getId()))
        .onErrorResume(error -> {
          log.error("Failed to send Kafka event for promo {}: {}", promo.getId(), error.getMessage());
          return Mono.just(promo); // Continue even if Kafka fails
        });
  }

  private PromoEvent buildPromoEvent(Promo promo, PromoEvent.EventType eventType, String description) {
    PromoEvent event = new PromoEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setEventType(eventType);
    event.setOccurredAt(Instant.now());

    PromoEvent.Payload payload = new PromoEvent.Payload();
    payload.setPromoId(promo.getId());
    payload.setName(promo.getName());
    payload.setDescription(description);
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

