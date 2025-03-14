package com.example.route;

import com.example.config.KafkaConfigProperties;
import com.example.config.TopicsProperties;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class F2bSpartaRoute extends RouteBuilder {

    @Autowired
    private KafkaConfigProperties kafkaConfigProperties;

    @Autowired
    private TopicsProperties topicsProperties;

    @Override
    public void configure() throws Exception {
        Map<String, Map<String, Map<String, TopicsProperties.TopicInfo>>> productTopics = topicsProperties.getProduct();
        if (productTopics == null || productTopics.isEmpty()) {
            log.warn("No product configuration found.");
            return;
        }

        for (Map.Entry<String, Map<String, Map<String, TopicsProperties.TopicInfo>>> productEntry : productTopics.entrySet()) {
            String productName = productEntry.getKey();
            Map<String, Map<String, TopicsProperties.TopicInfo>> channels = productEntry.getValue();
            if (channels == null || channels.isEmpty()) {
                log.warn("Product [{}] has no channel configuration.", productName);
                continue;
            }

            Map<String, TopicsProperties.TopicInfo> f2bTopics = channels.get("f2b");
            if (f2bTopics == null || f2bTopics.isEmpty()) {
                log.warn("Product [{}] has no f2b channel configuration.", productName);
                continue;
            }

            for (Map.Entry<String, TopicsProperties.TopicInfo> topicEntry : f2bTopics.entrySet()) {
                String topicName = topicEntry.getValue().getName();
                int concurrency = Math.max(topicEntry.getValue().getConcurrency(), 1); // Ensure at least 1 consumer

                String groupId = productName + "-f2b-consumer-group";

                Map<String, String> consumerDefaults = kafkaConfigProperties.getConsumerDefaults();
                String autoOffsetReset = consumerDefaults.get("auto-offset-reset");
                String securityProtocol = consumerDefaults.get("security-protocol");
                String saslMechanism = consumerDefaults.get("sasl-mechanism");
                String saslJaasConfig = consumerDefaults.get("sasl-jaas-config");
                String jmxEnabled = consumerDefaults.getOrDefault("jmxEnabled", "true"); // JMX enabled by default

                // 🔥 Manually create multiple consumers
                for (int i = 0; i < concurrency; i++) {
                    String clientId = String.format("%s-f2b-client-%d-%s", productName, i, UUID.randomUUID().toString().substring(0, 8)); // Unique clientId for each consumer

                    String kafkaUri = String.format("kafka:%s?brokers=%s" +
                                    "&groupId=%s" +
                                    "&clientId=%s" +
                                    "&autoOffsetReset=%s" +
                                    "&securityProtocol=%s" +
                                    "&saslMechanism=%s" +
                                    "&saslJaasConfig=%s" +
                                    "&concurrentConsumers=1" ,
                            topicName,
                            kafkaConfigProperties.getBootstrapServers(),
                            groupId,
                            clientId,
                            autoOffsetReset,
                            securityProtocol,
                            saslMechanism,
                            saslJaasConfig,
                            jmxEnabled);

                    String routeId = String.format("F2B_%s_%s_%d", productName, topicName, i);

                    from(kafkaUri)
                            .routeId(routeId)

                            .to("direct:process");
                }
            }
        }
    }
}
