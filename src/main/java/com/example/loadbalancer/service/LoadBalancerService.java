package com.example.loadbalancer.service;

import com.example.loadbalancer.model.BackendServer;
import com.example.loadbalancer.model.BatchRequestDto;
import com.example.loadbalancer.model.RequestRecord;
import com.example.loadbalancer.model.SimulationStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core Load Balancer Service implementing the Round-Robin scheduling algorithm
 * with health monitoring and automatic fault tolerance.
 */
@Service
public class LoadBalancerService {

    private final ServerHealthService serverHealthService;
    private final RequestHistoryService requestHistoryService;

    // Index of the server that handled the last successful request (-1 initially)
    private int lastAssignedIndex = -1;

    // Sequential counter for request identifiers
    private final AtomicLong requestSequence = new AtomicLong(0);

    public LoadBalancerService(ServerHealthService serverHealthService,
                               RequestHistoryService requestHistoryService) {
        this.serverHealthService = serverHealthService;
        this.requestHistoryService = requestHistoryService;
    }

    /**
     * Routes a single incoming request using strict Round-Robin dispatching.
     * Synchronized to guarantee atomic pointer advancement and thread safety.
     *
     * @param clientIp IP address of simulated client
     * @return RequestRecord containing outcome and diagnostic details
     */
    public synchronized RequestRecord routeRequest(String clientIp) {
        long seq = requestSequence.incrementAndGet();
        String reqId = String.format("REQ-%04d", seq);
        String clusterSnapshot = serverHealthService.getClusterStateSnapshot();

        List<BackendServer> servers = serverHealthService.getAllServers();
        int totalServers = servers.size();

        if (totalServers == 0) {
            RequestRecord failedRecord = new RequestRecord(
                    reqId,
                    null,
                    "None",
                    "N/A",
                    "FAILED",
                    503,
                    clusterSnapshot,
                    "No backend servers configured in load balancer cluster.",
                    0
            );
            requestHistoryService.record(failedRecord);
            return failedRecord;
        }

        // Cyclic search starting immediately after the last assigned server index
        BackendServer selectedServer = null;
        for (int i = 0; i < totalServers; i++) {
            int candidateIndex = (lastAssignedIndex + 1 + i) % totalServers;
            BackendServer candidate = servers.get(candidateIndex);

            if (candidate.isOnline()) {
                selectedServer = candidate;
                lastAssignedIndex = candidateIndex;
                break;
            }
        }

        if (selectedServer != null) {
            // Server found and online -> route successfully
            selectedServer.incrementRequestCount();
            long simulatedLatency = ThreadLocalRandom.current().nextLong(12, 48);

            RequestRecord successRecord = new RequestRecord(
                    reqId,
                    selectedServer.getId(),
                    selectedServer.getName(),
                    selectedServer.getEndpoint(),
                    "SUCCESS",
                    200,
                    clusterSnapshot,
                    "Routed to " + selectedServer.getName() + " via Round-Robin (" + selectedServer.getEndpoint() + ")",
                    simulatedLatency
            );
            requestHistoryService.record(successRecord);
            return successRecord;
        } else {
            // All servers offline -> fault-tolerant rejection with 503
            RequestRecord failedRecord = new RequestRecord(
                    reqId,
                    null,
                    "None",
                    "N/A",
                    "FAILED",
                    503,
                    clusterSnapshot,
                    "Service Unavailable: All backend server nodes are offline (Fault Tolerance Active).",
                    5
            );
            requestHistoryService.record(failedRecord);
            return failedRecord;
        }
    }

    /**
     * Executes a batch of sequential requests.
     *
     * @param batchSize Number of requests to route (validated between 1 and 100)
     * @param clientIp Simulated client IP
     * @return Batch execution summary and record list
     */
    public BatchRequestDto.Result routeBatch(int batchSize, String clientIp) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1.");
        }
        if (batchSize > 100) {
            throw new IllegalArgumentException("Batch size cannot exceed 100 requests per execution.");
        }

        List<RequestRecord> records = new ArrayList<>(batchSize);
        int successCount = 0;
        int failedCount = 0;

        for (int i = 0; i < batchSize; i++) {
            RequestRecord record = routeRequest(clientIp);
            records.add(record);
            if ("SUCCESS".equalsIgnoreCase(record.getResult())) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        return new BatchRequestDto.Result(batchSize, successCount, failedCount, records);
    }

    /**
     * Returns the name/ID of the server currently pointed to by rotation pointer.
     */
    public synchronized String getCurrentPointerServer() {
        List<BackendServer> servers = serverHealthService.getAllServers();
        if (lastAssignedIndex >= 0 && lastAssignedIndex < servers.size()) {
            return servers.get(lastAssignedIndex).getName();
        }
        return "Start (Server A)";
    }

    /**
     * Full simulation reset: clears counters, resets server statuses,
     * resets rotation pointer and request history.
     */
    public synchronized void resetSimulation() {
        lastAssignedIndex = -1;
        requestSequence.set(0);
        serverHealthService.resetAll();
        requestHistoryService.resetAll();
    }

    /**
     * Aggregate simulation statistics.
     */
    public SimulationStats getStats() {
        return requestHistoryService.getStatsSnapshot(
                serverHealthService.getOnlineCount(),
                serverHealthService.getOfflineCount(),
                serverHealthService.getTotalServerCount(),
                serverHealthService.getSystemHealth(),
                getCurrentPointerServer()
        );
    }
}
