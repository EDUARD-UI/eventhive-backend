# Documentación de endpoints de EventHive Backend

Este documento describe los principales endpoints del backend, indicando qué reciben, qué validaciones requieren y qué estructura devuelven.

## 1. Estructura general de respuesta

Todos los endpoints responden con un envoltorio `ApiResponse<T>`:

```json
{
  "success": true,
  "mensaje": "Mensaje descriptivo",
  "data": { }
}
```

Ejemplo concreto de un objeto devuelto:

```json
{
  "success": true,
  "mensaje": "Usuario obtenido",
  "data": {
    "id": 1,
    "nombre": "C",
    "edad": 34,
    "correo": "carlos@email.com"
  }
}
```

> Importante: `data` puede contener un objeto simple, un array, o un objeto paginado. Por ejemplo, un endpoint puede devolver un objeto como `{ "nombre": "C", "edad": 34 }` dentro de `data`, o una lista dentro de `content` cuando es paginado.

Cuando la respuesta es paginada, la API usa `PagedResponse<T>`:

```json
{
  "content": [
    {}
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 25,
  "totalPages": 3,
  "hasNext": true,
  "hasPrevious": false
}
```

En caso de error, la respuesta suele devolver:

```json
{
  "success": false,
  "mensaje": "Descripción del error",
  "data": null
}
```

---

## 2. Auth

### Base: `/api/auth`

#### `GET /api/auth/me`
- Recibe: no body, requiere sesión activa.
- Devuelve: `ApiResponse<UsuarioSesionDTO>`
- Descripción: devuelve la sesión actual del usuario autenticado.
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Sesión activa",
  "data": {
    "id": 7,
    "nombre": "Carlos",
    "correo": "carlos@email.com",
    "rol": "CLIENTE"
  }
}
```

#### `POST /api/auth/login`
- Recibe: body `LoginRequest` con `correo` y `clave`.
- Devuelve: `ApiResponse<LoginResponseDTO>`
- Descripción: autentica al usuario y devuelve tokens/usuario.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Login exitoso",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJzdWIiOiJjYXJsb3NAY...",
    "usuario": {
      "id": 7,
      "nombre": "Carlos",
      "correo": "carlos@email.com"
    }
  }
}
```

#### `POST /api/auth/refresh`
- Recibe: body `RefreshRequest` con `refreshToken`.
- Devuelve: `ApiResponse<LoginResponseDTO>`
- Descripción: renueva el token de acceso.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Token renovado",
  "data": {
    "accessToken": "newAccessToken123",
    "refreshToken": "refreshToken456",
    "usuario": {
      "id": 7,
      "nombre": "Carlos"
    }
  }
}
```

#### `POST /api/auth/logout`
- Recibe: no body.
- Devuelve: `ApiResponse<Void>`
- Descripción: cierra la sesión del usuario.
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Sesión cerrada",
  "data": null
}
```

#### `POST /api/auth/registrar-cliente`
- Recibe: body `RegistroRequest` con nombre, correo, teléfono y clave.
- Devuelve: `ApiResponse<Void>`
- Descripción: registra un usuario cliente.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Registro exitoso",
  "data": null
}
```

#### `POST /api/auth/registrar-organizacion`
- Recibe: body `RegistroRequest` con nombre, correo, teléfono y clave.
- Devuelve: `ApiResponse<Void>`
- Descripción: registra una organización con su usuario representante.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Registro exitoso",
  "data": null
}
```

---

## 3. Usuarios

### Base: `/api/usuarios`

