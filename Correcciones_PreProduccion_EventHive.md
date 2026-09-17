# EventHive --- Correcciones y ajustes previos a producción

## Objetivo

Este documento consolida las correcciones funcionales, de arquitectura,
seguridad, persistencia y documentación que se realizarán antes de
llevar EventHive a producción.

La intención es que este documento sirva como guía común para backend y
frontend y que, una vez aplicados los cambios, el comportamiento de los
módulos quede alineado entre:

-   Backend.
-   Frontend.
-   Modelo de datos.
-   Estados del sistema.
-   Permisos y roles.
-   Almacenamiento de archivos.
-   Documentación funcional.

> **Alcance:** este documento recoge las decisiones acordadas para la
> siguiente iteración. No pretende introducir funcionalidades que no
> hayan sido aprobadas.

------------------------------------------------------------------------

# 1. Corrección de referencias a `ServiceEvento`

Actualmente existen servicios y controladores que llaman métodos
antiguos o inexistentes de `ServiceEvento`.

### Problema

Se encontraron referencias a:

``` java
serviceEvento.obtenerEventoPorId(eventoId)
serviceEvento.obtenerEventoAdminPorId(id)
```

pero estos métodos no forman parte de la API actual de `ServiceEvento`.

### Corrección

Se deberán actualizar las llamadas de los demás servicios y
controladores para utilizar los métodos vigentes de `ServiceEvento`,
respetando el contexto desde el cual se consulta el evento.

La API interna debe quedar conceptualmente separada:

``` text
obtenerEventoPublicoPorId()
obtenerEventoAdministrativoPorId()
obtenerEventoDeOrganizacionPorId()
obtenerReferenciasEvento()
```

No se deben crear métodos duplicados únicamente para conservar nombres
antiguos.

### Objetivo

Evitar errores de compilación y mantener una única fuente de verdad para
las consultas de eventos.

------------------------------------------------------------------------

# 2. Dockerfile y Java 23

El proyecto utiliza Java 23 y el Dockerfile debe ser coherente con esa
versión.

## Dockerfile objetivo

``` dockerfile
FROM maven:3.9.6-eclipse-temurin-23 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:23-jre

WORKDIR /app

COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Consideraciones

-   La imagen de compilación utiliza Java 23.
-   La imagen final utiliza Java 23.
-   El nombre del JAR coincide con el `artifactId` actual: `app`.
-   No se utilizará Java 17.
-   La configuración del proyecto y Docker deben mantener la misma
    versión principal de Java.

------------------------------------------------------------------------

# 3. CORS

La configuración actual de CORS se mantendrá como está definida en el
proyecto.

No se realizará en esta iteración una modificación de la lógica de CORS.

La configuración de los dominios de producción se realizará
posteriormente en la configuración correspondiente del entorno.

------------------------------------------------------------------------

# 4. Separación de buckets públicos y privados

Se establecerá una separación clara entre archivos públicos y archivos
sensibles.

## Buckets públicos

Solamente serán públicos los recursos destinados a mostrarse
directamente en la plataforma:

``` text
event-images
category-images
```

Estos podrán utilizar URLs públicas.

## Bucket privado

Los documentos de verificación de organizaciones, especialmente el RUT,
se almacenarán en un bucket privado:

``` text
verification-docs
```

El RUT nunca deberá exponerse mediante una URL pública permanente.

## Flujo para visualizar el RUT

``` text
Moderador / Administrador
        |
        v
Solicita visualizar RUT
        |
        v
Backend valida permisos
        |
        v
Backend genera URL firmada temporal
        |
        v
Frontend abre documento
```

La URL firmada debe tener una duración limitada.

El frontend no debe construir ni almacenar permanentemente la URL del
RUT.

------------------------------------------------------------------------

# 5. Base de datos y migraciones

La aplicación continuará utilizando:

``` properties
spring.jpa.hibernate.ddl-auto=none
```

para producción.

Sin embargo, el esquema de PostgreSQL no debe depender de modificaciones
manuales.

Se incorporará un sistema de migraciones, preferiblemente Flyway.

## Estructura propuesta

``` text
src/main/resources/
└── db/
    └── migration/
        ├── V1__initial_schema.sql
        ├── V2__notifications.sql
        ├── V3__postgis.sql
        └── V4__...
