# Módulo de Administración --- EventHive

## 1. Propósito

El Administrador supervisa el funcionamiento general de EventHive y
gestiona aspectos administrativos de la plataforma.

El Administrador **no reemplaza al Moderador en la revisión ordinaria de
organizaciones y eventos**. Puede intervenir administrativamente cuando
exista una razón justificada, por ejemplo, suspender una organización o
retirar/suspender un evento publicado.

## 2. Roles del sistema

EventHive utiliza estos roles:

-   **ADMINISTRADOR:** gestión global de la plataforma.
-   **MODERADOR:** revisión y moderación de organizaciones y eventos.
-   **REPRESENTANTE:** usuario responsable de una organización; gestiona
    sus eventos y la información de su organización según los permisos
    definidos.
-   **OPERADOR:** integrante de una organización que puede ejecutar
    tareas operativas delegadas por esta.
-   **CLIENTE:** usuario que consulta eventos, compra entradas, sigue
    organizaciones y utiliza las funciones disponibles para asistentes.

> La **Organización es una entidad independiente**, no un rol. Los
> usuarios Representante y Operador pueden pertenecer a una
> organización.

## 3. Responsabilidades

El Administrador puede:

-   Gestionar moderadores.
-   Administrar categorías.
-   Administrar promociones globales.
-   Consultar estadísticas generales.
-   Consultar y gestionar organizaciones.
-   Suspender organizaciones cuando corresponda.
-   Suspender o retirar eventos por motivos administrativos.
-   Consultar reportes y métricas.
-   Consultar el historial administrativo y de moderación.

El Administrador **no debe aprobar normalmente solicitudes de
organizaciones ni eventos**, porque esa responsabilidad pertenece al
flujo de Moderación.

## 4. Dashboard administrativo

El dashboard debe mostrar indicadores agregados de la plataforma:

-   Total de usuarios.
-   Total de organizaciones.
-   Organizaciones pendientes de revisión.
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
-   Promociones activas.

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
solicitud pertenecen al Módulo de Moderación.

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

## 8. Promociones

Las promociones administradas globalmente pueden incluir:

-   Nombre.
-   Descripción.
-   Tipo de beneficio.
-   Valor o porcentaje.
-   Fecha de inicio.
-   Fecha de finalización.
-   Estado.
-   Reglas de aplicación.

Las promociones deben validarse antes de aplicarse a una compra.

## 9. Estadísticas de moderación

El Administrador puede consultar estadísticas como:

-   Revisiones realizadas.
-   Aprobaciones.
-   Rechazos.
-   Solicitudes de corrección.
-   Tiempo promedio de revisión.
-   Carga por moderador.

El endpoint de estadísticas de moderación debe estar separado del
endpoint de la bandeja de trabajo.

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

-   La moderación de contenido corresponde al Moderador.
-   La administración global corresponde al Administrador.
-   Una Organización no es un rol.
-   Representante y Operador son usuarios asociados a una Organización.
-   Las acciones administrativas deben dejar trazabilidad cuando afecten
    organizaciones, eventos o usuarios.
-   La eliminación física de usuarios no debe utilizarse cuando destruya
    historial de compras, tiquetes, valoraciones o relaciones. Se
    prioriza la desactivación lógica.
