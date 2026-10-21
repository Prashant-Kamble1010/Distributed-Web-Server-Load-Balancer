package com.example.loadbalancer.controller;

import com.example.loadbalancer.model.BackendServer;
import com.example.loadbalancer.model.BatchRequestDto;
import com.example.loadbalancer.model.RequestRecord;
import com.example.loadbalancer.model.ServerStatus;
import com.example.loadbalancer.model.SimulationStats;
import com.example.loadbalancer.service.LoadBalancerService;
import com.example.loadbalancer.service.RequestHistoryService;
import com.example.loadbalancer.service.ServerHealthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller exposing load balancer operations, server health toggles,
 * and monitoring metrics.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoadBalancerRestController {

    private final LoadBalancerService loadBalancerService;
    private final ServerHealthService serverHealthService;
    private final RequestHistoryService requestHistoryService;

    public LoadBalancerRestController(LoadBalancerService loadBalancerService,
                                      ServerHealthService serverHealthService,
                                      RequestHistoryService requestHistoryService) {
        this.loadBalancerService = loadBalancerService;
        this.serverHealthService = serverHealthService;
        this.requestHistoryService = requestHistoryService;
    }

    /**
     * GET /api/servers
     * Returns all server details, request counts, and load distribution percentages.
     */
    @GetMapping("/servers")
    public ResponseEntity<List<BackendServer>> getAllServers() {
        return ResponseEntity.ok(serverHealthService.getAllServers());
    }

    /**
     * POST /api/requests
     * Routes a single request through the Round-Robin load balancer.
     * Returns HTTP 200 on success, or HTTP 503 if all backend servers are offline.
     */
    @PostMapping("/requests")
    public ResponseEntity<RequestRecord> sendRequest(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        RequestRecord record = loadBalancerService.routeRequest(clientIp != null ? clientIp : "127.0.0.1");

        if ("SUCCESS".equalsIgnoreCase(record.getResult())) {
            return ResponseEntity.ok(record);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(record);
        }
    }

    /**
     * POST /api/requests/batch
     * Routes multiple requests sequentially through the load balancer.
     */
    @PostMapping("/requests/batch")
    public ResponseEntity<?> sendBatchRequests(@RequestBody(required = false) BatchRequestDto batchRequest,
                                               HttpServletRequest request) {
        int count = (batchRequest != null && batchRequest.getCount() > 0) ? batchRequest.getCount() : 5;
        if (count > 100) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Batch size exceeds maximum limit of 100.");
            return ResponseEntity.badRequest().body(error);
        }

        String clientIp = request.getRemoteAddr();
        BatchRequestDto.Result result = loadBalancerService.routeBatch(count, clientIp != null ? clientIp : "127.0.0.1");

        if (result.getSuccessfulCount() == 0 && result.getFailedCount() > 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/servers/{id}/offline
     * Takes a specific server offline.
     */
    @PostMapping("/servers/{id}/offline")
    public ResponseEntity<?> takeServerOffline(@PathVariable String id) {
        try {
            BackendServer server = serverHealthService.setServerStatus(id, ServerStatus.OFFLINE);
            return ResponseEntity.ok(server);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/servers/{id}/online
     * Restores a specific server online.
     */
    @PostMapping("/servers/{id}/online")
    public ResponseEntity<?> bringServerOnline(@PathVariable String id) {
        try {
            BackendServer server = serverHealthService.setServerStatus(id, ServerStatus.ONLINE);
            return ResponseEntity.ok(server);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/requests/history
     * Retrieves recent request execution history.
     */
    @GetMapping("/requests/history")
    public ResponseEntity<List<RequestRecord>> getRequestHistory(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(requestHistoryService.getRecentHistory(limit));
    }

    /**
     * POST /api/requests/history/clear
     * Clears historical audit entries.
     */
    @PostMapping("/requests/history/clear")
    public ResponseEntity<Map<String, String>> clearHistory() {
        requestHistoryService.clearHistoryOnly();
        return ResponseEntity.ok(Collections.singletonMap("message", "Request history cleared successfully."));
    }

    /**
     * GET /api/stats
     * Returns cumulative system metrics and health status.
     */
    @GetMapping("/stats")
    public ResponseEntity<SimulationStats> getAggregateStats() {
        return ResponseEntity.ok(loadBalancerService.getStats());
    }

    /**
     * POST /api/reset
     * Full simulation reset: clears counters, restores all servers online, resets rotation pointer.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetSimulation() {
        loadBalancerService.resetSimulation();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Simulation reset successfully: all servers restored online and metrics zeroed.");
        return ResponseEntity.ok(response);
    }
}
