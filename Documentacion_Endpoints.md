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

> Importante: `data` puede contener un objeto simple, un array o un objeto paginado. Si su valor es `null` (por ejemplo, en respuestas `Void` y errores), `ApiResponse` lo omite del JSON. Algunos ejemplos `Void` muestran `data: null` solo como representación conceptual.

La mayoría de respuestas paginadas usan `PagedResponse<T>`:

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

Algunos endpoints devuelven directamente `Page<T>` de Spring Data; su estructura paginada puede incluir metadatos adicionales propios de Spring.

En caso de error, la respuesta suele devolver:

```json
{
  "success": false,
  "mensaje": "Descripción del error"
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
    "telefono": "+56912345678",
    "imagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg",
    "urlImagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg",
    "rolNombre": "CLIENTE"
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
    "id": 7,
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJzdWIiOiJjYXJsb3NAY...",
    "tipo": "Bearer",
    "correo": "carlos@email.com",
    "rol": "CLIENTE"
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
    "id": 7,
    "accessToken": "newAccessToken123",
    "refreshToken": "refreshToken456",
    "tipo": "Bearer",
    "correo": "carlos@email.com",
    "rol": "CLIENTE"
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

#### `POST /api/auth/registro-organizador`
- Recibe: body JSON `SolicitudVerificacionRequest` con `nombreCompleto`, `correoUsuario`, `password`, `razonSocial`, `nit` y `correoEmpresarial`.
- Devuelve: `ApiResponse<Void>`
- Descripción: crea la cuenta del representante y la organización en pre-registro. El alta no exige el RUT; el representante puede completar este paso después. Al cargarlo mediante `/api/verificacion/{solicitudId}/subir-rut`, la solicitud queda disponible para revisión del Administrador.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Usuario y pre-registro de organización creados con éxito. Ya puedes ingresar al Dashboard.",
  "data": null
}
```

---

## 3. Usuarios

### Base: `/api/usuarios`

#### `GET /api/usuarios`
- Recibe: query params `page`, `size`, `sort` (paginación). Requiere rol ADMINISTRADOR.
- Devuelve: `ApiResponse<PagedResponse<UsuarioAdminDTO>>`; incluye teléfono, nombre del rol y resumen de organización, no correo.
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
        "telefono": "+56912345678",
        "rolNombre": "CLIENTE",
        "organizacion": null
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
- Devuelve: `ApiResponse<PagedResponse<UsuarioAdminDTO>>`
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
        "telefono": "+56987654321",
        "rolNombre": "OPERADOR",
        "organizacion": {
          "id": 10,
          "razonSocial": "Eventica SpA"
        }
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
- Devuelve: `ApiResponse<UsuarioAdminDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Usuario obtenido",
  "data": {
    "id": 3,
    "nombre": "María",
    "telefono": "+56911112222",
    "rolNombre": "REPRESENTANTE",
    "organizacion": {
      "id": 10,
      "razonSocial": "Eventica SpA"
    }
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
        "razonSocial": "Eventica SpA",
        "representante": "Carlos Pérez",
        "fechaCreacion": "2026-09-01T09:00:00",
        "promedioRating": 4.8,
        "totalValoraciones": 25,
        "totalSeguidores": 340,
        "totalEventosCreados": 18,
        "nivel": "NIVEL_2",
        "estado": "APROBADA"
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
    "imagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg",
    "urlImagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg",
    "rolNombre": "CLIENTE"
  }
}
```

#### `GET /api/usuarios/perfil/actividad`
- Recibe: no body.
- Devuelve: `ApiResponse<UsuarioActividadDTO>` con compras confirmadas, entradas y eventos comprados, favoritos y total gastado.
- Permisos: autenticado (disponible para todos los roles).
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Actividad obtenida",
  "data": {
    "comprasConfirmadas": 4,
    "entradasCompradas": 7,
    "eventosComprados": 3,
    "favoritos": 5,
    "totalGastado": 175000
  }
}
```

#### `PUT /api/usuarios/perfil`
- Recibe: body `ActualizarPerfilRequest`.
- Content-Type: `application/json`.
- Devuelve: `ApiResponse<UsuarioDTO>`
- Permisos: autenticado.
- Este endpoint actualiza los datos de texto del perfil. Para reemplazar la imagen, usar `PUT /api/usuarios/perfil/imagen`.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Perfil actualizado",
  "data": {
    "id": 7,
    "nombre": "Carlos Vega",
    "correo": "carlos@email.com",
    "imagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg",
    "urlImagenPerfil": "https://storage.example/imagenPerfil/user_123.jpg"
  }
}
```

