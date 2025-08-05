package com.lionido.dreams_track.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lionido.dreams_track.BaseActivity;
import com.lionido.dreams_track.MainActivity;
import com.lionido.dreams_track.R;

public class SettingsActivity extends BaseActivity {
    
    private static final String PREFS_NAME = "DreamPrefs";
    private static final String PREF_THEME = "app_theme";
    
    private RadioGroup themeRadioGroup;
    private RadioButton radioSystemTheme;
    private RadioButton radioLightTheme;
    private RadioButton radioDarkTheme;
    
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        initializePreferences();
        setupRadioGroup();
        loadCurrentTheme();
    }
    
    private void initializeViews() {
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        radioSystemTheme = findViewById(R.id.radio_theme_system);
        radioLightTheme = findViewById(R.id.radio_theme_light);
        radioDarkTheme = findViewById(R.id.radio_theme_dark);
    }
    
    private void initializePreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    private void setupRadioGroup() {
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = prefs.edit();
            
            if (checkedId == R.id.radio_theme_system) {
                editor.putInt(PREF_THEME, 0); // Системная тема
            } else if (checkedId == R.id.radio_theme_light) {
                editor.putInt(PREF_THEME, 1); // Светлая тема
            } else if (checkedId == R.id.radio_theme_dark) {
                editor.putInt(PREF_THEME, 2); // Темная тема
            }
            
            editor.apply();
            // Перезапускаем MainActivity для применения темы
            restartApp();
        });
    }
    
    private void loadCurrentTheme() {
        int theme = prefs.getInt(PREF_THEME, 0); // По умолчанию системная тема
        
        switch (theme) {
            case 0:
                radioSystemTheme.setChecked(true);
                break;
            case 1:
                radioLightTheme.setChecked(true);
                break;
            case 2:
                radioDarkTheme.setChecked(true);
                break;
        }
    }
    
    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        // Анимация перехода
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}