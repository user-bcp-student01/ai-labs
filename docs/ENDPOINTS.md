# Contratos HTTP

## auth — Backend Quarkus

Las respuestas se describen según el código actual. Base local de desarrollo:
`http://localhost:8080`. El servicio escucha en `127.0.0.1` fuera del perfil
`prod`; en `prod` escucha en `0.0.0.0`. Fuente: `auth/src/main/resources/application.properties:1-3`.

### GET /q/health

1. **Qué hace:** informa si el servicio de autenticación está disponible para
   que el orquestador o un operador pueda comprobar su estado.

2. **Autenticación:** no requiere autenticación ni un header `Authorization`.
   El README identifica este endpoint como público
   (`README.md`, sección “Arranque completo”). Lo implementa la extensión
   Quarkus SmallRye Health declarada en `auth/pom.xml:35-37`; no es un recurso
   Java propio.

3. **Request**

   No tiene cuerpo ni campos de query documentados.

   | campo | tipo | obligatorio | descripción |
   |---|---|---|---|
   | — | — | — | No aplica; es una petición `GET` sin cuerpo. |

   Ejemplo de petición: `GET http://localhost:8080/q/health`.

4. **Response exitosa**

   **Estado:** `200 OK` cuando el estado agregado es `UP`.

   | campo | tipo | descripción |
   |---|---|---|
   | `status` | string | Estado agregado del health check. |
   | `checks` | array | Lista de comprobaciones registradas. En el código de auth no hay comprobaciones personalizadas; la respuesta observada tiene una lista vacía. |

   Ejemplo JSON comprobado localmente:

   ```json
   {
     "status": "UP",
     "checks": []
   }
   ```

5. **Respuestas de error**

   - **`503 Service Unavailable`:** SmallRye Health responde cuando una
     comprobación de readiness informa `DOWN`. Se comprobó la respuesta HTTP
     con un health check registrado solo en el perfil de prueba; producción no
     registra este check.

     ```json
     {
       "status": "DOWN",
       "checks": [
         {
           "name": "test-down-check",
           "status": "DOWN"
         }
       ]
     }
     ```

     El cuerpo anterior es la respuesta observada con el check de prueba
     `HealthDownCheck`; otros checks pueden añadir campos a sus elementos.

6. **Notas de seguridad:** el endpoint es público según el README y sirve
   para estado de salud, no para autenticar una sesión. No incluir secretos ni
   datos de usuario en futuras comprobaciones de salud.

### POST /auth/login

1. **Qué hace:** autentica a una persona del taller y, si sus credenciales son
   válidas, devuelve su identidad básica y un token para iniciar sesión.
   Implementación: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28-47`.

2. **Autenticación:** no requiere autenticación previa ni un header
   `Authorization`; este endpoint es el mecanismo de inicio de sesión.
   Requiere `Content-Type: application/json` y responde JSON
   (`AuthResource.java:16-18`).

3. **Request**

   | campo | tipo | obligatorio | descripción |
   |---|---|---|---|
   | `username` | string | Sí | Nombre de usuario. Se rechaza si es `null` o queda vacío al aplicar `trim`; el valor no se normaliza antes de autenticar. |
   | `password` | string | Sí | Contraseña. Se rechaza si es `null` o queda vacía al aplicar `trim`; se verifica como credencial. |
   | `client` | string | No | Campo aceptado en el DTO de entrada; el flujo actual no lo consulta. |

   Fuente de campos: `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-7`.
   Validación: `AuthResource.java:57-72`.

   Ejemplo JSON completo con credenciales ficticias de desarrollo:

   ```json
   {
     "username": "student",
     "password": "Workshop123!",
     "client": "angular-workshop"
   }
   ```

4. **Response exitosa**

   **Estado:** `200 OK`.

   | campo | tipo | descripción |
   |---|---|---|
   | `token` | string | JWT firmado, codificado como tres segmentos separados por puntos. |
   | `expiresIn` | integer | Duración del token en segundos; actualmente `3600`. |
   | `user` | object | Identidad básica devuelta al cliente. |
   | `user.username` | string | Nombre de usuario autenticado. |
   | `user.displayName` | string | Nombre visible; se escapa para HTML en la respuesta. |
   | `user.role` | string | Rol devuelto para la cuenta, por ejemplo `STUDENT` en el seed local. |
   | `debug` | string | Campo heredado del contrato observado; actualmente vale `authenticated-with-legacy-flow`. |

   Fuentes: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:76-88`,
   `LoginResponse.java:3` y `LoginUserResponse.java:3`.

   Ejemplo JSON completo. El valor de `token` es un marcador ilustrativo: el
   servidor emite un JWT firmado dinámico; no copiar este marcador como
   credencial.

   ```json
   {
     "token": "<JWT firmado emitido por el servidor>",
     "expiresIn": 3600,
     "user": {
       "username": "student",
       "displayName": "Workshop Student",
       "role": "STUDENT"
     },
     "debug": "authenticated-with-legacy-flow"
   }
   ```

5. **Respuestas de error**

   - **`400 Bad Request`:** cuerpo JSON nulo, nombre de usuario ausente/vacío
     o contraseña ausente/vacía. Ejemplos de cada cuerpo:

     ```json
     {"message":"Request is null"}
     ```

     ```json
     {"message":"Username is required"}
     ```

     ```json
     {"message":"Password is required"}
     ```

   - **`401 Unauthorized`:** usuario o contraseña no válidos. El mensaje
     incluye el username enviado.

     ```json
     {"message":"Invalid user student or password"}
     ```

   - **`500 Internal Server Error`:** error interno durante la autenticación o
     el acceso a persistencia. El detalle se registra en el servidor; el cuerpo
     conserva valores genéricos.

     ```json
     {
       "message": "Authentication failed",
       "exception": "InternalServerError"
     }
     ```

     Fuentes de estados y cuerpos: `AuthResource.java:34-54,57-72`.
     La rama `500` también está comprobada por
     `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:194-213`.

   **Pregunta abierta:** el recurso no define cuerpos para JSON mal formado,
   tipo de contenido distinto de JSON u otros errores generados por REST;
   dichos estados y cuerpos dependen de Quarkus y no se documentan como
   contrato confirmado aquí.

6. **Notas de seguridad:**

   - El token es JWT firmado con HMAC-SHA256, incluye `sub`, `role`, `iat` y
     `exp`, y expira después de `3600` segundos. El secreto se configura con
     `AUTH_TOKEN_SECRET` y debe tener al menos 32 bytes
     (`auth/src/main/java/com/bcp/workshop/auth/AuthTokenService.java:20-45`).
   - No enviar ni registrar la contraseña. El cuerpo `401` refleja el username;
     los consumidores no deben interpretar el mensaje como prueba de que un
     usuario exista.
   - `debug` se conserva porque forma parte de la respuesta observada; el
     cliente no debe usarlo para decisiones de autenticación.
   - El token debe tratarse como credencial y transmitirse únicamente por un
     canal protegido en despliegues que expongan el servicio.

## api

Sección reservada para documentar contratos del componente `api`; fuera del
alcance de esta actualización.

## web

Sección reservada para documentar integraciones del componente `web`; fuera del
alcance de esta actualización.
