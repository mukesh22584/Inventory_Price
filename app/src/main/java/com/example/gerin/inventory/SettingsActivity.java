package com.example.gerin.inventory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.btn_theme_settings).setOnClickListener(v -> showThemeDialog());
	}

    private void showThemeDialog() {
        String[] themeOptions = {"Light", "Dark", "System Default"};
        int[] modeValues = {
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        };

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentSavedMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        int checkedItem = 2;
        for (int i = 0; i < modeValues.length; i++) {
            if (modeValues[i] == currentSavedMode) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeOptions, checkedItem, (dialog, which) -> {
                    int selectedMode = modeValues[which];
                    prefs.edit().putInt(KEY_THEME_MODE, selectedMode).apply();
                    AppCompatDelegate.setDefaultNightMode(selectedMode);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