```

Cada modificación estructural futura deberá realizarse mediante una
nueva migración.

## Invariantes importantes

Las restricciones importantes deben quedar protegidas tanto por lógica
de aplicación como, cuando corresponda, por PostgreSQL.

Ejemplos:

``` text
Usuario.correo → UNIQUE
Organizacion.nit → UNIQUE
Valoracion(usuario, organizacion) → UNIQUE
Seguidor(usuario, organizacion) → UNIQUE
Localidad.capacidad > 0
Localidad.disponibles >= 0
Localidad.disponibles <= capacidad
Valoracion.calificacion entre 1 y 5
Promocion.descuento dentro del rango permitido
```

El objetivo es evitar que una condición inválida pueda aparecer
únicamente por una carrera o error de aplicación.

------------------------------------------------------------------------

# 6. Restricción de valoraciones para miembros de la organización

Los usuarios con rol:

``` text
REPRESENTANTE
OPERADOR
```

no podrán valorar a la organización a la que pertenecen.

La validación no debe limitarse al representante.

## Regla

``` text
Usuario pertenece a la organización
        |
        +-- REPRESENTANTE → no puede valorar
        |
        +-- OPERADOR      → no puede valorar
```

Un cliente que no pertenezca a la organización sí podrá valorar, siempre
que cumpla las demás condiciones del sistema.

------------------------------------------------------------------------

# 7. Nueva valoración reemplaza la anterior

Cada usuario podrá tener como máximo una valoración por organización.

Si vuelve a valorar la misma organización, la nueva valoración
sustituirá la anterior.

## Flujo

``` text
Cliente
  |
  | primera valoración
  v
Valoración creada
  |
  v
Métricas actualizadas


Cliente
  |
  | nueva valoración
  v
Valoración existente
  |
  v
Actualizar valoración
  |
  v
Recalcular métricas
```

No se crearán múltiples registros históricos de valoración para el mismo
usuario y organización.

La restricción de unicidad debe mantenerse:

``` text
(usuario_id, organizacion_id) UNIQUE
```

## API recomendada

La operación de creación/actualización puede mantenerse bajo el endpoint
definido por el backend, pero la semántica debe ser consistente:

``` text
POST /valoraciones
```

-   Si no existe valoración → crear.
-   Si existe valoración → actualizar la existente.

------------------------------------------------------------------------

# 8. Comentario opcional

El comentario de una valoración es opcional.

La calificación continúa siendo obligatoria.

Ejemplo válido:

``` json
{
  "calificacion": 5,
  "comentario": "Excelente organización."
}
```

También debe ser válido:

``` json
{
  "calificacion": 5
}
```

Se mantendrán las validaciones de longitud máxima correspondientes.

------------------------------------------------------------------------

# 9. Localidad General y eliminación del aforo duplicado

El aforo no se almacenará como un atributo independiente dentro de
`Evento`.

La capacidad total del evento será determinada por sus localidades.

## Modelo

``` text
Evento
  |
  +-- Localidad General
  +-- Localidad VIP
  +-- Localidad Preferencial
```

El aforo total será:

``` text
SUM(localidad.capacidad)
```

De esta manera se evita almacenar dos valores que podrían quedar
desincronizados.

## Localidad General automática

Cuando un evento no tenga localidades configuradas y el flujo requiera
disponer de una localidad vendible, el sistema podrá crear
automáticamente una localidad:

``` text
Nombre: General
Precio: valor definido por el evento
Capacidad: capacidad definida para el evento
Disponibles: capacidad
```

La creación automática debe ejecutarse una sola vez.

No se deben crear varias localidades `General` por reintentos del mismo
proceso.

## Regla

``` text
Evento sin localidades
        |
        v
