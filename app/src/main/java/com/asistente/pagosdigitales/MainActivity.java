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

public class MainActivity extends AppCompatActivity {

    private TextView tvEstado;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEstado = findViewById(R.id.tvEstado);
        Button btnPermisos = findViewById(R.id.btnPermisos);
        Button btnBateria = findViewById(R.id.btnBateria);
        Button btnProbar = findViewById(R.id.btnProbar);
        Button btnAutoInicio = findViewById(R.id.btnAutoInicio);

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

        // ACCIÓN 2: Optimización de Batería (Abrir lista general)
        btnBateria.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.toast_optimizacion_bateria), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
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