# Instrucciones de AI Labs

Trabaja desde la raiz de ai-labs, el unico repositorio Git. Aplica las instrucciones de la seccion del componente elegido: auth, api o web. No leas ni modifiques codigo de otros componentes sin ampliar el alcance acordado.

Ejecuta Git desde la raiz y las pruebas con el directorio del componente. Desde la raiz puedes usar python tools/lab.py test auth, api o web, segun corresponda. Guarda evidencias generales en la raiz, con componente e IDs H-AUTH-NN, H-API-NN o H-WEB-NN; conserva las secciones existentes. Crea cada entregable cuando lo pida su paso.

En el Dia 1 usa solo ramas locales: day1/prompting, main y checkpoint/day1. Conserva origin creado al clonar tu copia del template; no crees otros remotos, tags ni PR y no hagas push durante el Dia 1. La revision y aprobacion humanas y las pruebas reales preceden a la integracion y al checkpoint; no las inventes. Las referencias a PR y controles remotos de las secciones originales son generales: conserva su configuracion, pero en el Dia 1 prevalece este cierre local.

## auth

### Contrato para agentes

1. Analiza el flujo completo antes de cambiar autenticación.
2. Conserva el contrato de `POST /auth/login`.
3. Crea pruebas de caracterización antes de refactorizar persistencia o token.
4. No silencies CodeQL, Dependency Review ni JaCoCo.
5. No agregues secretos reales; usa variables de entorno y valores de prueba aislados.
6. Separa API, servicio, repositorio y seguridad en cambios revisables.
7. Meta de cobertura del código modificado: 85% o superior.

#### Clean code

8. Cada método hace una sola cosa; si un método valida, accede a datos y arma
   la respuesta a la vez (como `AuthResource.login` hoy), sepáralo en capas.
9. Nombres que expliquen el propósito, no el tipo (evitar `data`, `temp`,
   `result2`, `handleStuff`).
10. Sin duplicación: un bloque de código repetido en dos lugares se extrae a
    un método reutilizable.
11. Evitar anidamiento profundo (`if` dentro de `if` dentro de `if`); usar
    guard clauses / retorno anticipado en su lugar.
12. Sin números o strings "mágicos" sueltos en el código (por ejemplo `3600`
    para la expiración del token); usar constantes con nombre.

## api

### Contrato para agentes

1. Conserva los endpoints y formatos usados por el frontend.
2. Agrega pruebas de caracterización antes de separar el monolito.
3. No conviertas un hallazgo en un `# noqa` o exclusión de scanner.
4. Mantén consultas parametrizadas y autorización ligada a la identidad.
5. Controla nombre, tamaño y extensión de archivos.
6. No uses secretos reales ni valores de producción.
7. Meta de cobertura del código modificado: 85% o superior.

#### Clean code

8. Cada función hace una sola cosa; si una ruta valida, arma el SQL y
   serializa la respuesta a la vez (como en `app/main.py` hoy), sepáralo en
   capas (ruta / servicio / repositorio).
9. Nombres que expliquen el propósito, no el tipo (evitar `data`, `temp`,
   `result2`, `handle_stuff`).
10. Sin duplicación: un bloque de código repetido en dos lugares se extrae a
    una función reutilizable.
11. Evitar anidamiento profundo (`if` dentro de `if` dentro de `if`, como en
    `summary()` hoy); usar retorno anticipado o condiciones combinadas.
12. Sin números o strings "mágicos" sueltos en el código; usar constantes con
    nombre.

## web

### Contrato para agentes de desarrollo

1. Analiza antes de modificar y enumera los riesgos funcionales.
2. Conserva los endpoints `/auth/login` y `/api/tasks`.
3. Agrega o mejora pruebas antes de una refactorización amplia.
4. No ocultes alertas de seguridad ni reduzcas los quality gates.
5. No introduzcas secretos reales ni dependencias nuevas sin justificar.
6. Propón cambios incrementales que puedan revisarse mediante PR.
7. Meta de cobertura del código modificado: 85% o superior.

#### Clean code

8. Cada componente hace una sola cosa; si un componente llama HTTP, maneja
   sesión y navega a la vez (como `LoginComponent`/`DashboardComponent` hoy),
   separa la llamada HTTP a un servicio.
9. Nombres que expliquen el propósito, no el tipo (evitar `data`, `temp`,
   tipar todo como `any`).
10. Sin duplicación: un bloque de código repetido en dos componentes se
    extrae a un servicio o función reutilizable.
11. Evitar anidamiento profundo (`if` dentro de `if` dentro de `if`, como en
    `login()` hoy); usar retorno anticipado o condiciones combinadas.
12. Sin valores "mágicos" sueltos en el código (URLs, claves de
    `localStorage`); centralizarlos en un solo lugar.
