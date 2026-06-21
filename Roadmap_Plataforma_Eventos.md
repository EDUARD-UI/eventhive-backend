# Roadmap de Mejoras - Plataforma de Eventos

## 1. Sistema de Notificaciones

* **Tecnología:** MongoDB Atlas.
* **Implementación:** Se gestionará completamente en una colección NoSQL. Cuando ocurra una acción relevante (nuevo evento, actualización o recordatorio), el backend identificará a los usuarios afectados y generará documentos JSON ligeros en MongoDB. Se utilizará un índice **TTL** para eliminar automáticamente las notificaciones después de 7 días, evitando crecimiento innecesario del almacenamiento y manteniendo el consumo dentro de los límites del plan gratuito.

---

## 2. Seguir Organizadores

* **Tecnología:** PostgreSQL (Supabase).
* **Implementación:** Se utilizará una relación muchos a muchos mediante una tabla intermedia que almacenará únicamente los identificadores de usuario y organizador. Esta estructura garantiza integridad referencial, consultas eficientes y un consumo mínimo de recursos incluso cuando la cantidad de seguidores crezca significativamente.

---

## 3. Mapa de Eventos

* **Tecnología:** PostgreSQL + PostGIS.
* **Implementación:** Las ubicaciones de los eventos se almacenarán utilizando tipos geográficos nativos de PostGIS. Esto permitirá realizar búsquedas por proximidad, calcular distancias y mostrar eventos cercanos al usuario mediante consultas optimizadas directamente desde la base de datos sin necesidad de servicios externos adicionales.

---

## 4. Integración con Calendario

* **Tecnología:** Backend (Java).
* **Implementación:** El backend generará dinámicamente archivos `.ics` utilizando la información existente de cada evento. El usuario podrá importar estos archivos en Google Calendar, Outlook u otras aplicaciones compatibles sin requerir integraciones complejas ni almacenamiento adicional.

---

## 5. Programa de Fidelización
 
* **Tecnología:** PostgreSQL.
* **Implementación:** En lugar de recalcular constantemente el historial completo de compras, el sistema mantendrá campos acumulados dentro del perfil del usuario, como total de compras, puntos acumulados y nivel actual. Estos valores se actualizarán automáticamente después de cada compra confirmada, reduciendo la carga de consultas y mejorando el rendimiento.

---

## 6. Reputación de Organizadores

* **Tecnología:** PostgreSQL.
* **Implementación:** Las valoraciones continuarán almacenándose en la base de datos relacional. Para optimizar el rendimiento, cada organizador mantendrá métricas agregadas como calificación promedio y cantidad total de reseñas, actualizadas al registrarse una nueva valoración. Esto evitará cálculos repetitivos en cada consulta.

---

<!-- POR PENSAR (Funcionalidades en evaluación para futuras iteraciones) -->

<!--
## 7. Lista de Espera para Eventos Agotados

* Estado: En evaluación.

* Posible implementación:
Se estudia manejar una lista de espera ordenada por fecha de registro para permitir que usuarios ocupen automáticamente cupos liberados por cancelaciones o vencimientos de reservas.

* Pendiente definir:
- PostgreSQL o MongoDB.
- Estrategia de asignación automática de cupos.
- Gestión de notificaciones cuando un espacio quede disponible.
-->

<!--
## 8. Recomendación de Eventos (IA)

* Estado: En evaluación.

* Posible implementación:
Se analiza generar recomendaciones mediante procesos programados (cron jobs) que calculen afinidades según categorías visitadas, eventos asistidos y organizadores seguidos.

* Pendiente definir:
- Algoritmo de recomendación.
- Frecuencia de actualización.
- Estructura de almacenamiento de recomendaciones precalculadas.
-->

---
📝 1. El Formulario de Registro (Vista del Organizador)Para no aburrir al usuario pero capturar lo necesario para validar, soliciten exactamente estos 4 campos en una interfaz limpia:Razón Social: El nombre legal de la empresa (Ej: Eventos Universitarios S.A.S.).NIT (Número de Identificación Tributaria): El número de identificación de la empresa.Nombre del Representante Legal: La persona responsable detrás de la organización.Carga de Documento (RUT): Un campo de tipo file para que suban el PDF o una foto legible del RUT.🖥️ 2. El Panel de Control (Vista del Administrador /admin)Esta es la pantalla que van a lucir en la feria. Cuando el administrador abra una solicitud pendiente, la interfaz debe mostrar un diseño (Datos del Aspirante): Muestra el Nombre, NIT, Representante y un botón grande que dice [📄 Ver RUT Adjunto] (abre el archivo en una ventana flotante) Botones de Acción: En la parte inferior, los dos botones decisivos: [Aprobar Empresa ✅] y [Rechazar Solicitud ❌].

## Objetivo de Arquitectura

La plataforma utilizará PostgreSQL (Supabase) como fuente principal de datos relacionales y MongoDB Atlas para información efímera o de alta frecuencia de escritura. El backend en Java centralizará toda la lógica de negocio, integraciones y procesos programados, manteniendo una arquitectura simple, escalable y compatible con los límites de los planes gratuitos durante las primeras etapas del proyecto.