#### `PUT /api/usuarios/perfil/imagen`
- Recibe: `multipart/form-data` con la parte `imagen` obligatoria (archivo PNG o JPG; máximo 5 MB).
- Devuelve: `ApiResponse<UsuarioDTO>` con HTTP 200; incluye la URL actualizada en `imagenPerfil` y `urlImagenPerfil`.
- Permisos: autenticado.
- Descripción: reemplaza únicamente la imagen de perfil del usuario autenticado y la almacena en Supabase Storage.
- Ejemplo de respuesta: misma estructura de `UsuarioDTO` que en `GET /api/usuarios/perfil`.

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
- Recibe: `categoriaId` y `fecha` opcionales, y paginación (`page`, `size`, `sort`). La fecha debe enviarse en formato `yyyy-MM-dd` y filtra por ese día exacto.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`
- Descripción: lista eventos públicos; permite combinar los filtros opcionales de categoría y fecha. El filtro de fecha se evalúa como fecha de calendario sin conversiones de zona horaria, evitando devolver eventos del día anterior. Los promocionados aparecen primero y el orden secundario predeterminado es por fecha, hora e ID ascendente.
- Ejemplos: `/api/eventos?fecha=2026-10-14` o `/api/eventos?categoriaId=1&fecha=2026-10-14`
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
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PUBLICADO",
        "promocionado": true,
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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

#### `GET /api/eventos/destacados`
- Recibe: paginación opcional (`page`, `size`, `sort`); `size` es 10 por defecto.
- Devuelve: `ApiResponse<PagedResponse<EventoDestacadoDTO>>`.
- Descripción: lista únicamente eventos `PUBLICADO` con `promocionado: true`, ordenados por fecha, hora e ID ascendente.
- Cada evento contiene los mismos campos de `EventoDTO` y `urlImagenDestacado`. La URL destacada solo se incluye en esta ruta; los otros endpoints de eventos no la devuelven.

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
        "descripcion": "Encuentro de arte, diseño y creatividad",
        "lugar": "Centro Cultural, Santiago",
        "foto": "https://storage.example/eventos/expo-creativa.jpg",
        "fecha": "2026-10-20",
        "hora": "10:00:00",
        "estado": "PUBLICADO",
        "latitud": -33.4372,
        "longitud": -70.6506,
        "categoria": {
          "id": 3,
          "nombre": "Arte"
        },
        "organizacion": {
          "id": 21,
          "nombre": "Colectivo Creativo"
        }
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
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "PUBLICADO",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    },
    "localidades": [
      {
        "id": 1,
        "nombre": "General",
        "precio": 25000,
        "capacidad": 150,
        "disponibles": 120
      },
      {
        "id": 2,
        "nombre": "VIP",
        "precio": 60000,
        "capacidad": 40,
        "disponibles": 35
      }
    ]
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "BORRADOR",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    },
    "localidades": [
      {
        "id": 1,
        "nombre": "General",
        "precio": 25000,
        "capacidad": 150,
        "disponibles": 120
      }
    ]
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
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PUBLICADO",
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "PENDIENTE_REVISION",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    },
    "localidades": [
      {
        "id": 1,
        "nombre": "General",
        "precio": 25000,
        "capacidad": 150,
        "disponibles": 120
      }
    ]
  }
}
```

#### `GET /api/eventos/mapa`
- Recibe: `categoriaId`, `lat`, `lng`, `radioKm` opcionales.
- Devuelve: `ApiResponse<List<EventoMapaDTO>>`
- Descripción: eventos para renderizar en mapa; los promocionados aparecen primero y luego se ordenan por fecha, hora e ID ascendente.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Eventos para el mapa",
  "data": [
    {
      "id": 15,
      "titulo": "Festival de Jazz",
      "descripcion": "Evento musical con artistas locales",
      "categoriaNombre": "Música",
      "latitud": -33.4489,
      "longitud": -70.6693
    }
  ]
}
```

#### `GET /api/eventos/buscar`
- Recibe: `nombre` o `titulo` para buscar por coincidencia parcial y paginación (`page`, `size`, `sort`). Debe enviarse `nombre` o `titulo`.
- Devuelve: `ApiResponse<PagedResponse<EventoDTO>>`, con los mismos campos de tarjeta que el listado público.
- Descripción: búsqueda pública de eventos `PUBLICADO` por coincidencia parcial en el título. Los promocionados aparecen primero; el orden predeterminado secundario es por fecha, hora e ID ascendente. Para filtrar por categoría o fecha se utiliza `GET /api/eventos`.
- Ejemplo: `/api/eventos/buscar?titulo=jazz`
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
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PUBLICADO",
        "promocionado": true,
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
        "titulo": "Festival de Jazz",
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PUBLICADO",
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PENDIENTE_REVISION",
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "BORRADOR",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    },
    "localidades": []
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "BORRADOR",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    },
    "localidades": [
      {
        "id": 1,
        "nombre": "General",
        "precio": 25000,
        "capacidad": 150,
        "disponibles": 120
      }
    ]
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

#### `PATCH /api/eventos/{id}/enviar-revision`
- Recibe: `id` del evento en path, sin body.
- Devuelve: `ApiResponse<Void>`
- Descripción: envía el evento a revisión.
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento enviado a revisión",
  "data": null
}
```

