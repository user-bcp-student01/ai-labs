# Plan de correccion

### Aprobación humana
- Jhon
- 2026/10/26
- H-AUTH-01,H-AUTH-02,H-AUTH-03,H-AUTH-04,H-AUTH-05,H-AUTH-06,H-AUTH-07
- Confirmada por el usuario en esta conversación el 2026-10-26; los siete IDs están aprobados.

## Alcance y condiciones

Se planificaron todos los hallazgos pendientes de auth en `HALLAZGOS.md`: `H-AUTH-01` a `H-AUTH-07`. La nota describe el plan previo a la implementación; las ejecuciones y el estado parcial actual se registran al final.

`docs/ENDPOINTS.md` no existe. Para `POST /auth/login`, se conserva el comportamiento HTTP observado en `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:15,22-24,26-50`: ruta y metodo, validaciones con `400`, rechazo de credenciales con `401` y respuesta exitosa `200` con las claves `token`, `expiresIn`, `user` y `debug`. Las propuestas no alteran esta envoltura de exito. Los tokens firmados cambiarian el valor/formato opaco de `token`; confirmar consumidores y documentar/acordar ese cambio antes de integrarlo. La respuesta de error actual incluye `message` y `exception` (`AuthResource.java:53-59`); para no filtrar detalles, se propone conservar `500` y ambas claves, con valores genericos fijos, sujeto a caracterizacion y confirmacion del contrato antes de implementar.

Aplicar las convenciones de `docs/GOOD_PRACTICES.md` y `AGENTS.md`: separar Resource, servicio, repositorio y seguridad; usar `PreparedStatement`; hash lento con salt; token firmado con expiracion y claims; configuracion externa; mensajes de error genericos y logging controlado. Mantener metodos con una responsabilidad, nombres descriptivos, sin duplicacion, anidamiento profundo ni valores magicos. Las pruebas de caracterizacion deben preceder a cualquier refactor de persistencia o token. Usar solo datos ficticios y configuracion de prueba aislada; no agregar dependencias sin justificar ni silenciar CodeQL, Dependency Review o JaCoCo. La meta del componente para el codigo modificado es cobertura de 85% o superior.

## H-AUTH-01 — Parametrizar la consulta de credenciales

- **Hallazgo de HALLAZGOS.md:** SQL de login concatena usuario y contraseña en `Statement`.
- **Problema:** los valores de entrada se interpretan como parte de la consulta, no exclusivamente como datos; una comilla puede cambiar la sintaxis o el resultado.
- **Cambio propuesto:** sustituir la consulta de login por `PreparedStatement` con placeholders para ambos valores y cierre de recursos con try-with-resources. Conservar columnas, tabla, criterios de igualdad y retorno de usuario; no cambiar `400`, `401` ni el formato de respuesta exitosa.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/UserRepository.java`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes del cambio:** ampliar la prueba HTTP para usuario ficticio válido (`200`, usuario y token), credenciales incorrectas (`401`) y entrada literal con apóstrofo en un usuario inexistente (no debe autenticar; caracterizar el estado observado). Ejecutar sobre base aislada. No usar payloads de explotación.
- **Verificación posterior:** repetir los casos y comprobar que entradas con apóstrofo son tratadas como valores y no autentican por cambiar la consulta.

## H-AUTH-02 — Hash lento con salt para contraseñas

- **Hallazgo de HALLAZGOS.md:** las contraseñas de cuentas y la columna de persistencia son texto plano.
- **Problema:** la lectura de la base revela credenciales directamente; comparar el texto recibido con una columna en claro perpetúa el riesgo.
- **Cambio propuesto:** persistir hashes lentos con salt y verificar la contraseña mediante una API de hash adecuada. Resolver antes de codificar el manejo de cuentas ya existentes: plan de migración/backfill para la base SQLite y compatibilidad de login de las cuentas de laboratorio. No mantener una ruta permanente de comparación en claro. Investigar primero una opción disponible/soportada por Quarkus; justificar y coordinar cualquier dependencia necesaria antes de añadirla.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/UserRepository.java`, `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`, `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java`. No se añadió dependencia: se usó PBKDF2 del JDK.
- **Caracterización antes del cambio:** con SQLite temporal aislada, caracterizar login válido e inválido para las cuentas ficticias iniciales y persistir el estado previo de una base de prueba con esquema antiguo. Acordar previamente el comportamiento esperado de una base existente para no perder acceso ni reescribir datos sin cobertura.
- **Verificación posterior:** comprobar login correcto e incorrecto, que el valor persistido no sea la contraseña, que los hashes incluyan salt y que la migración/arranque sea repetible sin alterar el contrato HTTP.

