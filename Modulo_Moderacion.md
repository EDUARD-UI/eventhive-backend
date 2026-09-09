# Módulo de Moderación --- EventHive

## 1. Propósito

El Moderador revisa las solicitudes de organizaciones y los eventos
enviados para publicación.

Su objetivo es comprobar que la información sea válida, completa y
cumpla las políticas de EventHive.

El Moderador **no administra categorías, promociones, usuarios ni
configuraciones globales**.

## 2. Flujos moderados

El módulo gestiona dos flujos independientes:

1.  Solicitud de Organización.
2.  Solicitud de publicación de Evento.

En ambos casos el resultado de una revisión puede ser:

-   Aprobado.
-   En corrección.
-   Rechazado.

## 3. Estados

### Organización

Flujo recomendado:

``` text
SOLICITUD
   ↓
PENDIENTE_REVISION
   ├── APROBADA
   ├── EN_CORRECCION → PENDIENTE_REVISION
   └── RECHAZADA
```

Una organización aprobada puede posteriormente quedar **SUSPENDIDA** por
una acción administrativa.

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

Las organizaciones de nivel superior que tengan habilitada la
publicación directa pueden pasar por validaciones automáticas antes de
quedar publicadas, sin eliminar la capacidad administrativa de suspender
posteriormente un evento.

## 4. Dashboard del Moderador

Indicadores:

-   Organizaciones pendientes.
-   Eventos pendientes.
-   Organizaciones en corrección.
-   Eventos en corrección.
-   Revisiones realizadas.
-   Aprobaciones.
-   Rechazos.
-   Tiempo promedio de revisión.

El dashboard representa la carga de trabajo del moderador y no
estadísticas comerciales.

## 5. Bandeja de trabajo

Debe permitir consultar y filtrar solicitudes.

### Organizaciones

Mostrar:

-   Razón social.
-   NIT.
-   Representante.
-   Fecha de solicitud.
-   Estado.

Acciones:

-   Ver detalle.
-   Aprobar.
-   Solicitar correcciones.
-   Rechazar.

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

## 6. Revisión de una organización

Información mínima:

-   Razón social.
-   Correo empresarial.
-   NIT.
-   Representante legal.
-   Documento RUT.
-   Redes sociales, si fueron registradas.
-   Estado actual.
-   Historial de revisiones.

### Documento RUT

El moderador debe poder consultar el archivo cargado para verificar que
sea legible y corresponda a la organización.

El RUT debe estar en un **bucket privado**. El frontend no debe asumir
que puede construir una URL pública permanente. El backend debe
autorizar la consulta y, cuando corresponda, entregar una URL
temporal/firma de acceso.

## 7. Solicitar correcciones

La corrección debe registrar uno o varios motivos definidos por el
dominio.

Ejemplos:

-   INFORMACION_INCOMPLETA.
-   DOCUMENTO_ILEGIBLE.
-   DOCUMENTO_INVALIDO.
-   DATOS_INCONSISTENTES.
-   OTRO.

Cuando el motivo sea `OTRO`, se debe exigir una observación.

La organización podrá corregir la información y reenviar la solicitud.

## 8. Revisión de un evento

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

## 9. Validaciones automáticas

Antes de que el moderador tome una decisión, el backend debe validar
reglas técnicas como:

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

## 10. Historial de moderación

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

## 11. Estadísticas del moderador

Endpoint conceptual:

``` text
GET /api/verificacion/estadisticas
```

Debe devolver información como:

-   Revisiones realizadas.
-   Aprobadas.
-   Rechazadas.
-   En corrección.
-   Tiempo promedio de revisión.

La ruta definitiva debe mantenerse alineada con la convención usada por
el resto del backend.

## 12. Información para el frontend

El frontend debe representar los estados como parte del flujo de trabajo
y no como simples etiquetas.

Por ejemplo:

-   `BORRADOR`: permitir continuar editando.
-   `PENDIENTE_REVISION`: mostrar que está esperando moderación.
-   `EN_CORRECCION`: mostrar los motivos y permitir editar/reenviar.
-   `PUBLICADO`: mostrar el evento como disponible.
-   `RECHAZADO`: mostrar el resultado y las reglas correspondientes.
-   `SUSPENDIDO`: impedir acciones normales de publicación.

Los botones de aprobar, rechazar y solicitar corrección solo deben
aparecer según el rol y el estado, pero el backend debe validar
nuevamente cada acción.

## 13. Reglas de autorización

-   Un Moderador solo debe ejecutar acciones de moderación autorizadas.
-   Un Moderador no debe administrar categorías ni promociones.
-   Un Moderador no debe modificar datos comerciales de una
    organización.
-   Un Administrador puede intervenir administrativamente, pero esa
    intervención debe quedar registrada.
-   La Organización y sus usuarios solo pueden modificar información que
    les pertenezca y según su rol.

## 14. Principio importante

El frontend no decide si algo puede aprobarse. El frontend presenta
información y facilita la revisión; el backend aplica las reglas de
negocio y seguridad.
