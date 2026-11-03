# Guía de Integración y Comportamiento para Frontend (EventHive)

Este documento detalla los requerimientos de comportamiento, manejo de estados, validaciones en cliente y directrices de UI que el equipo de **Frontend** debe implementar para alinearse con las reglas de negocio y los últimos cambios del servidor.

---

## 1. Regla de Seguimiento de Organizaciones
**Endpoints:** 
- `POST /api/seguidores/{organizacionId}/seguir`
- `DELETE /api/seguidores/{organizacionId}/seguir`
- `GET /api/seguidores/{organizacionId}/siguiendo`

### Comportamiento Requerido:
1. **Un representante legal NO puede seguir a su propia organización:**
   - Si el usuario autenticado es el representante de la organización o pertenece a ella e intenta seguirla, el servidor rechazará la petición con **HTTP 400 (Bad Request)** y el mensaje:
     > *"El representante de la organización no puede seguir a su propia organización"*
   - **En UI/Frontend:**
     - Si el usuario en sesión es el representante de la organización que está visualizando (`usuario.id === organizacion.representante.id` o `usuario.organizacionId === organizacion.id`), **ocultar o deshabilitar** el botón de "Seguir".
     - Al capturar un error 400 en esta acción, mostrar una alerta informativa o toast: *"No puedes seguir a tu propia organización"*.
2. **Dejar de seguir:**
   - Si se invoca `DELETE` cuando el usuario ya no es seguidor, el servidor responderá **HTTP 404 (Not Found)** (*"No sigues a esta organización"*). El frontend debe sincronizar el estado del botón a *"Seguir"*.

---

## 2. Corrección Crítica: Parámetros URL `[object Object]`
En los registros de red se detectaron llamadas con errores:
```http
Failed to load resource: the server responded with a status of 400 ...%5Bobject+Object%5D:1
```

### Causa:
Se está concatenando un objeto JavaScript directamente en la URL (`/api/eventos/${evento}` o en los query params `?categoria=${categoria}`) en lugar de acceder a su identificador numérico o string.

### Corrección requerida en Frontend:
- Asegurarse de desestructurar o acceder a las propiedades primitivas:
  ```typescript
  // ❌ INCORRECTO
  const url = `/api/eventos/${eventoSeleccionado}`; // produce /api/eventos/[object Object]
  
  // ✅ CORRECTO
  const url = `/api/eventos/${eventoSeleccionado.id}`;
  ```
- Para parámetros de consulta, utilizar siempre `URLSearchParams` con valores serializados:
  ```typescript
  const params = new URLSearchParams();
  if (categoriaId) params.append('categoriaId', String(categoriaId));
  if (lat && lng) {
    params.append('lat', String(lat));
    params.append('lng', String(lng));
  }
  ```

---

## 3. Asistente Virtual / Chatbot (HiveBot)
**Endpoints disponibles:**
- `POST /api/chatbot/mensaje`
- `POST /api/chat`
- `POST /api/chatbot`

### Formato de Petición:
```json
{
  "mensaje": "hola, ¿qué conciertos hay para el mes de diciembre?"
}
```

### Formato de Respuesta:
```json
{
  "exito": true,
  "mensaje": "¡Hola! Para diciembre tenemos los siguientes conciertos...",
  "datos": {
    "respuesta": "¡Hola! Para diciembre tenemos los siguientes conciertos...",
    "fuente": "GROQ",
    "modoContingencia": false
  }
}
```

### Comportamiento Requerido:
1. **Manejo de Respuestas:** El texto principal a renderizar en el globo de chat del bot está en `datos.respuesta` (o en la propiedad `mensaje` del envoltorio).
2. **Contingencias y Errores:**
   - Si ocurre una desconexión de red o falla técnica, **nunca** mostrar *"No pude entender tu solicitud, ¿puedes reformularla?"* como si la consulta del usuario estuviera mal escrita.
   - En caso de error HTTP de red, mostrar:
     > *"En este momento presentamos intermitencias. Por favor, intenta de nuevo en unos momentos."*
   - **Regla estricta de comunicación:** **NUNCA** mostrar mensajes como *"El backend no responde"*. Utilizar siempre la frase **"Error de servidor"** o mensajes amigables al usuario final.

---

## 4. Flujo de Creación de Eventos y Verificación de RUT de la Organización

### A. Ciclo de Vida del RUT: Solicitud vs Organización
1. **Fase de Solicitud de Verificación:**
   - Cuando la organización se registra y sube el RUT diferido (`POST /api/solicitudes-verificacion/mi-solicitud/rut`), el documento queda en la tabla `solicitud_verificacion` (`solicitud.urlRut`) con estado `PENDIENTE`.
   - El administrador revisa el RUT desde la solicitud (`GET /api/solicitudes-verificacion/{solicitudId}/rut`).
