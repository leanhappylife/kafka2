package com.example.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "topics")
public class TopicsProperties {
    private Map<String, Map<String, Map<String, TopicInfo>>> product;

    public Map<String, Map<String, Map<String, TopicInfo>>> getProduct() { return product; }
    public void setProduct(Map<String, Map<String, Map<String, TopicInfo>>> product) { this.product = product; }

    public static class TopicInfo {
        private String name;
        private int concurrency;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
    }

    @PostConstruct
    public void init() {
        System.out.println("Loaded product topics: " + product);
    }
}
