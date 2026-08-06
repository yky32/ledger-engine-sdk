package com.altech.ledger.sdk.kafka;

import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerNetworkException;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Channel 2 — Kafka MQ publish of {@link TransactionalEvent} to ledger-engine topic.
 * <p>
 * Requires {@code org.apache.kafka:kafka-clients} on the classpath (optional Maven dependency).
 * Engine must have {@code ledger.integration.kafka.enabled=true}.
 */
public final class KafkaLedgerClient implements AutoCloseable {
    private final LedgerClientConfig config;
    private final ObjectMapper mapper;
    private final KafkaProducer<String, String> producer;

    public KafkaLedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        if (config.getKafkaBootstrapServers() == null || config.getKafkaBootstrapServers().isBlank()) {
            throw new IllegalArgumentException("kafkaBootstrapServers is required for Kafka channel");
        }
        ensureKafkaOnClasspath();
        this.mapper = JsonSupport.mapper();
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        config.getKafkaExtra().forEach(props::put);
        this.producer = new KafkaProducer<>(props);
    }

    private static void ensureKafkaOnClasspath() {
        try {
            Class.forName("org.apache.kafka.clients.producer.KafkaProducer");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                "Kafka channel requires org.apache.kafka:kafka-clients on the classpath. "
                    + "Add the dependency (it is optional for REST-only consumers). "
                    + "CLI fat jar (classifier 'cli') already includes Kafka.",
                ex);
        }
    }

    /**
     * Publish event asynchronously; returns Future of broker metadata.
     * Engine processes asynchronously — no IngestionResult in this channel.
     */
    public Future<RecordMetadata> publishAsync(TransactionalEvent event) {
        event.validate();
        try {
            String json = mapper.writeValueAsString(event);
            // Key by userId for per-user ordering on a single partition
            String key = event.getUserId() != null ? event.getUserId() : event.getEventId();
            ProducerRecord<String, String> record = new ProducerRecord<>(
                config.getKafkaTopic(), key, json);
            return producer.send(record);
        } catch (Exception ex) {
            throw new LedgerException("Kafka serialize/publish failed: " + ex.getMessage(), ex);
        }
    }

    /** Publish and wait for broker ack. */
    public PublishResult publish(TransactionalEvent event) {
        try {
            RecordMetadata meta = publishAsync(event)
                .get(config.getHttpTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return new PublishResult(event.getEventId(), meta.topic(), meta.partition(), meta.offset());
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerNetworkException("Kafka publish wait failed: " + ex.getMessage(), ex);
        }
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        producer.close(Duration.ofSeconds(10));
    }
}
