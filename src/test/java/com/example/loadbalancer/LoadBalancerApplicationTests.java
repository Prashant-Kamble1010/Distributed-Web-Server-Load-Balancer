package com.example.loadbalancer;

import com.example.loadbalancer.controller.DashboardController;
import com.example.loadbalancer.controller.LoadBalancerRestController;
import com.example.loadbalancer.model.BackendServer;
import com.example.loadbalancer.model.RequestRecord;
import com.example.loadbalancer.model.ServerStatus;
import com.example.loadbalancer.service.LoadBalancerService;
import com.example.loadbalancer.service.RequestHistoryService;
import com.example.loadbalancer.service.ServerHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Spring Boot REST Endpoints & Web Integration Tests")
class LoadBalancerApplicationTests {

    @Autowired
    private LoadBalancerRestController restController;

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private ServerHealthService healthService;

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private RequestHistoryService historyService;

    @BeforeEach
    void setUp() {
        loadBalancerService.resetSimulation();
    }

    @Test
    @DisplayName("Dashboard controller loads and populates model correctly")
    void testDashboardPageLoads() {
        Model model = new ConcurrentModel();
        String view = dashboardController.index(model);

        assertEquals("index", view);
        assertTrue(model.containsAttribute("servers"));
        assertTrue(model.containsAttribute("stats"));
        assertTrue(model.containsAttribute("recentHistory"));

        @SuppressWarnings("unchecked")
        List<BackendServer> servers = (List<BackendServer>) model.getAttribute("servers");
        assertNotNull(servers);
        assertEquals(3, servers.size());
    }

    @Test
    @DisplayName("GET /api/servers returns all three initialized servers")
    void testGetServersApi() {
        ResponseEntity<List<BackendServer>> response = restController.getAllServers();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals("server-a", response.getBody().get(0).getId());
        assertEquals("server-b", response.getBody().get(1).getId());
        assertEquals("server-c", response.getBody().get(2).getId());
    }

    @Test
    @DisplayName("POST /api/requests routes request and returns HTTP 200")
    void testSendRequestApi() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");

        ResponseEntity<RequestRecord> response = restController.sendRequest(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getResult());
        assertEquals(200, response.getBody().getHttpStatusCode());
        assertEquals("server-a", response.getBody().getSelectedServerId());
    }

    @Test
    @DisplayName("Server offline/online lifecycle via REST APIs")
    void testServerStatusToggles() {
        // Take Server A offline
        ResponseEntity<?> offlineRes = restController.takeServerOffline("server-a");
        assertEquals(HttpStatus.OK, offlineRes.getStatusCode());
        BackendServer serverA = (BackendServer) offlineRes.getBody();
        assertEquals(ServerStatus.OFFLINE, serverA.getStatus());

        // Restore Server A online
        ResponseEntity<?> onlineRes = restController.bringServerOnline("server-a");
        assertEquals(HttpStatus.OK, onlineRes.getStatusCode());
        BackendServer restoredA = (BackendServer) onlineRes.getBody();
        assertEquals(ServerStatus.ONLINE, restoredA.getStatus());
    }

    @Test
    @DisplayName("GET /api/stats returns valid aggregate statistics")
    void testStatsApi() {
        var statsRes = restController.getAggregateStats();
        assertEquals(HttpStatus.OK, statsRes.getStatusCode());
        assertNotNull(statsRes.getBody());
        assertEquals(3, statsRes.getBody().getTotalServers());
        assertEquals("HEALTHY", statsRes.getBody().getSystemHealth());
    }

    @Test
    @DisplayName("POST /api/reset resets state and returns confirmation message")
    void testResetApi() {
        ResponseEntity<Map<String, String>> resetRes = restController.resetSimulation();
        assertEquals(HttpStatus.OK, resetRes.getStatusCode());
        assertNotNull(resetRes.getBody());
        assertTrue(resetRes.getBody().get("message").contains("reset successfully"));
    }
}
