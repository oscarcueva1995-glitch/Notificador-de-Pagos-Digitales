package com.asistente.pagosdigitales;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        VideoView videoView = findViewById(R.id.videoViewSplash);
        
        // Ruta del video en res/raw
        String path = "android.resource://" + getPackageName() + "/" + R.raw.video_intro;
        videoView.setVideoURI(Uri.parse(path));

        // Iniciar MainActivity después de 6 segundos o cuando el video termine
        new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, 6000);

        videoView.setOnCompletionListener(mp -> startMainActivity());
        
        videoView.start();
    }

    private void startMainActivity() {
        if (!isFinishing()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}