#### `PATCH /api/eventos/{id}/reabrir`
- Recibe: `id` del evento en path, sin body.
- Devuelve: `ApiResponse<Void>`
- Descripción: reabre el evento y lo devuelve a borrador.
- Permisos: REPRESENTANTE o OPERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento reabierto a borrador",
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
      "capacidad": 150,
      "disponibles": 120
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
    "capacidad": 40,
    "disponibles": 40
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
    "capacidad": 35,
    "disponibles": 35
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
        "fechaCompra": "2026-09-20T14:30:00",
        "total": 50000,
        "metodoPago": "TARJETA",
        "items": [
          {
            "eventoId": 20,
            "localidadId": 1,
            "localidadNombre": "General",
            "eventoNombre": "Festival de Jazz",
            "cantidad": 2,
            "precioUnitario": 25000,
            "subtotal": 50000
          }
        ]
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
    "fechaCompra": "2026-09-20T14:30:00",
    "total": 50000,
    "metodoPago": "TARJETA",
    "items": [
      {
        "localidadId": 1,
        "localidadNombre": "General",
        "eventoNombre": "Festival de Jazz",
        "cantidad": 2,
        "precioUnitario": 25000,
        "subtotal": 50000
      }
    ]
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
    "fechaCompra": "2026-09-21T10:15:00",
    "total": 25000,
    "metodoPago": "TARJETA",
    "items": [
      {
        "eventoId": 20,
        "localidadId": 2,
        "localidadNombre": "General",
        "eventoNombre": "Expo Creativa",
        "cantidad": 1,
        "precioUnitario": 25000,
        "subtotal": 25000
      }
    ]
  }
}
```

#### `POST /api/compras/eventos/{eventoId}/posicionamiento/pagos`
- Recibe: `eventoId` en path y `planId` como query param.
- Devuelve: `ApiResponse<PagoPosicionamientoDTO>`.
- Permisos: REPRESENTANTE de la organización propietaria.
- Descripción: registra un pago simulado `PENDIENTE` con el precio del plan seleccionado; el cliente no envía el precio.

#### `GET /api/compras/posicionamiento/pagos/{pagoId}`
- Recibe: `pagoId` en path.
- Devuelve: `ApiResponse<PagoPosicionamientoDTO>` con `id`, `eventoId`, `organizacionId`, `precio` y `estado`.
- Permisos: REPRESENTANTE de la organización propietaria.

#### `PATCH /api/compras/posicionamiento/pagos/{pagoId}/confirmar-simulado`
- Recibe: `pagoId` en path.
- Devuelve: `ApiResponse<PagoPosicionamientoDTO>`.
- Permisos: REPRESENTANTE de la organización propietaria.
- Descripción: al confirmar el pago simulado el evento queda destacado y se aplica la comisión congelada al contratar. Si el plan es Premium, entra al carrusel.

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
    "id": 101,
    "fechaCompra": "2026-09-20T14:30:00",
    "total": 50000,
    "metodoPago": "TARJETA",
    "tiqueteCompras": [
      {
        "id": 501,
        "tiquete": {
          "id": 501,
          "codigoQR": "BOL-001-ABC",
          "localidad": {
            "id": 1,
            "nombre": "General",
            "precio": 25000,
            "evento": {
              "id": 15,
              "titulo": "Festival de Jazz",
              "fecha": "2026-10-14",
              "hora": "19:30:00",
              "lugar": "Parque Central, Santiago"
            }
          }
        }
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
        "descripcion": "Concierto al aire libre",
        "capacidad": 35,
        "disponibles": 35
        "foto": "https://storage.example/eventos/concierto-nocturno.jpg",
        "fecha": "2026-11-12",
        "hora": "20:00:00",
        "estado": "PUBLICADO",
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
- `totalEventos` cuenta todos los eventos asociados a la categoría, sin filtrar por estado.
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
        "imagenUrl": "https://.../musica.jpg",
        "totalEventos": 45
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
- Devuelve: `ApiResponse<List<CategoriaNombreDTO>>`
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
    { "id": 1, "nombre": "Música", "imagenUrl": "https://.../musica.jpg", "totalEventos": 45 },
    { "id": 3, "nombre": "Arte", "imagenUrl": "https://.../arte.jpg", "totalEventos": 21 }
  ]
}
```

