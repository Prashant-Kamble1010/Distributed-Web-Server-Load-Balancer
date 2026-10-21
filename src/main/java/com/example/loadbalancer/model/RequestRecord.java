package com.example.loadbalancer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit record representing an HTTP request handled or rejected by the load balancer.
 */
public class RequestRecord {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String requestId;
    private final String timestamp;
    private final String selectedServerId;
    private final String selectedServerName;
    private final String serverEndpoint;
    private final String result; // "SUCCESS" or "FAILED"
    private final int httpStatusCode;
    private final String clusterStateSnapshot;
    private final String message;
    private final long latencyMs;

    public RequestRecord(String requestId,
                         String selectedServerId,
                         String selectedServerName,
                         String serverEndpoint,
                         String result,
                         int httpStatusCode,
                         String clusterStateSnapshot,
                         String message,
                         long latencyMs) {
        this.requestId = requestId;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
        this.selectedServerId = selectedServerId;
        this.selectedServerName = selectedServerName;
        this.serverEndpoint = serverEndpoint;
        this.result = result;
        this.httpStatusCode = httpStatusCode;
        this.clusterStateSnapshot = clusterStateSnapshot;
        this.message = message;
        this.latencyMs = latencyMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSelectedServerId() {
        return selectedServerId;
    }

    public String getSelectedServerName() {
        return selectedServerName;
    }

    public String getServerEndpoint() {
        return serverEndpoint;
    }

    public String getResult() {
        return result;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getClusterStateSnapshot() {
        return clusterStateSnapshot;
    }

    public String getMessage() {
        return message;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
