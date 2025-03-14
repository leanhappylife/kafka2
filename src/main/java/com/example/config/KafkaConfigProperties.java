package com.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka-config")
public class KafkaConfigProperties {
    private String bootstrapServers;
    private Map<String, String> consumerDefaults;
    private Producers producers;
    private Consumers consumers;

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public Map<String, String> getConsumerDefaults() { return consumerDefaults; }
    public void setConsumerDefaults(Map<String, String> consumerDefaults) { this.consumerDefaults = consumerDefaults; }
    public Producers getProducers() { return producers; }
    public void setProducers(Producers producers) { this.producers = producers; }
    public Consumers getConsumers() { return consumers; }
    public void setConsumers(Consumers consumers) { this.consumers = consumers; }

    public static class Producers {
        private ProducerProperties shareBrokerOrderDomainEvent;
        public ProducerProperties getShareBrokerOrderDomainEvent() { return shareBrokerOrderDomainEvent; }
        public void setShareBrokerOrderDomainEvent(ProducerProperties shareBrokerOrderDomainEvent) { this.shareBrokerOrderDomainEvent = shareBrokerOrderDomainEvent; }

        public static class ProducerProperties {
            private String clientId;
            private int retries;
            private String acks;
            public String getClientId() { return clientId; }
            public void setClientId(String clientId) { this.clientId = clientId; }
            public int getRetries() { return retries; }
            public void setRetries(int retries) { this.retries = retries; }
            public String getAcks() { return acks; }
            public void setAcks(String acks) { this.acks = acks; }
        }
    }

    public static class Consumers {
        private ConsumerProperties fixGateway;
        public ConsumerProperties getFixGateway() { return fixGateway; }
        public void setFixGateway(ConsumerProperties fixGateway) { this.fixGateway = fixGateway; }

        public static class ConsumerProperties {
            private String clientId;
            private String groupId;
            private String autoOffsetReset;
            private Map<String, String> properties;
            public String getClientId() { return clientId; }
            public void setClientId(String clientId) { this.clientId = clientId; }
            public String getGroupId() { return groupId; }
            public void setGroupId(String groupId) { this.groupId = groupId; }
            public String getAutoOffsetReset() { return autoOffsetReset; }
            public void setAutoOffsetReset(String autoOffsetReset) { this.autoOffsetReset = autoOffsetReset; }
            public Map<String, String> getProperties() { return properties; }
            public void setProperties(Map<String, String> properties) { this.properties = properties; }
        }
    }

    @PostConstruct
    public void init() {
        System.out.println("Bootstrap servers: " + bootstrapServers);
    }
}