#### `GET /api/usuarios`
- Recibe: query params `page`, `size`, `sort` (paginación). Requiere rol ADMINISTRADOR.
- Devuelve: `ApiResponse<PagedResponse<UsuarioDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Usuarios obtenidos",
  "data": {
    "content": [
      {
        "id": 1,
        "nombre": "Ana",
        "correo": "ana@email.com",
        "rol": "CLIENTE"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

#### `GET /api/usuarios/buscar`
- Recibe: `nombre`, `rolId`, y paginación.
- Devuelve: `ApiResponse<PagedResponse<UsuarioDTO>>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Resultados de búsqueda",
  "data": {
    "content": [
      {
        "id": 2,
        "nombre": "Luis",
        "correo": "luis@email.com",
        "rol": "OPERADOR"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/usuarios/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<UsuarioDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Usuario obtenido",
  "data": {
    "id": 3,
    "nombre": "María",
    "correo": "maria@email.com",
    "rol": "REPRESENTANTE"
  }
}
```

#### `GET /api/usuarios/misOrganizaciones-seguidas`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<OrganizacionPublicaDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organizaciones seguidas obtenidas",
  "data": {
    "content": [
      {
        "id": 10,
        "nombre": "Eventica",
        "descripcion": "Organización de eventos"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `DELETE /api/usuarios/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Usuario eliminado exitosamente",
  "data": null
}
```

#### `GET /api/usuarios/perfil`
- Recibe: no body.
- Devuelve: `ApiResponse<UsuarioDTO>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Perfil obtenido",
  "data": {
    "id": 7,
    "nombre": "Carlos",
    "correo": "carlos@email.com",
    "telefono": "+56912345678",
    "rol": "CLIENTE"
  }
}
```

#### `PUT /api/usuarios/perfil`
- Recibe: body `ActualizarPerfilRequest`.
- Devuelve: `ApiResponse<UsuarioDTO>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Perfil actualizado",
  "data": {
    "id": 7,
    "nombre": "Carlos Vega",
    "correo": "carlos@email.com"
  }
}
```

#### `PUT /api/usuarios/perfil/cambiar-clave`
- Recibe: body `EditarClaveRequest` con clave actual y nueva.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Contraseña actualizada correctamente",
  "data": null
}
```

---

## 4. Eventos

### Base: `/api/eventos`

#### `GET /api/eventos`
- Recibe: `categoriaId` opcional y paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Descripción: lista eventos públicos.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos obtenidos",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz",
        "categoria": "Música",
        "fecha": "2026-10-14",
        "precio": 25000
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/eventos/proximos`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Descripción: lista eventos próximos a la fecha actual.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Próximos eventos obtenidos",
  "data": {
    "content": [
      {
        "id": 18,
        "titulo": "Expo Creativa",
        "fecha": "2026-09-20",
        "estado": "PUBLICADO"
      }
    ]
  }
}
```

#### `GET /api/eventos/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<EventoDTO>`
- Descripción: detalle de un evento público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento obtenido",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "descripcion": "Evento musical con artistas locales",
    "fecha": "2026-10-14",
    "ubicacion": "Santiago",
    "precio": 25000,
    "estado": "PUBLICADO"
  }
}
```

#### `GET /api/eventos/organizador/{id}`
- Recibe: `id` del evento en path.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento obtenido",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "organizacionId": 20,
    "estado": "PENDIENTE"
  }
}
```

#### `GET /api/eventos/organizador`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Permisos: REPRESENTANTE o OPERADOR.
- Descripción: lista los eventos de la organización del usuario autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos obtenidos",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz",
        "estado": "PUBLICADO"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/eventos/admin/{id}`
- Recibe: `id` del evento.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento obtenido",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "PENDIENTE_REVISION",
    "organizacion": "Eventica"
  }
}
```

#### `GET /api/eventos/mapa`
- Recibe: `categoriaId`, `lat`, `lng`, `radioKm` opcionales.
- Devuelve: `ApiResponse<List<EventoMapaDTO>>`
- Descripción: eventos para renderizar en mapa.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos para el mapa",
  "data": [
    {
      "id": 15,
      "titulo": "Festival de Jazz",
      "latitud": -33.4489,
      "longitud": -70.6693
    }
  ]
}
```

#### `GET /api/eventos/buscar`
- Recibe: `titulo` y/o `fecha` y paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoBusquedaDTO>>`
- Descripción: búsqueda pública de eventos.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Resultados de búsqueda",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz",
        "fecha": "2026-10-14"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/eventos/organizador/buscar`
- Recibe: `titulo` y paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Resultados de búsqueda",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz"
      }
    ]
  }
}
```

