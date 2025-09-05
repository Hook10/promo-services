package com.kas.promoservice.repository;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;

public abstract class BaseDao<T> {

    protected final MongoClient client;
    protected final MongoCollection<T> collection;

    protected BaseDao(MongoClient client, String dbName, String collectionName, Class<T> clazz) {
        this.client = client;
        this.collection = client
                .getDatabase(dbName)
                .getCollection(collectionName, clazz);
    }

    public MongoCollection<T> getCollection() {
        return collection;
    }

    public MongoClient getClient() {
        return client;
    }
}
