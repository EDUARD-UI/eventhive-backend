# API de EventHive

Base URL: `/api`  
Todas las respuestas siguen el formato `{"success":true,"mensaje":"...","data":...}`. `data` se omite cuando no aplica. En endpoints paginados se usan `page`, `size` y `sort`.

## Boletos (`/boletos`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /boletos/{compraId}` | Consulta los boletos de una compra. Path: `compraId` | `{"success":true,"data":{"id":1,"tiqueteCompras":[]}}` | CLIENTE |
| `POST /boletos/checkin/{codigoQR}` | Valida un boleto. Path: `codigoQR` | `{"success":true,"mensaje":"Check-in realizado"}` | ORGANIZACION |

## Categorías (`/categorias`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /categorias` | Lista categorías. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"nombre":"Música"}],"totalElements":1}}` | Público |
| `GET /categorias/nombres` | Lista nombres de categorías | `{"success":true,"data":[{"id":1,"nombre":"Música"}]}` | Público |
| `GET /categorias/destacadas` | Lista categorías destacadas | `{"success":true,"data":[{"id":1,"nombre":"Música"}]}` | Público |
| `GET /categorias/con-eventos` | Lista categorías con eventos | `{"success":true,"data":[{"id":1,"nombre":"Música","totalEventos":3}]}` | Público |
| `GET /categorias/{id}` | Obtiene una categoría. Path: `id` | `{"success":true,"data":{"id":1,"nombre":"Música"}}` | Público |
| `POST /categorias` | Crea una categoría. Multipart: `nombre`, `foto` opcional | `{"success":true,"mensaje":"Categoría creada exitosamente"}` | ADMINISTRADOR |
| `PUT /categorias/{id}` | Actualiza una categoría. Path: `id`; multipart: `nombre`, `foto` opcional | `{"success":true,"mensaje":"Categoría actualizada exitosamente"}` | ADMINISTRADOR |
| `DELETE /categorias/{id}` | Elimina una categoría. Path: `id` | `{"success":true,"mensaje":"Categoría eliminada exitosamente"}` | ADMINISTRADOR |

## Compras (`/compras`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /compras` | Lista las compras del usuario. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"total":50000}],"totalElements":1}}` | Autenticado |
| `GET /compras/{id}` | Consulta una compra. Path: `id` | `{"success":true,"data":{"id":1,"total":50000,"items":[]}}` | Autenticado |
| `POST /compras` | Realiza una compra. Body: `idempotencyKey`, `items[]` (`localidadId`, `cantidad`) | `{"success":true,"data":{"id":1,"total":50000}}` | Autenticado |
| `DELETE /compras/{id}` | Cancela una compra. Path: `id` | `{"success":true,"mensaje":"Compra cancelada"}` | Autenticado |

## Estados (`/enums`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /enums/motivos-rechazos` | Lista motivos de rechazo | `{"success":true,"data":["DOCUMENTACION_INVALIDA"]}` | ADMINISTRADOR, MODERADOR |

## Eventos (`/eventos`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /eventos` | Lista eventos. Query opcional: `categoriaId`; paginación | `{"success":true,"data":{"content":[{"id":1,"titulo":"Concierto"}],"totalElements":1}}` | Público |
| `GET /eventos/{id}` | Obtiene un evento. Path: `id` | `{"success":true,"data":{"id":1,"titulo":"Concierto","estado":"PUBLICADO"}}` | Público |
| `GET /eventos/buscar` | Busca por título o fecha. Query: `titulo` o `fecha` (`yyyy-MM-dd`); paginación | `{"success":true,"data":{"content":[{"id":1,"titulo":"Concierto"}]}}` | Público |
| `GET /eventos/organizador` | Lista eventos propios. Query: paginación | `{"success":true,"data":{"content":[],"totalElements":0}}` | ORGANIZACION |
| `GET /eventos/mapa` | Busca eventos para el mapa. Query opcional: `categoriaId`, `lat`, `lng`, `radioKm` | `{"success":true,"data":[{"id":1,"titulo":"Concierto","latitud":4.6,"longitud":-74.1}]}` | Público |
| `GET /eventos/organizador/buscar` | Busca eventos propios. Query: `titulo`; paginación | `{"success":true,"data":{"content":[],"totalElements":0}}` | ORGANIZACION |
| `GET /eventos/admin/buscar` | Filtra eventos. Query opcional: `titulo`, `categoriaId`, `estado`; paginación | `{"success":true,"data":{"content":[{"id":1,"estado":"PENDIENTE"}]}}` | ADMINISTRADOR |
| `POST /eventos` | Crea un evento. Multipart: `datos` (`titulo`, `descripcion`, `lugar`, `fecha`, `hora`, `categoriaId`, `latitud`, `longitud`), `foto` opcional | `{"success":true,"data":{"id":1,"estado":"BORRADOR"}}` | ORGANIZACION |
| `PUT /eventos/{id}` | Actualiza un evento. Path: `id`; multipart: `datos`, `foto` opcional | `{"success":true,"data":{"id":1,"titulo":"Concierto actualizado"}}` | ORGANIZACION |
| `DELETE /eventos/{id}` | Elimina un evento. Path: `id` | `{"success":true,"mensaje":"Evento eliminado"}` | ORGANIZACION |
| `PATCH /eventos/{id}/suspender` | Suspende un evento. Path: `id`; body: `motivo`, `observacion` | `{"success":true,"data":{"id":1,"estado":"SUSPENDIDO"}}` | ADMINISTRADOR |
| `PATCH /eventos/{id}/reactivar` | Reactiva un evento. Path: `id` | `{"success":true,"data":{"id":1,"estado":"PUBLICADO"}}` | ADMINISTRADOR |

