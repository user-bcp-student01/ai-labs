package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class HealthDownTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("test.health.down", "true");
    }
}
