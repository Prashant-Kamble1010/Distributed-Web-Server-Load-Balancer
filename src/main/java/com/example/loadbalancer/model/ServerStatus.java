package com.example.loadbalancer.model;

/**
 * Represents the operational health state of a backend server.
 */
public enum ServerStatus {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE");

    private final String label;

    ServerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isOnline() {
        return this == ONLINE;
    }
}
