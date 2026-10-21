package com.example.loadbalancer.controller;

import com.example.loadbalancer.service.LoadBalancerService;
import com.example.loadbalancer.service.RequestHistoryService;
import com.example.loadbalancer.service.ServerHealthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the Thymeleaf Web Dashboard.
 */
@Controller
public class DashboardController {

    private final LoadBalancerService loadBalancerService;
    private final ServerHealthService serverHealthService;
    private final RequestHistoryService requestHistoryService;

    public DashboardController(LoadBalancerService loadBalancerService,
                               ServerHealthService serverHealthService,
                               RequestHistoryService requestHistoryService) {
        this.loadBalancerService = loadBalancerService;
        this.serverHealthService = serverHealthService;
        this.requestHistoryService = requestHistoryService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("servers", serverHealthService.getAllServers());
        model.addAttribute("stats", loadBalancerService.getStats());
        model.addAttribute("recentHistory", requestHistoryService.getRecentHistory(10));
        return "index";
    }
}