Crear General
        |
        v
Evento con localidad
        |
        v
No volver a crear General
```

## Validaciones

Una vez completado el evento:

-   Debe existir al menos una localidad.
-   La capacidad debe ser mayor que cero.
-   Disponibles no puede ser negativo.
-   Disponibles no puede superar la capacidad.
-   El aforo total se obtiene de la suma de capacidades.

------------------------------------------------------------------------

# 10. Nuevo flujo de eventos mediante estados

Se utilizarán los estados del evento de forma más estricta.

La creación del evento no implica que esté listo para publicarse.

## Flujo general

``` text
BORRADOR
    |
    | Completar información
    v
PENDIENTE_REVISION
    |
    +------------------+
    |                  |
    v                  v
PUBLICADO         EN_CORRECCION
                       |
                       | corregir y reenviar
                       v
                 PENDIENTE_REVISION
```

También pueden existir los estados administrativos correspondientes:

``` text
RECHAZADO
CANCELADO
SUSPENDIDO
FINALIZADO
```

según las reglas ya definidas por el dominio.

## Organización nivel 1 y 2

Los eventos de organizaciones nivel 1 y 2 deberán pasar por moderación:

``` text
BORRADOR
   |
   | Enviar a revisión
   v
PENDIENTE_REVISION
   |
   v
Moderador
   |
   +--> PUBLICADO
   |
   +--> EN_CORRECCION
   |
   +--> RECHAZADO
```

## Organización nivel 3

Las organizaciones de nivel 3 podrán publicar directamente después de
superar las validaciones automáticas:

``` text
BORRADOR
   |
   | Publicar
   v
VALIDACIONES AUTOMÁTICAS
   |
   v
PUBLICADO
```

No significa que los eventos de nivel 3 queden fuera del control
administrativo. Administración y moderación podrán aplicar las acciones
correspondientes posteriormente cuando exista una razón válida.

------------------------------------------------------------------------

# 11. Reglas más razonables para ascenso de organización

El ascenso de nivel no dependerá de que la organización nunca haya
tenido un rechazo.

Un único rechazo no debe bloquear permanentemente el crecimiento de una
organización.

## Criterios propuestos

### Nivel 1 → Nivel 2

Requisitos:

``` text
- Al menos 3 eventos finalizados.
- Al menos 30 días de actividad desde la aprobación.
- Buen comportamiento general.
- Sin sanción administrativa activa.
```

### Nivel 2 → Nivel 3

Requisitos:

``` text
- Al menos 8 eventos finalizados.
- Al menos 90 días de actividad desde la aprobación.
- Buen comportamiento general.
- Sin sanción administrativa activa.
```

## Buen comportamiento

La evaluación podrá considerar:

``` text
- Eventos aprobados.
- Eventos rechazados.
- Cancelaciones.
- Incidencias administrativas.
- Cumplimiento general.
```

Un rechazo aislado no bloqueará permanentemente el ascenso.

La lógica debe permitir que una organización mejore su historial.

------------------------------------------------------------------------

# 12. Evaluación del nivel únicamente al finalizar eventos

La evaluación de ascenso se realizará únicamente cuando un evento pase
a:

``` text
FINALIZADO
```

No se ejecutará el ascenso automáticamente al rechazar un evento.

## Flujo

``` text
Evento
  |
  v
FINALIZADO
  |
  v
Evaluar organización
  |
  +--> ¿cumple requisitos?
          |
          +-- Sí → ascender
          |
          +-- No → mantener nivel
```

Esto mantiene la lógica simple y evita evaluaciones innecesarias.

------------------------------------------------------------------------

# 13. Cancelación de compras segura ante concurrencia

La cancelación de una compra debe ser una operación condicional e
idempotente.

## Problema que se evita

Dos solicitudes simultáneas no deben devolver dos veces las mismas
entradas.

Ejemplo:

``` text
Compra CONFIRMADA
       |
       +---- Solicitud A
       |
       +---- Solicitud B
