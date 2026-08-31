Organizacion
⭐⭐⭐⭐⭐ 4.8 (328 valoraciones)

Seguidores
12.430

Eventos realizados
187

Tiempo en la plataforma
3 años

## reglas de valoracion
Solo usuarios autenticados pueden valorar.
Solo una valoración por organizacion (si vuelve a valorar, se actualiza la anterior).
Calificación de 1 a 5 estrellas.
Comentario opcional.
No puede valorarse a sí mismo (si es el representante o trabajador de la organizacion).
Se actualizan los datos agregados de la organizacion (ratingPromedio, totalValoraciones, etc.).

## flujo perfil de Organizaciones
Implementación: Para optimizar el rendimiento de la plataforma, las métricas de reputación y actividad de la organizacion
se almacenarán directamente en la entidad de organizacion.
Cada vez que ocurra una acción relevante, como recibir una nueva valoración,
obtener un nuevo seguidor, crear un evento o realizar una compra,
los valores agregados se actualizarán automáticamente. De esta manera, al consultar el perfil de una organizacion,
la aplicación solo deberá leer la información ya almacenada, evitando cálculos repetitivos y mejorando los tiempos de respuesta

## Registro de organizaciones
El Formulario de Registro (Vista de la organizacion)Para no aburrir al usuario pero capturar lo necesario para validar,
soliciten exactamente estos 5 campos en una interfaz limpia:

Razón Social: El nombre legal de la empresa (Ej: Eventos Universitarios S.A.S.).
CorreoEmpresarial: correo el cual se usa como contacto pricipal para la empresa.
NIT (Número de Identificación Tributaria): El número de identificación de la empresa.
Nombre del Representante Legal: La persona responsable detrás de la organización.
Carga de Documento (RUT): Un campo de tipo file para que suban el PDF o una foto legible del RUT.

El Panel de Control (Vista del Administrador/Moderador)Esta es la pantalla que van a lucir en la feria.
Cuando el administrador abra una solicitud pendiente, la interfaz debe mostrar un diseño (Datos del Aspirante):

Muestra el Nombre, NIT, Representante y un botón grande que dice [📄 Ver RUT Adjunto]
(abre el archivo en una ventana flotante) Botones de Acción:

En la parte inferior, los dos botones decisivos: [Aprobar Empresa ✅] y [Rechazar Solicitud ❌].