package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(HealthDownTestProfile.class)
class HealthDownEndpointTest {
    @Test
    void reportsHealthDownWhenAReadinessCheckFails() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(503)
                .body("status", is("DOWN"))
                .body("checks.size()", is(1))
                .body("checks[0].name", is("test-down-check"))
                .body("checks[0].status", is("DOWN"));
    }
}
