# Roadmap de Mejoras - Plataforma de Eventos

## 1. Sistema de Notificaciones
### Objetivo
Notificar a usuarios sobre cambios importantes en eventos y organizadores.

### Casos de uso
- Evento favorito actualizado.
- Nueva promoción.
- Entradas próximas a agotarse.
- Nuevo evento de un organizador seguido.

### Ejemplo técnico
```java
notificationService.send(
    usuario,
    "Nuevo evento disponible"
);
```
---

## 2. Lista de Espera para Eventos Agotados
### Objetivo
Permitir que usuarios se registren cuando un evento no tenga cupos.

### Modelo
```text
ListaEspera
- id
- usuarioId
- eventoId
- fechaRegistro
``

### Flujo
1. Evento agotado.
2. Usuario entra a lista de espera.
3. Se libera una entrada.
4. Se notifica al primer usuario de la cola.

### Ejemplo
```java
if(evento.getDisponibles() == 0){
    listaEsperaService.agregar(usuario, evento);
}
```
---

## 3. Mapa de Eventos
### Objetivo
Mostrar eventos en un mapa interactivo.

### Tecnologías sugeridas
- React Leaflet
- OpenStreetMap

### Campos necesarios
```text
latitud
longitud
```

### Ejemplo React
```jsx
<Marker position={[evento.latitud, evento.longitud]} />
```
---

## 4. Integración con Calendario
### Objetivo
Permitir agregar eventos al calendario del usuario.

### Estrategia
Generar archivos .ics.

### Flujo
1. Usuario selecciona "Agregar al calendario".
2. Backend genera archivo .ics.
3. Google Calendar u Outlook lo importan.

---

## 5. Seguir Organizadores
### Objetivo
Permitir a los usuarios seguir organizadores.

### Modelo
```text
SeguimientoOrganizador
- usuarioId
- organizadorId
```

### Ejemplo
```java
seguimientoRepository.save(seguimiento);
```

### Beneficio
Cuando el organizador publique un evento, sus seguidores recibirán una notificación.

---

## 6. Programa de Fidelización
### Objetivo
Premiar usuarios frecuentes.

### Niveles
- Bronce: 0-10 eventos
- Plata: 11-30 eventos
- Oro: 31-60 eventos
- Diamante: 60+ eventos

### Ejemplo
```java
if(totalCompras >= 60){
    usuario.setNivel(DIAMANTE);
}
```

### Beneficios
- Descuentos.
- Acceso anticipado.
- Insignias.

---

## 7. Reputación de Organizadores
### Objetivo
Evaluar organizadores en lugar de eventos.

### Métricas
- Calificación promedio.
- Eventos realizados.
- Eventos completados.
- Tiempo en plataforma.

### Ejemplo visual
```text
Organizador Verificado ✓
4.8 estrellas
58 eventos realizados
Desde 2024
```

---

## 8. Recomendación de Eventos (IA)
### Objetivo
Mostrar eventos personalizados.

### Variables
- Categorías favoritas.
- Historial de compras.
- Eventos guardados.
- Popularidad.

### Fórmula inicial
```text
40% Categorías favoritas
30% Eventos similares
20% Eventos populares
10% Eventos cercanos
```

### Futuro
Migrar a un modelo de recomendación basado en aprendizaje automático.

---

## Prioridad de Implementación

### Alta
1. Seguir organizadores
2. Notificaciones
3. Programa de fidelización

### Media
4. Lista de espera
5. Calendario (.ics)
6. Reputación de organizadores

### Baja
7. Mapa interactivo
8. Recomendación con IA
