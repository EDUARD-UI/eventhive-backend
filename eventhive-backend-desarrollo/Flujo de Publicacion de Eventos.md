# Flujo de Publicación de Eventos - EventHive

## Objetivo

Garantizar que únicamente organizaciones verificadas puedan publicar eventos de calidad dentro de la plataforma, manteniendo un proceso sencillo para pequeños organizadores, bares, hoteles, restaurantes y empresas de la ciudad de Cartagena de Indias.

El objetivo principal es facilitar la creación de eventos, manteniendo un control de calidad mediante un proceso de moderación y un sistema de confianza para las organizaciones.

---

# Flujo completo (de 0 a ticket vendido)

## 1. Registro de Usuario

Cualquier persona puede registrarse mediante correo electrónico o WhatsApp.

Rol inicial:

- CLIENTE

Desde este momento puede:

- Comprar entradas.
- Seguir organizaciones.
- Valorar eventos.
- Acumular puntos de fidelización.

No puede publicar eventos.

---

# 2. Solicitud para convertirse en Organización

Desde su perfil el usuario selecciona:

> "Quiero publicar eventos"

Se inicia una solicitud sencilla.

Información solicitada:

- Tipo de organización.
    - Persona Natural
    - Persona juridica
- Nombre comercial.
- Número de identificación (Cédula o NIT).
- Dirección.
- Ciudad.
- Redes sociales (opcional).
- numero de contacto
- Declaración de responsabilidad.

Estado inicial:

```text
SOLICITUD_PENDIENTE
```

---

# 3. Validación Inicial

Un Administrador revisa la solicitud.

El objetivo es validar que realmente exista una organización o persona que realizará eventos dentro de la ciudad.

Puede:

- Aprobar.
- Rechazar indicando el motivo.

Si es aprobada:

- El usuario obtiene el rol ORGANIZACION.
- Se asigna Nivel 1.
- Puede acceder al panel de creación de eventos.

---

# Sistema de Niveles

Los niveles representan la confianza que EventHive tiene sobre la organización.

Ejemplo:

- Nivel 1
- Nivel 2
- Nivel 3

Cada nivel define principalmente:

- Cantidad máxima de eventos activos.
- Nivel de supervisión.
- Beneficios futuros.

Ejemplo:

Nivel 1

- 3 eventos activos.
- Todos los eventos pasan por moderación.

Nivel 2

- 5 eventos activos.
- Algunos eventos podrán publicarse automáticamente según el estado de confianza.

Nivel 3

- Mayor cantidad de eventos.
- Flujo de publicación más ágil.

---

# Ascenso de Nivel

La plataforma monitorea automáticamente:

- Eventos finalizados.
- Eventos rechazados.
- Tiempo dentro de la plataforma.

Cuando una organización cumple los requisitos, el sistema genera una sugerencia para que el Administrador apruebe el ascenso.

---

# Creación del Evento

La organización puede crear un evento siempre que no haya superado el límite de eventos activos permitido para su nivel.

El evento inicia como:

```text
BORRADOR
```

La creación se realiza mediante un Wizard.

---

# Sistema de Localidades

Todo evento deberá tener al menos una localidad.

## Evento sin localidades personalizadas

Si la organización no crea localidades manualmente, el sistema generará automáticamente una localidad llamada:

```text
General
```

Ejemplo:

General

- Precio: $50.000
- Capacidad: 300 personas

De esta forma todos los eventos utilizan exactamente el mismo flujo de venta.

---

## Evento con localidades

Si el organizador desea vender diferentes tipos de entradas podrá crear múltiples localidades.

Ejemplo:

- General
- VIP
- Preferencial
- Palcos

Cada localidad administrará de forma independiente:

- Nombre.
- Precio.
- Capacidad.

---

## Validaciones automáticas

Antes de enviar el evento el sistema validará:

- Debe existir al menos una localidad.
- La suma de capacidades de todas las localidades no puede superar el aforo del evento.
- No pueden existir localidades con capacidad cero.
- Todas las localidades deben tener precio válido.
- No pueden existir nombres repetidos.

Estas validaciones son realizadas automáticamente por el sistema y no requieren intervención del Moderador.

---

# Permisos Legales

Algunos eventos pueden requerir permisos legales.

Durante la creación existirá un apartado opcional donde la organización podrá adjuntar:

- Código PULEP.
- Permisos municipales.
- Otros documentos.

Si no son necesarios simplemente se dejan vacíos.

Posteriormente el Moderador determinará si el evento requería dichos permisos.

---

# Declaración de Responsabilidad

Antes de enviar el evento, la organización deberá aceptar la siguiente declaración:

