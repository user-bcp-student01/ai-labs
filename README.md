# AI Labs

Un producto de práctica con autenticación Java/Quarkus en auth/, API Python/FastAPI en api/ e interfaz Angular en web/. Hay un solo repositorio Git. La documentación y las evidencias del laboratorio están en la raíz.

El código incluye defectos deliberados. Trabaja con datos ficticios, en localhost. Las tres plantillas originales se conservan sin cambios. Crea tu propio repositorio con Use this template en GitHub y luego clónalo dentro de tu carpeta del curso, con el nombre local ai-labs.

## Crear tu copia desde el template

1. Abre https://github.com/NTTData-Academy/ai-labs y elige Use this template → Create a new repository.
2. Elige tu cuenta como propietaria y usa ai-labs como nombre. Copia solo la rama principal.
3. Copia la URL de tu nuevo repositorio desde Code. Desde tu carpeta de matrícula y nombre, ejecuta git clone con esa URL y ai-labs como carpeta de destino.
4. Entra en ai-labs y abre VS Code. Si esa carpeta ya existe, conserva su contenido y consulta al instructor antes de clonar.

La clonación ya incluye .git y origin apuntando a tu repositorio. No ejecutes git init ni clones los componentes por separado. Los commits guardan versiones locales; push envía esas versiones a GitHub.

## Abrir el proyecto

Abre esta carpeta con Archivo → Abrir carpeta en VS Code, o ejecuta desde ella:

```bash
code .
git status
git switch -c day1/prompting
```

Si la rama ya existe, entra con git switch day1/prompting. Todos los comandos de Git se ejecutan aquí. No inicialices Git dentro de auth/, api/ o web/.

## Elegir recorrido

- Backend: elige auth o api. Revisa reglas, entradas, persistencia, errores y pruebas de ese servicio.
- Frontend: elige web. Revisa login, sesión, dashboard, accesibilidad y errores del servicio.
- Testing: elige uno o más componentes. Diseña casos desde los contratos, reproduce hallazgos y automatiza pruebas; coordina las correcciones de producto con su responsable.

Puedes recorrer los tres componentes. Conserva las evidencias anteriores y añade registros con componente e ID. Las pruebas viven dentro del componente cuyo comportamiento comprueban. En el Día 1 no crees una carpeta general `testing/`.

## Arranque completo

Necesitas Docker con Compose y el motor iniciado. Podman Compose es otra opción si ya está preparado en tu equipo.

El servicio de autenticación requiere `AUTH_TOKEN_SECRET` y
`AUTH_CORS_ORIGINS`. Define ambas variables en la terminal antes de iniciar
Compose; usa un secreto generado localmente y nunca lo guardes en el repositorio.
Para desarrollo local, permite solo el origen del frontend (`http://localhost:4200`).

PowerShell:

```powershell
$env:AUTH_TOKEN_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:AUTH_CORS_ORIGINS = "http://localhost:4200"
docker compose up --build
```

macOS/Linux:

```bash
export AUTH_TOKEN_SECRET="$(openssl rand -base64 32)"
export AUTH_CORS_ORIGINS="http://localhost:4200"
docker compose up --build
```

En otros entornos, inyecta el secreto con el gestor de secretos aprobado y
configura únicamente los orígenes web autorizados. Compose falla antes de
crear los servicios si falta cualquiera de las variables.

Abre http://localhost:4200. Usuario ficticio student, contraseña Workshop123!. Autenticación publica http://localhost:8080/q/health y la API http://localhost:8000/docs. La primera ejecución descarga imágenes y dependencias. Espera a que los tres servicios respondan: el orden de arranque no certifica que estén listos.

En otra terminal puedes ver el estado y la salida:

```bash
docker compose ps
docker compose logs
```

Al terminar, Ctrl+C y después docker compose down. Esto conserva los volúmenes de la API. No ejecutes down con eliminación de volúmenes si quieres conservar sus datos. El arranque con Compose construye las aplicaciones; no equivale a ejecutar sus pruebas.

## Ejecutar un componente

Para desarrollar sin contenedores necesitas JDK 21 y Maven 3.9+ para auth; Python 3.13 para api; Node.js 22.12+ y npm para web. Usa una terminal por servicio y ejecuta los comandos desde la raíz:

```bash
python tools/lab.py setup auth
python tools/lab.py start auth
```

```bash
python tools/lab.py setup api
python tools/lab.py start api
```

```bash
python tools/lab.py setup web
python tools/lab.py start web
```

En macOS/Linux, python3 puede ser el nombre del intérprete. Usa Python 3.13 para setup api. El script crea api/.venv si falta y no lo comparte con otros proyectos. Si ejecutas un solo componente, registra las integraciones que no pudiste probar. No arranques Compose y los procesos locales sobre los mismos puertos.

## Lab 01, paso a paso

Al comenzar se incluyen `README.md`, `AGENTS.md`, `RETO.md` y `docs/GOOD_PRACTICES.md` como referencias. El reto contiene candidatos por comprobar, no hallazgos confirmados. Los entregables `HALLAZGOS.md`, `PLAN.md`, `prompts/`, `docs/ENDPOINTS.md` y `docs/TESTING.md` se crean durante los pasos siguientes; no vienen preparados.

