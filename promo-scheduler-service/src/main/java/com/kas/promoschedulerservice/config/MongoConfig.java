package com.kas.promoschedulerservice.config;

import com.kas.promoschedulerservice.repository.PromoDao;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class MongoConfig {

  private final MongoProps mongoProps;

  public MongoConfig(MongoProps mongoProps) {
    this.mongoProps = mongoProps;
  }

  @Bean
  public MongoClient mongoClient() {
    ConnectionString connectionString = new ConnectionString(mongoProps.server());

    CodecRegistry pojoCodecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(PojoCodecProvider.builder().automatic(true).build())
    );

    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
        .codecRegistry(pojoCodecRegistry)
        .build();

    return MongoClients.create(settings);
  }

  @Bean
  public PromoDao bookDao(MongoClient client) {
    return new PromoDao(client);
  }
}
