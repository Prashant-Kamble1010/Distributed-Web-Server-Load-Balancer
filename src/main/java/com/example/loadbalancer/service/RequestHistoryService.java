package com.example.loadbalancer.service;

import com.example.loadbalancer.model.RequestRecord;
import com.example.loadbalancer.model.SimulationStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing in-memory request metrics and bounded historical audit logs.
 */
@Service
public class RequestHistoryService {

    private static final int MAX_HISTORY_CAPACITY = 100;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    // Bounded deque to store recent request records (newest first)
    private final Deque<RequestRecord> history = new ConcurrentLinkedDeque<>();

    /**
     * Records a new request entry, updating counters and maintaining bounded history.
     */
    public void record(RequestRecord record) {
        totalRequests.incrementAndGet();
        if ("SUCCESS".equalsIgnoreCase(record.getResult())) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }

        history.addFirst(record);
        // Evict older records if capacity is exceeded
        while (history.size() > MAX_HISTORY_CAPACITY) {
            history.pollLast();
        }
    }

    /**
     * Retrieve recent history up to the specified limit.
     */
    public List<RequestRecord> getRecentHistory(int limit) {
        List<RequestRecord> list = new ArrayList<>(history);
        if (limit > 0 && limit < list.size()) {
            return list.subList(0, limit);
        }
        return Collections.unmodifiableList(list);
    }

    public List<RequestRecord> getAllHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    /**
     * Clears only the audit table history, retaining cumulative counters.
     */
    public void clearHistoryOnly() {
        history.clear();
    }

    /**
     * Full reset of history and counters.
     */
    public void resetAll() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        history.clear();
    }

    /**
     * Constructs the aggregate simulation statistics snapshot.
     */
    public SimulationStats getStatsSnapshot(int onlineServers, int offlineServers, int totalServers, String systemHealth, String currentPointer) {
        return new SimulationStats(
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                onlineServers,
                offlineServers,
                totalServers,
                systemHealth,
                currentPointer
        );
    }
}
