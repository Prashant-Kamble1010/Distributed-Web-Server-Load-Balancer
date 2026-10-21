package com.example.loadbalancer.config;

import com.example.loadbalancer.model.BackendServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class that defines the initial simulated backend server cluster.
 * In a real distributed system, these would represent distinct physical or virtual instances.
 * In this educational simulator, they are simulated nodes inside the Spring Boot container.
 */
@Configuration
public class ServerConfiguration {

    @Bean
    public List<BackendServer> initialBackendServers() {
        List<BackendServer> servers = new ArrayList<>();
        servers.add(new BackendServer("server-a", "Server A", "127.0.0.1", 8081));
        servers.add(new BackendServer("server-b", "Server B", "127.0.0.1", 8082));
        servers.add(new BackendServer("server-c", "Server C", "127.0.0.1", 8083));
        return Collections.unmodifiableList(servers);
    }
}