## H-AUTH-03 — Token firmado con expiración y claims

- **Hallazgo de HALLAZGOS.md:** el token es Base64 de contenido concatenado con un secreto embebido, sin firma verificable.
- **Problema:** Base64 solo codifica; no acredita integridad/autenticidad ni protege el contenido.
- **Cambio propuesto:** emitir un JWT firmado con HMAC-SHA256 que incluya claims explícitos de identidad/rol y expiración, conservando en la respuesta HTTP las claves `token` y `expiresIn` y su significado temporal observado. Usar duración nombrada/configurable en vez del literal `3600`. El usuario confirmó que acepta el cambio del valor opaco dentro del mismo campo HTTP. Se reutiliza Jackson ya disponible en Quarkus; no se agrega dependencia.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthTokenService.java`, `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`; `auth/src/test/java/com/bcp/workshop/auth/AuthTokenServiceTest.java` y `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes del cambio:** ampliar la prueba de login válido para fijar el estado HTTP, las claves de éxito, usuario/rol y la relación entre `expiresIn` y la expiración observada del token actual; registrar que el token actual es opaco para el contrato HTTP, sin declarar compatible su formato interno.
- **Verificación posterior:** probar firma válida, rechazo de token alterado, expiración, claims y consistencia de `expiresIn`, además de repetir la caracterización del endpoint. Mantener valores de firma exclusivamente de prueba en pruebas.

## H-AUTH-04 — Externalizar secreto y restringir CORS

- **Hallazgo de HALLAZGOS.md:** el secreto del token está en código y CORS acepta cualquier origen.
- **Problema:** un secreto versionado puede ser conocido por quien acceda al código; permitir cualquier origen amplía innecesariamente los sitios que pueden leer respuestas del servicio en navegador.
- **Cambio propuesto:** cargar el secreto mediante `AUTH_TOKEN_SECRET`, sin fallback de producción y con validación mínima de 32 bytes. Mantener un valor exclusivamente ficticio en perfil de pruebas. Restringir CORS al origen de desarrollo `http://localhost:4200`, documentado en README; permitir override mediante `AUTH_CORS_ORIGINS` y requerirlo explícitamente en producción.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthTokenService.java`; `auth/src/main/resources/application.properties`; pruebas en `auth/src/test/java/com/bcp/workshop/auth/AuthTokenServiceTest.java` y `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes del cambio:** se comprobó con prueba local de preflight que un origen no listado recibía CORS permisivo y se caracterizó la forma Base64 anterior del token; se usaron datos ficticios y no se contactaron servicios remotos.
- **Verificación posterior:** pruebas verifican que `http://localhost:4200` recibe el header CORS, que un origen arbitrario no lo recibe, que el token se firma y valida con claims explícitos, que tokens alterados/expirados se rechazan y que el login conserva las claves HTTP. No se probó arranque de producción con variables ausentes.
- **Pendiente de despliegue:** configurar `AUTH_TOKEN_SECRET` y `AUTH_CORS_ORIGINS` en cada entorno productivo. No se inventó una lista de orígenes de producción.

## H-AUTH-05 — Errores genéricos y logging controlado