#### `GET /api/eventos/admin/buscar`
- Recibe: `titulo`, `categoriaId`, `estado`, paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Resultados de búsqueda",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz",
        "estado": "PENDIENTE"
      }
    ]
  }
}
```

#### `POST /api/eventos`
- Recibe: multipart/form-data con:
  - `datos`: `EventoRequest`
  - `foto`: archivo opcional
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento creado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "BORRADOR"
  }
}
```

#### `PUT /api/eventos/{id}`
- Recibe: multipart/form-data con:
  - `datos`: `EventoRequest`
  - `foto`: archivo opcional
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento actualizado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz Actualizado",
    "estado": "BORRADOR"
  }
}
```

#### `PATCH /api/eventos/{id}/cancelar`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "evento cancelado",
  "data": null
}
```

#### `PATCH /api/eventos/{id}/retirar`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento retirado a borrador",
  "data": null
}
```

#### `DELETE /api/eventos/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento eliminado",
  "data": null
}
```

---

## 5. Localidades

### Base: `/api/eventos/{eventoId}/localidades`

#### `GET /api/eventos/{eventoId}/localidades`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<List<LocalidadDTO>>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Localidades obtenidas",
  "data": [
    {
      "id": 1,
      "nombre": "General",
      "precio": 25000,
      "cantidad": 150
    }
  ]
}
```

#### `POST /api/eventos/{eventoId}/localidades`
- Recibe: `eventoId` en path y body `LocalidadRequest`.
- Devuelve: `ApiResponse<LocalidadDTO>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Localidad agregada",
  "data": {
    "id": 2,
    "nombre": "VIP",
    "precio": 60000,
    "cantidad": 40
  }
}
```

#### `PUT /api/eventos/{eventoId}/localidades/{localidadId}`
- Recibe: `eventoId` y `localidadId` en path, body `LocalidadRequest`.
- Devuelve: `ApiResponse<LocalidadDTO>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Localidad actualizada",
  "data": {
    "id": 2,
    "nombre": "VIP Premium",
    "precio": 65000,
    "cantidad": 35
  }
}
```

#### `DELETE /api/eventos/{eventoId}/localidades/{localidadId}`
- Recibe: `eventoId` y `localidadId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Localidad eliminada",
  "data": null
}
```

---

## 6. Compras

### Base: `/api/compras`

#### `GET /api/compras`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<CompraResponseDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Compras obtenidas",
  "data": {
    "content": [
      {
        "id": 101,
        "evento": "Festival de Jazz",
        "total": 50000,
        "estado": "PAGADA"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/compras/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<CompraResponseDTO>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Compra obtenida",
  "data": {
    "id": 101,
    "evento": "Festival de Jazz",
    "cantidadBoletos": 2,
    "total": 50000,
    "estado": "PAGADA"
  }
}
```

#### `POST /api/compras`
- Recibe: body `CompraRequestDTO`.
- Devuelve: `ApiResponse<CompraResponseDTO>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Compra realizada",
  "data": {
    "id": 102,
    "evento": "Expo Creativa",
    "cantidadBoletos": 1,
    "total": 25000,
    "estado": "PAGADA"
  }
}
```

#### `DELETE /api/compras/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Compra cancelada",
  "data": null
}
```

---

## 7. Boletos

### Base: `/api/boletos`

#### `GET /api/boletos/{compraId}`
- Recibe: `compraId` en path.
- Devuelve: `ApiResponse<BoletosCompraDTO>`
- Permisos: CLIENTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "boletos de Compra obtenida",
  "data": {
    "compraId": 101,
    "boletos": [
      {
        "codigoQR": "BOL-001-ABC",
        "nombreLocalidad": "General",
        "estado": "ACTIVO"
      }
    ]
  }
}
```

