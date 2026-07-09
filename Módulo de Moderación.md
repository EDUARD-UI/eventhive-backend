# Módulo de Moderación - EventHive

## Descripción

El Moderador es el encargado de garantizar que tanto las organizaciones como los eventos publicados en EventHive cumplan con las políticas de la plataforma.

Su trabajo consiste en revisar la información enviada por los usuarios y decidir si puede ser aprobada, necesita correcciones o debe ser rechazada.

No administra la plataforma, no gestiona categorías, promociones ni usuarios. Su única responsabilidad es mantener la calidad del contenido publicado.

---

# Dashboard del Moderador

Al ingresar al sistema el moderador visualizará un resumen de su carga de trabajo.

## Resumen

Indicadores principales:

- Organizaciones pendientes.
- Eventos pendientes.
- Eventos en corrección.
- Eventos rechazados.

---

# Bandeja de Trabajo

La bandeja reúne todas las solicitudes pendientes de revisión.

Se divide en dos módulos.

## Organizaciones

Información mostrada:

- Nombre comercial.
- Tipo de organización.
- Ciudad.
- Fecha de solicitud.
- Estado.

Acciones:

- Ver detalle.
- Aprobar.
- Solicitar correcciones.
- Rechazar.

---

## Eventos

Información mostrada:

- Nombre del evento.
- Organización.
- Fecha del evento.
- Estado.
- Fecha de envío.

Acciones:

- Ver detalle.
- Aprobar.
- Solicitar correcciones.
- Rechazar.

---

# Detalle de Organización

Permite revisar toda la información suministrada durante la solicitud.

Información disponible:

## Información General

- Nombre comercial.
- Tipo de organización.
- Persona responsable.
- Documento.
- Ciudad.
- Dirección.
- WhatsApp.
- Redes sociales.

---

## Documentos

- Documento de identidad.
- NIT (si aplica).
- Fotografías.
- Archivos adjuntos.

---

## Historial

- Fecha de solicitud.
- Correcciones realizadas.
- Revisiones anteriores.

---

## Acciones

### Aprobar

La organización obtiene permisos para publicar eventos.

---

### Solicitar Correcciones

El moderador selecciona uno o varios motivos.

Ejemplo:

- Información incompleta.
- Documento ilegible.
- Fotografía inválida.
- Datos inconsistentes.
- Otro.

Si selecciona **Otro**, podrá escribir una observación.

---

### Rechazar

Se registra el motivo correspondiente.

---

# Detalle del Evento

Esta vista permite revisar completamente el evento antes de su publicación.

## Información General

- Nombre.
- Descripción.
- Categoría.
- Imagen principal.
- Organización.
- Fecha.
- Hora.

---

## Lugar

- Dirección.
- Ciudad.
- Ubicación en mapa.
- Aforo.

---

## Localidades

Se visualizarán todas las localidades registradas.

Ejemplo:

| Localidad | Precio | Capacidad |
|-----------|--------|----------:|
| General | $40.000 | 500 |
| VIP | $90.000 | 100 |
| Preferencial | $60.000 | 200 |

Si el organizador no creó localidades, el sistema mostrará la localidad automática **General**.

---

## Permisos

Archivos adjuntos:

- Código PULEP.
- Permisos municipales.
- Otros documentos.

---

## Validaciones Automáticas

Información generada por el sistema.

Ejemplo:

- ✔ Capacidad válida.
- ✔ Localidades válidas.
- ✔ Total de localidades coincide con el aforo.
- ✔ Imagen cargada.
- ✔ Información completa.

Estas validaciones ayudan al moderador y reducen el tiempo de revisión.

---

# Acciones del Moderador

## Aprobar

El evento pasa al estado:

```text
PUBLICADO
```

---

## Solicitar Correcciones

El evento pasa al estado:

```text
EN_CORRECCION
```

El moderador seleccionará uno o varios motivos.

Ejemplos:

- Información incompleta.
- Imagen no permitida.
- Falta permiso legal.
- Lugar incorrecto.
- No corresponde con la categoría.
- Incumple políticas.
- Otro.

Solo cuando seleccione **Otro** podrá escribir una observación.

---

## Rechazar

Estado:

```text
RECHAZADO
```

También deberá indicar el motivo correspondiente.

---

# Historial de Moderación

Cada organización y cada evento tendrán un historial de revisiones.

Información mostrada:

- Moderador.
- Fecha.
- Resultado.
- Motivos.
- Observaciones.

Esto permitirá mantener trazabilidad completa de todas las decisiones.

---

# Estadísticas del Moderador

El Moderador contará con un pequeño panel para conocer su actividad.

Indicadores:

- Organizaciones revisadas.
- Eventos revisados.
- Eventos aprobados.
- Eventos rechazados.
- Eventos enviados a corrección.
- Tiempo promedio de revisión.

Estas métricas tienen como objetivo medir la carga de trabajo y mejorar el proceso de moderación.

---

# Flujo de Moderación

## Organización

```text
Solicitud

        │
        ▼

Pendiente de revisión

        │
        ▼

Moderador

 ┌──────────────┬──────────────┬──────────────┐
 │              │              │
 ▼              ▼              ▼

Aprobada   En corrección   Rechazada
```

---

## Evento

```text
Evento enviado

        │
        ▼

Pendiente de revisión

        │
        ▼

Moderador

 ┌──────────────┬──────────────┬──────────────┐
 │              │              │
 ▼              ▼              ▼

Publicado   En corrección   Rechazado
```

---

# Objetivo del Módulo

El módulo de Moderación busca mantener la calidad y confiabilidad de EventHive sin complicar el proceso para las organizaciones.

El sistema se encarga de validar automáticamente las reglas técnicas (aforo, localidades, capacidades, información obligatoria, etc.), mientras que el Moderador concentra su trabajo en validar el contenido, la autenticidad y el cumplimiento de las políticas de la plataforma.

De esta forma se reduce el tiempo de revisión y se garantiza una experiencia más ágil tanto para los moderadores como para las organizaciones.