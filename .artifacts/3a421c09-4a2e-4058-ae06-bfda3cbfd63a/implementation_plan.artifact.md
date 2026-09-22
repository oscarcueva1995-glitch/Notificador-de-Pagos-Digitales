# Plan de Implementación: Splash Screen con Video

Este plan describe cómo integrar un video de introducción (Splash Screen) que se reproducirá durante 6 segundos al iniciar la aplicación.

## Cambios Propuestos

### Recursos (Resources)

#### [NEW] [activity_splash.xml](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/res/layout/activity_splash.xml)
Se creará un layout simple que contendrá un `VideoView` ocupando toda la pantalla para reproducir el video de introducción.

### Código Fuente (Java)

#### [NEW] [SplashActivity.java](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/java/com/asistente/pagosdigitales/SplashActivity.java)
Esta nueva actividad se encargará de:
1. Configurar y reproducir el video desde `res/raw/video_intro.mp4`.
2. Escuchar cuando el video termine o esperar 6 segundos.
3. Iniciar `MainActivity` y cerrar la pantalla de splash.

### Configuración

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/USER/AndroidStudioProjects/NotificadordePagosDigitales/app/src/main/AndroidManifest.xml)
Se realizarán los siguientes cambios:
- Registrar la nueva `SplashActivity`.
- Mover el `intent-filter` de "LAUNCHER" de `MainActivity` a `SplashActivity`.
- Asegurar que la `SplashActivity` use un tema sin barra de título (FullScreen).

## Plan de Verificación

### Verificación Manual
- Ejecutar la aplicación en el dispositivo/emulador.
- Confirmar que el video se reproduce automáticamente al abrir la app.
- Verificar que después de que el video termine (o pasen los 6 segundos), la aplicación navega correctamente a la pantalla principal.
