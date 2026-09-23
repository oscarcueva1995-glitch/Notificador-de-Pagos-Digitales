package com.asistente.pagosdigitales;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView tvEstado, tvCodigoCliente;
    private InterstitialAd mInterstitialAd;
    private Button btnPermisos, btnProbar, btnAutoInicio;
    private boolean suscripcionActiva = false; // Por defecto FALSE para requerir pago al descargar/abrir la app
    private String codigoCliente;
    private String nombreTitular;
    private AlertDialog dialogBloqueo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEstado = findViewById(R.id.tvEstado);
        tvCodigoCliente = findViewById(R.id.tvCodigoCliente);
        btnPermisos = findViewById(R.id.btnPermisos);
        btnProbar = findViewById(R.id.btnProbar);
        btnAutoInicio = findViewById(R.id.btnAutoInicio);
        TextView btnCompartir = findViewById(R.id.btnCompartir);

        inicializarCodigoCliente();

        // 1. Permiso de Notificaciones para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // ACCIÓN 1: Activar el Parlante
        btnPermisos.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                mostrarGuiaAjustesRestringidos();
            }
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.toast_acceso_notificaciones), Toast.LENGTH_SHORT).show();
            }
        });

        // ACCIÓN 3: Auto-Inicio y Permisos Especiales (Redmi/Xiaomi)
        btnAutoInicio.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                Toast.makeText(this, "Busca 'Inicio automático' y 'Ahorro de batería -> Sin restricciones'", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Abre los ajustes de la app manualmente", Toast.LENGTH_SHORT).show();
            }
        });

        // ACCIÓN 4: Prueba con Anuncio Intersticial
        btnProbar.setOnClickListener(v -> {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(MainActivity.this);
            } else {
                ejecutarPruebaVoz();
            }
        });

        // ACCIÓN VOZ: Cambiar entre voz de Hombre / Mujer
        Button btnCambiarVoz = findViewById(R.id.btnCambiarVoz);
        btnCambiarVoz.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String vozActual = prefs.getString("voz_genero", "mujer");
            String nuevaVoz = vozActual.equals("mujer") ? "hombre" : "mujer";
            prefs.edit().putString("voz_genero", nuevaVoz).apply();

            Toast.makeText(this, "Voz cambiada a: " + (nuevaVoz.equals("hombre") ? "Hombre 👨" : "Mujer 👩"), Toast.LENGTH_SHORT).show();
            ejecutarPruebaVoz();
        });

        // ACCIÓN 5: Compartir Aplicación
        btnCompartir.setOnClickListener(v -> {
            try {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "¡Hola! Te invito a descargar la aplicación Notificador de Pagos Digitales desde Google Play: https://play.google.com/store/apps/details?id=com.asistente.pagosdigitales");
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, "Compartir con");
                startActivity(shareIntent);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo abrir el menú de compartir", Toast.LENGTH_SHORT).show();
            }
        });

        // ACCIÓN 6: Solicitar QR / Soporte
        View cardSoporteQr = findViewById(R.id.cardSoporteQr);
        if (cardSoporteQr != null) {
            cardSoporteQr.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://wa.me/qr/NZWMIRL3R5ZBD1"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Inicializa los anuncios
        MobileAds.initialize(this, initializationStatus -> {
            cargarIntersticial();
        });

        // Carga el banner
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void cargarIntersticial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        // ID Real del Intersticial
        InterstitialAd.load(this,"ca-app-pub-2597459253679263/8981950596", adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mInterstitialAd = null;
                            cargarIntersticial(); // Carga el siguiente
                            ejecutarPruebaVoz();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    private void ejecutarPruebaVoz() {
        Intent intent = new Intent(this, NotificacionService.class);
        intent.setAction("PROBAR_VOZ");
        startService(intent);
    }

    private void mostrarGuiaAjustesRestringidos() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.guia_titulo))
            .setMessage(getString(R.string.guia_mensaje))
            .setPositiveButton(getString(R.string.btn_entendido), null)
            .show();
    }

    private void inicializarCodigoCliente() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        codigoCliente = prefs.getString("codigo_cliente", null);
        nombreTitular = prefs.getString("nombre_titular", "");

        if (codigoCliente == null) {
            String uniqueId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            codigoCliente = "NPD-" + uniqueId;
            prefs.edit().putString("codigo_cliente", codigoCliente).apply();
        }
        if (tvCodigoCliente != null) {
            tvCodigoCliente.setText(codigoCliente);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        verificarEstadoSuscripcionLocalYServidor();
        actualizarEstado();
    }

    private void verificarEstadoSuscripcionLocalYServidor() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long fechaExpiracion = prefs.getLong("fecha_expiracion", 0);
        long ahora = System.currentTimeMillis();

        if (ahora < fechaExpiracion) {
            suscripcionActiva = true;
            desbloquearApp();
            return;
        }

        verificarSuscripcionPHP();
    }

    private void guardarSuscripcionActivaLocalmente() {
        long unMesEnMillis = 30L * 24 * 60 * 60 * 1000;
        long fechaExpiracion = System.currentTimeMillis() + unMesEnMillis;
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("suscripcion_activa", true)
                .putLong("fecha_expiracion", fechaExpiracion)
                .apply();
        suscripcionActiva = true;
        desbloquearApp();
    }

    private void verificarSuscripcionPHP() {
        new Thread(() -> {
            String usuarioBuscar = (nombreTitular != null && !nombreTitular.trim().isEmpty())
                    ? nombreTitular.trim()
                    : codigoCliente;

            String urlTarget = "https://oscarcueva95.alwaysdata.net/api_app.php?usuario=" + Uri.encode(usuarioBuscar);

            String respStr = realizarPeticionHttp(urlTarget);
            if (respStr != null) {
                int startJson = respStr.indexOf('{');
                int endJson = respStr.lastIndexOf('}');

                if (startJson != -1 && endJson > startJson) {
                    try {
                        String jsonClean = respStr.substring(startJson, endJson + 1);
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(jsonClean);
                        boolean acceso = jsonResponse.optBoolean("acceso", false);
                        String mensaje = jsonResponse.optString("mensaje", "Suscripción requerida de S/ 5.00.");

                        if (acceso) {
                            runOnUiThread(() -> {
                                guardarSuscripcionActivaLocalmente();
                            });
                            return;
                        } else {
                            runOnUiThread(() -> {
                                suscripcionActiva = false;
                                bloquearAppPorFaltaDePago(mensaje);
                            });
                            return;
                        }
                    } catch (Exception e) {
                        Log.e("API_CHECK", "Error parseando JSON: " + e.getMessage());
                    }
                }
            }

            runOnUiThread(() -> {
                suscripcionActiva = false;
                bloquearAppPorFaltaDePago("No se encontraron pagos para '" + usuarioBuscar + "'. Ingresa tu nombre exacto de Yape.");
            });
        }).start();
    }

    private String realizarPeticionHttp(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                conn.disconnect();
                return response.toString().trim();
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e("API_CHECK", "Error HTTP: " + e.getMessage());
        }
        return null;
    }

    private void bloquearAppPorFaltaDePago(String mensajeServer) {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;

            tvEstado.setText("SUSCRIPCIÓN REQUERIDA (S/ 5.00) ⚠️");
            tvEstado.setTextColor(Color.parseColor("#E65100"));

            btnPermisos.setEnabled(false);
            btnProbar.setEnabled(false);
            btnAutoInicio.setEnabled(false);

            if (dialogBloqueo != null && dialogBloqueo.isShowing()) {
                TextView tvMsgDlg = dialogBloqueo.findViewById(R.id.tvDialogMensaje);
                if (tvMsgDlg != null && mensajeServer != null && !mensajeServer.isEmpty()) {
                    tvMsgDlg.setText(mensajeServer);
                }
                return;
            }

            try {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_bloqueo_pago, null);

                TextView tvCodigoDlg = dialogView.findViewById(R.id.tvCodigoClienteDialog);
                if (tvCodigoDlg != null) {
                    tvCodigoDlg.setText(codigoCliente);
                }

                TextView tvMsgDlg = dialogView.findViewById(R.id.tvDialogMensaje);
                if (tvMsgDlg != null && mensajeServer != null && !mensajeServer.isEmpty()) {
                    tvMsgDlg.setText(mensajeServer);
                }

                android.widget.EditText etNombreDlg = dialogView.findViewById(R.id.etNombreTitular);
                if (etNombreDlg != null) {
                    etNombreDlg.setText(nombreTitular);
                }

                Button btnWhatsapp = dialogView.findViewById(R.id.btnWhatsapp);
                if (btnWhatsapp != null) {
                    btnWhatsapp.setOnClickListener(v -> {
                        try {
                            String datoCopiado = (nombreTitular != null && !nombreTitular.trim().isEmpty()) ? nombreTitular : codigoCliente;
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Nombre Yape", datoCopiado);
                            if (clipboard != null) clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, "Nombre '" + datoCopiado + "' copiado al portapapeles", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://wa.me/qr/NZWMIRL3R5ZBD1"));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                Button btnVerificar = dialogView.findViewById(R.id.btnVerificarPago);
                if (btnVerificar != null) {
                    btnVerificar.setOnClickListener(v -> {
                        if (etNombreDlg != null) {
                            String nuevoNombre = etNombreDlg.getText().toString().trim();
                            nombreTitular = nuevoNombre;
                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("nombre_titular", nuevoNombre)
                                    .apply();
                        }
                        Toast.makeText(this, "Verificando pago...", Toast.LENGTH_SHORT).show();
                        verificarSuscripcionPHP();
                    });
                }

                Button btnCerrar = dialogView.findViewById(R.id.btnCerrarApp);
                if (btnCerrar != null) {
                    btnCerrar.setOnClickListener(v -> finish());
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(dialogView);
                builder.setCancelable(false);

                dialogBloqueo = builder.create();
                dialogBloqueo.show();
            } catch (Exception e) {
                Log.e("DIALOG_ERROR", "Error mostrando diálogo de bloqueo: " + e.getMessage());
            }
        });
    }

    private void desbloquearApp() {
        if (dialogBloqueo != null && dialogBloqueo.isShowing()) {
            dialogBloqueo.dismiss();
        }
        btnPermisos.setEnabled(true);
        btnProbar.setEnabled(true);
        btnAutoInicio.setEnabled(true);
        Toast.makeText(this, "¡Suscripción Verificada y Activa! ✅", Toast.LENGTH_LONG).show();
        actualizarEstado();
    }

    private void actualizarEstado() {
        if (isNotificationServiceEnabled()) {
            tvEstado.setText(getString(R.string.estado_activo));
            tvEstado.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvEstado.setText(getString(R.string.estado_detenido));
            tvEstado.setTextColor(Color.parseColor("#C62828"));
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) return true;
            }
        }
        return false;
    }
}
