## Flujo completo (de 0 a ticket vendido)
1. Registro CLIENTE

Cualquiera se registra con WhatsApp o email. Rol por defecto: CLIENTE. Puede comprar, no puede publicar.
2. Solicitud "Quiero organizar"

Desde su perfil toca "Activar modo organizador".
Formulario ligero (3 minutos):
Tipo: persona natural / bar / hotel / café / empresa
Nombre comercial
Cédula o NIT (solo número)
Dirección exacta en Cartagena + pin
WhatsApp + Instagram/Facebook del local
1 foto fachada
Acepta: "soy responsable de la veracidad"
Estado: SOLICITUD_PENDIENTE
3. Validación inicial (ADMINISTRADOR, dashboard)

El admin ve cola, abre en 1 click: valida mapa, Instagram, que no sea spam.
Aprueba → usuario pasa a rol ORGANIZACION nivel 1.
Rechaza → motivo (ej: "dirección no existe").
Tiempo objetivo: <24h. No pide RUT todavía.
4. Creación de evento (ORGANIZACION)

Formulario en 4 pasos:
Datos básicos
Lugar y aforo
¿Música en vivo? ¿Venta alcohol? ¿Espacio público?
Sistema clasifica riesgo:
Bajo (<200 personas, local privado): no pide permiso
Medio/Alto (>200, concierto, espacio público): muestra alerta roja y campo obligatorio "Radicado SIGOB o código PULEP". La norma de Cartagena exige 15 días hábiles de anticipación, 30 días para conciertos, y para >200 personas pide visto bueno DADIS. 
Firma declaración y envía.
Estado: PENDIENTE_REVISION
5. Moderación (MODERADOR, dashboard)

Ve cola con semáforo: verde (bajo riesgo), amarillo, rojo.
Revisa: ¿es evento real? ¿fotos ok? ¿categoría correcta? ¿si era rojo, adjuntó permiso?
Acciones: Aprobar → PUBLICADO / Pedir corrección → EN_CORRECCION (con comentario) / Rechazar → RECHAZADO
6. Publicación y venta (CLIENTE)

Evento visible, compra de tickets.
7. Post-evento

Automático a FINALIZADO 24h después. Se registra: asistentes, quejas, reembolsos.




## ¿Quién decide el ascenso?
Nivel 1 → 2 (automático con supervisión)

La app monitorea en BD:
≥5 eventos PUBLICADOS y FINALIZADOS
Tasa aprobación >90%
0 eventos rechazados por "falso" o "sin permiso" en últimos 60 días
Antigüedad >30 días
Cuando cumple, la app crea una tarea en dashboard: "ED Bar Centro cumple para Nivel 2".
El ADMINISTRADOR solo da click "Aprobar". No revisa papeles, solo valida comportamiento. Si quiere, en ese momento pide subir RUT (opcional pero recomendado).
Nivel 2 → 3 (manual con documentos)

La app propone cuando:
≥20 eventos finalizados
95% aprobación

Ventas >$10M acumuladas o 3 meses sin incidencias
El ADMINISTRADOR abre el perfil y pide desde dashboard: RUT, certificado Sayco/Acinpro, paz y salvo EPA si aplica. El organizador los sube en su panel.
Admin valida y aprueba. Aquí sí es manual porque ya estás dando "llave maestra".
¿Y si baja?

La app también propone downgrade automático: 2 rechazos por spam en 30 días → baja a nivel 1 y vuelve a moderación 100%. El MODERADOR lo ejecuta desde dashboard.

En el dashboard del ADMINISTRADOR verías una tarjeta así:
"La Santísima (bar) - 6 eventos, 100% aprobados, 45 días. ¿Subir a Verificado? [Ver perfil] "[Aprobar][Rechazar]