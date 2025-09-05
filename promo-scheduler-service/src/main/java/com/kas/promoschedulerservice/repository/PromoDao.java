package com.kas.promoschedulerservice.repository;


import com.kas.promoschedulerservice.model.Promo;
import com.kas.promoschedulerservice.model.Status;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class PromoDao extends BaseDao<Promo> {

    public PromoDao(MongoClient mongoClient) {
        super(mongoClient, "promo_db", "promos", Promo.class);
    }

    public Mono<Promo> save(Promo promo) {
        if (promo.getId() == null) {
            promo.setId(UUID.randomUUID().toString());
        }
        return Mono.from(collection.insertOne(promo))
                .then(Mono.just(promo));
    }

    public Mono<Promo> saveWithSession(Promo promo, ClientSession session) {
        if (promo.getVersion() == null) {
            promo.setVersion(0L);
        }
        if (promo.getId() == null) {
            promo.setId(UUID.randomUUID().toString());
        }
        return Mono.from(collection.insertOne(session, promo))
                .then(Mono.just(promo));
    }

    public Flux<Promo> pagination(int page, int size) {
        return Flux.from(collection.find().skip(page).limit(size));
    }

    public Mono<Promo> findById(String id) {
        Bson filter = Filters.eq("_id", id);
        return Mono.from(collection.find(filter));
    }

    public Mono<Promo> findByIdWithSession(String id, ClientSession session) {
        Bson filter = Filters.eq("_id", id);
        return Mono.from(collection.find(session, filter));
    }

    public Mono<Promo> updateWithSession(String id, long expectedVersion, Promo promo, ClientSession session) {
        Bson filter = Filters.and(
                Filters.eq("_id", id),
                Filters.eq("version", expectedVersion)
        );
        Bson updates = Updates.combine(
                Updates.set("name", promo.getName()),
                Updates.set("description", promo.getDescription()),
                Updates.set("discountPercent", promo.getDiscountPercent()),
                Updates.set("bookIds", promo.getBookIds()),
                Updates.set("status", promo.getStatus()),
                Updates.set("startsAt", promo.getStartsAt()),
                Updates.set("endsAt", promo.getEndsAt()),
                Updates.set("createdAt", promo.getCreatedAt()),
                Updates.set("updatedAt", promo.getUpdatedAt()),
                Updates.set("version", expectedVersion + 1)
        );
        return Mono.from(collection.findOneAndUpdate(session, filter, updates))
                .flatMap(updatedDoc -> {
                    if (updatedDoc == null) {
                        return Mono.error(new IllegalStateException("Optimistic lock failed for promo " + id));
                    }
                    return findByIdWithSession(id, session);
                });
    }

    public Mono<Boolean> deleteWithSession(String id, ClientSession session) {
        return Mono.from(collection.deleteOne(session, Filters.eq("_id", id)))
                .map(result -> result.getDeletedCount() == 1)
                .defaultIfEmpty(false);
    }

    public Mono<ClientSession> startSession() {
        return Mono.from(client.startSession());
    }

    public Flux<Promo> findPromosToStart(LocalDateTime currentTime) {
        Bson filter = Filters.and(
            Filters.in("status", Status.ENABLED.name(), Status.PENDING.name()),
            Filters.lte("startsAt", currentTime),
            Filters.or(
                Filters.exists("endsAt", false),
                Filters.gt("endsAt", currentTime)
            )
        );
        return Flux.from(collection.find(filter));
    }

    public Flux<Promo> findPromosToEnd(LocalDateTime currentTime) {
        Bson filter = Filters.and(
            Filters.in("status", Status.ENABLED.name(), Status.PENDING.name()),
            Filters.lte("endsAt", currentTime)
        );
        return Flux.from(collection.find(filter));
    }

    public Mono<Promo> update(String id, Long expectedVersion, Promo promo) {
        Bson filter = Filters.and(
            Filters.eq("_id", id),
            Filters.eq("version", expectedVersion)
        );

        Bson updates = Updates.combine(
            Updates.set("name", promo.getName()),
            Updates.set("description", promo.getDescription()),
            Updates.set("discountPercent", promo.getDiscountPercent()),
            Updates.set("bookIds", promo.getBookIds()),
            Updates.set("status", promo.getStatus()),
            Updates.set("startsAt", promo.getStartsAt()),
            Updates.set("endsAt", promo.getEndsAt()),
            Updates.set("updatedAt", promo.getUpdatedAt()),
            Updates.set("version", expectedVersion + 1)
        );

        return Mono.from(collection.findOneAndUpdate(filter, updates))
            .flatMap(updatedDoc -> {
                if (updatedDoc == null) {
                    return Mono.error(new IllegalStateException("Optimistic lock failed for promo " + id));
                }
                return findById(id);
            });
    }
}
