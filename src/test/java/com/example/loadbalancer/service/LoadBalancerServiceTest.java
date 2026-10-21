package com.example.loadbalancer.service;

import com.example.loadbalancer.model.BackendServer;
import com.example.loadbalancer.model.BatchRequestDto;
import com.example.loadbalancer.model.RequestRecord;
import com.example.loadbalancer.model.ServerStatus;
import com.example.loadbalancer.model.SimulationStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Load Balancer Service & Round-Robin Scheduling Tests")
class LoadBalancerServiceTest {

    private ServerHealthService healthService;
    private RequestHistoryService historyService;
    private LoadBalancerService loadBalancerService;

    @BeforeEach
    void setUp() {
        List<BackendServer> servers = new ArrayList<>();
        servers.add(new BackendServer("server-a", "Server A", "127.0.0.1", 8081));
        servers.add(new BackendServer("server-b", "Server B", "127.0.0.1", 8082));
        servers.add(new BackendServer("server-c", "Server C", "127.0.0.1", 8083));

        healthService = new ServerHealthService(servers);
        historyService = new RequestHistoryService();
        loadBalancerService = new LoadBalancerService(healthService, historyService);
    }

    @Test
    @DisplayName("Scenario 1: With three online servers, the first six requests follow A -> B -> C -> A -> B -> C")
    void testRoundRobinSequenceWithThreeOnlineServers() {
        String[] expectedSequence = {"server-a", "server-b", "server-c", "server-a", "server-b", "server-c"};

        for (int i = 0; i < 6; i++) {
            RequestRecord record = loadBalancerService.routeRequest("192.168.1.10" + i);
            assertEquals("SUCCESS", record.getResult());
            assertEquals(200, record.getHttpStatusCode());
            assertEquals(expectedSequence[i], record.getSelectedServerId(),
                    "Request " + (i + 1) + " should be routed to " + expectedSequence[i]);
        }

        // Verify each server handled exactly 2 requests
        assertEquals(2, healthService.getServerById("server-a").get().getRequestCount());
        assertEquals(2, healthService.getServerById("server-b").get().getRequestCount());
        assertEquals(2, healthService.getServerById("server-c").get().getRequestCount());
        assertEquals(6, historyService.getTotalRequests());
        assertEquals(6, historyService.getSuccessfulRequests());
        assertEquals(0, historyService.getFailedRequests());
    }

    @Test
    @DisplayName("Scenario 2: When Server B is offline, requests are routed only to A and C in alternating order")
    void testRoutingWhenServerBIsOffline() {
        // Take Server B offline
        healthService.setServerStatus("server-b", ServerStatus.OFFLINE);
        assertFalse(healthService.getServerById("server-b").get().isOnline());

        String[] expectedSequence = {"server-a", "server-c", "server-a", "server-c"};

        for (int i = 0; i < 4; i++) {
            RequestRecord record = loadBalancerService.routeRequest("192.168.1.20" + i);
            assertEquals("SUCCESS", record.getResult());
            assertEquals(expectedSequence[i], record.getSelectedServerId(),
                    "Request " + (i + 1) + " should route to " + expectedSequence[i]);
        }

        // Verify Server B handled 0 requests, A handled 2, C handled 2
        assertEquals(2, healthService.getServerById("server-a").get().getRequestCount());
        assertEquals(0, healthService.getServerById("server-b").get().getRequestCount());
        assertEquals(2, healthService.getServerById("server-c").get().getRequestCount());
    }

    @Test
    @DisplayName("Scenario 3: When a server is restored, it seamlessly becomes eligible for future routing")
    void testServerRecoveryAndReintegration() {
        // Initially take Server B offline
        healthService.setServerStatus("server-b", ServerStatus.OFFLINE);

        // Route 2 requests -> A, then C
        RequestRecord req1 = loadBalancerService.routeRequest("10.0.0.1");
        assertEquals("server-a", req1.getSelectedServerId());

        RequestRecord req2 = loadBalancerService.routeRequest("10.0.0.2");
        assertEquals("server-c", req2.getSelectedServerId());

        // Now restore Server B online
        healthService.setServerStatus("server-b", ServerStatus.ONLINE);
        assertTrue(healthService.getServerById("server-b").get().isOnline());

        // Next request after Server C (index 2) must cycle to Server A (index 0)
        RequestRecord req3 = loadBalancerService.routeRequest("10.0.0.3");
        assertEquals("server-a", req3.getSelectedServerId());

        // Next request must select restored Server B (index 1)
        RequestRecord req4 = loadBalancerService.routeRequest("10.0.0.4");
        assertEquals("server-b", req4.getSelectedServerId());

        // Next request selects Server C (index 2)
        RequestRecord req5 = loadBalancerService.routeRequest("10.0.0.5");
        assertEquals("server-c", req5.getSelectedServerId());
    }