## Lista de deseos (`/deseos`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /deseos` | Lista eventos guardados. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"titulo":"Concierto"}]}}` | Autenticado |
| `POST /deseos/{eventoId}` | Agrega un evento. Path: `eventoId` | `{"success":true,"mensaje":"Evento agregado a tu lista de deseados"}` | Autenticado |
| `DELETE /deseos/{eventoId}` | Quita un evento. Path: `eventoId` | `{"success":true,"mensaje":"Evento removido de tu lista de deseados"}` | Autenticado |

## Localidades (`/eventos/{eventoId}/localidades`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /eventos/{eventoId}/localidades` | Lista localidades. Path: `eventoId` | `{"success":true,"data":[{"id":1,"nombre":"VIP","precio":100000}]}` | ORGANIZACION |
| `POST /eventos/{eventoId}/localidades` | Crea una localidad. Path: `eventoId`; body: `nombre`, `precio`, `capacidad` | `{"success":true,"data":{"id":1,"nombre":"VIP","capacidad":100}}` | ORGANIZACION |
| `PUT /eventos/{eventoId}/localidades/{localidadId}` | Actualiza una localidad. Paths: `eventoId`, `localidadId`; body: `nombre`, `precio`, `capacidad` | `{"success":true,"data":{"id":1,"precio":120000}}` | ORGANIZACION |
| `DELETE /eventos/{eventoId}/localidades/{localidadId}` | Elimina una localidad. Paths: `eventoId`, `localidadId` | `{"success":true,"mensaje":"Localidad eliminada"}` | ORGANIZACION |

## Moderación (`/moderadores`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /moderadores` | Lista moderadores. Query: paginación | `{"success":true,"data":{"content":[{"id":2,"rolNombre":"MODERADOR"}]}}` | ADMINISTRADOR |
| `PUT /moderadores/{id}/revocar` | Revoca el rol. Path: `id` | `{"success":true,"mensaje":"Rol de moderador revocado correctamente"}` | ADMINISTRADOR |
| `PUT /moderadores/{id}/asignar` | Asigna el rol. Path: `id` | `{"success":true,"mensaje":"Rol de moderador asignado correctamente"}` | ADMINISTRADOR |
| `GET /moderadores/moderacion/pendientes` | Lista eventos pendientes. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"estado":"PENDIENTE_REVISION"}]}}` | MODERADOR, ADMINISTRADOR |
| `GET /moderadores/{id}/moderaciones` | Consulta moderaciones de un evento. Path: `id`; query: paginación | `{"success":true,"data":{"content":[{"id":5,"estadoResultante":"APROBADO"}]}}` | Autenticado |
| `PATCH /moderadores/{id}/aprobar` | Aprueba un evento. Path: `id` | `{"success":true,"data":{"id":1,"estado":"PUBLICADO"}}` | MODERADOR, ADMINISTRADOR |
| `PATCH /moderadores/{id}/solicitar-correccion` | Solicita correcciones. Path: `id`; body: `motivo`, `observacion` | `{"success":true,"data":{"id":1,"estado":"CORRECCION_SOLICITADA"}}` | MODERADOR, ADMINISTRADOR |
| `PATCH /moderadores/{id}/rechazar` | Rechaza un evento. Path: `id`; body: `motivo`, `observacion` | `{"success":true,"data":{"id":1,"estado":"RECHAZADO"}}` | MODERADOR, ADMINISTRADOR |

## Notificaciones (`/notificaciones`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /notificaciones` | Lista notificaciones | `{"success":true,"data":[{"id":"n1","leida":false}]}` | Autenticado |
| `GET /notificaciones/no-leidas` | Cuenta notificaciones no leídas | `{"success":true,"data":3}` | Autenticado |
| `PUT /notificaciones/{id}/leer` | Marca una notificación como leída. Path: `id` | `{"success":true,"mensaje":"Notificación marcada como leída"}` | Autenticado |
| `DELETE /notificaciones/limpiar` | Elimina notificaciones leídas | `{"success":true,"mensaje":"Notificaciones leídas eliminadas"}` | Autenticado |