#### `GET /api/categorias/con-eventos`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<List<CategoriaConteoDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categorías con eventos",
  "data": [
    {
      "id": 1,
      "nombre": "Música",
      "totalEventos": 45
    }
  ]
}
```

#### `GET /api/categorias/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<CategoriaConteoDTO>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Categoría obtenida",
  "data": {
    "id": 1,
    "nombre": "Música",
    "totalEventos": 45
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
- Recibe: parámetro opcional `estado` y paginación (`page`, `size`, `sort`).
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
        "razonSocial": "Eventica SpA",
        "representante": "Carlos Pérez",
        "fechaCreacion": "2026-09-01T09:00:00",
        "promedioRating": 4.8,
        "totalValoraciones": 25,
        "totalSeguidores": 340,
        "totalEventosCreados": 18,
        "nivel": "NIVEL_2",
        "estado": "APROBADA"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### `POST /api/organizaciones/buscar`
- Recibe: body JSON `BuscarOrganizacionRequest` con `razonSocial`; parámetro opcional `estado` y parámetros de paginación (`page`, `size`, `sort`) en la query.
- Devuelve: `ApiResponse<PagedResponse<OrganizacionPublicaDTO>>`
- Descripción: busca organizaciones por razón social.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organizaciones encontradas",
  "data": {
    "content": [
      {
        "id": 10,
        "razonSocial": "Eventica SpA",
        "representante": "Carlos Pérez",
        "fechaCreacion": "2026-09-01T09:00:00",
        "promedioRating": 4.8,
        "totalValoraciones": 25,
        "totalSeguidores": 340,
        "totalEventosCreados": 18,
        "nivel": "NIVEL_2",
        "estado": "APROBADA"
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
    "representante": "Carlos Pérez",
    "razonSocial": "Eventica SpA",
    "nit": "76.123.456-7",
    "correoContacto": "contacto@eventica.cl",
    "urlLogo": "https://storage.example/perfilOrganizacion/org_123.jpg",
    "fechaCreacion": "2026-09-01T09:00:00",
    "promedioRating": 4.8,
    "totalValoraciones": 25,
    "totalSeguidores": 340,
    "totalEventosCreados": 18,
    "eventosFinalizados": 12,
    "eventosRechazados": 1,
    "nivel": "NIVEL_2",
    "estado": "APROBADA"
  }
}
```

#### `GET /api/organizaciones/{organizacionId}/rut-url`
- Recibe: `organizacionId` en path.
- Devuelve: `ApiResponse<RutUrlDTO>` con `url` temporal y `expiresInSeconds`.
- Descripción: obtiene un enlace temporal para consultar el documento RUT de la organización.
- Permisos: autenticado.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "URL temporal del RUT obtenida",
  "data": {
    "url": "https://storage.example/rut-temporal",
    "expiresInSeconds": 300
  }
}
```

#### `GET /api/organizaciones/top`
- Recibe: parámetro opcional `estado` y paginación (`page`, `size`, `sort`).
- Devuelve: `ApiResponse<PagedResponse<OrganizacionPublicaDTO>>`
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Top de organizaciones",
  "data": {
    "content": [
      {
        "id": 10,
        "razonSocial": "Eventica SpA",
        "representante": "Carlos Pérez",
        "fechaCreacion": "2026-09-01T09:00:00",
        "promedioRating": 4.8,
        "totalValoraciones": 25,
        "totalSeguidores": 340,
        "totalEventosCreados": 18,
        "nivel": "NIVEL_2",
        "estado": "APROBADA"
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
    "representante": "Carlos Pérez",
    "razonSocial": "Eventica SpA",
    "nit": "76.123.456-7",
    "correoContacto": "contacto@eventica.cl",
    "urlLogo": "https://storage.example/perfilOrganizacion/org_123.jpg",
    "fechaCreacion": "2026-09-01T09:00:00",
    "promedioRating": 4.8,
    "totalValoraciones": 25,
    "totalSeguidores": 340,
    "totalEventosCreados": 18,
    "eventosFinalizados": 12,
    "eventosRechazados": 1,
    "nivel": "NIVEL_2",
    "estado": "APROBADA"
  }
}
```

#### `PUT /api/organizaciones/mi-organizacion`
- Recibe: `multipart/form-data` con la parte `datos` obligatoria (JSON `ActualizarOrganizacionRequest`: `razonSocial`, `descripcion`, `correoContacto`) y la parte `imagen` opcional (archivo PNG o JPG; máximo 5 MB).
- Devuelve: `ApiResponse<OrganizacionDTO>` con HTTP 200, incluyendo `urlLogo` si existe.
- Permisos: REPRESENTANTE.
- Descripción: actualiza los datos de la organización del representante autenticado y, si se envía `imagen`, reemplaza su logo en Supabase Storage. Si no se envía imagen, conserva el logo actual.

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
        "correoInvitado": "carlos@email.com",
        "organizacionNombre": "Eventica SpA",
        "invitadoPorNombre": "Ana Pérez",
        "estado": "PENDIENTE",
        "fechaInvitacion": "2026-09-26T10:30:00",
        "fechaRespuesta": null
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
        "correoInvitado": "usuario@correo.com",
        "organizacionNombre": "Eventica SpA",
        "invitadoPorNombre": "Carlos Pérez",
        "estado": "ENVIADA",
        "fechaInvitacion": "2026-09-26T10:30:00",
        "fechaRespuesta": null
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
        "nombreCompleto": "Luis González",
        "correo": "luis@correo.com",
        "permisosEvento": ["CREAR_EVENTO", "EDITAR_EVENTO"]
      }
    ]
  }
}
```

Los operadores se agregan mediante invitación por correo, aceptan o rechazan la invitación y pueden ser expulsados por el representante. No existe un estado organizacional de operador; `usuarios.activo` se reserva para el control de acceso al sistema.

#### `GET /api/organizaciones/mi-organizacion/estadisticas`
- Recibe: paginación estándar (`page`, `size`, `sort`).
- Devuelve: `ApiResponse<PanelEntradasDTO>` con total de eventos, boletas, ingresos netos y eventos paginados con sus ventas.
- Permisos: REPRESENTANTE u OPERADOR asociado a la organización.
- Los ingresos son el neto para la organización después de la comisión de Eventhive.

#### `GET /api/organizaciones/mi-organizacion/valoraciones`
- Recibe: paginación estándar (`page`, `size`, `sort`).
- Devuelve: `ApiResponse<PagedResponse<ValoracionDTO>>`.
- Permisos: REPRESENTANTE u OPERADOR asociado a la organización. La organización se determina desde el usuario autenticado.

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
      {
        "id": 7,
        "nombre": "Carlos",
        "correo": "carlos@email.com",
        "telefono": "+56912345678",
        "rolNombre": "CLIENTE"
      }
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
- Recibe: query param `correo`; envía una invitación nueva para el rol OPERADOR.
- Devuelve: `ApiResponse<Void>`
- Permisos: REPRESENTANTE.
- El destinatario acepta desde el enlace de correo, que vence en 24 horas; puede completar una cuenta nueva o actualizar una cuenta CLIENTE existente.
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
- Ruta heredada para invitaciones anteriores; las invitaciones nuevas se aceptan con `POST /api/invitaciones/roles/aceptar`.
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
        "organizacionId": 10,
        "organizacionNombre": "Eventica SpA",
        "nivelActual": "BRONCE",
        "nivelSugerido": "PLATA",
        "estado": "PENDIENTE",
        "fechaGeneracion": "2026-09-26T10:30:00",
        "fechaResolucion": null
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
        "organizacionId": 10,
        "organizacionNombre": "Eventica SpA",
        "clienteId": 7,
        "clienteNombre": "Carlos"
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
        "organizacionId": 10,
        "organizacionNombre": "Eventica SpA",
        "clienteId": 8,
        "clienteNombre": "Ana"
      }
    ]
  }
}
```

Para el panel autenticado se recomienda `GET /api/organizaciones/mi-organizacion/valoraciones`, que aplica el mismo paginado sin recibir un ID de organización desde el frontend.

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
      "tipoNotificacion": "NUEVO_EVENTO",
      "organizacionId": 10,
      "eventoId": 15,
      "nombreEvento": "Festival de Jazz",
      "leida": false,
      "fechaCreacion": "2026-09-26T10:30:00"
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
    "organizacionId": 10,
    "representanteId": 7,
    "representanteNombre": "Carlos Pérez",
    "representanteCorreo": "carlos@email.com",
    "razonSocial": "Eventica SpA",
    "nit": "76.123.456-7",
    "representanteLegal": 7,
    "correoEmpresarial": "contacto@eventica.cl",
    "mensaje": null,
    "estado": "PENDIENTE",
    "fechaSolicitud": "2026-09-26T10:30:00",
    "fechaResolucion": null,
    "administradorNombre": null,
    "motivoRechazo": null
  }
}
```

