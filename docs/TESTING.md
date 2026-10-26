# Pruebas — componente auth

## Matriz de endpoints

Las pruebas HTTP de login están en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`; las pruebas
de salud están en `auth/src/test/java/com/bcp/workshop/auth/HealthDownEndpointTest.java`
y `HealthDownCheck.java`. Las pruebas del token y el hash son complementarias;
no agregan endpoints al contrato.

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `GET /q/health` | Sin header `Authorization`; respuesta `UP` y lista `checks` vacía | Camino feliz y borde (sin checks registrados) | `200`; `status` es `UP`, `checks` tiene tamaño 0 |
| `GET /q/health` | Un check de readiness temporal del perfil de prueba informa `DOWN` | Error documentado | `503`; `status` es `DOWN`, `checks` incluye `test-down-check` |
| `POST /auth/login` | Credenciales de desarrollo válidas | Camino feliz | `200`; cuerpo con `token`, `expiresIn`, `user` (`username`, `displayName`, `role`) y `debug`; el token verifica y expira a los 3600 segundos |
| `POST /auth/login` | Nombre de usuario desconocido con apóstrofo | Borde / regresión SQL (`H-AUTH-01`) | `401`; mensaje `Invalid user unknown'user or password`; no error SQL |
| `POST /auth/login` | Contraseña incorrecta | Error de credenciales | `401`; mensaje `Invalid user student or password` |
| `POST /auth/login` | `username` compuesto solo por espacios | Error de validación | `400`; `{"message":"Username is required"}` |
| `POST /auth/login` | `password` compuesto solo por espacios | Error de validación | `400`; `{"message":"Password is required"}` |
| `POST /auth/login` | Campo `username` omitido (se deserializa como `null`) | Borde / error de validación | `400`; `{"message":"Username is required"}` |
| `POST /auth/login` | Campo `password` omitido (se deserializa como `null`) | Borde / error de validación | `400`; `{"message":"Password is required"}` |
| `POST /auth/login` | Cuerpo JSON `null` | Error de validación | `400`; `{"message":"Request is null"}` |
| `POST /auth/login` | Tabla de autenticación temporalmente no disponible | Error de persistencia; respuesta HTTP (`H-AUTH-05`) | `500`; `{"message":"Authentication failed","exception":"InternalServerError"}`; la tabla se restaura en `finally` |

La respuesta `503` es condicional a un check `DOWN`. Se comprobó el endpoint
con un check controlado por el perfil de prueba; producción no registra ese
check y la prueba no cambia el comportamiento de producción.

## Código de los tests HTTP

Fragmentos de `AuthResourceTest` que ejercitan el contrato de los endpoints:

```java
@Test
void reportsHealthUpWithoutAuthenticationAndWithoutRegisteredChecks() {
    given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("checks.size()", is(0));
}
```

El caso `DOWN` usa un check de prueba condicionado por un perfil específico;
no registra comprobaciones nuevas en la aplicación de producción:

```java
@Readiness
@ApplicationScoped
@IfBuildProperty(name = "test.health.down", stringValue = "true")
public class HealthDownCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("test-down-check").down().build();
    }
}

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
```

El perfil activa el check únicamente en esta aplicación de prueba:

```java
public class HealthDownTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("test.health.down", "true");
    }
}
```

```java
@Test
void logsInWithWorkshopUser() {
    String token = given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(200)
            .body("user.username", is("student"))
            .body("user.displayName", is("Workshop Student"))
            .body("user.role", is("STUDENT"))
            .body("expiresIn", is(3600))
            .body("debug", is("authenticated-with-legacy-flow"))
            .body("token", notNullValue())
            .extract()
            .path("token");

    AuthTokenService.TokenClaims claims = tokenService.verify(token);
    assertEquals("student", claims.subject());
    assertEquals("STUDENT", claims.role());
    assertEquals(3600, claims.expiresAt() - claims.issuedAt());
}
```

```java
@Test
void treatsQuoteInUnknownUsernameAsCredentialData() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"unknown'user\",\"password\":\"incorrect\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("$", equalTo(Map.of("message", "Invalid user unknown'user or password")));
}

@Test
void rejectsInvalidCredentials() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"incorrect\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("$", equalTo(Map.of("message", "Invalid user student or password")));
}
```

