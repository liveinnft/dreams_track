package com.lionido.dreams_track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lionido.dreams_track.BaseActivity;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.adapter.SymbolsAdapter;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.EmotionDetector;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DreamDetailActivity extends BaseActivity {

    private TextView tvDreamText;
    private TextView tvDreamDate;
    private TextView tvEmotion;
    private TextView tvInputMethod;
    private RecyclerView recyclerSymbols;
    private Button btnEdit;
    private Button btnDelete;
    private Button btnBack;

    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;
    private EmotionDetector emotionDetector;

    private DreamEntity currentDream;
    private SymbolsAdapter symbolAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_detail);

        initializeDatabase();
        initializeViews();
        setupClickListeners();
        loadDreamDetails();
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
        emotionDetector = new EmotionDetector();
    }

    private void initializeViews() {
        tvDreamText = findViewById(R.id.tv_dream_text);
        tvDreamDate = findViewById(R.id.tv_dream_date);
        tvEmotion = findViewById(R.id.tv_emotion);
        tvInputMethod = findViewById(R.id.tv_input_method);
        recyclerSymbols = findViewById(R.id.recycler_symbols);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);

        // Используем цвета для темной темы по умолчанию
        tvEmotion.setTextColor(getColor(R.color.text_secondary_dark));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(DreamDetailActivity.this, EditDreamActivity.class);
            intent.putExtra("dream_id", currentDream.getId());
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadDreamDetails() {
        Intent intent = getIntent();
        int dreamId = intent.getIntExtra("dream_id", -1);

        if (dreamId == -1) {
            Toast.makeText(this, "Ошибка загрузки сна", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        executor.execute(() -> {
            try {
                currentDream = dreamDao.getDreamById(dreamId);

                if (currentDream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(DreamDetailActivity.this, "Сон не найден", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Загружаем символы
                List<Symbol> symbols = currentDream.getSymbols();

                runOnUiThread(() -> {
                    displayDreamDetails(symbols);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(DreamDetailActivity.this, "Ошибка загрузки: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    private void displayDreamDetails(List<Symbol> symbols) {
        // Отображаем основную информацию о сне
        String dreamText = currentDream.getText();
        tvDreamText.setText(dreamText != null ? dreamText : "Текст сна отсутствует");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
        String formattedDate = sdf.format(new Date(currentDream.getTimestamp()));
        tvDreamDate.setText(formattedDate);

        // Отображаем эмоцию
        String emotion = currentDream.getEmotion();
        if (emotion != null && !emotion.isEmpty()) {
            updateEmotionDisplay(emotion);
        } else {
            tvEmotion.setText("Не определена");
            tvEmotion.setTextColor(getColor(R.color.text_secondary_dark));
        }

        // Отображаем способ ввода
        String inputMethod = currentDream.getInputMethod();
        if ("voice".equals(inputMethod)) {
            tvInputMethod.setText("🎤 Голосовой ввод");
        } else if ("text".equals(inputMethod)) {
            tvInputMethod.setText("⌨️ Ручной ввод");
        } else {
            tvInputMethod.setText("Не указан");
        }

        // Отображаем символы
        if (symbols != null && !symbols.isEmpty()) {
            symbolAdapter = new SymbolsAdapter(this, symbols);
            recyclerSymbols.setLayoutManager(new LinearLayoutManager(this));
            recyclerSymbols.setAdapter(symbolAdapter);
            recyclerSymbols.setVisibility(View.VISIBLE);
        } else {
            recyclerSymbols.setVisibility(View.GONE);
        }
    }

    private void updateEmotionDisplay(String emotion) {
        tvEmotion.setText(emotionDetector.getEmotionDisplayName(emotion));
        tvEmotion.setTextColor(getEmotionColor(emotion));
    }

    private int getEmotionColor(String emotion) {
        // Используем цвета для темной темы по умолчанию
        switch (emotion.toLowerCase()) {
            case "fear":
                return getColor(R.color.emotion_fear_dark);
            case "joy":
                return getColor(R.color.emotion_joy_dark);
            case "sadness":
                return getColor(R.color.emotion_sadness_dark);
            case "anger":
                return getColor(R.color.emotion_anger_dark);
            case "surprise":
                return getColor(R.color.emotion_surprise_dark);
            case "calm":
                return getColor(R.color.emotion_calm_dark);
            case "love":
                return getColor(R.color.emotion_love_dark);
            case "shame":
                return getColor(R.color.emotion_shame_dark);
            case "despair":
                return getColor(R.color.emotion_despair_dark);
            case "anxiety":
                return getColor(R.color.emotion_anxious_dark);
            case "strange":
                return getColor(R.color.emotion_strange_dark);
            default:
                return getColor(R.color.text_secondary_dark);
        }
    }

    private void editDream() {
        if (currentDream != null) {
            Intent intent = new Intent(this, EditDreamActivity.class);
            intent.putExtra("dream_id", currentDream.getId());
            startActivity(intent);
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить сон")
                .setMessage("Вы уверены, что хотите удалить этот сон? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteDream())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteDream() {
        if (currentDream == null) return;

        executor.execute(() -> {
            try {
                dreamDao.delete(currentDream);

                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(DreamDetailActivity.this, "Сон удален", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // Указываем, что данные изменились
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(DreamDetailActivity.this, "Ошибка удаления: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагружаем данные при возвращении на экран
        if (currentDream != null) {
            loadDreamDetails();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}