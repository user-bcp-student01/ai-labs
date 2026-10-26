# Reto de ReqFlow

## auth

### Reto — Backend Quarkus

El contrato de `POST /auth/login` que hay que preservar está en
`docs/ENDPOINTS.md`. Este archivo es solo la lista de lo que hay que encontrar.

#### Hallazgos sembrados

| Área | Caso | Objetivo de aprendizaje |
|---|---|---|
| SCA | Quarkus `3.27.4` y Commons Text `1.9` | Investigar advisories y actualizar/remover con evidencia. |
| Datos | Contraseñas en texto plano | Aplicar hash lento con salt. |
| SQL | Concatenación de credenciales | Usar `PreparedStatement`. |
| Token | Base64 con secreto embebido | Implementar token firmado, expiración y claims. |
| Configuración | Secreto y CORS hardcodeados | Externalizar configuración. |
| Clean code | Resource con validación, negocio, persistencia y respuesta | Separar capas y responsabilidades. |
| Errores | Respuesta expone detalles internos | Usar mensajes genéricos y logging controlado. |

No se incluyen payloads de explotación.

## api

### Reto — API FastAPI

Los contratos que hay que preservar están en `docs/ENDPOINTS.md`. Este archivo
es solo la lista de lo que hay que encontrar.

#### Hallazgos sembrados

| Área | Caso | Objetivo |
|---|---|---|
| SCA | `urllib3 2.5.0` y `python-multipart 0.0.21` | Analizar CVEs y aplicar versiones corregidas. |
| Auth | Token Base64 con secreto embebido | Verificar firma, emisor, audiencia y expiración. |
| Autorización | El cliente puede consultar otro `owner` | Aplicar autorización por identidad/rol. |
| SQL | Filtros e inserts concatenados | Parametrizar consultas. |
| Files | Se conserva directamente `upload.filename` | Normalizar nombre, limitar extensión y tamaño. |
| Arquitectura | Rutas, DB, reglas y serialización en un archivo | Separar capas. |
| Calidad | `dict`, excepciones amplias, condicionales y duplicación | Aplicar Pydantic, tipos y funciones pequeñas. |

No se incluyen payloads de explotación.

## web

### Reto — Frontend Angular

El flujo funcional que hay que preservar y los endpoints que consume esta app
están en `docs/ENDPOINTS.md`. Este archivo es solo la lista de lo que hay que
encontrar.

#### Deuda técnica intencional

| Área | Caso sembrado | Resultado esperado del estudiante |
|---|---|---|
| Dependencias | `lodash 4.17.21` y `axios 0.31.0` | Analizar CVEs, eliminar o actualizar con evidencia. |
| Seguridad | Token en `localStorage` | Diseñar una alternativa acotada para el laboratorio. |
| Seguridad | Mensaje dinámico renderizado con `[innerHTML]` | Evitar HTML no confiable. |
| Arquitectura | Componentes conocen URLs, HTTP, sesión y navegación | Separar responsabilidades. |
| Calidad | Uso extensivo de `any`, condicionales anidados y duplicación | Introducir tipos, servicios y funciones pequeñas. |
| Testing | Cobertura limitada a una utilidad | Definir pruebas de flujo y reglas de negocio. |

No se incluyen payloads de explotación. El propósito es identificar, priorizar y
remediar hallazgos.

## Identificación común

Usa H-AUTH-01, H-API-01 y H-WEB-01, con numeración por componente. Las referencias al código se escriben desde la raíz. HALLAZGOS.md, PLAN.md, prompts/ y docs/ son compartidos.