#### `POST /api/boletos/checkin/{codigoQR}`
- Recibe: `codigoQR` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "¡¡Tiquete valido!! check-in realizado",
  "data": null
}
```

---

## 8. Lista de deseos

### Base: `/api/deseos`

#### `GET /api/deseos`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos deseados obtenidos",
  "data": {
    "content": [
      {
        "id": 20,
        "titulo": "Concierto Nocturno",
        "fecha": "2026-11-12"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `POST /api/deseos/{eventoId}`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento agregado a tu lista de deseados",
  "data": null
}
```

#### `DELETE /api/deseos/{eventoId}`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento removido de tu lista de deseados",
  "data": null
}
```

---

## 9. Categorías

### Base: `/api/categorias`

#### `GET /api/categorias`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<CategoriaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categorías obtenidas",
  "data": {
    "content": [
      {
        "id": 1,
        "nombre": "Música",
        "descripcion": "Eventos musicales"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/categorias/nombres`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<CategoriaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categorías obtenidas",
  "data": [
    { "id": 1, "nombre": "Música" },
    { "id": 2, "nombre": "Tecnología" }
  ]
}
```

#### `GET /api/categorias/destacadas`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<CategoriaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categorías destacadas",
  "data": [
    { "id": 1, "nombre": "Música" },
    { "id": 3, "nombre": "Arte" }
  ]
}
```

#### `GET /api/categorias/con-eventos`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<CategoriaEventosDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categorías con eventos",
  "data": [
    {
      "id": 1,
      "nombre": "Música",
      "cantidadEventos": 5
    }
  ]
}
```

#### `GET /api/categorias/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<CategoriaDTO>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categoría obtenida",
  "data": {
    "id": 1,
    "nombre": "Música",
    "descripcion": "Eventos musicales"
  }
}
```

#### `POST /api/categorias`
- Recibe: multipart/form-data con `datos` y `foto` opcional.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categoría creada exitosamente",
  "data": null
}
```

#### `PUT /api/categorias/{id}`
- Recibe: multipart/form-data con `datos` y `foto` opcional.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categoría actualizada exitosamente",
  "data": null
}
```

#### `DELETE /api/categorias/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categoría eliminada exitosamente",
  "data": null
}
```

---

## 10. Organizaciones

### Base: `/api/organizaciones`

#### `GET /api/organizaciones`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<OrganizacionPublicaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organizaciones obtenidas",
  "data": {
    "content": [
      {
        "id": 10,
        "nombre": "Eventica",
        "descripcion": "Organización de eventos"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `GET /api/organizaciones/{organizacionId}`
- Recibe: `organizacionId` en path.
- Devuelve: `ApiResponse<OrganizacionDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organización obtenida",
  "data": {
    "id": 10,
    "nombre": "Eventica",
    "rut": "76.123.456-7",
    "estado": "VERIFICADA"
  }
}
```

#### `GET /api/organizaciones/top`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<OrganizacionPublicaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Top de organizaciones",
  "data": {
    "content": [
      { "id": 10, "nombre": "Eventica" },
      { "id": 11, "nombre": "Arena Live" }
    ]
  }
}
```

#### `GET /api/organizaciones/mi-organizacion`
- Recibe: sin body.
- Devuelve: `ApiResponse<OrganizacionDTO>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organización obtenida",
  "data": {
    "id": 10,
    "nombre": "Eventica",
    "estado": "VERIFICADA"
  }
}
```

#### `GET /api/organizaciones/mis-invitaciones`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<InvitacionOrganizacionDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Invitaciones pendientes obtenidas",
  "data": {
    "content": [
      {
        "id": 3,
        "organizacion": "Eventica",
        "estado": "PENDIENTE"
      }
    ]
  }
}
```

#### `GET /api/organizaciones/invitaciones-enviadas`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<InvitacionOrganizacionDTO>>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Invitaciones de la organización obtenidas",
  "data": {
    "content": [
      {
        "id": 4,
        "correo": "usuario@correo.com",
        "estado": "ENVIADA"
      }
    ]
  }
}
```

