# Plan de Implementación: Sistema de Suscripción mediante Servidor PHP

Este plan detalla la integración de una verificación remota que validará si el usuario tiene una suscripción activa de 5 soles antes de permitirle utilizar las funciones principales de la aplicación.

## Arquitectura de la Solución
Dado que el cobro se maneja mediante Yape/Plin de forma externa y la app ya envía datos a un backend, realizaremos lo siguiente:
1. Al abrir la app (`MainActivity`), se enviará una consulta HTTP automática al servidor PHP con el identificador único o el estado del sistema.
2. El servidor responderá si el usuario tiene el acceso permitido.
3. Si la respuesta es negativa, la app bloqueará los botones y mostrará un mensaje indicando que debe renovar su suscripción de 5 soles.

## Cambios Propuestos

### Código Fuente (Java)

#### [MODIFY] [MainActivity.java](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/java/com/asistente/pagosdigitales/MainActivity.java)
- Implementar un método asíncrono en `onResume()` para verificar el estado de la suscripción consultando a tu servidor (`verificarSuscripcionPHP()`).
- Si el servidor indica que no está pagado, deshabilitar (`setEnabled(false)`) los botones de activación y prueba, y cambiar el texto de estado por un aviso de pago pendiente.

## Plan de Verificación

### Verificación de Flujo
- Compilar la aplicación y comprobar que no cause bloqueos inesperados.
- Dado que la URL actual apunta a `tu-servidor.com`, configuraremos una estructura limpia y segura para que puedas reemplazarla por tu dominio web real fácilmente.