## Organizaciones (`/organizaciones`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /organizaciones` | Lista organizaciones. Query: paginación | `{"success":true,"data":{"content":[{"id":2,"nombre":"Empresa"}]}}` | Público |
| `GET /organizaciones/top` | Lista organizaciones mejor valoradas. Query: paginación | `{"success":true,"data":{"content":[{"id":2,"promedioRating":4.8}]}}` | Público |
| `GET /organizaciones/mi-organizacion` | Consulta la organización propia | `{"success":true,"data":{"id":2,"nivel":"ORO"}}` | ORGANIZACION |
| `GET /organizaciones/{id}` | Consulta una organización. Path: `id` | `{"success":true,"data":{"id":2,"razonSocial":"Empresa"}}` | ADMINISTRADOR |
| `PATCH /organizaciones/{id}/nivel` | Cambia el nivel. Path: `id`; body: `nivel` | `{"success":true,"mensaje":"Nivel actualizado"}` | ADMINISTRADOR |
| `GET /organizaciones/sugerencias-pendientes` | Lista sugerencias de ascenso. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"estado":"PENDIENTE"}]}}` | ADMINISTRADOR |
| `PATCH /organizaciones/{id}/aprobar-sugerencia` | Aprueba un ascenso. Path: `id` | `{"success":true,"mensaje":"Ascenso aprobado"}` | ADMINISTRADOR |
| `PATCH /organizaciones/{id}/rechazar-sugerencia` | Rechaza un ascenso. Path: `id` | `{"success":true,"mensaje":"Ascenso rechazado"}` | ADMINISTRADOR |

## Promociones (`/promociones`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /promociones/evento/{eventoId}` | Consulta la promoción de un evento. Path: `eventoId` | `{"success":true,"data":{"id":1,"descuento":20}}` | Público |
| `GET /promociones` | Lista promociones. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"descuento":20}]}}` | ADMINISTRADOR |
| `GET /promociones/organizador` | Lista promociones propias. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"eventoId":3}]}}` | ORGANIZACION |
| `POST /promociones` | Crea una promoción. Body: `eventoId`, `descripcion`, `descuento`, `fechaInicio`, `fechaFin` | `{"success":true,"mensaje":"Promoción creada"}` | ORGANIZACION, ADMINISTRADOR |
| `PUT /promociones/{id}` | Actualiza una promoción. Path: `id`; body: campos de promoción | `{"success":true,"mensaje":"Promoción actualizada"}` | ORGANIZACION, ADMINISTRADOR |
| `DELETE /promociones/{id}` | Elimina una promoción. Path: `id` | `{"success":true,"mensaje":"Promoción eliminada"}` | ORGANIZACION, ADMINISTRADOR |

## Roles (`/roles`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /roles` | Lista roles. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"nombre":"CLIENTE"}]}}` | Público |
| `POST /roles` | Crea un rol. Body: `nombre` | `{"success":true,"mensaje":"Rol creado exitosamente"}` | ADMINISTRADOR |
| `PUT /roles/{id}` | Actualiza un rol. Path: `id`; query: `nombre` | `{"success":true,"mensaje":"Rol actualizado exitosamente"}` | ADMINISTRADOR |
| `DELETE /roles/{id}` | Elimina un rol. Path: `id` | `{"success":true,"mensaje":"Rol eliminado exitosamente"}` | ADMINISTRADOR |

## Seguidores (`/organizadores`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `POST /organizadores/{organizadorId}/seguir` | Sigue un organizador. Path: `organizadorId` | `{"success":true,"mensaje":"Ahora sigues a este organizador"}` | Autenticado |
| `DELETE /organizadores/{organizadorId}/seguir` | Deja de seguir. Path: `organizadorId` | `{"success":true,"mensaje":"Dejaste de seguir al organizador"}` | Autenticado |
| `GET /organizadores/{organizadorId}/seguidores/total` | Cuenta seguidores. Path: `organizadorId` | `{"success":true,"data":42}` | Público |
| `GET /organizadores/siguiendo` | Lista organizadores seguidos | `{"success":true,"data":[{"id":2,"nombre":"Empresa"}]}` | Autenticado |