```

Solo una solicitud debe conseguir cambiar el estado de:

``` text
CONFIRMADA → CANCELADA
```

## Flujo

``` text
UPDATE compra
SET estado = CANCELADA
WHERE id = ?
  AND cliente_id = ?
  AND estado IN (PENDIENTE, CONFIRMADA)
```

Si:

``` text
filas actualizadas = 1
```

se restituyen las entradas.

Si:

``` text
filas actualizadas = 0
```

la compra ya fue cancelada o no pertenece al usuario y no se deben
devolver entradas nuevamente.

------------------------------------------------------------------------

# 14. Logout y módulo `security`

Se mantendrá un enfoque JWT stateless.

El logout será simple:

``` text
Frontend
   |
   | elimina access token / refresh token almacenado
   v
Sesión local cerrada
```

No se implementará por ahora una blacklist de JWT ni Redis únicamente
para invalidar tokens.

## Objetivo

Toda la carpeta `security` debe quedar funcional y estable para
facilitar la integración con los desarrolladores frontend.

Debe estar correctamente definido y probado:

``` text
- Login
- Registro
- JWT
- Autenticación
- Roles
- Autorización
- Usuario autenticado
- Manejo de expiración
- Logout del cliente
```

El backend debe devolver respuestas consistentes para que el frontend
pueda distinguir:

``` text
401 → no autenticado / token inválido
403 → autenticado pero sin permisos
```

El login no debe revelar si un correo existe o no. Para credenciales
incorrectas se debe utilizar una respuesta uniforme.

------------------------------------------------------------------------

# 15. Eliminación lógica de usuarios

No se recomienda eliminar físicamente usuarios que ya tengan información
histórica.

Un usuario puede estar relacionado con:

``` text
Compras
Tiquetes
Valoraciones
Seguimientos
Organizaciones
Eventos
Historial de moderación
Notificaciones
```

Por ello se utilizará una estrategia de eliminación lógica o
desactivación.

## Concepto

``` text
Usuario
  |
  +-- activo = true
  |
  +-- activo = false
```

Un usuario desactivado no podrá iniciar sesión ni utilizar las
funcionalidades que requieran una cuenta activa.

La información histórica se conserva.

------------------------------------------------------------------------

# 16. Validación de DTO

Todos los DTO de entrada deben utilizar las validaciones de Bean
Validation correspondientes.

Los controladores deberán utilizar:

``` java
@Valid
```

cuando corresponda.

## Validaciones esperadas

### Login

``` text
correo → obligatorio + formato email
contraseña → obligatoria
```

### Registro de cliente

``` text
nombre → obligatorio
correo → obligatorio + email
contraseña → obligatoria
```

### Registro de organización

``` text
razón social → obligatorio
correo empresarial → obligatorio + email
NIT → obligatorio
representante → obligatorio
RUT → obligatorio
```

### Solicitud de verificación

Los campos obligatorios deben validarse antes de iniciar el proceso de
almacenamiento.

### Valoración

``` text
calificación → obligatoria, entre 1 y 5
comentario → opcional, con límite de longitud
```

### Otros DTO

Todo DTO que reciba información del frontend deberá revisar:

``` text
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
@Positive
```

según la naturaleza de cada campo.

La validación de DTO no reemplaza las reglas de negocio del Service.

------------------------------------------------------------------------

# 17. Coherencia del bucket de RUT

La configuración de almacenamiento debe diferenciar claramente:

``` text
PUBLICO
├── event-images
└── category-images

