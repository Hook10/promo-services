package com.kas.promoservice.service;

import com.mongodb.reactivestreams.client.ClientSession;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TransactionalOperation<T> {
    Mono<T> execute(ClientSession session);
}
