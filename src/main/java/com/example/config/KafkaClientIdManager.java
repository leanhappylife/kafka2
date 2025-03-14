package com.example.config;


import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.UUID;

public class KafkaClientIdManager {

    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * Generates a unique Kafka clientId
     * - If the clientId is not registered yet, use it directly.
     * - If the clientId is already registered, generate a new one with UUID to avoid MBean conflict.
     *
     * @param productName Product name used to generate clientId
     * @return A unique clientId
     */
    public static String getUniqueClientId(String productName) {
        String baseClientId = productName + "-f2b-client";
        try {
            ObjectName mbeanName = new ObjectName("kafka.consumer:type=app-info,id=" + baseClientId);
            if (!mBeanServer.isRegistered(mbeanName)) {
                // If clientId is not in use, return it directly
                return baseClientId;
            }
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        // If clientId is already registered (MBean conflict), generate a unique one
        return baseClientId;
    }
}
