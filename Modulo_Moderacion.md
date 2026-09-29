# Módulo de Moderación --- EventHive

## 1. Propósito

El Moderador revisa únicamente los eventos que el sistema de reglas
deriva a revisión humana antes de su publicación.

Su objetivo es comprobar que la información sea válida, completa y
cumpla las políticas de EventHive.

El Moderador **no administra categorías, promociones, usuarios ni
configuraciones globales**.

La verificación de organizaciones y de sus documentos RUT corresponde
al Administrador y no forma parte de la bandeja del Moderador.

## 2. Flujo moderado

El sistema evalúa las solicitudes de publicación de eventos mediante
reglas automáticas. Solo los eventos que esas reglas envían a revisión
personal aparecen en la bandeja del Moderador. Los demás siguen el
resultado que determine el sistema, incluida la publicación directa
cuando las reglas y condiciones del dominio lo permitan.

Para un evento derivado a revisión humana, el resultado puede ser:

-   Aprobado.
-   En corrección.
-   Rechazado.

## 3. Estados

### Evento

``` text
BORRADOR
   ↓
PENDIENTE_REVISION
   ├── PUBLICADO
   ├── EN_CORRECCION → PENDIENTE_REVISION
   └── RECHAZADO
```

Un evento publicado puede posteriormente pasar a **CANCELADO**,
**SUSPENDIDO** o **FINALIZADO**, según las reglas del dominio.

El estado `PENDIENTE_REVISION` representa un evento que las reglas
enviaron a la cola de revisión humana; no implica que todos los eventos
deban pasar por un Moderador. La publicación automática, cuando esté
habilitada, no elimina la capacidad del Administrador de suspender
posteriormente un evento por motivos administrativos.

## 4. Dashboard del Moderador

Indicadores:

-   Eventos pendientes asignados a revisión humana.
-   Eventos en corrección.
-   Revisiones realizadas.
-   Aprobaciones.
-   Rechazos.
-   Tiempo promedio de revisión.

El dashboard representa exclusivamente la carga de trabajo y las
decisiones de moderación de eventos; no incluye solicitudes de
verificación de organizaciones ni estadísticas comerciales.

## 5. Bandeja de trabajo

Debe permitir consultar y filtrar los eventos enviados a revisión
personal por el sistema de reglas. No presenta solicitudes de
verificación de organizaciones.

### Eventos

Mostrar:

-   Nombre.
-   Organización.
-   Fecha del evento.
-   Fecha de envío.
-   Estado.

Acciones:

-   Ver detalle.
-   Aprobar.
-   Solicitar correcciones.
-   Rechazar.

autorizar la consulta y, cuando corresponda, entregar una URL
## 6. Revisión de un evento

Información:

-   Nombre.
-   Descripción.
-   Categoría.
-   Imagen principal.
-   Organización.
-   Fecha.
-   Hora.
-   Ubicación.
-   Localidades.
-   Archivos de soporte o permisos cuando correspondan.

### Localidades

Ejemplo:

  Localidad          Precio   Capacidad
  -------------- ---------- -----------
  General          \$40.000         500
  Preferencial     \$60.000         200
  VIP              \$90.000         100

Si el modelo de dominio establece que un evento sin localidades debe
tener una localidad automática, el sistema puede crear `General`. Esta
regla debe implementarse en el servicio de eventos y no depender del
frontend.

El aforo total debe derivarse de la suma de las capacidades de las
localidades. No debe mantenerse un segundo campo de aforo que pueda
quedar desactualizado.

La creación automática de `General` debe evitar duplicados cuando el
frontend reintente una operación.

## 7. Validaciones automáticas

Antes de que el evento se publique automáticamente o el moderador tome
una decisión, el backend debe validar reglas técnicas como:

-   Datos obligatorios completos.
-   Fecha y hora válidas.
-   Capacidad mayor que cero.
-   Existencia de al menos una localidad válida antes de publicar.
-   Precios válidos.
-   Categoría válida y activa.
-   Imagen requerida disponible.
-   Ubicación válida.
-   Archivos requeridos presentes.

El frontend puede ayudar visualmente, pero **la validación definitiva
siempre debe ejecutarse en el backend**.

## 8. Historial de moderación

Cada revisión debe registrar:

-   Elemento revisado.
-   Tipo de elemento.
-   Moderador.
-   Fecha y hora.
-   Resultado.
-   Motivos.
-   Observación.
-   Estado anterior.
-   Estado posterior.

Esto permite conocer quién tomó cada decisión y mantener trazabilidad.

## 9. Estadísticas del moderador

Endpoint conceptual de estadísticas de moderación de eventos:

``` text
GET /api/moderaciones/estadisticas
```

Debe devolver información como:

-   Revisiones realizadas.
-   Aprobadas.
-   Rechazadas.
-   En corrección.
-   Tiempo promedio de revisión.

La ruta definitiva debe mantenerse alineada con la convención usada por
el resto del backend.

## 10. Información para el frontend

El frontend debe representar los estados como parte del flujo de trabajo
y no como simples etiquetas.

Por ejemplo:

-   `BORRADOR`: permitir continuar editando.
-   `PENDIENTE_REVISION`: mostrar que el sistema derivó el evento a
   revisión humana.
-   `EN_CORRECCION`: mostrar los motivos y permitir editar/reenviar.
-   `PUBLICADO`: mostrar el evento como disponible.
-   `RECHAZADO`: mostrar el resultado y las reglas correspondientes.
-   `SUSPENDIDO`: impedir acciones normales de publicación.

Los botones de aprobar, rechazar y solicitar corrección solo deben
aparecer según el rol y el estado, pero el backend debe validar
nuevamente cada acción.

## 11. Reglas de autorización

-   Un Moderador solo debe ejecutar acciones de moderación autorizadas
   sobre eventos enviados a su revisión por el sistema de reglas.
-   La aprobación, corrección o rechazo de solicitudes de verificación
   de organizaciones corresponde al Administrador.
-   Un Moderador no debe administrar categorías ni promociones.
-   Un Moderador no debe modificar datos comerciales de una
    organización.
-   Un Administrador puede intervenir administrativamente, pero esa
    intervención debe quedar registrada.
-   La Organización y sus usuarios solo pueden modificar información que
    les pertenezca y según su rol.

## 12. Principio importante

El frontend no decide si algo puede aprobarse. El frontend presenta
información y facilita la revisión; el backend aplica las reglas de
negocio y seguridad.
