package com.example.loadbalancer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a simulated backend server node in the distributed cluster.
 * Thread-safe request counting using AtomicLong.
 */
public class BackendServer {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String name;
    private final String host;
    private final int simulatedPort;
    private volatile ServerStatus status;
    private final AtomicLong requestCount;
    private volatile String lastActiveTimestamp;
    private volatile double percentage;

    public BackendServer(String id, String name, String host, int simulatedPort) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.simulatedPort = simulatedPort;
        this.status = ServerStatus.ONLINE;
        this.requestCount = new AtomicLong(0);
        this.lastActiveTimestamp = LocalDateTime.now().format(FORMATTER);
        this.percentage = 0.0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getSimulatedPort() {
        return simulatedPort;
    }

    public String getEndpoint() {
        return host + ":" + simulatedPort;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
        this.lastActiveTimestamp = LocalDateTime.now().format(FORMATTER);
    }

    public boolean isOnline() {
        return this.status == ServerStatus.ONLINE;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long incrementRequestCount() {
        this.lastActiveTimestamp = LocalDateTime.now().format(FORMATTER);
        return requestCount.incrementAndGet();
    }

    public void resetRequestCount() {
        this.requestCount.set(0);
        this.percentage = 0.0;
        this.lastActiveTimestamp = LocalDateTime.now().format(FORMATTER);
    }

    public String getLastActiveTimestamp() {
        return lastActiveTimestamp;
    }

    public void setLastActiveTimestamp(String lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = Math.round(percentage * 10.0) / 10.0;
    }
}
