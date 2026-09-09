# Roadmap de la Plataforma de Eventos --- EventHive

## 1. Objetivo

Este documento reúne funcionalidades complementarias y mejoras del
producto.

Los estados `IMPLEMENTADO`, `EN_DESARROLLO` y `PENDIENTE` deben reflejar
el estado real del backend. No se debe marcar una funcionalidad como
implementada únicamente porque exista el diseño.

------------------------------------------------------------------------

## 2. Sistema de Notificaciones --- IMPLEMENTADO / VALIDAR

**Tecnología:** MongoDB Atlas.

Las notificaciones se almacenan en una colección NoSQL.

Casos de uso:

-   Nuevo evento relevante.
-   Actualización de un evento.
-   Cambios relacionados con una organización seguida.
-   Resultado de una revisión.
-   Recordatorios.
-   Resultado de una compra.

Las notificaciones deben dirigirse únicamente a los usuarios afectados.

Se utilizará una política de expiración aproximada de **7 días**.
MongoDB debe tener realmente creado el índice TTL correspondiente; no
basta con declarar el comportamiento en el código.

El backend se desplegará inicialmente en una sola instancia, por lo que
los procesos `@Scheduled` pueden continuar funcionando sin un mecanismo
distribuido. Si posteriormente se escala horizontalmente, deberá
incorporarse un mecanismo de bloqueo distribuido.

------------------------------------------------------------------------

## 3. Seguir Organizaciones --- IMPLEMENTADO / VALIDAR

**Tecnología:** PostgreSQL / Supabase.

Relación muchos a muchos:

``` text
Usuario ←── Seguimiento ──→ Organización
```

Reglas:

-   No permitir duplicados.
-   Un usuario puede seguir varias organizaciones.
-   Una organización puede tener muchos seguidores.
-   Seguir una organización puede generar una notificación si el
    producto lo define.

La cantidad de seguidores puede mantenerse como métrica agregada en la
organización.

El frontend debe poder consultar si el usuario actual sigue una
organización para cambiar el estado visual del botón entre `Seguir` y
`Dejar de seguir`.

------------------------------------------------------------------------

## 4. Pasarela de pago simulada --- EN DESARROLLO

**Tecnología:** Java / Spring Boot.

La pasarela será simulada, por lo que el objetivo principal es modelar
correctamente el flujo de compra y sus estados.

Flujo conceptual:

``` text
Selección de entradas
        ↓
Creación de compra/reserva
        ↓
Pago simulado
   ┌────┴────┐
   ↓         ↓
APROBADO   RECHAZADO
   ↓
Confirmación de compra
   ↓
Generación de tiquetes
```

Se deben definir estados claros para:

-   Orden.
-   Pago.
-   Tiquete.

El pago simulado no debe marcar una compra como exitosa antes de recibir
el resultado de la operación.

La lógica de disponibilidad y capacidad debe ejecutarse en el backend
para evitar sobreventa.

También debe quedar preparada la separación conceptual entre:

-   Valor pagado por el cliente.
-   Comisión de EventHive.
-   Valor correspondiente a la organización.

Esto permitirá evolucionar posteriormente hacia una pasarela real.

Las cancelaciones deben ser seguras ante solicitudes simultáneas. Si dos
solicitudes intentan cancelar la misma compra, solo una debe poder
realizar la devolución y restaurar inventario.

------------------------------------------------------------------------

## 5. Mapa de Eventos --- EN DESARROLLO / VALIDAR

**Tecnología:** PostgreSQL + PostGIS.

Las ubicaciones pueden almacenarse mediante tipos geográficos de
PostGIS.

Casos de uso:

-   Mostrar ubicación.
-   Buscar eventos cercanos.
-   Calcular distancia.
-   Filtrar eventos por proximidad.

La lógica geográfica debe mantenerse en el backend/base de datos y no
depender exclusivamente del frontend.

Las búsquedas por radio deben tener límites razonables para evitar
consultas geográficas demasiado costosas.

------------------------------------------------------------------------

## 6. Integración con Calendario --- PENDIENTE

El backend puede generar archivos `.ics` utilizando:

-   Nombre del evento.
-   Descripción.
-   Fecha.
-   Hora.
-   Ubicación.
-   Duración, cuando esté disponible.

El usuario podrá importar el archivo en aplicaciones compatibles con
iCalendar.

No es necesario almacenar el archivo si puede generarse dinámicamente.

------------------------------------------------------------------------

## 7. Reputación de Organizaciones --- IMPLEMENTADO / VALIDAR

**Tecnología:** PostgreSQL.

Las valoraciones se almacenan en una entidad relacionada con:

-   Usuario.
-   Organización.
-   Calificación.
-   Comentario.
-   Fecha.

Reglas:

-   Una valoración por usuario y organización.
-   La actualización reemplaza la valoración anterior.
-   Calificación entre 1 y 5.
-   Un Representante u Operador perteneciente a la organización no puede
    valorarla.

La organización puede mantener métricas agregadas:

-   `ratingPromedio`.
-   `totalValoraciones`.

Las métricas deben actualizarse de forma consistente con las operaciones
de valoración.

------------------------------------------------------------------------

## 8. Lista de espera --- PENDIENTE

Funcionalidad opcional para eventos agotados.

Flujo propuesto:

``` text
Evento agotado
      ↓
Usuario entra en lista de espera
      ↓
Se libera una capacidad
      ↓
Sistema selecciona al siguiente usuario
      ↓
Notificación
```

Antes de implementarla deben definirse:

-   Orden de asignación.
-   Tiempo para aceptar la oportunidad.
-   Qué sucede si el usuario no compra.
-   Integración con notificaciones.
-   Manejo de concurrencia.

