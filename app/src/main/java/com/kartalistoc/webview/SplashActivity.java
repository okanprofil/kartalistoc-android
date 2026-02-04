package com.kartalistoc.webview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout logoContainer = findViewById(R.id.logoContainer);
        ImageView logo = findViewById(R.id.logo);
        TextView appName = findViewById(R.id.appName);
        View loader = findViewById(R.id.loader);

        // 1. Logo Animasyonu: Ölçeklenme ve Parlama
        AnimationSet logoAnim = new AnimationSet(true);
        logoAnim.addAnimation(new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f, 
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        logoAnim.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        logoAnim.setDuration(800);
        logoAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        logo.startAnimation(logoAnim);

        // 2. Metin Animasyonu: Aşağıdan Yukarı Gelme
        Animation textAnim = new TranslateAnimation(0, 0, 50, 0);
        textAnim.setDuration(1000);
        textAnim.setStartOffset(200);
        textAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        appName.startAnimation(textAnim);
        
        // Metin Opaklık
        AlphaAnimation textAlpha = new AlphaAnimation(0.0f, 1.0f);
        textAlpha.setDuration(800);
        textAlpha.setStartOffset(300);
        appName.startAnimation(textAlpha);

        // 3. Loader Animasyonu
        loader.setAlpha(0);
        loader.animate().alpha(1).setDuration(500).setStartDelay(500).start();

        // Toplam 1.5 saniye sonra MainActivity'e geç (Daha kısa ve profesyonel)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            // Geçiş animasyonu
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1800);
    }
}
