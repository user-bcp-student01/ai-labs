# Hallazgos de discovery-analysis

## api

Alcance: `api/` (FastAPI, `app/main.py`, `tests/test_api.py`, `requirements.txt`, `Dockerfile`).
Estado general: todos los hallazgos están como `CANDIDATO` y requieren revisión humana antes de crear issues o modificar código.

Verificación realizada: `python -m pytest -p no:cacheprovider tests` desde `api/` → 4 passed. Las pruebas de comportamiento se ejecutaron contra una base y carpeta de subida temporales, que se eliminaron al terminar. No se modificó código.

### H-API-01 Inyección SQL por concatenación de cadenas
- Estado: CANDIDATO
- Evidencia:
  - `api/app/main.py:70`: `WHERE owner='" + requested_owner + "'` en `GET /api/tasks`.
  - `api/app/main.py:84`: `INSERT ... VALUES ('" + title + "','" + description + "',...` en `POST /api/tasks`.
  - `api/app/main.py:93`: `WHERE owner='" + requested_owner + "'` en `GET /api/summary`.
  - Prueba de comportamiento: `owner=x' OR '1'='1` devuelve las 3 filas (en lugar de 0) en `/api/tasks` y `total=3` en `/api/summary`.
- Impacto: lectura de datos de cualquier propietario y posible manipulación de la base de datos mediante parámetros o campos del cuerpo de la petición.
- Prioridad propuesta: Crítica.
- Contrato potencialmente afectado: `GET /api/tasks`, `POST /api/tasks`, `GET /api/summary` (parámetro `owner`, campos del cuerpo JSON). Debe conservarse el formato de respuesta.
- Cómo validarlo: prueba de caracterización previa con datos válidos; después, probar `owner` con comillas y comprobar que devuelve solo las filas del propietario indicado o un error 4xx. Revisar que las consultas usen parámetros (`?`).

### H-API-02 Autorización no ligada a la identidad (IDOR y rol sin uso)
- Estado: CANDIDATO
- Evidencia:
  - `api/app/main.py:22-24`: el token solo se valida por firma compartida y devuelve `role`, que no se usa en ningún endpoint.
  - `api/app/main.py:69` y `api/app/main.py:92`: `owner or identity["username"]` permite elegir el propietario mediante query string.
  - `api/app/main.py:83`: `owner = payload.get("owner", identity["username"])` permite crear tareas para otro usuario.
  - Prueba de comportamiento: un token de usuario `intruso` con rol `ADMIN` lee las tareas de `owner=trainer` (devuelve la tarea "Trainer notes").
- Impacto: cualquier usuario autenticado lee o crea datos de otros usuarios; el control de rol no protege nada.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: parámetro `owner` de `GET /api/tasks` y `GET /api/summary`, y campo `owner` de `POST /api/tasks`. Si el frontend depende de `owner`, la decisión sobre su eliminación requiere revisión humana.
- Cómo validarlo: con dos identidades de prueba, comprobar que una no puede leer ni crear tareas de la otra; verificar que el rol se aplica a los endpoints que lo requieran.

### H-API-03 Token forjable con secreto compartido fijo en el código
- Estado: CANDIDATO
- Evidencia:
  - `api/app/main.py:14`: `LEGACY_SECRET` está fijado en el código fuente.
  - `api/app/main.py:19-22`: el token es base64 de `usuario|rol|expiración|secreto`, sin firma criptográfica ni verificación de emisor.
  - `api/tests/test_api.py:9-12`: las pruebas generan tokens con el mismo secreto literal.
  - Prueba de comportamiento: un token generado localmente con rol `ADMIN` fue aceptado por la API.
- Impacto: cualquiera que conozca el repositorio puede emitir tokens válidos para cualquier usuario y rol.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: formato del header `Authorization: Bearer <token>` y el esquema de `401` en todos los endpoints `/api/*`. Sustituir el esquema requiere coordinación con `auth/`, por lo que debe ampliarse el alcance antes de cambiarlo.
- Cómo validarlo: revisar que el secreto se lea de variable de entorno en pruebas y que un token sin firma válida devuelva `401`; confirmar que ningún secreto real queda en el repositorio.

### H-API-04 Subida de archivos sin control de nombre ni tamaño
- Estado: CANDIDATO
- Evidencia:
  - `api/app/main.py:109`: `target = Path(selected_uploads) / str(file.filename)` usa el nombre enviado por el cliente sin sanitizar.
  - `api/app/main.py:110-111`: `await file.read()` carga el contenido completo en memoria y lo escribe sin límite de tamaño.
  - Prueba de comportamiento: el nombre `../evil_disc.txt` se guardó fuera de la carpeta de subida (`storedAt` apunta al directorio padre) y un archivo de 20 MB devolvió `201`.
- Impacto: escritura de archivos fuera del directorio previsto (path traversal) y agotamiento de disco o memoria.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: `POST /api/requirements` (campo `fileName` y `storedAt` de la respuesta, código `201`).
- Cómo validarlo: prueba de caracterización de la subida válida; después, comprobar que un nombre con `../` se rechaza o se normaliza dentro de la carpeta, y que archivos sobre el límite devuelven `413`. Verificar que `api/uploads/` solo contenga archivos esperados.
