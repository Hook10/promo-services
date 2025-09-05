package com.kas.promoservice.config;

import com.kas.promoservice.dto.event.PromoEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProps kafkaProps;

    public KafkaConfig(KafkaProps kafkaProps) {
        this.kafkaProps = kafkaProps;
    }

    @Bean
    public KafkaSender<String, PromoEvent> kafkaSender() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.bootstrapServers());
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProps.clientId());
        configProps.put(ProducerConfig.ACKS_CONFIG, kafkaProps.producer().acks());
        configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaProps.producer().retries());
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, kafkaProps.producer().retryBackoffMsConfig());
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaProps.producer().requestTimeoutMsConfig());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProps.producer().keySerializerClassConfig());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProps.producer().valueSerializerClassConfig());
        configProps.put("security.protocol", kafkaProps.securityProtocol());
        configProps.put("sasl.mechanism", kafkaProps.saslMechanism());
        configProps.put("sasl.jaas.config", kafkaProps.saslJaasConfig());

        SenderOptions<String, PromoEvent> senderOptions = SenderOptions.create(configProps);
        return KafkaSender.create(senderOptions);
    }
}
