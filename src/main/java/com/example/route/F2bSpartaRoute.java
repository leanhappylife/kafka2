package com.example.route;

import com.example.config.KafkaConfigProperties;
import com.example.config.TopicsProperties;
import com.example.config.KafkaClientIdManager;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

            // Retrieve f2b channel
            Map<String, TopicsProperties.TopicInfo> f2bTopics = channels.get("f2b");
            if (f2bTopics == null || f2bTopics.isEmpty()) {
                log.warn("Product [{}] has no f2b channel configuration.", productName);
                continue;
            }

            // Compute max concurrency and remove duplicate topics
            Set<String> topicSet = new HashSet<>();
            int maxConcurrency = 1; // Ensure at least 1 consumer
            for (TopicsProperties.TopicInfo topicInfo : f2bTopics.values()) {
                topicSet.add(topicInfo.getName());
                if (topicInfo.getConcurrency() > maxConcurrency) {
                    maxConcurrency = topicInfo.getConcurrency();
                }
            }
            if (topicSet.isEmpty()) {
                log.warn("Product [{}] has no topics configured for the f2b channel.", productName);
                continue;
            }
            String joinedTopics = String.join(",", topicSet);

            // Generate a unique clientId (prefer reuse, avoid MBean conflict)
            String clientId = KafkaClientIdManager.getUniqueClientId(productName);
            String groupId = productName + "-f2b-consumer-group";

            // Retrieve global Kafka consumer configuration
            Map<String, String> consumerDefaults = kafkaConfigProperties.getConsumerDefaults();
            String autoOffsetReset = consumerDefaults.get("auto-offset-reset");
            String securityProtocol = consumerDefaults.get("security-protocol");
            String saslMechanism = consumerDefaults.get("sasl-mechanism");
            String saslJaasConfig = consumerDefaults.get("sasl-jaas-config");
            String jmxEnabled = consumerDefaults.getOrDefault("jmxEnabled", "true"); // JMX enabled by default, can be disabled

            // Construct Kafka URI
            String kafkaUri = String.format("kafka:%s?brokers=%s" +
                            "&groupId=%s" +
                            "&clientId=%s" +
                            "&autoOffsetReset=%s" +
                            "&securityProtocol=%s" +
                            "&saslMechanism=%s" +
                            "&saslJaasConfig=%s" +
                            "&concurrentConsumers=%d" +
                            "&jmxEnabled=%s", // Allow JMX disabling
                    joinedTopics,
                    kafkaConfigProperties.getBootstrapServers(),
                    groupId,
                    clientId,
                    autoOffsetReset,
                    securityProtocol,
                    saslMechanism,
                    saslJaasConfig,
                    maxConcurrency,
                    jmxEnabled);

            // Define route ID
            String routeId = "F2B_" + productName;

            from(kafkaUri)
                    .routeId(routeId)
//                    .log(LoggingLevel.INFO, "Product [{}] f2b route received a message from topic [{}]: {}", productName, "${header.CamelKafkaTopic}", "${body}")
                    .to("direct:process");
        }
    }
}
