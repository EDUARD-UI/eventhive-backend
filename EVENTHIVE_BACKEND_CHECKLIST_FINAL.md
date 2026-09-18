# Eventhive Backend — Checklist final para despliegue

## Estado de la revisión

Se revisó el código actual del ZIP y se comparó con los pendientes históricos. Varias correcciones que estaban pendientes **ya fueron implementadas** en esta versión:

- URL firmada para el RUT.
- `urlRut` ya no se expone en los DTO de respuesta.
- Validación de extensión y firma básica del contenido de archivos.
- Límite del RUT unificado a 5 MB.
- Endpoint `BORRADOR/EN_CORRECCION → PENDIENTE_REVISION`.
- Flujo de corrección y reenvío de solicitudes de organización.
- Protección por roles en los endpoints principales.
- Idempotencia y control de concurrencia básica en compras.

Por eso no conviene volver a implementar esos puntos. El trabajo final debe concentrarse en los siguientes ajustes.

---

# 1. CRÍTICO — Revisar la base de datos de producción

Actualmente el proyecto tiene:

`spring.jpa.hibernate.ddl-auto=none`

y no contiene migraciones Flyway ni archivos SQL de esquema.

### Antes del deploy

- [ ] Crear/verificar todas las tablas en la BD de producción.
- [ ] Verificar claves primarias y foráneas.
- [ ] Verificar índices y restricciones únicas.
- [ ] Verificar columnas `NOT NULL`.
- [ ] Verificar que los nombres de tablas/columnas coincidan con las entidades JPA.
- [ ] Verificar especialmente:

`tiquetes.evento_id → eventos.id`

- [ ] Verificar que PostgreSQL tenga habilitado **PostGIS**, porque `Evento` utiliza información geoespacial.
- [ ] Probar el backend contra una BD limpia o una copia equivalente a producción.

### Importante

Este es el punto que más puede impedir el despliegue. El backend no creará las tablas automáticamente porque `ddl-auto` está en `none`.

Para esta entrega no es obligatorio introducir Flyway si quieren mantener el alcance controlado, pero sí deben dejar la BD de producción completamente preparada.

---

# 2. CRÍTICO — RUT y almacenamiento privado

Esta parte ya está bastante bien implementada.

Actualmente:

- `OrganizacionDTO` no expone `urlRut`.
- `SolicitudVerificacionDTO` no expone `urlRut`.
- Existe:

`GET /api/organizaciones/{organizacionId}/rut-url`

- El servicio comprueba que el usuario sea administrador/moderador o representante de esa organización.
- Se genera una URL firmada temporal.
- El bucket de verificaciones debe permanecer privado.

### Solo verificar

- [ ] Confirmar en Supabase que `verificacion-docs` sea realmente privado.
- [ ] Confirmar que la URL firmada funcione en producción.
- [ ] Confirmar que un cliente normal no pueda obtener el RUT de otra organización.
- [ ] Confirmar que el enlace expire correctamente.

No hace falta modificar los DTO actuales por este punto.

---

# 3. CRÍTICO — Flujo de eventos

El endpoint faltante histórico ya existe:

`PATCH /api/eventos/{id}/enviar-revision`

También existe:

`PATCH /api/eventos/{id}/retirar`

y el servicio permite:

`BORRADOR / EN_CORRECCION → PENDIENTE_REVISION`

### Probar

- [ ] Crear evento como organización.
- [ ] Confirmar estado inicial según nivel.
- [ ] Si queda `BORRADOR`, enviarlo a revisión.
- [ ] Moderador solicita corrección.
- [ ] Evento queda `EN_CORRECCION`.
- [ ] Organización corrige.
- [ ] Volver a enviarlo a revisión.
- [ ] Moderador aprueba.
- [ ] Evento queda `PUBLICADO`.
- [ ] Confirmar que un evento no pueda saltarse estados inválidos.

---

# 4. CRÍTICO — Seguridad de autorización

La mayoría de endpoints ya tiene `@PreAuthorize` y los servicios tienen comprobaciones de pertenencia.

### Hacer una prueba manual de IDOR

Con dos cuentas/organizaciones diferentes:

- [ ] Intentar editar el evento de otra organización.
- [ ] Intentar cancelar el evento de otra organización.
- [ ] Intentar eliminar el evento de otra organización.
- [ ] Intentar modificar localidades de otro evento.
- [ ] Intentar modificar operadores de otra organización.
- [ ] Intentar obtener el RUT de otra organización.
- [ ] Intentar hacer check-in con un operador de otra organización.
- [ ] Intentar consultar una compra de otro usuario.