## Usuarios (`/usuarios`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /usuarios` | Lista usuarios. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"nombre":"Ana"}]}}` | ADMINISTRADOR |
| `GET /usuarios/buscar` | Busca usuarios. Query opcional: `nombre`, `rolId`; paginación | `{"success":true,"data":{"content":[{"id":1,"nombre":"Ana"}]}}` | ADMINISTRADOR |
| `GET /usuarios/{id}` | Consulta un usuario. Path: `id` | `{"success":true,"data":{"id":1,"nombre":"Ana"}}` | ADMINISTRADOR |
| `POST /usuarios` | Crea un usuario. Query: `nombre`, `correo`, `telefono`, `clave`, `rolId` | `{"success":true,"mensaje":"Usuario creado exitosamente"}` | ADMINISTRADOR |
| `PUT /usuarios/{id}/asignar-rol` | Asigna un rol. Path: `id`; query: `rolId` | `{"success":true,"mensaje":"Rol asignado exitosamente"}` | ADMINISTRADOR |
| `DELETE /usuarios/{id}` | Elimina un usuario. Path: `id` | `{"success":true,"mensaje":"Usuario eliminado exitosamente"}` | Autenticado |
| `GET /usuarios/perfil` | Consulta el perfil propio | `{"success":true,"data":{"id":1,"nombre":"Ana"}}` | Autenticado |
| `PUT /usuarios/perfil` | Actualiza el perfil. Body: datos del usuario | `{"success":true,"data":{"id":1,"nombre":"Ana Actualizada"}}` | Autenticado |
| `PUT /usuarios/perfil/cambiar-clave` | Cambia la contraseña. Body: `claveActual`, `claveNueva` | `{"success":true,"mensaje":"Contraseña actualizada correctamente"}` | Autenticado |

## Valoraciones (`/valoraciones`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `GET /valoraciones/usuario` | Lista valoraciones propias. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"calificacion":5}]}}` | CLIENTE |
| `GET /valoraciones/organizador/{organizadorId}` | Lista valoraciones de un organizador. Path: `organizadorId`; paginación | `{"success":true,"data":{"content":[{"id":1,"calificacion":5}]}}` | Público |
| `POST /valoraciones` | Crea una valoración. Body: `organizacionId`, `comentario`, `calificacion` | `{"success":true,"mensaje":"Valoración creada exitosamente"}` | CLIENTE |
| `PUT /valoraciones/{id}` | Actualiza una valoración. Path: `id`; query: `comentario`, `calificacion` | `{"success":true,"mensaje":"Valoración actualizada exitosamente"}` | CLIENTE |
| `DELETE /valoraciones/{id}` | Elimina una valoración. Path: `id` | `{"success":true,"mensaje":"Valoración eliminada exitosamente"}` | CLIENTE |

## Verificación (`/verificacion`)

| Método y ruta | Descripción y parámetros | Respuesta | Rol |
|---|---|---|---|
| `POST /verificacion/solicitar` | Solicita verificación. Multipart: `datos` (`razonSocial`, `nit`, `representanteLegal`, `correoEmpresarial`, `mensaje`), `rut` opcional | `{"success":true,"mensaje":"Solicitud de verificación enviada correctamente"}` | ORGANIZACION |
| `GET /verificacion/mis-solicitudes` | Consulta solicitudes propias | `{"success":true,"data":{"id":1,"estado":"PENDIENTE"}}` | ORGANIZACION |
| `GET /verificacion/pendientes` | Lista solicitudes pendientes. Query: paginación | `{"success":true,"data":{"content":[{"id":1,"estado":"PENDIENTE"}]}}` | ADMINISTRADOR, MODERADOR |
| `GET /verificacion/{solicitudId}` | Consulta una solicitud. Path: `solicitudId` | `{"success":true,"data":{"id":1,"estado":"PENDIENTE"}}` | ADMINISTRADOR, MODERADOR |
| `PUT /verificacion/{solicitudId}/aprobar` | Aprueba una solicitud. Path: `solicitudId` | `{"success":true,"mensaje":"Solicitud aprobada","data":"clave-generada"}` | ADMINISTRADOR, MODERADOR |
| `PATCH /verificacion/{solicitudId}/solicitar-correccion` | Solicita correcciones. Path: `solicitudId`; query: `motivo` | `{"success":true,"mensaje":"Solicitud marcada para corrección"}` | ADMINISTRADOR, MODERADOR |
| `PUT /verificacion/{solicitudId}/rechazar` | Rechaza una solicitud. Path: `solicitudId`; query: `motivo` | `{"success":true,"mensaje":"Solicitud rechazada"}` | ADMINISTRADOR, MODERADOR |