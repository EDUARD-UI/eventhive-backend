# Roadmap de Mejoras - Plataforma de Eventos

## 1. Sistema de Notificaciones✅

* **Tecnología:** MongoDB Atlas.
* **Implementación:** Se gestionará completamente en una colección NoSQL. Cuando ocurra una acción relevante (nuevo evento, actualización o recordatorio), el backend identificará a los usuarios afectados y generará documentos JSON ligeros en MongoDB. Se utilizará un índice **TTL** para eliminar automáticamente las notificaciones después de 7 días, evitando crecimiento innecesario del almacenamiento y manteniendo el consumo dentro de los límites del plan gratuito.

---

## 2. Seguir Organizaciones✅

* **Tecnología:** PostgreSQL (Supabase).
* **Implementación:** Se utilizará una relación muchos a muchos mediante una tabla intermedia que almacenará únicamente los identificadores de usuario y organizador. Esta estructura garantiza integridad referencial, consultas eficientes y un consumo mínimo de recursos incluso cuando la cantidad de seguidores crezca significativamente.


## 3. Integracion de paserela de pago(simulada)

* **Tecnología:** Backend (Java).
* **Implementación:** Se hara la implementacion de una pasarela de pagos para mas fiabilidad y uso de los estados del enum al proceso de los pagos con la app.

## 4. Mapa de Eventos✅

* **Tecnología:** PostgreSQL + PostGIS.
* **Implementación:** Las ubicaciones de los eventos se almacenarán utilizando tipos geográficos nativos de PostGIS. Esto permitirá realizar búsquedas por proximidad, calcular distancias y mostrar eventos cercanos al usuario mediante consultas optimizadas directamente desde la base de datos sin necesidad de servicios externos adicionales.



## 5. Integración con Calendario

* **Tecnología:** Backend (Java).
* **Implementación:** El backend generará dinámicamente archivos `.ics` utilizando la información existente de cada evento. El usuario podrá importar estos archivos en Google Calendar, Outlook u otras aplicaciones compatibles sin requerir integraciones complejas ni almacenamiento adicional.

---

## 6. Reputación de Organizaciones✅

* **Tecnología:** PostgreSQL.
* **Implementación:** Las valoraciones continuarán almacenándose en la base de datos relacional. Para optimizar el rendimiento, cada organizador mantendrá métricas agregadas como calificación promedio y cantidad total de reseñas, actualizadas al registrarse una nueva valoración. Esto evitará cálculos repetitivos en cada consulta.

---

<!--
## 7. Lista de Espera para Eventos Agotados

* Posible implementación:
Se estudia manejar una lista de espera ordenada por fecha de registro para permitir que usuarios ocupen automáticamente cupos liberados por cancelaciones o vencimientos de reservas.

* Pendiente definir:
- PostgreSQL o MongoDB.
- Estrategia de asignación automática de cupos.
- Gestión de notificaciones cuando un espacio quede disponible.
-->

<!--

## .8 Recomendación de Eventos (IA)

* Posible implementación:
Se analiza generar recomendaciones mediante procesos programados (cron jobs) que calculen afinidades según categorías visitadas, eventos asistidos y organizadores seguidos.

* Pendiente definir:
- Algoritmo de recomendación.
- Frecuencia de actualización.
- Estructura de almacenamiento de recomendaciones precalculadas.
-->

---