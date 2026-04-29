package com.asistente.pagosdigitales;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class NotificacionService extends NotificationListenerService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean isReady = false;
    private String mensajePendiente = null;
    private static final String CHANNEL_ID = "notification_channel_gen";
    private static final String TAG = "NOTIF_APP";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio Creado. Iniciando TTS...");
        tts = new TextToSpeech(this, this);
        crearCanalNotificacion();
        iniciarForeground();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.notif_canal_nombre), NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void iniciarForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_titulo))
                .setContentText(getString(R.string.notif_texto))
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("es", "ES"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma no compatible, intentando español genérico.");
                tts.setLanguage(new Locale("es"));
            }
            isReady = true;
            Log.d(TAG, "TTS listo para hablar.");
            
            if (mensajePendiente != null) {
                hablar(mensajePendiente);
                mensajePendiente = null;
            }
        } else {
            Log.e(TAG, "Error al iniciar el motor TTS.");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName().toLowerCase();
        Log.d(TAG, "Notificación recibida de: " + packageName);
        
        if (packageName.contains("yape") || packageName.contains("bcp")) {
            Bundle extras = sbn.getNotification().extras;
            String titulo = extras.getString("android.title", "");
            String texto = extras.getString("android.text", "");
            
            if (texto != null && !texto.isEmpty()) {
                Log.d(TAG, "Procesando: " + titulo + " - " + texto);
                hablar(titulo + ". " + texto);
                enviarAPHP(titulo, texto);
            }
        }
    }

    private void hablar(String mensaje) {
        if (isReady && tts != null) {
            Log.d(TAG, "Hablando: " + mensaje);
            tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "MSG_ID");
        } else {
            Log.d(TAG, "TTS no listo. Guardando mensaje en cola.");
            mensajePendiente = mensaje;
            if (tts == null) tts = new TextToSpeech(this, this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "PROBAR_VOZ".equals(intent.getAction())) {
            new Handler(Looper.getMainLooper()).post(() -> 
                Toast.makeText(getApplicationContext(), "Probando voz...", Toast.LENGTH_SHORT).show());
            hablar(getString(R.string.prueba_voz_texto));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private void enviarAPHP(final String titulo, final String texto) {
        new Thread(() -> {
            try {
                URL url = new URL("http://tu-servidor.com/guardar_pago.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                String data = "titulo=" + titulo + "&texto=" + texto;
                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error PHP: " + e.getMessage());
            }
        }).start();
    }
}