PRIVADO
└── verification-docs
```

El servicio de almacenamiento debe conocer qué tipo de recurso está
procesando.

## Reglas

### Imágenes de eventos

``` text
Bucket público
Extensiones permitidas:
.jpg
.jpeg
.png
```

### Imágenes de categorías

``` text
Bucket público
Extensiones permitidas:
.jpg
.jpeg
.png
```

### RUT

``` text
Bucket privado
Extensiones permitidas:
.pdf
.jpg
.jpeg
.png
```

El backend deberá generar nombres internos mediante UUID y no depender
del nombre original enviado por el usuario.

No se permitirá utilizar extensiones arbitrarias.

------------------------------------------------------------------------

# 18. Validación y manejo de archivos

La validación de archivos se realizará en el backend.

No se confiará únicamente en la extensión enviada por el cliente.

## Reglas generales

Validar:

``` text
- Tamaño máximo.
- Content-Type permitido.
- Extensión permitida.
- Tipo de recurso.
- Nombre generado por el servidor.
```

Los archivos se almacenarán con nombres generados por UUID.

Ejemplo:

``` text
9b8c6a2e-....png
```

en lugar de conservar directamente:

``` text
rut_empresa_juan.pdf
```

Esto evita problemas de nombres, colisiones y rutas no deseadas.

Los límites configurados en Spring Multipart y los límites definidos por
el servicio de almacenamiento deben ser coherentes.

------------------------------------------------------------------------

# 19. TTL de MongoDB para notificaciones

Las notificaciones se mantendrán en MongoDB con una expiración de
aproximadamente 7 días.

La aplicación deberá garantizar que MongoDB cree el índice TTL.

## Requisito

La creación automática de índices deberá estar habilitada o el índice
deberá crearse explícitamente durante la preparación del entorno.

Ejemplo conceptual:

``` text
fechaCreacion
    |
    v
TTL = 7 días
    |
    v
