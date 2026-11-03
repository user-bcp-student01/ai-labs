# Buenas prácticas de ReqFlow

## auth

### Buenas prácticas — Backend Quarkus

> Convenciones a las que debe converger el refactor. Se
> consultan durante el refactor, no después.

- **Capas separadas**: Resource (HTTP) → Service (reglas de negocio) →
  Repository (persistencia). Nada de lógica de negocio ni acceso a datos dentro
  del Resource.
- **DTOs propios**: la entidad de persistencia (`UserRecord`) no se devuelve tal
  cual en la respuesta HTTP; se mapea a un DTO de salida.
- **Contraseñas**: hash lento con salt (por ejemplo BCrypt). Nunca en texto
  plano, nunca en logs.
- **Tokens**: firmados, con expiración y claims explícitos. Nada de Base64
  casero con un secreto embebido en el código.
- **Configuración**: variables de entorno o `application.properties`. Ningún
  secreto hardcodeado en una clase Java.
- **SQL**: siempre `PreparedStatement` o parámetros nombrados. Nunca
  concatenación de strings.
- **Errores**: mensajes genéricos al cliente. El detalle completo (stacktrace,
  clase de la excepción) va solo al log del servidor, nunca a la respuesta HTTP.
- **Dependencias**: si CodeQL o Dependency Review marca un CVE, se investiga y
  se corrige — no se silencia.

## api

### Buenas prácticas — API FastAPI

> Convenciones a las que debe converger el refactor. Se
> consultan durante el refactor, no después.

- **Modelos Pydantic**: request y response tipados, no `dict[str, Any]` crudo.
- **Capas separadas**: rutas (FastAPI) → servicio (reglas) → repositorio (SQL
  parametrizado). Nada de SQL directo dentro de la función de la ruta.
- **Autenticación real**: verificar firma, emisor, audiencia y expiración del
  token. Nada de decodificar Base64 y confiar en el contenido.
- **Autorización por identidad**: las consultas siempre usan
  `identity["username"]` (lo que viene del token verificado), nunca un
  parámetro `owner` que manda el cliente.
- **Archivos subidos**: normalizar el nombre, limitar extensión permitida y
  tamaño máximo antes de guardar. Nunca usar `file.filename` tal cual.
- **Errores**: `HTTPException` con mensajes genéricos. Nunca el `str(exception)`
  crudo en la respuesta al cliente.
- **Dependencias**: versiones sin CVEs conocidos; no silenciar advisories.

## web

### Buenas prácticas — Frontend Angular

> Convenciones a las que debe converger el refactor. Se
> consultan durante el refactor, no después.

- **Tipado estricto**: sin `any`. Interfaces propias para el request y la
  response de cada endpoint consumido (login, tareas).
- **Componentes no llaman HTTP directo**: la llamada a `axios`/`HttpClient` vive
  en un servicio (`AuthService`, `TasksService`); el componente solo lo consume.
- **Nunca `[innerHTML]` con datos dinámicos sin sanitizar**: usar interpolación
  de texto, o `DomSanitizer` explícito si de verdad hace falta HTML.
- **Configuración por ambiente**: las URLs de los backends van en
  `environment.ts`, nunca hardcodeadas dentro de un componente.
- **Sesión centralizada**: un único servicio lee/escribe el token y los datos
  de sesión; ningún componente toca `localStorage` por su cuenta.
- **Dependencias**: versiones sin CVEs conocidos (`lodash`, `axios`); preferir
  utilidades nativas del lenguaje o de Angular cuando alcancen.
- **Errores**: manejo consistente, mensajes al usuario sin exponer detalles
  internos del backend.