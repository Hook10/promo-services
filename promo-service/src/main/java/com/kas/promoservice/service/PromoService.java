package com.kas.promoservice.service;

import com.kas.promoservice.dto.PromoDto;
import com.kas.promoservice.dto.event.PromoEvent;
import com.kas.promoservice.exception.KafkaSendException;
import com.kas.promoservice.exception.PromoNotFoundException;
import com.kas.promoservice.exception.TransactionFailedException;
import com.kas.promoservice.model.Promo;
import com.kas.promoservice.repository.PromoDao;
import com.kas.promoservice.util.mapper.PromoMapper;
import com.mongodb.reactivestreams.client.ClientSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.TransactionManager;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PromoService {

    private final PromoDao promoDao;
    private final PromoMapper promoMapper;
    private final KafkaSender<String, PromoEvent> kafkaSender;


  public Mono<PromoDto> savePromo(PromoDto promoDto) {
    log.info("Save promo with Kafka transaction: {}", promoDto);
    Promo entity = promoMapper.toEntity(promoDto);
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }

    TransactionManager transactionManager = kafkaSender.transactionManager();
    return executeInKafkaTransaction(entity, transactionManager)
        .map(promoMapper::toDto);
  }

  private Mono<Promo> executeInKafkaTransaction(Promo entity, TransactionManager transactionManager) {
    return transactionManager.begin()
        .then(Mono.defer(() ->
            executeInMongoTransaction(entity)
                .flatMap(savedPromo ->
                    sendKafkaEventInTransaction(savedPromo, PromoEvent.EventType.PROMO_CREATED)
                        .thenReturn(savedPromo)
                )
        ))
        .flatMap(result ->
            transactionManager.commit()
                .thenReturn(result)
        )
        .onErrorResume(error -> {
          log.error("Kafka transaction failed, aborting: {}", error.getMessage());
          return transactionManager.abort()
              .then(Mono.error(new TransactionFailedException("Kafka transaction failed: " + error.getMessage(), error)));
        });
  }

  private Mono<Promo> executeInMongoTransaction(Promo entity) {
    return promoDao.startSession()
        .flatMap(session -> {
          session.startTransaction();
          return promoDao.saveWithSession(entity, session)
              .flatMap(savedPromo ->
                  Mono.from(session.commitTransaction())
                      .thenReturn(savedPromo)
              )
              .onErrorResume(error -> {
                log.error("MongoDB transaction failed, aborting: {}", error.getMessage());
                return Mono.from(session.abortTransaction())
                    .then(Mono.error(new TransactionFailedException("MongoDB transaction failed: " + error.getMessage(), error)));
              })
              .doFinally(signal -> session.close());
        });
  }

  private Mono<Void> sendKafkaEventInTransaction(Promo promo, PromoEvent.EventType type) {
    PromoEvent event = buildPromoEvent(promo, type);
    SenderRecord<String, PromoEvent, String> senderRecord =
        createSenderRecord(promo.getId(), event);

    return kafkaSender.sendTransactionally(
            Mono.just(Mono.just(senderRecord))
        )
        .timeout(Duration.ofSeconds(10))
        .onErrorMap(error -> new KafkaSendException(
            "Failed to send Kafka message: " + error.getMessage(), error
        ))
        .then();
  }



  public Mono<PromoDto> updatePromo(PromoDto promoDto, String id) {
    log.info("Update promo with Kafka transaction: {}", id);

    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .flatMap(existing -> {
          Promo updatedEntity = promoMapper.toEntity(promoDto);
          updatedEntity.setId(id);
          updatedEntity.setVersion(existing.getVersion());

          TransactionManager transactionManager = kafkaSender.transactionManager();
          return executeUpdateInKafkaTransaction(existing, updatedEntity, transactionManager)
              .map(promoMapper::toDto);
        });
  }

  private Mono<Promo> executeUpdateInKafkaTransaction(Promo existing, Promo updated, TransactionManager transactionManager) {
    return transactionManager.begin()
        .then(Mono.defer(() ->
            executeMongoUpdate(existing.getId(), existing.getVersion(), updated)
                .flatMap(updatedPromo ->
                    sendKafkaEventInTransaction(updatedPromo, PromoEvent.EventType.PROMO_UPDATED)
                        .thenReturn(updatedPromo)
                )
        ))
        .flatMap(result ->
            transactionManager.commit()
                .thenReturn(result)
        )
        .onErrorResume(error -> {
          log.error("Kafka transaction failed for update, aborting: {}", error.getMessage());
          return transactionManager.abort()
              .then(Mono.error(new TransactionFailedException("Kafka transaction failed for update: " + error.getMessage(), error)));
        });
  }

  private Mono<Promo> executeMongoUpdate(String id, Long expectedVersion, Promo updated) {
    return promoDao.startSession()
        .flatMap(session -> {
          session.startTransaction();
          return promoDao.updateWithSession(id, expectedVersion, updated, session)
              .flatMap(updatedPromo ->
                  Mono.from(session.commitTransaction())
                      .thenReturn(updatedPromo)
              )
              .onErrorResume(error -> {
                log.error("MongoDB update transaction failed, aborting: {}", error.getMessage());
                return Mono.from(session.abortTransaction())
                    .then(Mono.error(new TransactionFailedException("MongoDB update transaction failed: " + error.getMessage(), error)));
              })
              .doFinally(signal -> session.close());
        });
  }

  public Mono<Void> deletePromo(String id) {
    log.info("Delete promo with Kafka transaction: {}", id);

    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .flatMap(existing -> {
          TransactionManager transactionManager = kafkaSender.transactionManager();
          return executeDeleteInKafkaTransaction(existing, transactionManager);
        });
  }

  private Mono<Void> executeDeleteInKafkaTransaction(Promo promo, TransactionManager transactionManager) {
    return transactionManager.begin()
        .then(Mono.defer(() ->
            executeMongoDelete(promo.getId())
                .then(sendKafkaEventInTransaction(promo, PromoEvent.EventType.PROMO_DELETED))
        ))
        .then(transactionManager.commit())
        .onErrorResume(error -> {
          log.error("Kafka transaction failed for delete, aborting: {}", error.getMessage());
          return transactionManager.abort()
              .then(Mono.error(new TransactionFailedException("Kafka transaction failed for delete: " + error.getMessage(), error)));
        }).then();
  }

  private Mono<Boolean> executeMongoDelete(String id) {
    return promoDao.startSession()
        .flatMap(session -> {
          session.startTransaction();
          return promoDao.deleteWithSession(id, session)
              .flatMap(deleted ->
                  Mono.from(session.commitTransaction())
                      .thenReturn(deleted)
              )
              .onErrorResume(error -> {
                log.error("MongoDB delete transaction failed, aborting: {}", error.getMessage());
                return Mono.from(session.abortTransaction())
                    .then(Mono.error(new TransactionFailedException("MongoDB delete transaction failed: " + error.getMessage(), error)));
              })
              .doFinally(signal -> session.close());
        });
  }

  public Flux<PromoDto> getPaginatedPromos(int page, int size) {
    log.info("Get paginated promos with page {} and size {}", page, size);
    return promoDao.pagination(page, size)
        .map(promoMapper::toDto);
  }

  public Mono<PromoDto> getPromoById(String id) {
    log.info("Get promo by id {}", id);
    return promoDao.findById(id)
        .switchIfEmpty(Mono.error(new PromoNotFoundException("Promo not found with id " + id)))
        .map(promoMapper::toDto);
  }

  private SenderRecord<String, PromoEvent, String> createSenderRecord(String key, PromoEvent event) {
    return SenderRecord.create(
        "promo-topic",
        null,
        System.currentTimeMillis(),
        key,
        event,
        null
    );
  }

  private PromoEvent buildPromoEvent(Promo promo, PromoEvent.EventType eventType) {
    PromoEvent event = new PromoEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setEventType(eventType);
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