    @Test
    @DisplayName("Scenario 4: When all servers are offline, request fails gracefully with 503 and no server counters increase")
    void testAllServersOfflineGracefulFailure() {
        healthService.setServerStatus("server-a", ServerStatus.OFFLINE);
        healthService.setServerStatus("server-b", ServerStatus.OFFLINE);
        healthService.setServerStatus("server-c", ServerStatus.OFFLINE);

        assertTrue(healthService.areAllServersOffline());
        assertEquals(0, healthService.getOnlineCount());
        assertEquals("CRITICAL", healthService.getSystemHealth());

        RequestRecord record = loadBalancerService.routeRequest("192.168.1.99");

        assertNotNull(record);
        assertEquals("FAILED", record.getResult());
        assertEquals(503, record.getHttpStatusCode());
        assertNull(record.getSelectedServerId());
        assertEquals("None", record.getSelectedServerName());
        assertTrue(record.getMessage().contains("Service Unavailable"));

        // No backend server counter should have been incremented
        assertEquals(0, healthService.getServerById("server-a").get().getRequestCount());
        assertEquals(0, healthService.getServerById("server-b").get().getRequestCount());
        assertEquals(0, healthService.getServerById("server-c").get().getRequestCount());

        // Metrics reflect 1 total request and 1 failed request
        assertEquals(1, historyService.getTotalRequests());
        assertEquals(0, historyService.getSuccessfulRequests());
        assertEquals(1, historyService.getFailedRequests());
    }

    @Test
    @DisplayName("Scenario 5: Request counters increase only for the server that actually handles the request")
    void testRequestCounterAccuracy() {
        healthService.setServerStatus("server-c", ServerStatus.OFFLINE);

        // 3 requests: A, B, A
        loadBalancerService.routeRequest("client-1"); // Server A
        loadBalancerService.routeRequest("client-2"); // Server B
        loadBalancerService.routeRequest("client-3"); // Server A

        assertEquals(2, healthService.getServerById("server-a").get().getRequestCount());
        assertEquals(1, healthService.getServerById("server-b").get().getRequestCount());
        assertEquals(0, healthService.getServerById("server-c").get().getRequestCount());
    }

    @Test
    @DisplayName("Scenario 6: Reset operation restores all servers online, zeroes counters and clears history")
    void testSimulationReset() {
        // Modify state
        healthService.setServerStatus("server-b", ServerStatus.OFFLINE);
        loadBalancerService.routeRequest("client-1");
        loadBalancerService.routeRequest("client-2");

        // Trigger reset
        loadBalancerService.resetSimulation();

        // Verify all servers are ONLINE
        for (BackendServer server : healthService.getAllServers()) {
            assertTrue(server.isOnline());
            assertEquals(0, server.getRequestCount());
            assertEquals(0.0, server.getPercentage());
        }

        // Verify history & metrics are zeroed
        assertEquals(0, historyService.getTotalRequests());
        assertEquals(0, historyService.getSuccessfulRequests());
        assertEquals(0, historyService.getFailedRequests());
        assertTrue(historyService.getAllHistory().isEmpty());

        // Verify next request begins anew at Server A
        RequestRecord nextReq = loadBalancerService.routeRequest("client-new");
        assertEquals("server-a", nextReq.getSelectedServerId());
    }

    @Test
    @DisplayName("Scenario 7: Concurrent requests do not corrupt round-robin state or counters")
    void testConcurrentRequestsThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 30; // Total 300 requests
        int totalExpectedRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int r = 0; r < requestsPerThread; r++) {
                        loadBalancerService.routeRequest("thread-" + threadId + "-req-" + r);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finishedInTime = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finishedInTime, "All threads should complete within timeout");

        long countA = healthService.getServerById("server-a").get().getRequestCount();
        long countB = healthService.getServerById("server-b").get().getRequestCount();
        long countC = healthService.getServerById("server-c").get().getRequestCount();

        assertEquals(totalExpectedRequests, countA + countB + countC,
                "Sum of server counters must exactly match total requests executed");

        // Since 300 is divisible by 3, each server should have handled exactly 100 requests
        assertEquals(100, countA);
        assertEquals(100, countB);
        assertEquals(100, countC);

        assertEquals(totalExpectedRequests, historyService.getTotalRequests());
        assertEquals(totalExpectedRequests, historyService.getSuccessfulRequests());
        assertEquals(0, historyService.getFailedRequests());
    }

    @Test
    @DisplayName("Batch Request Test: Routing 6 requests at once distributes evenly")
    void testBatchRouting() {
        BatchRequestDto.Result result = loadBalancerService.routeBatch(6, "127.0.0.1");

        assertEquals(6, result.getRequestedCount());
        assertEquals(6, result.getSuccessfulCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(6, result.getRecords().size());

        assertEquals("server-a", result.getRecords().get(0).getSelectedServerId());
        assertEquals("server-b", result.getRecords().get(1).getSelectedServerId());
        assertEquals("server-c", result.getRecords().get(2).getSelectedServerId());
        assertEquals("server-a", result.getRecords().get(3).getSelectedServerId());
        assertEquals("server-b", result.getRecords().get(4).getSelectedServerId());
        assertEquals("server-c", result.getRecords().get(5).getSelectedServerId());
    }

    @Test
    @DisplayName("Validation: Invalid batch size throws IllegalArgumentException")
    void testInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> loadBalancerService.routeBatch(0, "127.0.0.1"));
        assertThrows(IllegalArgumentException.class, () -> loadBalancerService.routeBatch(101, "127.0.0.1"));
    }
}
