package com.example.util;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.UUID;

public class KafkaClientIdManager {

    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * Generates a unique Kafka clientId.
     * - If the clientId is already registered, unregister the old one before generating a new one.
     * - If the clientId is not registered, return it directly.
     *
     * @param productName Product name used to generate clientId
     * @return A unique clientId
     */
    public static String getUniqueClientId(String productName) {
        String baseClientId = productName + "-f2b-client";
        try {
            ObjectName mbeanName = new ObjectName("kafka.consumer:type=app-info,id=" + baseClientId);

            if (mBeanServer.isRegistered(mbeanName)) {
                // 🔥 FIX: Unregister old MBean before using the same clientId
                mBeanServer.unregisterMBean(mbeanName);
                System.out.println("Unregistered existing MBean: " + baseClientId);
            }
            return baseClientId;
        } catch (InstanceNotFoundException e) {
            // If the MBean is not found, it's safe to use the base clientId
            return baseClientId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baseClientId + "-" + UUID.randomUUID();
    }
}
