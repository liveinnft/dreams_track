package com.lionido.dreams_track;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "DreamPrefs";
    private static final String PREF_THEME = "app_theme";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
    }
    
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = prefs.getInt(PREF_THEME, 0); // По умолчанию системная тема
        
        switch (theme) {
            case 0:
                // Системная тема - не изменяем тему явно
                break;
            case 1:
                // Светлая тема
                setTheme(R.style.Base_Theme_Dreams_track_Light);
                break;
            case 2:
                // Темная тема
                setTheme(R.style.Base_Theme_Dreams_track_Dark);
                break;
        }
    }
}