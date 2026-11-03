# HU-01 - Token firmado con expiracion y claims minimos

## Veredicto
**Usable con ajustes.** La historia conecta seguridad, configuracion, testing y evidencia.

## Contexto
El servicio de autenticacion expone `POST /auth/login` y devuelve `token`, `expiresIn` y `user`. La implementacion usa firma HMAC y lee `AUTH_TOKEN_SECRET`.

## Ajustes necesarios
- Exigir los claims `sub`, `client`, `iat` y `exp`.
- Obtener `client` desde la solicitud, validarlo y reflejarlo en el token.
- Comprobar que `expiresIn` coincide con `exp - now` usando una tolerancia documentada.
- Eliminar el secreto por defecto fuera del laboratorio o documentarlo como exclusivo de laboratorio.
- Evitar logs del token y del secreto.
- Mantener como decision tecnica si se conserva HMAC o se adopta una libreria JWT.

## Criterios de aceptacion
- Un token valido contiene `sub`, `client`, `iat` y `exp`.
- `expiresIn` es coherente con `exp - now` dentro de la tolerancia definida.
- Login valido, credenciales invalidas y token expirado tienen pruebas.
- No se registran tokens, secretos ni valores sensibles.
- La decision sobre HMAC frente a una libreria JWT queda documentada.

## Evidencia esperada
- Documento generado por los Skills a partir de este archivo.
- Pruebas de claims, expiracion y errores de autenticacion.
- Configuracion documentada de `AUTH_TOKEN_SECRET`.
- Evidencia de logs redactados.