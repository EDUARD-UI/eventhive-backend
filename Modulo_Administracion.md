# Módulo de Administración --- EventHive

## 1. Propósito

El Administrador supervisa el funcionamiento general de EventHive y
gestiona aspectos administrativos de la plataforma.

El Administrador revisa las solicitudes de verificación de
organizaciones cuando el Representante carga su RUT. No revisa ni aprueba
ordinariamente eventos; esa revisión humana corresponde al Moderador
solo para los eventos que el sistema de reglas deriva a su bandeja.

También puede intervenir administrativamente cuando exista una razón
justificada, por ejemplo, suspender una organización o retirar/suspender
un evento publicado.

## 2. Roles del sistema

EventHive utiliza estos roles:

-   **ADMINISTRADOR:** gestión global de la plataforma y revisión de
    solicitudes de verificación de organizaciones tras la carga del RUT.
-   **MODERADOR:** revisión humana únicamente de los eventos que el
    sistema de reglas deriva a moderación.
-   **REPRESENTANTE:** usuario responsable de una organización; gestiona
    sus eventos y la información de su organización según los permisos
    definidos.
-   **OPERADOR:** integrante de una organización que puede ejecutar
    tareas operativas delegadas por esta.
-   **MARKETING:** gestiona los planes de promoción pagada, las campañas
    asociadas a eventos y los banners del home.
-   **CLIENTE:** usuario que consulta eventos, compra entradas, sigue
    organizaciones y utiliza las funciones disponibles para asistentes.

> La **Organización es una entidad independiente**, no un rol. Los
> usuarios Representante y Operador pueden pertenecer a una
> organización.

## 3. Responsabilidades

El Administrador puede:

-   Gestionar moderadores.
-   Administrar categorías.
-   Gestionar invitaciones de moderadores y usuarios de Marketing.
-   Consultar estadísticas generales.
-   Consultar y gestionar organizaciones.
-   Aprobar, rechazar o solicitar correcciones en solicitudes de
    verificación de organizaciones que ya tienen el RUT cargado.
-   Suspender organizaciones cuando corresponda.
-   Suspender o retirar eventos por motivos administrativos.
-   Consultar reportes y métricas.
-   Consultar el historial administrativo y de moderación.

El Administrador no aprueba ni modera eventos dentro del flujo ordinario.
La verificación de organizaciones es una responsabilidad administrativa
específica y se inicia cuando se envía el RUT; no equivale a administrar
el resto de los datos comerciales de la organización.

## 4. Dashboard administrativo

El dashboard debe mostrar indicadores agregados de la plataforma:

-   Total de usuarios.
-   Total de organizaciones.
-   Solicitudes de verificación pendientes (RUT enviado).
-   Organizaciones aprobadas.
-   Organizaciones suspendidas.
-   Eventos pendientes de revisión.
-   Eventos en corrección.
-   Eventos publicados.
-   Eventos finalizados.
-   Eventos cancelados.
-   Eventos suspendidos.
-   Tickets vendidos.
-   Ventas totales.
-   Eventos con promoción pagada.

Los indicadores deben obtenerse mediante consultas agregadas y no
mediante la carga completa de las entidades.

## 5. Gestión de organizaciones

El Administrador puede consultar:

-   Nombre o razón social.
-   NIT.
-   Estado.
-   Representante.
-   Fecha de registro.
-   Cantidad de eventos.
-   Valoración promedio.
-   Cantidad de valoraciones.

Acciones administrativas:

-   Ver perfil.
-   Consultar historial.
-   Suspender organización.
-   Reactivar organización cuando corresponda.

La aprobación inicial, solicitud de correcciones y rechazo de la
verificación pertenecen al Administrador y solo aplican después de que
el Representante haya enviado el RUT. El registro inicial puede quedar
como pre-registro sin documento y no entra todavía a la cola de revisión.

La información administrativa sensible, como el RUT, no debe aparecer en
el perfil público. El documento debe mantenerse en almacenamiento
privado y solo estar disponible para usuarios autorizados.