#### `GET /api/organizaciones/mis-operadores`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<OperadorDTO>>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Operadores obtenidos",
  "data": {
    "content": [
      {
        "id": 12,
        "nombre": "Luis",
        "rol": "OPERADOR"
      }
    ]
  }
}
```

#### `GET /api/organizaciones/mi-organizacion/seguidores`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<UsuarioDTO>>`
- Permisos: REPRESENTANTE u OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Seguidores de la organizacion obtenidos",
  "data": {
    "content": [
      { "id": 7, "nombre": "Carlos" }
    ]
  }
}
```

#### `PATCH /api/organizaciones/operadores/{operadorId}/actualizar-permisos`
- Recibe: `operadorId` y body `PermisosOperadorRequest`.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Permisos actualizados",
  "data": null
}
```

#### `DELETE /api/organizaciones/expulsar-operador/{operadorId}`
- Recibe: `operadorId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Operador removido de la organización",
  "data": null
}
```

#### `POST /api/organizaciones/invitar`
- Recibe: query param `correo`.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Invitación enviada",
  "data": null
}
```

#### `PATCH /api/organizaciones/invitaciones/{invitacionId}/aceptar`
- Recibe: `invitacionId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: público/autenticado según flujo de negocio.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Invitación aceptada",
  "data": null
}
```

#### `PATCH /api/organizaciones/invitaciones/{operadorId}/rechazar`
- Recibe: `operadorId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Invitación rechazada",
  "data": null
}
```

#### `GET /api/organizaciones/sugerencias-pendientes`
- Recibe: paginación.
- Devuelve: `ApiResponse<Page<SugerenciaAscensoDTO>>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Sugerencias de ascenso pendientes",
  "data": {
    "content": [
      {
        "id": 5,
        "usuario": "Pedro",
        "organizacion": "Eventica",
        "estado": "PENDIENTE"
      }
    ]
  }
}
```

#### `PATCH /api/organizaciones/{sugerenciaId}/aprobar-sugerencia`
- Recibe: `sugerenciaId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Ascenso aprobado",
  "data": null
}
```

#### `PATCH /api/organizaciones/{sugerenciaId}/rechazar-sugerencia`
- Recibe: `sugerenciaId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Ascenso rechazado",
  "data": null
}
```

---

## 11. Seguidoras / seguidores

### Base: `/api/seguidores`

#### `POST /api/seguidores/{organizacionId}/seguir`
- Recibe: `organizacionId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Ahora sigues a este organizador",
  "data": null
}
```

#### `DELETE /api/seguidores/{organizacionId}/seguir`
- Recibe: `organizacionId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Dejaste de seguir al organizador",
  "data": null
}
```

---

## 12. Valoraciones

### Base: `/api/valoraciones`

#### `GET /api/valoraciones/usuario`
- Recibe: paginación.
- Devuelve: `ApiResponse<Page<ValoracionDTO>>`
- Permisos: CLIENTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Valoraciones obtenidas",
  "data": {
    "content": [
      {
        "id": 1,
        "comentario": "Excelente servicio",
        "calificacion": 5,
        "usuario": "Carlos"
      }
    ]
  }
}
```

#### `GET /api/valoraciones/organizacion/{organizacionId}`
- Recibe: `organizacionId` en path, paginación.
- Devuelve: `ApiResponse<Page<ValoracionDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Valoraciones de la organización",
  "data": {
    "content": [
      {
        "id": 1,
        "comentario": "Muy buena atención",
        "calificacion": 4,
        "usuario": "Ana"
      }
    ]
  }
}
```

#### `POST /api/valoraciones`
- Recibe: body `ValoracionRequest` con `organizacionId`, `comentario`, `calificacion`.
- Devuelve: `ApiResponse<Void>`
- Permisos: CLIENTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Valoración creada exitosamente",
  "data": null
}
```

#### `PUT /api/valoraciones/{id}`
- Recibe: `id` en path y query params `comentario` y `calificacion`.
- Devuelve: `ApiResponse<Void>`
- Permisos: CLIENTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Valoración actualizada exitosamente",
  "data": null
}
```

