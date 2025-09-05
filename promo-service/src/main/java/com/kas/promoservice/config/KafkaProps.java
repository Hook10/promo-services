package com.kas.promoservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@ConfigurationProperties(prefix = "kafka")
public record KafkaProps(
        String bootstrapServers,
        String securityProtocol,
        String saslMechanism,
        String saslJaasConfig,
        String clientId,
        Topics topics,
        Producer producer
) {
    public record Topics(
            String promo
    ){}
    public record Producer(
            String acks,
            Integer retries,
            Integer retryBackoffMsConfig,
            Integer requestTimeoutMsConfig,
            String keySerializerClassConfig,
            String valueSerializerClassConfig
    ){}
}