#### `GET /api/verificacion/pendientes`
- Recibe: paginación.
- Devuelve: `ApiResponse<Page<SolicitudVerificacionDTO>>`
- Descripción: lista las solicitudes con RUT enviado que esperan verificación administrativa.
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitudes obtenidas",
  "data": {
    "content": [
      {
        "id": 5,
        "organizacionId": 10,
        "representanteId": 7,
        "representanteNombre": "Carlos Pérez",
        "representanteCorreo": "carlos@email.com",
        "razonSocial": "Eventica SpA",
        "nit": "76.123.456-7",
        "representanteLegal": 7,
        "correoEmpresarial": "contacto@eventica.cl",
        "mensaje": "Documento recibido",
        "estado": "PENDIENTE",
        "fechaSolicitud": "2026-09-26T10:30:00",
        "fechaResolucion": null,
        "administradorNombre": null,
        "motivoRechazo": null
      }
    ]
  }
}
```

#### `GET /api/verificacion/{solicitudId}`
- Recibe: `solicitudId` en path.
- Devuelve: `ApiResponse<SolicitudVerificacionDTO>`
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud obtenida",
  "data": {
    "id": 5,
    "organizacionId": 10,
    "representanteId": 7,
    "representanteNombre": "Carlos Pérez",
    "representanteCorreo": "carlos@email.com",
    "razonSocial": "Eventica SpA",
    "nit": "76.123.456-7",
    "representanteLegal": 7,
    "correoEmpresarial": "contacto@eventica.cl",
    "mensaje": "Documento recibido",
    "estado": "PENDIENTE",
    "fechaSolicitud": "2026-09-26T10:30:00",
    "fechaResolucion": null,
    "administradorNombre": null,
    "motivoRechazo": null
  }
}
```

#### `POST /api/verificacion/registro-organizador`
- Recibe: body JSON `SolicitudVerificacionRequest` con `nombreCompleto`, `correoUsuario`, `password`, `razonSocial`, `nit` y `correoEmpresarial`.
- Devuelve: `ApiResponse<Void>`; crea la cuenta y la organización en pre-registro sin exigir el RUT en el alta. El representante puede cargar el documento después. Este flujo también está disponible en `POST /api/auth/registro-organizador`.
- Permisos: público.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Cuenta y organización creadas. Completa tu RUT para publicar eventos.",
  "data": null
}
```

#### `PATCH /api/verificacion/{solicitudId}/subir-rut`
- Recibe: `solicitudId` en path y archivo obligatorio `rut` como multipart/form-data.
- Devuelve: `ApiResponse<Void>`
- Descripción: carga el RUT de la organización y envía la solicitud a la cola de verificación del Administrador. El registro inicial puede existir sin este documento.
- Permisos: REPRESENTANTE.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "RUT cargado. Tu solicitud entró a cola de revisión.",
  "data": null
}
```

#### `PUT /api/verificacion/{solicitudId}/aprobar`
- Recibe: `solicitudId` en path.
- Devuelve: `ApiResponse<String>` (el endpoint actualmente deja `data` en `null`).
- Descripción: aprueba la verificación de la organización después de revisar los datos y el RUT.
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Organización verificada.",
  "data": null
}
```

#### `PUT /api/verificacion/{solicitudId}/rechazar`
- Recibe: `solicitudId` en path y parámetro de query `motivo`.
- Devuelve: `ApiResponse<Void>`
- Descripción: rechaza la solicitud de verificación de la organización.
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud rechazada",
  "data": null
}
```

