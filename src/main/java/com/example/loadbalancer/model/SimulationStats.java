package com.example.loadbalancer.model;

/**
 * Encapsulates system-wide aggregate metrics for dashboard monitoring.
 */
public class SimulationStats {

    private final long totalRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final int onlineServers;
    private final int offlineServers;
    private final int totalServers;
    private final String systemHealth; // "HEALTHY", "DEGRADED", "CRITICAL"
    private final String currentRotationPointer;

    public SimulationStats(long totalRequests,
                           long successfulRequests,
                           long failedRequests,
                           int onlineServers,
                           int offlineServers,
                           int totalServers,
                           String systemHealth,
                           String currentRotationPointer) {
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.onlineServers = onlineServers;
        this.offlineServers = offlineServers;
        this.totalServers = totalServers;
        this.systemHealth = systemHealth;
        this.currentRotationPointer = currentRotationPointer;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getSuccessfulRequests() {
        return successfulRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public int getOnlineServers() {
        return onlineServers;
    }

    public int getOfflineServers() {
        return offlineServers;
    }

    public int getTotalServers() {
        return totalServers;
    }

    public String getSystemHealth() {
        return systemHealth;
    }

    public String getCurrentRotationPointer() {
        return currentRotationPointer;
    }
}
