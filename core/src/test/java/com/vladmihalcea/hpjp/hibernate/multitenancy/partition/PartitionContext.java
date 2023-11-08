package com.vladmihalcea.hpjp.hibernate.multitenancy.partition;

/**
 * @author Vlad Mihalcea
 */
public class PartitionContext {

    public static final String DEFAULT_PARTITION = "default";

    private static final ThreadLocal<String> CURRENT_PARTITION = new ThreadLocal<>();

    public static String get() {
        String currentTenantId = CURRENT_PARTITION.get();
        return currentTenantId != null ? currentTenantId : DEFAULT_PARTITION;
    }

    public static void set(String tenantId) {
        CURRENT_PARTITION.set(tenantId);
    }

    public static void reset() {
        CURRENT_PARTITION.remove();
    }
}