#### `DELETE /api/valoraciones/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: CLIENTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Valoración eliminada exitosamente",
  "data": null
}
```

---

## 13. Notificaciones

### Base: `/api/notificaciones`

#### `GET /api/notificaciones`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<NotificationDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Notificaciones obtenidas",
  "data": [
    {
      "id": "n1",
      "titulo": "Nuevo evento",
      "mensaje": "Se ha publicado un nuevo evento",
      "leida": false
    }
  ]
}
```

#### `GET /api/notificaciones/no-leidas`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<Long>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Total no leídas",
  "data": 3
}
```

#### `PUT /api/notificaciones/{id}/leer`
- Recibe: `id` de notificación en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Notificación marcada como leída",
  "data": null
}
```

#### `DELETE /api/notificaciones/limpiar`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<Void>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Notificaciones leídas eliminadas",
  "data": null
}
```

---

## 14. Verificación de organizaciones

### Base: `/api/verificacion`

#### `GET /api/verificacion/mis-solicitudes`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<SolicitudVerificacionDTO>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud obtenida",
  "data": {
    "id": 5,
    "estado": "PENDIENTE",
    "organizacion": "Eventica"
  }
}
```

#### `GET /api/verificacion/pendientes`
- Recibe: paginación.
- Devuelve: `ApiResponse<Page<SolicitudVerificacionDTO>>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitudes obtenidas",
  "data": {
    "content": [
      {
        "id": 5,
        "estado": "PENDIENTE",
        "organizacion": "Eventica"
      }
    ]
  }
}
```

#### `GET /api/verificacion/{solicitudId}`
- Recibe: `solicitudId` en path.
- Devuelve: `ApiResponse<SolicitudVerificacionDTO>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud obtenida",
  "data": {
    "id": 5,
    "estado": "PENDIENTE",
    "organizacion": "Eventica",
    "motivo": "Documentación incompleta"
  }
}
```

#### `POST /api/verificacion/solicitar`
- Recibe: multipart/form-data con `datos` y `rut` opcional.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud de verificación enviada correctamente",
  "data": null
}
```

#### `PUT /api/verificacion/{solicitudId}/aprobar`
- Recibe: `solicitudId` en path.
- Devuelve: `ApiResponse<String>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud aprobada. Comparte esta contraseña con el organizador",
  "data": "CLAVE-VERIFICACION-123"
}
```

#### `PUT /api/verificacion/{solicitudId}/rechazar`
- Recibe: `solicitudId` en path y parámetro `motivo`.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud rechazada",
  "data": null
}
```

#### `PATCH /api/verificacion/{solicitudId}/solicitar-correccion`
- Recibe: `solicitudId` en path y parámetro `motivo`.
- Devuelve: `ApiResponse<SolicitudVerificacionDTO>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud marcada para correccion",
  "data": {
    "id": 5,
    "estado": "CORRECCION",
    "motivo": "Falta firma digital"
  }
}
```

#### `PATCH /api/verificacion/{solicitudId}/reenviar`
- Recibe: `solicitudId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud reenviada a revisión",
  "data": null
}
```

---

## 15. Moderaciones

### Base: `/api/moderaciones`

#### `GET /api/moderaciones/moderadores`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<UsuarioDTO>>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Moderadores obtenidos",
  "data": {
    "content": [
      { "id": 22, "nombre": "Sofía", "rol": "MODERADOR" }
    ]
  }
}
```

#### `PUT /api/moderaciones/moderadores/{moderadorId}/revocar`
- Recibe: `moderadorId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Rol de moderador revocado correctamente",
  "data": null
}
```

#### `PUT /api/moderaciones/moderadores/{moderadorId}/asignar`
- Recibe: `moderadorId` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Rol de moderador asigando correctamente",
  "data": null
}
```

