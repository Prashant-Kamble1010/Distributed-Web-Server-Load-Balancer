package com.example.loadbalancer.service;

import com.example.loadbalancer.model.BackendServer;
import com.example.loadbalancer.model.ServerStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for managing server registration, health state transitions,
 * and cluster status calculations.
 */
@Service
public class ServerHealthService {

    private final List<BackendServer> servers;
    private final Map<String, BackendServer> serverMap;

    public ServerHealthService(List<BackendServer> initialBackendServers) {
        this.servers = Collections.unmodifiableList(initialBackendServers);
        this.serverMap = new ConcurrentHashMap<>();
        for (BackendServer server : initialBackendServers) {
            serverMap.put(server.getId(), server);
        }
    }

    /**
     * Retrieve all backend servers with updated percentage distributions.
     */
    public List<BackendServer> getAllServers() {
        updatePercentages();
        return servers;
    }

    public Optional<BackendServer> getServerById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(serverMap.get(id.toLowerCase().trim()));
    }

    /**
     * Updates the status of a specific server.
     */
    public synchronized BackendServer setServerStatus(String id, ServerStatus status) {
        BackendServer server = getServerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Server with ID '" + id + "' not found."));
        server.setStatus(status);
        return server;
    }

    public int getOnlineCount() {
        return (int) servers.stream().filter(BackendServer::isOnline).count();
    }

    public int getOfflineCount() {
        return (int) servers.stream().filter(s -> !s.isOnline()).count();
    }

    public int getTotalServerCount() {
        return servers.size();
    }

    public boolean areAllServersOffline() {
        return getOnlineCount() == 0;
    }

    /**
     * Resets all servers to ONLINE and clears request counters.
     */
    public synchronized void resetAll() {
        for (BackendServer server : servers) {
            server.setStatus(ServerStatus.ONLINE);
            server.resetRequestCount();
        }
    }

    /**
     * Generates a concise snapshot of all servers' current status.
     * e.g. "Server A: ONLINE | Server B: OFFLINE | Server C: ONLINE"
     */
    public String getClusterStateSnapshot() {
        return servers.stream()
                .map(s -> s.getName() + ": " + s.getStatus().name())
                .collect(Collectors.joining(" | "));
    }

    /**
     * Calculates overall system health:
     * - HEALTHY: all servers are online
     * - DEGRADED: at least one online, at least one offline
     * - CRITICAL: all servers are offline
     */
    public String getSystemHealth() {
        int online = getOnlineCount();
        int total = servers.size();
        if (online == total) {
            return "HEALTHY";
        } else if (online > 0) {
            return "DEGRADED";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * Recomputes load percentage share for each server based on total handled requests.
     */
    private void updatePercentages() {
        long totalHandled = servers.stream()
                .mapToLong(BackendServer::getRequestCount)
                .sum();

        for (BackendServer server : servers) {
            if (totalHandled == 0) {
                server.setPercentage(0.0);
            } else {
                double pct = (server.getRequestCount() * 100.0) / totalHandled;
                server.setPercentage(pct);
            }
        }
    }
}
