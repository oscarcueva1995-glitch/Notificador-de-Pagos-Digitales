# Plan de Implementación: Botón "Invitar a tus Amigos"

Este plan describe cómo agregar la opción de compartir la aplicación en las redes sociales y servicios de mensajería (WhatsApp, Facebook, etc.).

## Cambios Propuestos

### Recursos (Resources)

#### [MODIFY] [activity_main.xml](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/res/layout/activity_main.xml)
- Agregar un botón o un elemento visual elegante pero más discreto debajo del botón de prueba que diga "Invitar a tus Amigos", tal como en la imagen de referencia.
- Se configurará con el mismo color morado o una variante estilizada y texto visible.

### Código Fuente (Java)

#### [MODIFY] [MainActivity.java](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/java/com/asistente/pagosdigitales/MainActivity.java)
- Vincular el nuevo botón de invitación en `onCreate`.
- Implementar un `Intent.ACTION_SEND` para abrir el menú de compartición nativo del sistema operativo, permitiendo enviar el texto personalizado con el enlace de Google Play: `https://play.google.com/store/apps/details?id=com.asistente.pagosdigitales`

## Plan de Verificación

### Verificación Manual
- Ejecutar la aplicación.
- Pulsar en "Invitar a tus Amigos".
- Asegurar que se despliegue el menú del sistema para elegir WhatsApp, Facebook u otra aplicación de mensajería con el texto y enlace correctos.
