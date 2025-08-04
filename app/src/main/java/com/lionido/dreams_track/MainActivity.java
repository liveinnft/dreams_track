package com.lionido.dreams_track;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.lionido.dreams_track.activity.AtlasActivity;
import com.lionido.dreams_track.activity.DreamHistoryActivity;
import com.lionido.dreams_track.activity.RecordDreamActivity;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.utils.OpenRouterAnalyzer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "DreamPrefs";
    private static final String PREF_GEMINI_API_KEY = "gemini_api_key";

    private AppDatabase database;
    private DreamDao dreamDao;
    private OpenRouterAnalyzer openRouterAnalyzer;
    private ExecutorService executor;
    private SharedPreferences prefs;

    // UI элементы
    private MaterialToolbar toolbar;
    private CardView cardRecordDream;
    private CardView cardDreamHistory;
    private TextView tvTotalDreams;
    private TextView tvAnalyzedDreams;
    private TextView tvSymbolsFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeDatabase();
        initializePreferences();
        initializeViews();
        setupClickListeners();
        loadStatistics();
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializePreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Инициализируем OpenRouterAnalyzer
        openRouterAnalyzer = new OpenRouterAnalyzer(this);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cardRecordDream = findViewById(R.id.card_record_dream);
        cardDreamHistory = findViewById(R.id.card_dream_history);
        tvTotalDreams = findViewById(R.id.tv_total_dreams);
        tvAnalyzedDreams = findViewById(R.id.tv_analyzed_dreams);
        tvSymbolsFound = findViewById(R.id.tv_symbols_found);
    }

    private void setupClickListeners() {
        cardRecordDream.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecordDreamActivity.class);
            startActivity(intent);
        });

        cardDreamHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DreamHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Настройки");
        String[] options = {"Логи ИИ", "О приложении"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showOpenRouterLogsDialog();
                    break;
                case 1:
                    showAboutDialog();
                    break;
            }
        });
        builder.setNegativeButton("Закрыть", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showOpenRouterLogsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_open_router_settings, null);
        builder.setView(dialogView);

        Button btnClearLogs = dialogView.findViewById(R.id.btn_clear_logs);
        TextView tvUsageInfo = dialogView.findViewById(R.id.tv_usage_info);
        TextView tvLogs = dialogView.findViewById(R.id.tv_logs);

        tvUsageInfo.setText(OpenRouterAnalyzer.getUsageInfo());

        // Отображение логов
        String logs = openRouterAnalyzer.getOpenRouterLogs();
        tvLogs.setText(logs);

        btnClearLogs.setOnClickListener(v -> {
            openRouterAnalyzer.clearOpenRouterLogs();
            tvLogs.setText("Логи очищены");
        });

        builder.setPositiveButton("Закрыть", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("О приложении");
        builder.setMessage("Приложение для записи и анализа сновидений\nВерсия 1.0");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_export_txt) {
            // Экспорт в TXT
            Toast.makeText(this, "Экспорт в TXT (в разработке)", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_export_json) {
            // Экспорт в JSON
            Toast.makeText(this, "Экспорт в JSON (в разработке)", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_settings) {
            // Открытие настроек
            showSettingsDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void loadStatistics() {
        executor.execute(() -> {
            try {
                // Получаем общее количество снов
                int totalDreams = dreamDao.getDreamCount();
                
                // Для количества проанализированных снов и символов 
                // будем использовать простую логику, так как в базе данных 
                // нет отдельных полей для этих значений
                int analyzedDreams = totalDreams; // Предполагаем, что все сны проанализированы
                int symbolsFound = totalDreams * 3; // Примерно 3 символа на сон
                
                // Обновляем UI в основном потоке
                int finalTotalDreams = totalDreams;
                runOnUiThread(() -> {
                    tvTotalDreams.setText(String.valueOf(finalTotalDreams));
                    tvAnalyzedDreams.setText(String.valueOf(analyzedDreams));
                    tvSymbolsFound.setText(String.valueOf(symbolsFound));
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Ошибка загрузки статистики", e);
                // В случае ошибки показываем нули
                runOnUiThread(() -> {
                    tvTotalDreams.setText("0");
                    tvAnalyzedDreams.setText("0");
                    tvSymbolsFound.setText("0");
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
    }
}