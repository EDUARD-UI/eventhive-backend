# Refactorización de Eventos y Fidelización

## Objetivo

Actualizar la capa Repository, Service y Controller para reflejar los cambios recientes del modelo de datos:

1. Eliminar completamente la entidad `Estado`.
2. Utilizar el enum `EstadoEvento`.
3. Adaptar consultas, filtros y validaciones al nuevo enum.
4. Implementar la lógica de fidelización mediante `NivelUsuario`.
5. Implementar el sistema de acceso anticipado basado en el nivel del usuario.
6. Mantener buenas prácticas de Spring Boot, JPA y arquitectura por capas.

---

# Estado actual

La entidad `Evento` ya no utiliza:

```java
@ManyToOne
@JoinColumn(name = "estado_id")
private Estado estado;
```

Ahora utiliza:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EstadoEvento estado;
```

El enum es:

```java
public enum EstadoEvento {
    BORRADOR,
    PUBLICADO,
    CANCELADO,
    FINALIZADO
}
```

---

# Cambios requeridos en Repository

## Eliminar consultas relacionadas con Estado

Eliminar cualquier método como:

```java
findByEstadoId(...)
findByEstadoNombre(...)
findByEstado(...)
```

que dependan de la entidad Estado.

---

## Crear consultas usando EstadoEvento

Ejemplos:

```java
List<Evento> findByEstado(EstadoEvento estado);
```

```java
List<Evento> findByEstadoOrderByFechaAsc(
    EstadoEvento estado
);
```

```java
List<Evento> findByCategoriaIdAndEstado(
    Long categoriaId,
    EstadoEvento estado
);
```

```java
List<Evento> findByOrganizadorIdOrderByFechaCreacionDesc(
    String organizadorId
);
```

---

# Cambios requeridos en Service

## Actualizar creación de eventos

Al crear un evento:

```java
evento.setEstado(
    EstadoEvento.BORRADOR
);
```

No debe buscarse ningún registro en una tabla Estado.

Eliminar cualquier uso de:

```java
estadoRepository
```

---

## Publicar evento

Crear lógica:

```java
evento.setEstado(
    EstadoEvento.PUBLICADO
);
```

---

## Cancelar evento

```java
evento.setEstado(
    EstadoEvento.CANCELADO
);
```

---

## Finalizar evento

```java
evento.setEstado(
    EstadoEvento.FINALIZADO
);
```

---

## Filtrar eventos públicos

Todas las consultas visibles para clientes deben utilizar:

```java
EstadoEvento.PUBLICADO
```

---

# Sistema de Fidelización

## Modelo

Usuario contiene:

```java
private Integer cantidadCompras;

private NivelUsuario nivel;
```

Enum:

```java
public enum NivelUsuario {

    BRONCE,
    PLATA,
    ORO,
    DIAMANTE

}
```

---

# Actualización automática de nivel

Cuando una compra cambia a estado CONFIRMADA:

```java
usuario.setCantidadCompras(
    usuario.getCantidadCompras() + 1
);
```

Luego recalcular:

```java
private NivelUsuario calcularNivel(
    Integer compras
)
```

Reglas:

```text
0 - 4 compras      -> BRONCE
5 - 14 compras     -> PLATA
15 - 29 compras    -> ORO
30 o más compras   -> DIAMANTE
```

Ejemplo:

```java
usuario.setNivel(
    calcularNivel(
        usuario.getCantidadCompras()
    )
);
```

Persistir usuario actualizado.

---

# Acceso Anticipado

## Regla de negocio

BRONCE:

```text
0 horas
```

PLATA:

```text
12 horas
```

ORO:

```text
24 horas
```

DIAMANTE:

```text
48 horas
```

---

# Método auxiliar

Crear servicio:

```java
long obtenerHorasAnticipacion(
    NivelUsuario nivel
)
```

Retornar:

```java
DIAMANTE -> 48
ORO      -> 24
PLATA    -> 12
BRONCE   -> 0
```

---

# Visibilidad de eventos

La entidad Evento posee:

```java
private LocalDateTime fechaPublicacion;
```

Calcular:

```java
fechaVisible =
fechaPublicacion.minusHours(
    horasAnticipacion
);
```

Si:

```java
LocalDateTime.now()
             .isAfter(fechaVisible)
```

entonces el usuario puede visualizar el evento.

---

# Listado de eventos para clientes

Antes de devolver eventos:

1. Obtener eventos PUBLICADOS.
2. Aplicar validación de acceso anticipado.
3. Retornar únicamente eventos visibles para el nivel del usuario.

---

# Listado para organizadores

Ordenar por:

```java
fechaCreacion DESC
```

para mostrar primero los eventos más recientemente creados.

Ejemplo:

```java
findByOrganizadorIdOrderByFechaCreacionDesc(...)
```

---

# Controllers

Actualizar todos los endpoints para:

* Recibir y devolver EstadoEvento.
* Eliminar dependencias de EstadoRepository.
* No consultar tablas de estados.
* Utilizar directamente el enum EstadoEvento.

---

# Resultado esperado

* Eliminación completa de la entidad Estado.
* Eliminación completa de EstadoRepository.
* Eliminación completa de servicios relacionados con Estado.
* Uso exclusivo de EstadoEvento.
* Sistema de fidelización basado en NivelUsuario.
* Actualización automática de niveles.
* Sistema de acceso anticipado funcional.
* Eventos ordenados por fechaCreacion para organizadores.
* Código alineado con Spring Boot, JPA y buenas prácticas.