```java
@Test
void rejectsMissingUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":\" \",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("$", equalTo(Map.of("message", "Username is required")));
}

@Test
void rejectsNullUsername() {
    given()
            .contentType("application/json")
            .body("{\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("$", equalTo(Map.of("message", "Username is required")));
}

@Test
void rejectsMissingCredentials() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\" \"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("$", equalTo(Map.of("message", "Password is required")));
}

@Test
void rejectsNullPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("$", equalTo(Map.of("message", "Password is required")));
}

@Test
void rejectsNullRequestWithDocumentedErrorBody() {
    given()
            .contentType("application/json")
            .body("null")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("$", equalTo(Map.of("message", "Request is null")));
}
```

```java
@Test
void returnsGenericErrorBodyWhenAuthenticationStorageIsUnavailable() throws Exception {
    try (Connection connection = DriverManager.getConnection(DATABASE_URL);
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("ALTER TABLE legacy_users RENAME TO legacy_users_unavailable");
        try {
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(500)
                    .body("$", equalTo(Map.of(
                            "message", "Authentication failed",
                            "exception", "InternalServerError"
                    )));
        } finally {
            statement.executeUpdate("ALTER TABLE legacy_users_unavailable RENAME TO legacy_users");
        }
    }
}
```

El error `500` se provoca únicamente en la base SQLite local de pruebas:
renombra la tabla durante la solicitud y la restaura incluso si falla la
aserción. No realiza cambios permanentes ni usa credenciales reales. Los casos
de error para JSON mal formado y tipo de contenido no admitido no se incluyen:
`docs/ENDPOINTS.md` los deja como preguntas abiertas, no como respuestas
confirmadas.

## Ejecuciones

Todos los comandos corregidos se ejecutaron desde `auth/`, con una base SQLite
ficticia local en `target/auth-testing.db`.

| componente e IDs | directorio | comando | código de salida | resultado |
|---|---|---|---:|---|
| auth — contrato de endpoints | `auth/` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-testing.db'; mvn -Dtest=HealthDownEndpointTest test` | 0 | 1 prueba, 0 fallidas, 0 errores; HTTP `503` y cuerpo `DOWN` verificados usando un check exclusivo del test profile. |
| auth — contrato de endpoints; `H-AUTH-01`, `H-AUTH-05` | `auth/` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-testing.db'; mvn -Dtest=AuthResourceTest test` | 0 | 19 pruebas, 0 fallidas, 0 errores. Incluye health `200`, login y cuerpos `400`, `401`, `500`. |
| auth — contrato de endpoints; `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07` | `auth/` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-testing.db'; mvn clean verify` | 0 | 25 pruebas, 0 fallidas, 0 errores; JaCoCo generó el reporte. |
| auth — intento de ejecución | raíz `ai-labs/` (directorio incorrecto) | `mvn -Dtest=AuthResourceTest test` | 1 | Maven no encontró un `pom.xml` en la raíz; no se ejecutaron pruebas. El comando se repitió desde `auth/` y pasó, según la fila anterior. |

## Cobertura

**Método:** reporte JaCoCo generado por `mvn clean verify`; se sumaron
`INSTRUCTION_COVERED` y `INSTRUCTION_MISSED` del archivo
`auth/target/site/jacoco/jacoco.csv`, filtrando el paquete
`com.bcp.workshop.auth`.

- `AuthResource`: 127/127 instrucciones cubiertas (**100%**).
- Todo el paquete de producción `com.bcp.workshop.auth`: 840/880 instrucciones
  cubiertas (**95.45%**).
- No se modificó código de producción en este paso; la cobertura describe el
  código de auth ejercitado por la suite, no una métrica aislada del archivo de
  pruebas modificado.
- Meta indicada en `AGENTS.md`: 85%; el resultado medido supera esa meta. No se
  cambió ninguna configuración ni umbral de quality gate.

## Fallos, pendientes y pruebas propuestas

### Fallos

- Ninguna prueba falló en las ejecuciones completadas. El único comando fallido
  fue la invocación accidental de Maven desde la raíz, sin proyecto Maven; se
  corrigió el directorio y se ejecutó la suite.

### Pendientes

- No se probaron errores de parseo JSON ni contenido no JSON porque no son
  respuestas confirmadas en `docs/ENDPOINTS.md`.

### Pruebas propuestas

- No quedan pruebas propuestas para las respuestas de error documentadas. Si
  se agrega un health check real a producción, volver a verificar el cuerpo
  `/q/health` cuando informe `DOWN`, porque puede incluir campos específicos
  del nuevo check.