> Declaro que la información suministrada es verídica y que cuento con los permisos necesarios para la realización del evento cuando estos sean exigidos por la normativa vigente.

---

# Envío del Evento

Al finalizar el Wizard el evento pasa a uno de los siguientes estados.

## Organizaciones nuevas

```text
PENDIENTE_REVISION
```

Todos los eventos deberán ser revisados por un Moderador.

---

## Organizaciones con confianza

Dependiendo del nivel del organizador, el evento podrá:

- Publicarse automáticamente.
- Pasar nuevamente por revisión.

Esta lógica podrá evolucionar en futuras versiones sin modificar el flujo de creación.

---

# Moderación

El Moderador revisa únicamente aspectos relacionados con el contenido y el cumplimiento de las políticas de EventHive.

Verifica:

- Que realmente sea un evento.
- Información clara.
- Imágenes apropiadas.
- Categoría correcta.
- Lugar válido.
- Que no sea spam.
- Que los permisos legales hayan sido adjuntados cuando correspondan.

Puede realizar tres acciones.

## Aprobar

Estado:

```text
PUBLICADO
```

---

## Solicitar Correcciones

Estado:

```text
EN_CORRECCION
```

El Moderador seleccionará uno o varios motivos predefinidos.

Ejemplos:

- Información incompleta.
- Imagen no permitida.
- Falta permiso legal.
- Categoría incorrecta.
- Incumple políticas.

---

## Rechazar

Estado:

```text
RECHAZADO
```

También deberá indicar el motivo correspondiente.

---

# Publicación

Una vez aprobado el evento:

- Se publica.
- Se habilita la venta de entradas.
- Se generan los tickets.
- Se habilitan las compras.

---

# Finalización

Después del evento.

Estado:

```text
FINALIZADO
```

---

# Estados del Evento

- BORRADOR
- PENDIENTE_REVISION
- EN_CORRECCION
- PUBLICADO
- FINALIZADO
- RECHAZADO
- CANCELADO

---

# Roles

## Cliente

- Compra entradas.
- Sigue organizaciones.
- Valora eventos.

---

## Organización

- Crea eventos.
- Administra localidades.
- Consulta ventas.
- Consulta estadísticas.

---

## Moderador

- Revisa eventos.
- Aprueba.
- Solicita correcciones.
- Rechaza.

---

## Administrador

- Aprueba organizaciones.
- Gestiona moderadores.
- Configura la plataforma.
- Administra categorías.
- Gestiona promociones.

---

# Wizard para Crear Eventos

## Paso 1 - Información General

- Nombre del evento.
- Descripción.
- Categoría.
- Imagen principal.
- Fecha.
- Hora de inicio.
- Hora de finalización.

---

## Paso 2 - Lugar

- Dirección.
- Ciudad.
- Google Maps.
- Aforo máximo.

---

## Paso 3 - Venta de Entradas

Configuración de localidades.

Cada evento tendrá como mínimo una localidad.

Por defecto:

```text
General
```

Cada localidad contiene:

- Nombre.
- Precio.
- Capacidad.

El organizador podrá agregar tantas localidades como necesite.

---

## Paso 4 - Información Adicional

- Código PULEP (opcional).
- Adjuntar permisos (opcional).
- Políticas del evento.
- Declaración de responsabilidad.

---

## Paso 5 - Confirmación

Resumen completo del evento.

El sistema ejecuta todas las validaciones automáticas.

Si todo es correcto:

Enviar evento.

---

# Entidades Nuevas Propuestas

## Localidad

Cada evento tendrá una o varias localidades.

Responsabilidades:
- nombre.
- Precio.
- Capacidad.
- Disponibilidad.

> Todo evento deberá tener al menos una localidad llamada **General**, creada automáticamente cuando el organizador no defina localidades personalizadas.

---

## Evento

Agregar:

- Estado del evento.
- Organización propietaria.
- Fecha de revisión.
- Moderador responsable (opcional).
- Motivo de revisión (enum).
- Observación (solo cuando el motivo sea "OTRO").

No será necesario agregar un tipo de evento para diferenciar "General" o "Por Localidades", ya que esta condición se deduce automáticamente de las localidades asociadas al evento.

---

# Reglas de Negocio

- Una organización no puede superar el límite de eventos activos de su nivel.
- Todo evento debe tener al menos una localidad.
- Si el organizador no crea localidades, el sistema crea automáticamente la localidad **General**.
- La suma de capacidades de las localidades no puede superar el aforo del evento.
- El Moderador revisa únicamente el contenido y cumplimiento del evento, mientras que el sistema valida automáticamente las reglas técnicas e integridad de la información.