MongoDB elimina el documento
```

No es necesario que la eliminación ocurra exactamente en el segundo en
que se cumplen los 7 días.

Lo importante es que el índice TTL exista y que MongoDB pueda ejecutar
la eliminación automática.

------------------------------------------------------------------------

# 20. Jobs programados y despliegue con una sola instancia

EventHive se desplegará inicialmente con una sola instancia del backend.

Los procesos `@Scheduled` continuarán funcionando normalmente.

No se implementará por ahora un sistema distribuido como ShedLock.

### Nota para el código

En el método `@Scheduled` que genera recordatorios se dejará un
comentario de una línea indicando que:

``` java
// Actualmente se ejecuta en una sola instancia; si se escala horizontalmente, implementar un mecanismo de bloqueo distribuido.
```

Si en el futuro existen múltiples instancias del backend, deberá
revisarse esta decisión para evitar ejecuciones duplicadas.

------------------------------------------------------------------------

# 21. Gestión y métricas para organizaciones

EventHive no debe limitar el valor de la plataforma para las
organizaciones únicamente a publicar eventos.

El principal rol comercial de la plataforma será la organización, por lo
que se incorporará un conjunto de herramientas de gestión que le
permitan entender y promocionar sus eventos.

La primera versión de métricas será sencilla y útil para la feria y para
una futura evolución.

## Panel de organización

El representante podrá consultar información como:

``` text
- Eventos creados.
- Eventos publicados.
- Eventos finalizados.
- Eventos cancelados.
- Entradas vendidas.
- Ingresos generados.
- Eventos con mayor cantidad de ventas.
- Seguidores de la organización.
- Valoración promedio.
- Cantidad de valoraciones.
```

No se busca inicialmente construir analítica avanzada.

## Endpoints propuestos

``` text
GET /api/organizaciones/{id}/estadisticas
```

Resumen general de la organización.

``` text
GET /api/organizaciones/{id}/ventas
```

Resumen de ventas de sus eventos.

``` text
GET /api/organizaciones/{id}/eventos
```

Eventos pertenecientes a la organización con filtros útiles.

``` text
GET /api/organizaciones/{id}/promociones
```

Promociones creadas por la organización.

La información privada de gestión solo podrá ser consultada por:

``` text
REPRESENTANTE
OPERADOR con permiso correspondiente
ADMINISTRADOR cuando corresponda
```

El usuario no podrá consultar métricas de otra organización.

------------------------------------------------------------------------

# 22. Perfil público de organización

La organización tendrá un perfil público.

El perfil público estará diseñado para que un usuario pueda:

``` text
- Conocer la organización.
- Ver su reputación.
- Ver sus seguidores.
- Ver eventos publicados.
- Consultar información general.
- Seguir la organización.
```

## Información pública

Ejemplo:

``` text
Nombre comercial
Descripción
Imagen / logo
Redes sociales
Valoración promedio
Cantidad de valoraciones
Cantidad de seguidores
Eventos publicados
Eventos realizados
```

## Información que NO debe mostrarse públicamente

No se expondrá:

``` text
RUT
NIT
Documento de verificación
Correo empresarial privado
Información administrativa interna
Historial de moderación interno
Datos privados del representante
```

El perfil público no utilizará el DTO administrativo completo.

------------------------------------------------------------------------

# 23. Separación entre organización pública y administrativa

Se mantendrán dos representaciones distintas.

## `OrganizacionPublicaDTO`

Destinada a:

``` text
Clientes
Usuarios autenticados
Perfil público
Búsqueda de organizaciones
Eventos
```

Contendrá únicamente información que pueda mostrarse públicamente.

Ejemplo:

``` text
id
nombre
descripcion
imagen
redesSociales
ratingPromedio
totalValoraciones
totalSeguidores
eventosPublicados
```

## `OrganizacionDTO` / DTO administrativo

Destinado a:

``` text
Administrador
Moderador
Gestión interna
```

Puede contener información sensible o administrativa, siempre protegida
mediante autorización.

Ejemplo:

``` text
id
razonSocial
NIT
correoEmpresarial
representante
estado
nivel
RUT
historial
métricas administrativas
```

El RUT seguirá protegido por el bucket privado incluso cuando el DTO
administrativo haga referencia al documento.

------------------------------------------------------------------------

# 24. Estadísticas administrativas

El panel administrativo tendrá métricas generales, pero se mantendrán
sencillas.

## Indicadores iniciales

``` text
- Usuarios registrados.
- Organizaciones registradas.
- Organizaciones pendientes.
- Eventos pendientes.
- Eventos publicados.
- Eventos finalizados.
- Eventos cancelados.
- Tickets vendidos.
- Ventas totales.
```

Se podrán agregar métricas básicas de crecimiento y actividad sin
convertir el módulo en un sistema de analítica avanzada.

------------------------------------------------------------------------

# 25. Moderación y flujo de frontend

Los estados deben determinar los botones disponibles en el frontend.

## Evento en `BORRADOR`

Acciones:

``` text
Editar
Agregar localidad
Eliminar
Enviar a revisión
```

## Evento en `PENDIENTE_REVISION`

Acciones de organización:

``` text
Consultar estado
```

No debe poder editarse mientras está siendo revisado, salvo que el
diseño final permita retirarlo de revisión.

Acciones de moderador:

``` text
Aprobar
Solicitar correcciones
Rechazar
```

## Evento en `EN_CORRECCION`

Acciones:

``` text
Editar
Corregir información
Reenviar a revisión
```

## Evento en `PUBLICADO`

Acciones de organización:

``` text
Consultar
Promocionar
Cancelar
```

La edición de información sensible que pueda afectar la validez del
evento deberá respetar las reglas de negocio definidas para eventos
publicados.

## Evento `CANCELADO`

Acciones:

``` text
Consultar
```

No debe permitirse una nueva compra.

## Evento `FINALIZADO`

Acciones:

``` text
Consultar
Ver métricas
```

No debe poder modificarse como si estuviera activo.

------------------------------------------------------------------------

# 26. Flujo de organización

El flujo completo queda definido así:

``` text
USUARIO
   |
   | Solicitar organización
   v
SOLICITUD
   |
   v