La respuesta debe ser `403` o `404`, según el caso, nunca entregar/modificar el recurso.

---

# 5. IMPORTANTE — Corrección de solicitudes de organización

El flujo ya fue mejorado:

`CORRECCION_SOLICITADA → PENDIENTE`

y ahora permite actualizar:

- razón social
- NIT
- correo empresarial
- RUT opcionalmente

### Falta revisar

En `reenviarSolicitud()` actualmente no se vuelven a comprobar:

- [ ] NIT duplicado.
- [ ] correo empresarial duplicado.

Antes de guardar los datos corregidos, volver a validar:

`organizacionRepository.existsByNit(...)`

y

`organizacionRepository.existsByCorreoContacto(...)`

Esto debe hacerse excluyendo, si corresponde, la organización/solicitud actual.

**Este sí es un ajuste de código recomendable antes del deploy.**

---

# 6. IMPORTANTE — Validación de archivos

Actualmente ya existe validación de:

- tamaño
- `Content-Type`
- extensión
- firma básica del contenido

Se aceptan:

- PDF
- PNG
- JPG/JPEG

### Verificar

- [ ] Imagen PNG real con extensión `.png` → aceptar.
- [ ] JPG real con `.jpg` → aceptar.
- [ ] PDF real con `.pdf` → aceptar.
- [ ] Cambiar `.exe` a `.pdf` → rechazar.
- [ ] Cambiar una imagen a `.pdf` → rechazar.
- [ ] Archivo mayor de 5 MB → rechazar.

No es necesario agregar otra capa compleja de validación para esta entrega.

---

# 7. IMPORTANTE — Compra y disponibilidad

La compra ya tiene:

- validación de cantidad mínima
- control de disponibilidad mediante actualización atómica
- idempotency key
- conservación del histórico de compras/tiquetes

### Probar concurrencia básica

- [ ] Dos compras simultáneas de los últimos boletos.
- [ ] Confirmar que nunca se vendan más boletos que la capacidad.
- [ ] Repetir la misma solicitud con la misma `idempotencyKey`.
- [ ] Confirmar que no cree una segunda compra.
- [ ] Cancelar una compra.
- [ ] Confirmar devolución de disponibilidad.
- [ ] Confirmar que un tiquete usado impida cancelar la compra.

---

# 8. IMPORTANTE — Check-in

Probar:

- [ ] Tiquete válido → check-in exitoso.
- [ ] Mismo QR nuevamente → rechazado.
- [ ] Tiquete de otra organización → rechazado.
- [ ] Operador sin `CHECK_IN` → rechazado.
- [ ] Representante → permitido.
- [ ] Tiquete de compra cancelada → rechazado.

---

# 9. CORS y producción

`SecurityConfig` ya tiene CORS configurable mediante:

`CORS_ALLOWED_ORIGINS`

y actualmente incluye el frontend de Vercel como valor por defecto.

### Antes del deploy

- [ ] Configurar `CORS_ALLOWED_ORIGINS` en producción.
- [ ] Dejar únicamente los dominios reales necesarios.
- [ ] No depender del dominio de desarrollo.
- [ ] Verificar que el frontend pueda hacer:
  - GET
  - POST
  - PUT
  - PATCH
  - DELETE
  - OPTIONS

---

# 10. Variables de entorno

Configurar en el servidor:

- [ ] `DB_URL`
- [ ] `DB_USERNAME`
- [ ] `DB_PASSWORD`
- [ ] `MONGO_URI`
- [ ] `MONGO_DB`
- [ ] `SUPABASE_URL`
- [ ] `SUPABASE_SECRET_KEY`
- [ ] `JWT_SECRET`
- [ ] `JWT_EXPIRATION_MS`
- [ ] `JWT_REFRESH_EXPIRATION_MS`
- [ ] `CORS_ALLOWED_ORIGINS`
- [ ] `SPRING_PROFILES_ACTIVE`

### Seguridad

- [ ] Ninguna contraseña, JWT secret o API key debe estar escrita directamente en el código.
- [ ] No subir `.env` ni secretos al repositorio.

---

# 11. MongoDB

El proyecto utiliza MongoDB además de PostgreSQL.

Antes de desplegar:

- [ ] Confirmar que `MONGO_URI` sea válido.
- [ ] Confirmar que `MONGO_DB` exista/se pueda crear.
- [ ] Confirmar que el backend realmente necesite MongoDB en producción.
- [ ] Si alguna funcionalidad depende de MongoDB, probarla antes del deploy.