- **Hallazgo de HALLAZGOS.md:** el `500` devuelve el mensaje de excepción y el nombre de su clase al cliente.
- **Problema:** mensajes y tipos internos pueden revelar información de implementación o persistencia.
- **Cambio propuesto:** mantener el estado `500` y la forma observada con las claves `message` y `exception`, pero asignar valores genéricos constantes y nunca incluir mensaje, clase o stacktrace en la respuesta. Registrar detalle solo en logging controlado del servidor. Distinguir fallo de persistencia de credenciales inválidas para que un error de infraestructura no se convierta silenciosamente en `401`; esto requiere coordinar el tratamiento actual de excepciones de `LegacyDatabase`.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`, `auth/src/main/java/com/bcp/workshop/auth/UserRepository.java`, `auth/src/main/java/com/bcp/workshop/auth/AuthPersistenceException.java`; pruebas en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes del cambio:** agregar una prueba con fallo de persistencia simulado que registre el `500`, las claves y valores actuales sin afirmar que los detalles expuestos sean un comportamiento que deba conservarse; caracterizar por separado credenciales incorrectas (`401`) para impedir confundir ambos casos.
- **Verificación posterior:** verificar estado y claves genéricas, ausencia de mensaje/clase/stacktrace en el cuerpo, detalle disponible solo en log controlado y conservación de `401` para credenciales inválidas.

## H-AUTH-06 — Revisión SCA de Quarkus y Commons Text

- **Hallazgo de HALLAZGOS.md:** el plan inicial anotó Quarkus `3.27.4` y Commons Text `1.9` como candidatos SCA. El advisory oficial de Apache confirma que Commons Text anterior a `1.10.0` está afectado por CVE-2022-42889 si se usa `StringSubstitutor` con entradas no confiables.
- **Problema:** el proyecto usaba Commons Text `1.9`; el código revisado usa `StringEscapeUtils.escapeHtml4`, no `StringSubstitutor`. El estado de advisories para Quarkus y dependencias transitivas requiere análisis SCA completo.
- **Cambio realizado:** actualizar Commons Text a `1.15.0`, versión estable actual según Maven Central, para salir del rango afectado. No se cambió Quarkus sin evidencia de una alerta aplicable.
- **Archivos tocados:** `auth/pom.xml`; no se requirieron cambios de comportamiento ni nuevas pruebas específicas.
- **Caracterización antes del cambio:** `mvn clean verify` pasó con 19 pruebas antes de actualizar la dependencia; se usó la base local aislada indicada en la bitácora.
- **Verificación posterior:** `mvn clean verify` pasó con 19 pruebas tras la actualización. Se intentó Dependency-Check, pero no produjo resultado: sin clave NVD comenzó una actualización extensa de 414,758 registros y se detuvo.
- **Pendiente:** análisis SCA de Quarkus y del árbol transitivo. No se afirma que esas dependencias estén libres de advisories.

## H-AUTH-07 — Separar responsabilidades del flujo de login

- **Hallazgo de HALLAZGOS.md:** `AuthResource.login` combina validación, llamada a persistencia, token y armado de respuesta.
- **Problema:** responsabilidades acopladas dificultan probar el contrato HTTP y cambiar persistencia o seguridad sin alterar la capa REST.
- **Cambio propuesto:** tras caracterizar los contratos, dejar en el Resource la traducción HTTP y validación de request; mover reglas de login a un servicio; encapsular acceso SQLite en repositorio; mantener emisión de token en seguridad y mapear un DTO de salida sin exponer directamente el registro de persistencia. Extraer lógica duplicada y usar guard clauses, métodos de responsabilidad única y constantes con nombre.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`, `auth/src/main/java/com/bcp/workshop/auth/AuthService.java`, `auth/src/main/java/com/bcp/workshop/auth/UserRepository.java`, `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java`, `auth/src/main/java/com/bcp/workshop/auth/LoginUserResponse.java`; pruebas en `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterización antes del cambio:** ampliar primero las pruebas HTTP para request nulo o campos ausentes/vacíos (`400`), credenciales inválidas (`401`), login válido (`200` con las claves y campos observados) y fallo interno (`500`, según H-AUTH-05). Ejecutar antes de mover responsabilidades.
- **Verificación posterior:** repetir casos HTTP, añadir pruebas unitarias por servicio/repositorio y medir cobertura de código modificado; buscar cobertura de 85% o superior sin reducir gates.

## Orden propuesto y bloqueos

1. Escribir y ejecutar pruebas de caracterización para el endpoint y el almacenamiento aislado antes de refactorizar persistencia o token.
2. Resolver y probar `H-AUTH-01` y `H-AUTH-02` en persistencia, incluido el plan de migración de datos existentes.
3. Acordar con el usuario el valor opaco del token y el origen de desarrollo; implementados `H-AUTH-03` y `H-AUTH-04`. Los orígenes de producción quedan como configuración operativa pendiente.
4. Resolver el mapeo de errores `H-AUTH-05` y luego separar capas para `H-AUTH-07`, preservando estados y respuesta de éxito observados.
5. Ejecutar análisis SCA para `H-AUTH-06`; tocar dependencias solo con evidencia y coordinación.
6. Ejecutar pruebas y medir cobertura del código modificado; revisión/aprobación humana precede a la implementación y al cierre.

## Ejecuciones y resultado parcial (componente auth)

### Caracterización anterior a los cambios

- **IDs:** `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-05`, `H-AUTH-07`.
- **Comando desde `auth/`:** `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn test`.
- **Resultado:** 6 pruebas, 0 fallidas, 0 errores. Login ficticio válido, credenciales inválidas, usuario desconocido con apóstrofo (`401`), campo vacío (`400`), contraseña inicial almacenada en claro y exposición del detalle de excepción en el `500` quedaron caracterizados antes de modificar producción.
- **Observación:** el caso con apóstrofo devolvió `401`, pero la concatenación produjo y registró un error de sintaxis SQLite; después del cambio la misma prueba sigue esperando `401` y ya no depende de SQL formado con entradas.

### Verificación posterior

- **IDs:** `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07`.
- **Comando desde `auth/`:** `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify`.
- **Resultado final:** 19 pruebas, 0 fallidas, 0 errores. Se verificaron SQL parametrizado, rechazo de credenciales con apóstrofo, migración y hash con salt, autenticación conservada, errores genéricos, fallos de persistencia diferenciados, JWT con firma/claims/expiración, CORS restringido y campos HTTP conservados.
- **Cobertura:** JaCoCo final para clases de producción modificadas: 822 instrucciones cubiertas de 862 (95.4%), calculado desde `auth/target/site/jacoco/jacoco.csv`; supera la meta de 85%.

### Bitácora de ejecuciones

Todos los comandos se ejecutaron con cwd `auth/` y la base local ficticia indicada. Se incluyen los intentos fallidos y los reportes que se descartaron.

| # | Componente e IDs | Comando | Resultado |
|---|---|---|---|
| 1 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn test` (antes del cambio) | 6 pruebas, 0 fallidas. La entrada con apóstrofo produjo error de sintaxis SQLite en la implementación anterior y devolvió `401`; la contraseña inicial estaba en claro y el `500` exponía detalles. |
| 2 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn test` (primer intento posterior) | Falló el arranque de Quarkus porque `PasswordHasher` aún no tenía alcance CDI; no se ejecutaron pruebas. Se corrigió agregando `@ApplicationScoped`. |
| 3 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn test` | 8 pruebas, 0 fallidas. |
| 4 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn verify` | 8 pruebas, 0 fallidas. El reporte JaCoCo se descartó porque la ejecución sin `clean` incluía una clase eliminada que permanecía compilada. |
| 5 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 11 pruebas, 0 fallidas. |
| 6 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 11 pruebas; una falló al esperar `AuthPersistenceException` mientras la inicialización propagaba `NullPointerException`. Se ajustó el manejo para envolver y relanzar explícitamente el fallo de inicialización. |
| 7 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 11 pruebas, 0 fallidas. |
| 8 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 14 pruebas, 0 fallidas antes de iniciar `H-AUTH-03` y `H-AUTH-04`. |
| 9 | auth — `H-AUTH-03`, `H-AUTH-04` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn test` (caracterización antes de cambios) | 16 pruebas, 0 fallidas. Se comprobó que la versión previa codificaba el token en Base64 y admitía un origen CORS no listado. |
| 10 | auth — `H-AUTH-03`, `H-AUTH-04` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | Falló compilación por una llave de cierre adicional en `AuthTokenService.java`; no se ejecutaron pruebas. Se reescribió el archivo correctamente. |
| 11 | auth — `H-AUTH-03`, `H-AUTH-04` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | Falló compilación porque Jackson declara `IOException` al leer JSON; no se ejecutaron pruebas. Se añadió el manejo tipado correspondiente. |
| 12 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 19 pruebas, 0 fallidas tras corregir compilación; antes del renombrado. |
| 13 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 19 pruebas, 0 fallidas tras renombrar `LegacyTokenUtil` a `AuthTokenService`. |
| 14 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` | 19 pruebas, 0 fallidas; verificó el JWT emitido en el flujo real de login y la política CORS configurada. |
| 15 | auth — `H-AUTH-01`, `H-AUTH-02`, `H-AUTH-03`, `H-AUTH-04`, `H-AUTH-05`, `H-AUTH-07` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-characterization.db'; mvn clean verify` (final) | 19 pruebas, 0 fallidas; JaCoCo final 822/862 instrucciones (95.4%) en clases modificadas y prueba de secreto ausente/débil. |
| 16 | auth — `H-AUTH-06` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-review.db'; mvn clean verify` (antes de actualizar Commons Text) | 19 pruebas, 0 fallidas; JaCoCo para clases de producción modificadas: 798/838 instrucciones (95.2%). |
| 17 | auth — `H-AUTH-06` | `$env:AUTH_DB_URL='jdbc:sqlite:./target/auth-review.db'; mvn clean verify` (después de actualizar Commons Text a `1.15.0`) | 19 pruebas, 0 fallidas. |
| 18 | auth — `H-AUTH-06` | `mvn org.owasp:dependency-check-maven:12.1.0:check -DskipTests` | Sin resultado SCA: el plugin advirtió que no había clave NVD y comenzó a actualizar 414,758 registros; se detuvo la ejecución prolongada. No se confirma ni se descarta estado de advisories para Quarkus y dependencias transitivas. |