## 6. Gestión de moderadores

Permite:

-   Consultar moderadores.
-   Activar o desactivar su acceso según las reglas de seguridad.
-   Consultar carga de trabajo.
-   Consultar estadísticas de moderación.

No se debe permitir que un Administrador cambie arbitrariamente el rol
de cualquier usuario sin una regla explícita de autorización.

## 7. Categorías

El Administrador administra las categorías disponibles para los eventos:

-   Crear.
-   Consultar.
-   Editar.
-   Activar/desactivar.
-   Eliminar únicamente cuando no existan referencias que rompan la
    integridad del sistema.

Si una categoría ya tiene eventos asociados, es preferible desactivarla
en lugar de eliminarla físicamente.

Las imágenes de categorías pueden almacenarse en un bucket público
separado de los documentos privados.

## 8. Marketing y promoción pagada

El rol MARKETING administra una tabla de planes con precio, comisión por
venta, condición Premium, pauta en redes y disponibilidad. El plan Impulso
Digital inicia en $500.000 y el plan Sold Out Absoluto en $1.000.000.
Administración invita a los roles MODERADOR y MARKETING; un Representante
invita OPERADORES.

Una promoción pagada destaca el evento, pero solamente los eventos con un
pago confirmado del plan Premium aparecen en la lista del carrusel. El
carrusel no rota ni selecciona eventos al azar. La comisión pactada se
conserva al contratar el plan para que un cambio posterior de tarifa no
recalcule ventas previas.

Los descuentos sobre ventas de boletos no forman parte del flujo de
promoción y no reducen el precio durante el checkout.

## 9. Estadísticas de moderación

El Administrador puede consultar estadísticas como:

-   Revisiones realizadas.
-   Aprobaciones.
-   Rechazos.
-   Solicitudes de corrección.
-   Tiempo promedio de revisión.
-   Carga por moderador.

Las estadísticas de moderación de eventos deben estar separadas de la
bandeja del Moderador. La consulta de verificaciones de organizaciones
pertenece al flujo administrativo y no debe contarse como carga de
moderación de eventos.

## 10. Métricas comerciales

El sistema puede mostrar:

-   Ventas del período.
-   Tickets vendidos.
-   Eventos con mayores ventas.
-   Organizaciones con mayores ventas.
-   Ingresos generados por la plataforma.

Debe definirse por separado qué significa **venta**, **ingreso bruto**,
**comisión de EventHive** e **ingreso de la organización**,
especialmente porque inicialmente la pasarela será simulada.

## 11. Información que el frontend debe considerar

Las vistas administrativas deben distinguir entre datos públicos, datos
administrativos y datos sensibles.

Los botones deben depender del estado real devuelto por el backend. Por
ejemplo, una organización suspendida no debería mostrar acciones de
aprobación como si estuviera pendiente.

Cuando una acción no esté permitida, el frontend puede ocultar el botón,
pero el backend siempre debe volver a comprobar la autorización.

Las respuestas de error deben tratarse mediante códigos HTTP:

-   `401`: usuario no autenticado.
-   `403`: usuario autenticado pero sin permiso.
-   `404`: recurso no encontrado.
-   `409`: conflicto con el estado actual.
-   `400/422`: datos inválidos, según la convención utilizada.

## 12. Reglas de consistencia

-   La revisión del RUT y la verificación inicial de organizaciones
    corresponden al Administrador.
-   La moderación humana de eventos corresponde al Moderador únicamente
    cuando las reglas del sistema derivan el evento a revisión.
-   La administración global corresponde al Administrador.
-   Una Organización no es un rol.
-   Representante y Operador son usuarios asociados a una Organización.
-   Las acciones administrativas deben dejar trazabilidad cuando afecten
    organizaciones, eventos o usuarios.
-   La eliminación física de usuarios no debe utilizarse cuando destruya
    historial de compras, tiquetes, valoraciones o relaciones. Se
    prioriza la desactivación lógica.