2. **Aprobación de la Organización:**
   - Al aprobar la solicitud (`POST /api/solicitudes-verificacion/{solicitudId}/aprobar`), el servidor copia automáticamente `solicitud.urlRut` al campo persistente de la organización (`organizacion.urlRut`) y actualiza el estado a `APROBADA`.
   - Si el administrador consulta el RUT de la entidad (`GET /api/organizaciones/{id}/rut`), el servidor lo busca directamente en `organizacion.urlRut`. Si aún estuviera en trámite, busca en la tabla de solicitudes.
3. **Consulta de Perfil de Organización (`GET /api/organizaciones/mi-organizacion`):**
   - Si la organización está `APROBADA` y cuenta con RUT, responderá con:
     - `"estado": "APROBADA"`
     - `"estadoValidacionRut": "APROBADA"`
     - `"urlRutTemporal": "https://..."` (URL firmada de lectura)

### B. Creación del Evento (`POST /api/eventos`):
- Los representantes legales con organización válida **pueden crear eventos** sin bloqueos indebidos de permiso.
- Todo nuevo evento se guarda inicialmente en estado **`BORRADOR`**.
- La respuesta HTTP 201 devuelve el detalle del evento y un mensaje orientador indicando si la organización está aprobada o pendiente.

### C. Envío del Evento a Revisión (`PATCH /api/eventos/{id}/enviar-revision`):
- Para enviar a moderación:
  - El evento debe estar en `BORRADOR` o `EN_CORRECCION`.
  - La organización no debe estar `SUSPENDIDA`.
  - La organización debe tener su documento RUT cargado (bien sea en `organizacion.urlRut` o en su solicitud de verificación activa).
- Si la organización tiene RUT cargado y está `APROBADA`, el evento transiciona inmediatamente a `PENDIENTE_REVISION` y activa las reglas automáticas de moderación.

---

## 5. Visualización del Mapa y Filtros de Proximidad
**Endpoint:** `GET /api/eventos/mapa`

### Parámetros opcionales:
| Parámetro | Tipo | Descripción |
| :--- | :--- | :--- |
| `categoriaId` | `Long` | Filtra los primeros 10 eventos por fecha próxima de esa categoría. |
| `lat` | `Double` | Latitud del usuario para cálculo y ordenamiento por cercanía geográfica. |
| `lng` | `Double` | Longitud del usuario para cálculo y ordenamiento por cercanía geográfica. |
| `limite` | `Integer` | Límite de marcadores en el mapa (por defecto `10`). |

### Comportamiento Requerido en Frontend:
- Solicitar geolocalización al usuario en el mapa y enviar `lat` y `lng` para recibir los eventos más cercanos en orden cronológico.
- Cuando el usuario filtre por una categoría en el mapa, enviar `categoriaId` y resetear la vista para centrar los 10 eventos resultantes.

---

## 6. Dashboard Administrador: Estadísticas de Organizaciones
**Endpoint:** `GET /api/admin/estadisticas/organizaciones-por-validacion`

### Estructura recibida:
```json
{
  "total": 36,
  "solicitudesPendientes": 2,
  "solicitudesIncompletas": 0,
  "porEstado": [
    { "estado": "PENDIENTE_REVISION", "cantidad": 28 },
    { "estado": "APROBADA", "cantidad": 7 },
    { "estado": "SUSPENDIDA", "cantidad": 1 }
  ]
}
```

### Comportamiento Requerido en Frontend:
- La tarjeta **TOTAL EMPRESAS** muestra `total` (36).
- Las tarjetas de estados deben alimentarse de `porEstado`:
  - **APROBADAS:** buscar el ítem con `estado === "APROBADA"`.
  - **EN REVISIÓN:** buscar el ítem con `estado === "PENDIENTE_REVISION"`.
  - **SUSPENDIDAS:** buscar el ítem con `estado === "SUSPENDIDA"`.
- La sumatoria de las organizaciones por estado ahora cuadra con el 100% del total (36).

---

## 7. Módulo de Moderación: Localidades y Aforo
**Endpoint:** `GET /api/eventos/{id}/localidades`

### Comportamiento Requerido:
- Los usuarios con rol `MODERADOR` tienen acceso completo para auditar el aforo total, nombre de localidades, precios y disponibilidad de cualquier evento que esté en revisión o activo.
- Si el moderador inspecciona un evento, ya no recibirá error 403 al desplegar la lista de localidades.

---

## 8. Nomenclatura de Archivos y Storage Supabase
- El bucket oficial y privado para documentos de verificación es:
  - **`verificacion-docs`**
- Los buckets públicos para imágenes son:
  - `perfilOrganizacion`
  - `imagenPerfil`
  - `banners-home`
  - `categorias-images`
  - `eventos-images`

---

## Resumen de Mensajería de Error Permitida
| Caso | ❌ Prohibido | ✅ Permitido |
| :--- | :--- | :--- |
| Timeout / Caída de conexión | *"El backend no responde"* | *"Error de servidor. Por favor, reintenta más tarde."* |
| Fallo en Chatbot | *"No pude entender tu solicitud"* | *"Servicio temporalmente no disponible"* |
| Seguir propia organización | Mensaje genérico de error | *"No puedes seguir a tu propia organización"* |