------------------------------------------------------------------------

## 9. Recomendación de eventos --- PENDIENTE

Funcionalidad futura.

Se podrían utilizar señales como:

-   Categorías consultadas.
-   Eventos comprados.
-   Eventos asistidos.
-   Organizaciones seguidas.

La recomendación con IA deberá integrarse como una funcionalidad
adicional y no sustituir las búsquedas y filtros normales.

No debe implementarse IA únicamente por añadir IA. Primero debe
comprobarse que existe suficiente información y una necesidad real de
recomendación.

------------------------------------------------------------------------

## 10. Gestión de organizaciones --- EN DESARROLLO / VALIDAR

EventHive está orientado principalmente a las organizaciones como actor
comercial.

Además de publicar eventos, la organización deberá contar con
herramientas sencillas para gestionar su actividad.

Se contemplan:

``` text
GET /api/organizaciones/{id}/estadisticas
GET /api/organizaciones/{id}/ventas
GET /api/organizaciones/{id}/eventos
GET /api/organizaciones/{id}/promociones
```

Las métricas iniciales serán:

-   Eventos creados.
-   Eventos publicados.
-   Eventos finalizados.
-   Eventos cancelados.
-   Entradas vendidas.
-   Ingresos.
-   Eventos con mayor venta.
-   Seguidores.
-   Valoración promedio.
-   Cantidad de valoraciones.

El frontend debe mostrar estas métricas en un panel de organización,
separado del perfil público.

------------------------------------------------------------------------

## 11. Flujo de eventos --- EN DESARROLLO / VALIDAR

El flujo recomendado es:

``` text
BORRADOR
   ↓
PENDIENTE_REVISION
   ├── PUBLICADO
   ├── EN_CORRECCION → PENDIENTE_REVISION
   └── RECHAZADO
```

Para organizaciones de nivel 3 puede habilitarse:

``` text
BORRADOR
   ↓
VALIDACIONES AUTOMÁTICAS
   ↓
PUBLICADO
```

La publicación automática no elimina la posibilidad de suspensión
administrativa posterior.

El frontend debe utilizar el estado recibido por el backend para decidir
qué acciones mostrar.

------------------------------------------------------------------------

## 12. Localidades y aforo --- EN DESARROLLO / VALIDAR

El aforo del evento debe derivarse de las capacidades de sus
localidades:

``` text
Aforo = SUM(capacidad de localidades)
```

No se recomienda mantener un segundo campo independiente de `aforo` en
`Evento`, porque puede quedar inconsistente.

Si un evento no tiene localidades y la regla de negocio lo permite, el
backend puede crear automáticamente una localidad `General`.

La creación debe ser idempotente para evitar múltiples localidades
`General` cuando se reintente la operación.

Antes de publicar, debe existir al menos una localidad con capacidad
válida.

------------------------------------------------------------------------

## 13. Archivos y almacenamiento --- EN DESARROLLO / VALIDAR

Separación recomendada:

``` text
event-images       → público
category-images    → público
verification-docs  → privado
```

El RUT debe estar en almacenamiento privado.

El backend debe validar:

-   Tamaño.
-   Content-Type.
-   Extensión.
-   Tipo de recurso.
-   Nombre interno.

Extensiones permitidas:

**Imágenes:** `.jpg`, `.jpeg`, `.png`

**RUT:** `.pdf`, `.jpg`, `.jpeg`, `.png`

Los nombres internos deben generarse con UUID.

------------------------------------------------------------------------

## 14. Seguridad y usuarios --- EN DESARROLLO / VALIDAR

La autenticación será stateless mediante JWT.

El frontend debe diferenciar:

-   `401`: sesión/token no válido o ausente.
-   `403`: autenticado pero sin permisos.

Por ahora no se requiere una blacklist de tokens para logout. El cliente
puede eliminar su token y finalizar la sesión local.

La eliminación física de usuarios debe evitarse cuando destruya
relaciones históricas. Se prioriza la desactivación lógica.

El backend debe validar siempre roles, pertenencia a organización y
propiedad del recurso.

------------------------------------------------------------------------

## 15. Nivel de organizaciones --- PENDIENTE / VALIDAR

Propuesta inicial:

``` text
NIVEL 1 → NIVEL 2 → NIVEL 3
```

Referencia:

-   Nivel 1 → 2: 3 eventos finalizados + 30 días + buen estado.
-   Nivel 2 → 3: 8 eventos finalizados + 90 días + buen estado.

La evaluación de ascenso se realiza cuando un evento pasa a
`FINALIZADO`.

Un único rechazo no debería bloquear permanentemente el ascenso; deben
considerarse el historial y las sanciones activas.

------------------------------------------------------------------------

## 16. Prioridad recomendada

Para el estado actual del proyecto:

1.  Consolidar estados y permisos de Organizaciones.
2.  Consolidar flujo de Moderación.
3.  Completar compra + pasarela simulada + estados de pago.
4.  Validar notificaciones y seguimiento.
5.  Completar mapa/PostGIS.
6.  Integrar calendario.
7.  Mejoras opcionales: lista de espera y recomendaciones.

## 17. Regla de arquitectura

Cada funcionalidad debe tener una responsabilidad clara:

-   **Controller:** HTTP y validación de entrada.
-   **Service:** reglas de negocio.
-   **Repository:** acceso a datos.
-   **Entity/DTO:** representación de datos.
-   **Security:** autenticación y autorización.
-   **Notificaciones:** eventos y avisos desacoplados cuando sea
    conveniente.

El frontend no debe ser la fuente de verdad de las reglas de negocio.
