package com.example.route;

import com.example.config.KafkaConfigProperties;
import com.example.config.TopicsProperties;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
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

        // Retrieve product-level configurations from the TopicsProperties bean
        Map<String, Map<String, Map<String, TopicsProperties.TopicInfo>>> productTopics = topicsProperties.getProduct();
        if (productTopics == null || productTopics.isEmpty()) {
            log.warn("No product configuration found.");
            return;
        }

        // Iterate over all products
        for (Map.Entry<String, Map<String, Map<String, TopicsProperties.TopicInfo>>> productEntry : productTopics.entrySet()) {
            String productName = productEntry.getKey();
            Map<String, Map<String, TopicsProperties.TopicInfo>> channels = productEntry.getValue();
            if (channels == null || channels.isEmpty()) {
                log.warn("Product [{}] has no channel configuration.", productName);
                continue;
            }

            // Only process the 'f2b' channel
            Map<String, TopicsProperties.TopicInfo> f2bTopics = channels.get("f2b");
            if (f2bTopics == null || f2bTopics.isEmpty()) {
                log.warn("Product [{}] has no f2b channel configuration.", productName);
                continue;
            }

            // Collect all topics under the f2b channel for this product,
            // and calculate the maximum concurrency setting
            Set<String> topicSet = new HashSet<>();
            int maxConcurrency = 0;
            for (TopicsProperties.TopicInfo topicInfo : f2bTopics.values()) {
                topicSet.add(topicInfo.getName());
                if (topicInfo.getConcurrency() > maxConcurrency) {
                    maxConcurrency = topicInfo.getConcurrency();
                }
            }
            if (topicSet.isEmpty()) {
                log.warn("Product [{}] f2b channel has no topics configured.", productName);
                continue;
            }
            String joinedTopics = String.join(",", topicSet);

            // Generate groupId and clientId based on the product name
            String groupId = productName + "-f2b-consumer-group";
            String clientId = productName + "-f2b-client";

            // Retrieve default consumer settings from KafkaConfigProperties
            Map<String, String> consumerDefaults = kafkaConfigProperties.getConsumerDefaults();
            String autoOffsetReset = consumerDefaults.get("auto-offset-reset");
            String securityProtocol = consumerDefaults.get("security-protocol");
            String saslMechanism = consumerDefaults.get("sasl-mechanism");
            String saslJaasConfig = consumerDefaults.get("sasl-jaas-config");

            // Build the Kafka endpoint URI (multiple topics separated by commas)
            String kafkaUri = String.format("kafka:%s?brokers=%s"
                            + "&groupId=%s"
                            + "&clientId=%s"
                            + "&autoOffsetReset=%s"
                            + "&securityProtocol=%s"
                            + "&saslMechanism=%s"
                            + "&saslJaasConfig=%s"
                            + "&consumersCount=%d",
                    joinedTopics,
                    kafkaConfigProperties.getBootstrapServers(),
                    groupId,
                    clientId,
                    autoOffsetReset,
                    securityProtocol,
                    saslMechanism,
                    saslJaasConfig,
                    maxConcurrency > 0 ? maxConcurrency : 1);

            // Define a unique route ID for each product
            String routeId = "F2B_" + productName;

            from(kafkaUri)
                    .routeId(routeId)
                    .doTry()
                    .choice()
                    // If the message body starts with '{' and is not null, treat it as JSON
                    .when(PredicateBuilder.and(body().startsWith("{"), body().isNotNull()))
                    .log(LoggingLevel.INFO, log,
                            "Received a JSON message from Kafka -> Body: ${body}")
                    // Use JSONPath to extract requestAction and store in header
                    .setHeader("headerMessage.requestAction",
                            jsonpath("headerMessage.requestAction", String.class))
                    // Send to some endpoint as needed
                    .to("seda:core")
                    .endChoice()

                    // Otherwise, forward to direct:core
                    .otherwise()
                    .log(LoggingLevel.INFO, log,
                            "Received a non-JSON message from Kafka -> Body: ${body}. Redirecting to direct:core.")
                    .to("direct:core")
                    .endChoice()
                    .endChoice()
                    .endDoTry()
                    .doCatch(Exception.class)
                    .log(LoggingLevel.ERROR, log,
                            "Error processing use case: ${header.requestAction} - Exception: ${exception.message} -> Body: ${body}")
                    .end()
                    .end();
        }
    }
}