#### `PATCH /api/verificacion/{solicitudId}/solicitar-correccion`
- Recibe: `solicitudId` en path y parámetro de query `motivo`.
- Devuelve: `ApiResponse<SolicitudVerificacionDTO>` (el endpoint actualmente deja `data` en `null`).
- Descripción: solicita al Representante corregir la solicitud de verificación; luego podrá actualizar los datos y reenviarla.
- Permisos: ADMINISTRADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Solicitud marcada para corrección",
  "data": null
}
```

#### `PATCH /api/verificacion/{solicitudId}/reenviar`
- Recibe: `solicitudId` en path y multipart/form-data con `datos` (`SolicitudVerificacionRequest`) obligatorio y `rut` opcional.
- Devuelve: `ApiResponse<Void>`
- Descripción: permite al Representante reenviar los datos corregidos y, opcionalmente, reemplazar el RUT.
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

#### `GET /api/moderaciones/estadisticas`
- Recibe: sin parámetros.
- Devuelve: `ApiResponse<ModeracionEstadisticasDTO>` con `revisados`, `aprobados`, `rechazados` y `correccionesSolicitadas`.
- Permisos: MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Estadísticas de moderación obtenidas",
  "data": {
    "revisados": 24,
    "aprobados": 18,
    "rechazados": 2,
    "correccionesSolicitadas": 4
  }
}
```

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
      {
        "id": 22,
        "nombre": "Sofía",
        "correo": "sofia@eventhive.com",
        "telefono": "+56912345678",
        "rolNombre": "MODERADOR"
      }
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
- Descripción: lista exclusivamente los eventos que las reglas del sistema derivaron a revisión humana; no representa todos los eventos enviados para publicación.
- Permisos: MODERADOR.
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
        "descripcion": "Evento musical con artistas locales",
        "lugar": "Parque Central, Santiago",
        "foto": "https://storage.example/eventos/festival-jazz.jpg",
        "fecha": "2026-10-14",
        "hora": "19:30:00",
        "estado": "PENDIENTE_REVISION",
        "latitud": -33.4489,
        "longitud": -70.6693,
        "categoria": {
          "id": 1,
          "nombre": "Música"
        },
        "organizacion": {
          "id": 20,
          "nombre": "Eventica"
        }
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
        "accion": "CORRECCION_SOLICITADA",
        "moderadorId": 22,
        "moderadorNombre": "Sofía",
        "eventoId": 15,
        "eventoNombre": "Festival de Jazz",
        "estadoResultante": "EN_CORRECCION",
        "motivo": "INFORMACION_INSUFICIENTE",
        "observacion": "Falta información de ubicación",
        "fecha": "2026-09-26T10:30:00"
      }
    ]
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/aprobar`
- Recibe: `eventoId` en path.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento aprobado y publicado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "PUBLICADO",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    }
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/solicitar-correccion`
- Recibe: `eventoId` y body `ModeracionEventoRequest`.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Se solicitaron correcciones al organizador",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "EN_CORRECCION",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    }
  }
}
```

#### `PATCH /api/moderaciones/eventos/{eventoId}/rechazar`
- Recibe: `eventoId` y body `ModeracionEventoRequest`.
- Devuelve: `ApiResponse<EventoDTO>`
- Permisos: MODERADOR.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Evento rechazado",
  "data": {
    "id": 15,
    "titulo": "Festival de Jazz",
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "RECHAZADO",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    }
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "SUSPENDIDO",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    }
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
    "descripcion": "Evento musical con artistas locales",
    "lugar": "Parque Central, Santiago",
    "foto": "https://storage.example/eventos/festival-jazz.jpg",
    "fecha": "2026-10-14",
    "hora": "19:30:00",
    "estado": "PUBLICADO",
    "latitud": -33.4489,
    "longitud": -70.6693,
    "categoria": {
      "id": 1,
      "nombre": "Música"
    },
    "organizacion": {
      "id": 20,
      "nombre": "Eventica"
    }
  }
}
```

---

## 16. Promoción pagada de eventos

La promoción de eventos es distinta a ofrecer descuentos en boletos. Los
descuentos de compra ya no están disponibles ni se aplican al checkout.

#### `GET /api/promociones/planes`
- Público. Devuelve los planes activos con precio, comisión, bandera `premium` y `pautaRedes`.
- También incluye `detallePublicidad`. Impulso Digital inicia en $500.000/10% (anuncios pagados en redes por 1-2 semanas); Sold Out Absoluto inicia en $1.000.000/15% (Premium, carrusel, pauta de alto presupuesto y publicaciones fijas).

#### `POST /api/compras/eventos/{eventoId}/posicionamiento/pagos?planId={id}`
- Permisos: REPRESENTANTE del evento.
- Registra una intención de pago simulada usando el precio vigente del plan.

#### `PATCH /api/compras/posicionamiento/pagos/{pagoId}/confirmar-simulado`
- Permisos: REPRESENTANTE propietario.
- Confirma el pago simulado, destaca el evento y conserva la comisión contratada.
- En producción debe reemplazarse la confirmación simulada por una confirmación de pasarela.

#### `GET /api/eventos/destacados`
- Devuelve únicamente eventos publicados con pago confirmado cuyo plan comprado era Premium.
- La lista está ordenada por fecha del evento; no hay selección aleatoria ni rotación.

#### `POST /api/promociones/eventos/{eventoId}/posicionar`
- Body: `{ "urlImagenDestacado": "https://..." }`.
- Permisos: REPRESENTANTE propietario con pago confirmado.
- Asigna o actualiza la imagen del evento destacado; no activa ni cancela el pago.

### Dashboard MARKETING

- `GET /api/marketing/planes`: lista todos los planes.
- `PUT /api/marketing/planes/{id}`: actualiza nombre, precio, comisión porcentual, Premium, pauta en redes y estado activo.
- `GET /api/marketing/eventos-promocionados`: lista eventos/organizaciones con pago confirmado, plan y características contratadas.
- `GET /api/banners-home/admin` y las operaciones de escritura de `/api/banners-home` permiten ADMINISTRADOR y MARKETING.

La aplicación usa `spring.jpa.hibernate.ddl-auto=none`. Antes de desplegar
esta versión, aplicar una vez
[`V1__marketing_invitations_and_paid_promotions.sql`](./demo/src/main/resources/db/manual/V1__marketing_invitations_and_paid_promotions.sql).
Configurar el envío por Brevo SMTP mediante las variables `MAIL_HOST`,
`MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` y
`eventhive.frontend-url` (URL base de la web para aceptar invitaciones).

---

## 17. Roles

### Base: `/api/roles`

#### `GET /api/roles`
- Recibe: paginación.
- Devuelve: `ApiResponse<PagedResponse<RolDTO>>`
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

