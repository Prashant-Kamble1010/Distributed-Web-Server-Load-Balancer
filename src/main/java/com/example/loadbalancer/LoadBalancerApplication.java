package com.example.loadbalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Distributed Web Server Load Balancer Simulator.
 * Distributed Computing Systems (DCS) Final Year B.Tech Mini-Project.
 */
@SpringBootApplication
public class LoadBalancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancerApplication.class, args);
    }
}
