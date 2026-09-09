# Organizaciones --- EventHive

## 1. Concepto

Una **Organización es una entidad del dominio**, no un rol de usuario.

Una organización puede tener varios usuarios asociados:

-   Un **Representante**, responsable principal.
-   Uno o varios **Operadores**, encargados de tareas delegadas.

El usuario puede pertenecer a una organización sin que la organización
sea su rol.

## 2. Datos principales

Una organización puede contener:

-   Razón social.
-   Correo empresarial.
-   NIT.
-   Representante legal.
-   Documento RUT.
-   Redes sociales.
-   Estado.
-   Fecha de registro.
-   Nivel de organización.
-   Métricas agregadas de reputación y actividad.

## 3. Registro

Para solicitar el registro se consideran como datos mínimos:

-   Razón Social.
-   Correo empresarial.
-   NIT.
-   Nombre del Representante Legal.
-   Documento RUT.

El registro crea una solicitud que debe pasar por el flujo de
Moderación.

No debe considerarse que una organización está habilitada para publicar
eventos hasta que su solicitud haya sido aprobada.

## 4. Estados

Estado conceptual:

``` text
PENDIENTE_REVISION
      ↓
   APROBADA
      ↓
  SUSPENDIDA

PENDIENTE_REVISION
   ├── EN_CORRECCION → PENDIENTE_REVISION
   └── RECHAZADA
```

La implementación exacta debe mantenerse alineada con los enums
existentes del backend.

## 5. Usuarios asociados

### Representante

Puede, según las reglas de autorización:

-   Gestionar información de la organización.
-   Crear eventos.
-   Editar sus eventos.
-   Enviar eventos a revisión.
-   Cancelar eventos propios.
-   Gestionar operadores.
-   Consultar las métricas de su organización.

### Operador

Puede realizar únicamente las operaciones delegadas por la organización.

No debe asumir automáticamente todos los permisos del Representante.

El operador puede ejecutar tareas operativas, como apoyo en eventos o
check-in, según los permisos definidos para la organización.

## 6. Perfil público

El perfil de una organización puede mostrar:

-   Nombre.
-   Imagen o identidad visual.
-   Descripción.
-   Valoración promedio.
-   Total de valoraciones.
-   Seguidores.
-   Eventos publicados.
-   Eventos realizados.
-   Información general de contacto cuando el producto decida hacerla
    pública.

Las métricas deben diferenciarse de los datos administrativos privados.

El perfil público **no debe exponer**:

-   RUT.
-   Datos internos de moderación.
-   Información privada del representante.
-   Credenciales.
-   Datos internos de gestión.
-   Información sensible utilizada para verificar la organización.

Se recomienda separar claramente un DTO público de un DTO
administrativo.

## 7. Valoraciones

Reglas:

-   Solo usuarios autenticados pueden valorar.
-   Un usuario puede tener una valoración por organización.
-   Si vuelve a valorar, se actualiza su valoración anterior.
-   La calificación está entre 1 y 5.
-   El comentario es opcional.
-   Un usuario con rol **REPRESENTANTE u OPERADOR perteneciente a esa
    organización** no puede valorarla.

Después de crear o actualizar una valoración deben actualizarse las
métricas agregadas.

## 8. Métricas agregadas

Para evitar cálculos repetitivos en cada consulta, la organización puede
almacenar:

-   `ratingPromedio`.
-   `totalValoraciones`.
-   `totalSeguidores`.
-   Otras métricas que realmente sean consultadas con frecuencia.

Estas métricas deben actualizarse desde servicios de negocio y
transacciones controladas, no desde el frontend.

## 9. Seguimiento

La relación usuario-organización es muchos a muchos.

Conceptualmente:

``` text
Usuario ─────< Seguimiento >───── Organización
```

La tabla intermedia debe mantener las referencias necesarias y
restricciones de unicidad para evitar seguidores duplicados.

Acciones:

-   Seguir organización.
-   Dejar de seguir.
-   Consultar seguidores.
-   Consultar si el usuario actual sigue a la organización.

## 10. Gestión y métricas de la organización

El representante debe disponer de una vista de gestión separada del
perfil público.

Se contemplan endpoints como:

``` text
GET /api/organizaciones/{id}/estadisticas
GET /api/organizaciones/{id}/ventas
GET /api/organizaciones/{id}/eventos
GET /api/organizaciones/{id}/promociones
```

Estas rutas son de gestión y no deben permitir que un cliente consulte
las métricas privadas de otra organización.

Las métricas iniciales pueden incluir:

-   Eventos creados.
-   Eventos publicados.
-   Eventos finalizados.
-   Eventos cancelados.
-   Entradas vendidas.
-   Ingresos generados.
-   Eventos con mayor cantidad de ventas.
-   Seguidores.
-   Valoración promedio.
-   Cantidad de valoraciones.

No se busca inicialmente construir analítica avanzada.

## 11. Nivel de organización

El nivel representa el grado de confianza/experiencia de la organización
dentro de EventHive.

Una propuesta inicial es:

``` text
NIVEL 1 → NIVEL 2 → NIVEL 3
```

Los ascensos deben depender de actividad real y buen comportamiento, no
únicamente de la antigüedad.

Como referencia:

-   Nivel 1 → 2: al menos 3 eventos finalizados, mínimo 30 días de
    actividad y buen estado.
-   Nivel 2 → 3: al menos 8 eventos finalizados, mínimo 90 días de
    actividad y buen estado.

No se recomienda bloquear permanentemente el ascenso por un único
rechazo. Deben considerarse el historial general y las sanciones
activas.

La evaluación de ascenso se realizará cuando un evento pase a estado
`FINALIZADO`.

## 12. Seguridad

Un usuario solo puede modificar información de una organización cuando
tenga una relación válida con ella y el rol correspondiente.

La pertenencia a una organización no debe conceder automáticamente
permisos administrativos globales.

El backend debe comprobar la propiedad o relación con la organización en
cada operación protegida.

## 13. Archivos

Las imágenes de eventos y categorías pueden utilizar almacenamiento
público, con extensiones controladas como:

``` text
.jpg
.jpeg
.png
```

El RUT debe utilizar almacenamiento privado y permitir:

``` text
.pdf
.jpg
.jpeg
.png
```

El backend debe validar tamaño, `Content-Type`, extensión y tipo de
recurso. Los nombres internos deben generarse con UUID y no depender del
nombre enviado por el usuario.

## 14. Perfil administrativo/moderación

La información privada utilizada para verificar una organización no debe
mezclarse con el perfil público.

Los documentos como el RUT deben estar protegidos mediante autorización
y no exponerse como información pública de la organización.

El frontend debe recibir únicamente los campos necesarios para la
pantalla que está mostrando.