### Invitaciones de roles por enlace temporal

Una Administración autenticada puede invitar a MODERADOR o MARKETING; un
REPRESENTANTE solo puede invitar a OPERADOR. Se puede invitar un correo
sin cuenta o uno que ya tenga una cuenta CLIENTE. El enlace de Brevo es
de un solo uso y vence 24 horas después del envío.

#### `POST /api/invitaciones/roles`
- Body: `{ "correo": "persona@correo.com", "rol": "MARKETING" }`.
- Permisos: ADMINISTRADOR o REPRESENTANTE. La autorización limita qué roles puede asignar cada remitente.

#### `GET /api/invitaciones/roles/validar?token={token}`
- Público; devuelve el correo, rol y fecha de expiración para mostrar el formulario.

#### `POST /api/invitaciones/roles/aceptar`
- Público. Body: `{ "token": "...", "nombre": "Nombre", "telefono": "+573001112233", "clave": "contraseña segura" }`.
- Crea la cuenta nueva o actualiza la cuenta CLIENTE existente con el rol invitado.

#### `GET /api/invitaciones/roles/enviadas`
- ADMINISTRADOR o REPRESENTANTE. Devuelve la lista paginada de invitaciones enviadas por la persona autenticada.

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

## 19. Estadísticas de administración

### Base: `/api/admin/estadisticas`

Todos los endpoints son de solo lectura, no reciben parámetros y requieren rol ADMINISTRADOR.

#### `GET /api/admin/estadisticas/organizaciones-por-validacion`
- Devuelve: `ApiResponse<OrganizacionesPorValidacionDTO>` con conteos de organizaciones según el estado de validación del RUT.
- Ejemplo de `data`:

```json
{
  "pendientesValidacionRut": 3,
  "sinRut": 2,
  "total": 12,
  "porEstado": [
    { "estado": "APROBADA", "cantidad": 7 },
    { "estado": "PENDIENTE_REVISION", "cantidad": 3 },
    { "estado": "SUSPENDIDA", "cantidad": 2 }
  ]
}
```

#### `GET /api/admin/estadisticas/eventos-por-estado`
- Devuelve: `ApiResponse<List<EventoEstadoConteoDTO>>`.
- Ejemplo de `data`:

```json
[
  { "estado": "PUBLICADO", "cantidad": 18 },
  { "estado": "PENDIENTE_REVISION", "cantidad": 4 }
]
```

#### `GET /api/admin/estadisticas/eventos-por-categoria`
- Devuelve: `ApiResponse<List<EventosPorCategoriaDTO>>`.
- Ejemplo de `data`:

```json
[
  { "categoriaId": 1, "nombre": "Música", "cantidadEventos": 12 },
  { "categoriaId": 3, "nombre": "Arte", "cantidadEventos": 6 }
]
```

#### `GET /api/admin/estadisticas/top-eventos-ventas`
- Devuelve: `ApiResponse<List<TopEventoVentasDTO>>` con los cinco eventos con más ventas.
- Ejemplo de `data`:

```json
[
  {
    "eventoId": 15,
    "nombre": "Festival de Jazz",
    "organizacionId": 10,
    "organizacion": "Eventica SpA",
    "entradasVendidas": 240,
    "totalVentas": 6000000
  }
]
```

#### `GET /api/admin/estadisticas/top-organizaciones-ventas`
- Devuelve: `ApiResponse<List<TopOrganizacionVentasDTO>>` con las cinco organizaciones con más ventas.
- Ejemplo de `data`:

```json
[
  {
    "organizacionId": 10,
    "razonSocial": "Eventica SpA",
    "entradasVendidas": 480,
    "totalVentas": 12000000
  }
]
```

---

## 20. Palabras prohibidas

### Base: `/api/administracion/palabras-prohibidas`

Todos los endpoints de este módulo requieren rol ADMINISTRADOR. `PalabraProhibidaRequest` recibe `palabra` (texto de hasta 100 caracteres) y `severidad` (`GRAVE` o `SOSPECHOSA`, enum `SeveridadPalabra`).