Si MongoDB no se utiliza actualmente, conviene confirmar que su conexión no haga fallar el arranque.

---

# 12. Correo

El proyecto utiliza el correo como dato de cuenta y para el flujo de invitaciones, pero en el código actual `ServiceInvitacionOrganizacion` registra la invitación y **no se observa un envío real de correo**.

### Decidir antes del deploy

- [ ] Si el requisito actual solo necesita registrar/iniciar la invitación → no tocarlo.
- [ ] Si el requisito exige enviar realmente el correo → implementar/configurar el servicio de correo antes de presentar esa funcionalidad como terminada.

No bloquearía el despliegue por esto si el correo real no forma parte de la entrega actual.

---

# 13. Docker y compilación

El proyecto ya tiene:

- `Dockerfile`
- Maven Wrapper
- `spring-boot-maven-plugin`

El `Dockerfile` utiliza Java 23.

### Ejecutar localmente

`./mvnw clean package`

Luego:

`docker build -t eventhive-backend .`

Y probar:

`docker run ...`

### Verificar

- [ ] Compilación correcta.
- [ ] JAR generado.
- [ ] Imagen Docker creada.
- [ ] Contenedor inicia.
- [ ] Puerto `8080` responde.
- [ ] Conexión a PostgreSQL funciona.
- [ ] Conexión a Supabase funciona.

---

# 14. Prueba mínima después del despliegue

Después de subir el backend:

- [ ] Health/endpoint básico responde.
- [ ] Registro funciona.
- [ ] Login funciona.
- [ ] Refresh token funciona.
- [ ] Listado de eventos funciona.
- [ ] Mapa funciona.
- [ ] Organización funciona.
- [ ] Solicitud de verificación funciona.
- [ ] Moderación funciona.
- [ ] Crear evento funciona.
- [ ] Enviar evento a revisión funciona.
- [ ] Compra funciona.
- [ ] Tiquetes funcionan.
- [ ] Check-in funciona.
- [ ] Notificaciones funcionan.
- [ ] URL firmada del RUT funciona.

---

# 15. Estadísticas del moderador

El endpoint de estadísticas del moderador sigue sin existir.

Propuesta:

`GET /api/verificacion/estadisticas`

Con:

- revisados
- aprobados
- rechazados
- tiempo promedio de revisión

### Decisión

- [ ] Si es requisito de la entrega → implementarlo.
- [ ] Si no es requisito → dejarlo para la siguiente versión.

No lo considero un bloqueo técnico para desplegar el backend actual.

---

# Prioridad final

## 🔴 Hacer antes del deploy

1. Preparar/verificar completamente la BD de producción.
2. Verificar PostGIS.
3. Corregir validación de NIT/correo al reenviar una solicitud.
4. Probar autorización e IDOR.
5. Configurar variables de entorno.
6. Configurar CORS de producción.
7. Ejecutar compilación limpia.
8. Probar Docker.
9. Ejecutar prueba funcional de los flujos principales.

## 🟡 Verificar

10. RUT privado y URL firmada.
11. Compra concurrente/idempotencia.
12. Check-in.
13. MongoDB.
14. Notificaciones.
15. Correo real, si es requisito.

## 🟢 Puede quedar para después

16. Estadísticas avanzadas del moderador.
17. Nuevas funcionalidades.
18. Mejoras de arquitectura no necesarias para la entrega.

---

# Corrección recomendada antes de cerrar el código

El único cambio funcional que detecté claramente al revisar esta versión y que agregaría antes del despliegue es:

**Volver a validar NIT y correo empresarial cuando una solicitud en `CORRECCION_SOLICITADA` sea reenviada.**

El resto de los pendientes históricos importantes ya aparecen corregidos en el código actual.

---

# Criterio de "LISTO PARA DEPLOY"

El backend está listo cuando:

- [ ] La BD de producción está creada y coincide con las entidades.
- [ ] PostGIS está disponible.
- [ ] El proyecto compila.
- [ ] Docker construye correctamente.
- [ ] La aplicación arranca.
- [ ] JWT funciona.
- [ ] CORS funciona con el frontend real.
- [ ] Supabase Storage funciona.
- [ ] RUT permanece privado.
- [ ] Los permisos por rol funcionan.
- [ ] No existen accesos IDOR conocidos.
- [ ] Organización → verificación → aprobación funciona.
- [ ] Evento → revisión → aprobación funciona.
- [ ] Compra → tiquete → check-in funciona.

**Una vez cumplidos estos puntos, no conviene seguir agregando funcionalidades grandes antes del despliegue.**