REVISIÓN
   |
   +--> CORRECCIONES
   |       |
   |       v
   |    REVISIÓN
   |
   +--> RECHAZADA
   |
   +--> APROBADA
           |
           v
      ORGANIZACIÓN
           |
           v
      REPRESENTANTE
           |
           +--> Gestionar organización
           +--> Crear eventos
           +--> Gestionar operadores
           +--> Gestionar promociones
           +--> Consultar métricas
```

El rol `REPRESENTANTE` identifica al usuario responsable de la
organización, mientras que la organización continúa siendo una entidad
independiente del usuario.

------------------------------------------------------------------------

# 27. Flujo de operadores

Una organización puede tener operadores.

``` text
Organización
    |
    +-- Representante
    |
    +-- Operador
    +-- Operador
    +-- Operador
```

El representante podrá otorgar permisos específicos.

Ejemplo:

``` text
OPERADOR A
- CHECK_IN
- VER_EVENTOS

OPERADOR B
- CREAR_EVENTO
- EDITAR_EVENTO
- CHECK_IN
```

El operador nunca debe poder acceder a recursos de una organización
diferente.

------------------------------------------------------------------------

# 28. Reglas de propiedad y seguridad

Todas las operaciones sensibles deberán validar dos cosas:

``` text
1. ¿El usuario tiene el rol requerido?
2. ¿El recurso pertenece al contexto que puede administrar?
```

No basta con comprobar únicamente el ID enviado en la URL.

Ejemplo:

``` text
PUT /api/eventos/150
```

El backend debe comprobar:

``` text
¿Evento 150 pertenece a la organización del usuario?
```

antes de permitir la modificación.

Esto aplica especialmente a:

``` text
Eventos
Localidades
Compras
Promociones
Organización
Métricas
Operadores
Check-in
```

------------------------------------------------------------------------

# 29. PostGIS e índice espacial

Las búsquedas de eventos cercanos continuarán utilizando PostGIS.

Se deberá comprobar que exista un índice espacial GiST sobre la columna
geográfica correspondiente.

Conceptualmente:

``` sql
CREATE INDEX ...
ON eventos
USING GIST (ubicacion);
```

El endpoint de búsqueda por proximidad deberá limitar el radio recibido
por el usuario a un rango razonable.

Ejemplo:

``` text
radioKm > 0
radioKm <= límite definido por la aplicación
```

Esto evita consultas innecesariamente costosas.

------------------------------------------------------------------------

# 30. Pasarela de pago simulada

La pasarela continuará siendo simulada.

El objetivo de esta primera versión es demostrar:

``` text
Compra
  |
  v
Procesamiento de pago simulado
  |
  v
Estado de compra
  |
  v
Confirmación
  |
  v
Generación de tiquetes
```

Se conservarán los estados de compra definidos en el dominio.

No se integrará todavía una pasarela bancaria real.

La implementación debe mantener la separación suficiente para que
posteriormente pueda reemplazarse el simulador por un proveedor real.

------------------------------------------------------------------------

# 31. Notificaciones

MongoDB continuará siendo utilizado para almacenar notificaciones.

PostgreSQL seguirá siendo la fuente principal para los datos
transaccionales.

``` text
PostgreSQL
→ usuarios
→ eventos
→ compras
→ organizaciones
→ etc.

MongoDB
→ notificaciones
```

Las notificaciones se eliminarán automáticamente mediante TTL.

Los procesos programados continuarán ejecutándose con una sola instancia
durante esta etapa.

------------------------------------------------------------------------

# 32. Recomendaciones de eventos con IA

La recomendación inteligente de eventos queda como funcionalidad futura.

La plataforma podrá evolucionar hacia:

``` text
Preferencias del usuario
        +
Categorías consultadas
        +
Eventos vistos
        +
Eventos asistidos
        +
Organizaciones seguidas
        |
        v
Sistema de recomendación
        |
        v