#### `GET /api/administracion/palabras-prohibidas`
- Recibe: parámetros de paginación (`page`, `size`, `sort`).
- Devuelve: `ApiResponse<PagedResponse<PalabraProhibidaDTO>>`; cada elemento contiene `id`, `palabra`, `severidad`, `activa` y `fechaCreacion`.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Palabras prohibidas obtenidas",
  "data": {
    "content": [
      {
        "id": 1,
        "palabra": "ejemplo",
        "severidad": "GRAVE",
        "activa": true,
        "fechaCreacion": "2026-09-26T10:30:00"
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

#### `POST /api/administracion/palabras-prohibidas`
- Recibe: body JSON `PalabraProhibidaRequest` con `palabra` y `severidad`.
- Devuelve: `ApiResponse<PalabraProhibidaDTO>`.
- Ejemplo de respuesta:

```json
{
  "success": true,
  "mensaje": "Palabra creada",
  "data": {
    "id": 1,
    "palabra": "ejemplo",
    "severidad": "GRAVE",
    "activa": true,
    "fechaCreacion": "2026-09-26T10:30:00"
  }
}
```

#### `PUT /api/administracion/palabras-prohibidas/{id}`
- Recibe: `id` en path y body JSON `PalabraProhibidaRequest` con `palabra` y `severidad`.
- Devuelve: `ApiResponse<PalabraProhibidaDTO>`.
- Ejemplo de respuesta: `data` contiene el DTO actualizado con `id`, `palabra`, `severidad`, `activa` y `fechaCreacion`.

#### `PATCH /api/administracion/palabras-prohibidas/{id}/activa`
- Recibe: `id` en path y parámetro de query booleano `activa`.
- Devuelve: `ApiResponse<Void>`.
- Ejemplo de respuesta: `{"success":true,"mensaje":"Palabra desactivada","data":null}`. Si `activa=true`, el mensaje es `Palabra activada`.

#### `DELETE /api/administracion/palabras-prohibidas/{id}`
- Recibe: `id` en path.
- Devuelve: `ApiResponse<Void>`.
- Ejemplo de respuesta: `{"success":true,"mensaje":"Palabra eliminada","data":null}`.

---

## 21. Banners del Home

### Base: `/api/banners-home`

Este módulo administra los dos espacios promocionales propios de Eventhive. No está relacionado con organizaciones, eventos ni con el módulo `/api/promociones`. Las únicas posiciones válidas son `1` y `2`.

#### `GET /api/banners-home`
- Público.
- Devuelve: `ApiResponse<List<BannerHomeDTO>>` con los banners configurados, ordenados por posición. Los espacios vacíos no se incluyen.
- Cada `BannerHomeDTO` contiene `id`, `titulo`, `imagenUrl`, `textoBoton`, `enlaceUrl` y `posicion`.
- Respuesta: HTTP 200.

```json
{
  "success": true,
  "mensaje": "Banners del Home obtenidos",
  "data": [
    {
      "id": 1,
      "titulo": "Promociona tu evento en Eventhive",
      "imagenUrl": "https://storage.example/banners-home/banner-1",
      "textoBoton": "Conocer más",
      "enlaceUrl": "https://eventhive.example/promociones",
      "posicion": 1
    }
  ]
}
```

#### `GET /api/banners-home/admin`
- Requiere rol `ADMINISTRADOR`.
- Devuelve: `ApiResponse<List<BannerHomeDTO>>` con exactamente los dos espacios administrativos (posiciones 1 y 2). Si un espacio no tiene banner, sus campos son `null`, excepto `posicion`.
- Respuesta: HTTP 200.

#### `POST /api/banners-home`
- Requiere rol `ADMINISTRADOR`.
- Content-Type: `multipart/form-data`.
- Parámetros: `posicion` (int: 1 o 2), `titulo` (string), `textoBoton` (string), `enlaceUrl` (string), `imagen` (archivo obligatorio).
- Devuelve: `ApiResponse<BannerHomeDTO>` con HTTP 201 Created.
- Validaciones: posición válida, la posición debe estar vacía, los campos de texto no pueden estar vacíos y la imagen es obligatoria. No se puede crear un tercer espacio ni duplicar una posición.
- Imagen: se carga a Supabase Storage (PNG, JPG o WebP, máximo 5 MB); la respuesta contiene `imagenUrl`, no el archivo.

#### `POST /api/banners-home/{posicion}`
- Requiere rol `ADMINISTRADOR`.
- Content-Type: `multipart/form-data`.
- Parámetros: `posicion` en el path (int: 1 o 2), `titulo`, `textoBoton`, `enlaceUrl` e `imagen` (archivo obligatorio).
- Devuelve: `ApiResponse<BannerHomeDTO>` con HTTP 201 Created.
- Aplica las mismas validaciones de creación; si la posición ya existe, devuelve error 400.

#### `PUT /api/banners-home/{posicion}`
- Requiere rol `ADMINISTRADOR`.
- Content-Type: `multipart/form-data`.
- Parámetros: `posicion` en el path (int: 1 o 2), `titulo`, `textoBoton`, `enlaceUrl` e `imagen` (archivo opcional; si se omite, se conserva la imagen actual).
- Devuelve: `ApiResponse<BannerHomeDTO>` con HTTP 200.
- Validaciones: los campos de texto no pueden estar vacíos y la posición debe tener un banner creado; si no existe, devuelve error 404.

#### `DELETE /api/banners-home/{posicion}`
- Requiere rol `ADMINISTRADOR`.
- Parámetro: `posicion` en el path (int: 1 o 2).
- Elimina el registro de la posición indicada y libera ese espacio. Si no existe, devuelve error 404.
- Devuelve `ApiResponse<Void>` con HTTP 200 y el mensaje `Banner eliminado exitosamente`.

#### Respuestas de error
- Posición distinta de `1` o `2`, campos de texto vacíos, creación sobre una posición ocupada o imagen faltante al crear: HTTP 400.
- Intentar actualizar o eliminar una posición sin banner: HTTP 404.
- Usuario sin rol `ADMINISTRADOR`: HTTP 403.

---

## 22. Resumen de permisos por rol

- Público: registro/login, consulta pública de eventos y organizaciones, banners del home
- Autenticado: perfil, compras, lista de deseos, notificaciones, seguimientos
- CLIENTE: compras, boletos, valoraciones
- REPRESENTANTE: gestión de eventos, organización, contratación de promoción pagada y verificación
- OPERADOR: gestión de eventos y localidades
- MODERADOR: revisión y moderación de eventos
- MARKETING: planes y comisiones de promoción, campañas de eventos pagados y banners del home
- ADMINISTRADOR: administración general, invitaciones de moderadores/marketing, categorías, palabras prohibidas y verificación de organizaciones

---

## 23. Nota práctica

Si quieres, este documento puede ampliarse con:

1. Ejemplos reales de JSON de request/response por endpoint
2. Una versión Swagger/OpenAPI
3. Una tabla más estructurada por módulo
4. Colección Postman para probar cada endpoint