La aprobación humana precede a la implementación: el usuario confirmó en esta conversación que Jhon aprobó los siete IDs el 2026-10-26.

### IDs aún pendientes y controles no ejecutados

- `H-AUTH-06`: Apache documenta CVE-2022-42889 para Commons Text anterior a `1.10.0`; la versión `1.9` fue actualizada a `1.15.0` en `auth/pom.xml`. `AuthResource` solo usa `StringEscapeUtils.escapeHtml4`, no `StringSubstitutor`, por lo que no se encontró el uso vulnerable descrito por Apache. El análisis SCA de Quarkus y el árbol transitivo no concluyó; el hallazgo permanece **Pendiente** hasta obtener ese resultado.
- No se probó el arranque del perfil de producción con `AUTH_TOKEN_SECRET` o `AUTH_CORS_ORIGINS` ausentes; ambos son requisitos de configuración operativa. El origen de desarrollo configurado es `http://localhost:4200`, a partir de README.
- CodeQL remoto y Dependency Review remoto: no ejecutados, según el alcance local. Dependency-Check local se intentó sin completar la actualización NVD; no se afirma que Quarkus ni las dependencias transitivas estén libres de advisories.
- No se creó `docs/ENDPOINTS.md` ni `docs/TESTING.md`, conforme al alcance de esta etapa. `H-AUTH-01`, `02`, `03`, `04`, `05` y `07` quedan resueltos; `H-AUTH-06` permanece pendiente hasta completar el análisis SCA.
