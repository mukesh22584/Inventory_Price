package com.example.gerin.inventory;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        ImageView logo = findViewById(R.id.splash_screen_logo);
        TextView name = findViewById(R.id.splash_screen_name);

        Animation fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        logo.startAnimation(fromBottom);
        name.startAnimation(fadeIn);

        fadeIn.setStartOffset(2000);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent catalogIntent = new Intent(MainActivity.this, CatalogActivity.class);
                startActivity(catalogIntent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