#### `GET /api/moderaciones/eventos/pendientes`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos pendientes de revisión",
  "data": {
    "content": [
      {
        "id": 15,
        "titulo": "Festival de Jazz",
        "estado": "PENDIENTE"
      }
    ]
  }
}
```

#### `GET /api/moderaciones/eventos/{eventoId}/moderaciones`
- Recibe: `eventoId` en path, paginación.
- Devuelve: `ApiResponse<Page<ModeracionEventoDTO>>`
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Historial de moderación",
  "data": {
    "content": [
      {
        "id": 1,
        "eventoId": 15,
        "accion": "CORRECCION",
        "observacion": "Falta información de ubicacion"
      }
    ]
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/aprobar`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento aprobado y publicado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "PUBLICADO"
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/solicitar-correccion`
- Recibe: `eventoId` y body `ModeracionEventoRequest`.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Se solicitaron correcciones al organizador",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "CORRECCION"
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/rechazar`
- Recibe: `eventoId` y body `ModeracionEventoRequest`.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento rechazado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "RECHAZADO"
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/suspender`
- Recibe: `eventoId` y body `ModeracionEventoRequest`.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento suspendido",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "SUSPENDIDO"
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/reactivar`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento reactivado y publicado exitosamente",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "estado": "PUBLICADO"
  }
}
```

---

## 16. Promociones

### Base: `/api/promociones`

#### `GET /api/promociones/evento/{eventoId}`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<PromocionDTO>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promoción obtenida",
  "data": {
    "id": 1,
    "eventoId": 15,
    "descripcion": "Descuento del 20%",
    "descuento": 20,
    "fechaInicio": "2026-09-01",
    "fechaFin": "2026-09-30"
  }
}
```

#### `GET /api/promociones`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<PromocionDTO>>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promociones obtenidas",
  "data": {
    "content": [
      {
        "id": 1,
        "descripcion": "Descuento del 20%",
        "descuento": 20
      }
    ]
  }
}
```

#### `GET /api/promociones/mi-organizacion`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<PromocionDTO>>`
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promociones obtenidas",
  "data": {
    "content": [
      {
        "id": 1,
        "eventoId": 15,
        "descripcion": "Descuento del 20%"
      }
    ]
  }
}
```

#### `POST /api/promociones`
- Recibe: body `PromocionRequest`.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promoción creada",
  "data": null
}
```

#### `PUT /api/promociones/{id}`
- Recibe: `id` en path y body `PromocionRequest`.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promoción actualizada",
  "data": null
}
```

#### `DELETE /api/promociones/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE o ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Promoción eliminada",
  "data": null
}
```

---

## 17. Roles

### Base: `/api/roles`

#### `GET /api/roles`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<Rol>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Roles obtenidos",
  "data": {
    "content": [
      { "id": 1, "nombre": "CLIENTE" },
      { "id": 2, "nombre": "REPRESENTANTE" }
    ]
  }
}
```

#### `POST /api/roles`
- Recibe: body `RolRequest`.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Rol creado exitosamente",
  "data": null
}
```

#### `PUT /api/roles/{id}`
- Recibe: `id` en path y query param `nombre`.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Rol actualizado exitosamente",
  "data": null
}
```

#### `DELETE /api/roles/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Rol eliminado exitosamente",
  "data": null
}
```

---

## 18. Enums / motivos de rechazo

### Base: `/api/enums`

#### `GET /api/enums/motivos-rechazos`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<MotivosRechazos>>`
- Permisos: ADMINISTRADOR o MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Motivos de rechazos obtenidos",
  "data": [
    "INFORMACION_INSUFICIENTE",
    "DOCUMENTACION_INVALIDA",
    "VIOLACION_POLITICA"
  ]
}
```

---

## 19. Resumen de permisos por rol

- Público: registro/login, consulta pública de eventos y organizaciones
- Autenticado: perfil, compras, lista de deseos, notificaciones, seguimientos
- CLIENTE: compras, boletos, valoraciones
- REPRESENTANTE: gestión de eventos, organización, promociones, verificación
- OPERADOR: gestión de eventos y localidades
- MODERADOR: revisión y moderación de eventos, verificación
- ADMINISTRADOR: administración general, roles, moderadores, categorías, promociones, aprobaciones

---

## 20. Nota práctica

Si quieres, este documento puede ampliarse con:

1. Ejemplos reales de JSON de request/response por endpoint
2. Una versión Swagger/OpenAPI
3. Una tabla más estructurada por módulo
4. Colección Postman para probar cada endpoint