`AGENTS.md` reúne las instrucciones comunes y las secciones de auth, api y web. VS Code con Copilot Agent carga el archivo raíz por defecto; no necesitas pedir que lo lea en cada prompt. Revisa sus reglas y acuerda los ajustes antes de editar código.

Las guías y los prompts del portal permanecen fuera del producto. Su secuencia es:

1. Comprueba las herramientas del componente elegido y abre ai-labs en VS Code desde su raíz.
2. Revisa las instrucciones de AGENTS.md, RETO.md y docs/GOOD_PRACTICES.md. Acuerda quién implementa y quién revisa.
3. Crea `prompts/` y guarda ahí el pedido adaptado, su alcance, criterios y justificación. Usa nombres como `auth-discovery.md` o `web-testing.md`.
4. Discovery: sigue el flujo del componente sin modificar código. Crea `HALLAZGOS.md` con evidencia desde la raíz, IDs `H-AUTH-01`, `H-API-01` o `H-WEB-01`, y estados `Pendiente` o `No encontrado`. Si el archivo ya existe, conserva las filas de los demás componentes. No presupongas que toda la lista de `RETO.md` está confirmada.
5. Selecciona hallazgos y crea o actualiza `PLAN.md`. Registra rutas, contratos, pruebas previas y posteriores. Una persona revisa y registra su decisión antes de implementar.
6. Ejecuta caracterización, implementa solo el alcance aprobado y ejecuta regresión. Actualiza cada hallazgo con evidencia real. Si el contrato entre auth y api cambia, revisa ambos consumidores y coordina el cambio.
7. Otra persona revisa el diff y confirma o reabre hallazgos. Una revisión de IA no reemplaza esa decisión.
8. Crea o actualiza `docs/ENDPOINTS.md` por componente, sin inventar endpoints o comportamientos.
9. Crea o actualiza `docs/TESTING.md` con casos válidos, borde y error, comandos, resultados y cobertura. Si añades código o pruebas tras la revisión, vuelve a revisar el diff.
10. Revisa los archivos que vas a incluir, haz commit y solicita la revisión humana de cierre. Solo entonces crea checkpoint/day1. Las pruebas fallidas y las operaciones no ejecutadas permanecen visibles.

Para pruebas ejecuta uno de estos comandos:

```bash
python tools/lab.py test auth
python tools/lab.py test api
python tools/lab.py test web
```

python tools/lab.py test ejecuta los tres y devuelve error si falla cualquiera. Requiere haber preparado cada componente. auth genera auth/target/site/jacoco/index.html; api genera api/htmlcov/index.html; web genera web/coverage/. Registra las rutas reales de tu ejecución.

La meta del laboratorio es 85% del código modificado. El comando de api conserva además el gate de 85% de toda app, una métrica diferente. Ningún comando anterior demuestra por sí solo la cobertura del diff. Registra numerador, denominador y método; no cambies umbrales para aprobar. Antes de cerrar deben estar revisados los riesgos pendientes, no solo el porcentaje.

## Cierre local por ramas

Después de completar la revisión humana, agrega únicamente los archivos del cambio y sus evidencias, comprueba el diff preparado y haz commit. No agregues bases, secretos, dependencias ni tokens.

```bash
git status
git diff
git diff --cached
git commit -m "Lab 01: cambio y evidencias revisados"
git switch main
git merge --no-ff day1/prompting -m "Integra Lab 01 revisado"
git branch checkpoint/day1 main
```

git commit requiere que hayas preparado antes los archivos con git add indicando sus rutas. Ejecuta cada comando solo si el anterior terminó correctamente. Si el merge tiene conflictos, conserva el trabajo, resuélvelos con revisión y repite las pruebas afectadas antes de crear el checkpoint. Comprueba que git status --short no muestre cambios pendientes. Si checkpoint/day1 ya existe, comprueba su commit con git log checkpoint/day1; no la fuerces ni la muevas para ocultar un cierre anterior. La rama guarda un punto de continuidad local y no certifica calidad, ejecución remota ni aprobación. No crees esa rama sobre la plantilla vacía. No se usan tags.

Tu copia tiene origin desde la clonación. El Día 1 se completa con ramas y commits locales; no requiere push ni PR. Cuando el recorrido pida publicar, comprueba el remoto y los resultados de CI por separado; no los inventes. Los días siguientes reutilizan este producto, sus evidencias raíz y sus ramas; las guías históricas que esperan tres repositorios necesitan adaptación.

## Procedencia de las plantillas

- `auth/`: `workshop-auth-backend-template` en `17db05a4a2675a91cd3bc466046d17b9860b8805`.
- `api/`: `workshop-requirements-api-template` en `c1d308ad8f7f0ee3150cbaa214a8e2d4260d072f`.
- `web/`: `workshop-login-frontend-template` en `67b57a9db442708c9e9d1d7d7880d879719c1850`.

Se importaron archivos de Git, sin archivos locales ni secretos. Los repositorios originales conservan su historial.
