package com.altech.ledger.sdk.kafka;

/**
 * Broker ack for a published {@link com.altech.ledger.sdk.model.TransactionalEvent}.
 * Hides Kafka {@code RecordMetadata} from product code.
 */
public final class PublishResult {
    private final String eventId;
    private final String topic;
    private final int partition;
    private final long offset;

    public PublishResult(String eventId, String topic, int partition, long offset) {
        this.eventId = eventId;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
    }

    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public int getPartition() { return partition; }
    public long getOffset() { return offset; }

    @Override
    public String toString() {
        return "PublishResult{eventId='" + eventId + "', topic='" + topic
            + "', partition=" + partition + ", offset=" + offset + "}";
    }
}