Eventos sugeridos
```

No forma parte de los cambios bloqueantes de esta versión de producción.

------------------------------------------------------------------------

# 33. Checklist final de implementación

## Backend

-   [ ] Corregir llamadas antiguas a `ServiceEvento`.
-   [ ] Verificar compilación completa.
-   [ ] Actualizar Dockerfile a Java 23.
-   [ ] Verificar nombre correcto del JAR.
-   [ ] Mantener CORS actual.
-   [ ] Separar buckets públicos y privados.
-   [ ] Hacer privado el bucket de RUT.
-   [ ] Implementar URL firmada para RUT.
-   [ ] Incorporar migraciones de base de datos.
-   [ ] Corregir restricción de valoraciones para representantes y
    operadores.
-   [ ] Actualizar valoración existente en lugar de duplicarla.
-   [ ] Mantener comentario opcional.
-   [ ] Implementar localidad General.
-   [ ] Eliminar aforo duplicado de `Evento`.
-   [ ] Ajustar flujo de estados del evento.
-   [ ] Revisar permisos según estado.
-   [ ] Ajustar criterios de ascenso.
-   [ ] Evaluar ascenso únicamente al finalizar.
-   [ ] Hacer cancelación de compras segura ante concurrencia.
-   [ ] Revisar módulo `security`.
-   [ ] Uniformar respuestas 401/403.
-   [ ] Evitar enumeración de usuarios en login.
-   [ ] Implementar desactivación de usuarios.
-   [ ] Aplicar `@Valid` a DTO.
-   [ ] Validar archivos y tipos permitidos.
-   [ ] Unificar límites de archivos.
-   [ ] Garantizar índice TTL de MongoDB.
-   [ ] Mantener una sola instancia para `@Scheduled`.
-   [ ] Añadir comentario al método programado.
-   [ ] Crear endpoints básicos de gestión y métricas de organización.
-   [ ] Crear DTO público de organización.
-   [ ] Mantener DTO administrativo separado.
-   [ ] Crear/ajustar endpoint público de organización.
-   [ ] Revisar índice espacial PostGIS.

## Frontend

-   [ ] Utilizar los estados del evento para mostrar botones correctos.
-   [ ] Separar vista pública y administrativa de organización.
-   [ ] Mostrar RUT únicamente mediante el flujo autorizado.
-   [ ] No guardar URLs permanentes del RUT.
-   [ ] Implementar manejo de 401.
-   [ ] Implementar manejo de 403.
-   [ ] Implementar logout eliminando tokens locales.
-   [ ] Mostrar métricas de organización.
-   [ ] Mostrar ventas básicas de organización.
-   [ ] Permitir gestión de promociones.
-   [ ] Implementar flujo BORRADOR → REVISIÓN → PUBLICADO.
-   [ ] Implementar flujo EN_CORRECCION → REVISIÓN.

------------------------------------------------------------------------

# 34. Resultado esperado

Después de aplicar estas correcciones, EventHive tendrá una arquitectura
coherente entre backend y frontend y un flujo de negocio más claro:

``` text
                    EVENTHIVE
                        |
        +---------------+---------------+
        |               |               |
    Clientes       Organizaciones    Administración
        |               |               |
        |               |               |
    Eventos         Gestión          Supervisión
    Mapa            Eventos          Moderación
    Compras         Localidades      Usuarios
    Seguimientos    Promociones      Categorías
    Valoraciones    Métricas         Métricas
    Notificaciones  Ventas           Organizaciones
        |               |               |
        +---------------+---------------+
                        |
                   Seguridad
                        |
             JWT + Roles + Permisos
                        |
             +----------+----------+
             |                     |
        PostgreSQL              MongoDB
        + PostGIS             Notificaciones
             |
        Storage público
        + Storage privado
```

La prioridad será cerrar primero los cambios marcados como necesarios
para compilación, seguridad, persistencia y consistencia del dominio.
Las funcionalidades futuras, como recomendaciones mediante IA y
analítica avanzada, permanecen fuera del bloqueo de esta versión.
