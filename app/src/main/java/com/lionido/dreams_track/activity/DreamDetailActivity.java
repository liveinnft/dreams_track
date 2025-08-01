package com.lionido.dreams_track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.adapter.SymbolAdapter;
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

public class DreamDetailActivity extends AppCompatActivity {

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
    private SymbolAdapter symbolAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_detail);

        initializeDatabase();
        initializeViews();
        loadDreamData();
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

        // Настройка RecyclerView для символов
        recyclerSymbols.setLayoutManager(new LinearLayoutManager(this));

        // Обработчики кнопок
        btnEdit.setOnClickListener(v -> editDream());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadDreamData() {
        int dreamId = getIntent().getIntExtra("dream_id", -1);
        if (dreamId == -1) {
            Toast.makeText(this, "Ошибка загрузки сна", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        executor.execute(() -> {
            currentDream = dreamDao.getDreamById(dreamId);

            runOnUiThread(() -> {
                if (currentDream != null) {
                    displayDreamData();
                } else {
                    Toast.makeText(this, "Сон не найден", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayDreamData() {
        // Отображение текста сна
        tvDreamText.setText(currentDream.getText());

        // Форматирование и отображение даты
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"));
        String dateText = sdf.format(new Date(currentDream.getTimestamp()));
        tvDreamDate.setText(dateText);

        // Отображение эмоции
        String emotion = currentDream.getEmotion();
        if (emotion != null && !emotion.isEmpty()) {
            String emotionDisplay = emotionDetector.getEmotionDisplayName(emotion);
            tvEmotion.setText(emotionDisplay);
            tvEmotion.setTextColor(getEmotionColor(emotion));
        } else {
            tvEmotion.setText("Не определена");
            tvEmotion.setTextColor(getColor(R.color.text_secondary));
        }

        // Отображение способа ввода
        String inputMethod = currentDream.getInputMethod();
        if ("voice".equals(inputMethod)) {
            tvInputMethod.setText("🎤 Голосовой ввод");
        } else if ("text".equals(inputMethod)) {
            tvInputMethod.setText("⌨️ Текстовый ввод");
        } else {
            tvInputMethod.setText("Не указан");
        }

        // Отображение символов
        List<Symbol> symbols = currentDream.getSymbols();
        if (symbols != null && !symbols.isEmpty()) {
            symbolAdapter = new SymbolAdapter(symbols);
            recyclerSymbols.setAdapter(symbolAdapter);
            recyclerSymbols.setVisibility(View.VISIBLE);
        } else {
            recyclerSymbols.setVisibility(View.GONE);
        }
    }

    private int getEmotionColor(String emotion) {
        switch (emotion) {
            case "fear":
                return getColor(R.color.emotion_fear);
            case "joy":
                return getColor(R.color.emotion_joy);
            case "sadness":
                return getColor(R.color.emotion_sadness);
            case "anger":
                return getColor(R.color.emotion_anger);
            case "surprise":
                return getColor(R.color.emotion_surprise);
            case "calm":
                return getColor(R.color.emotion_calm);
            case "love":
                return getColor(R.color.emotion_love);
            case "shame":
                return getColor(R.color.emotion_shame);
            case "despair":
                return getColor(R.color.emotion_despair);
            default:
                return getColor(R.color.text_secondary);
        }
    }

    private void editDream() {
        Intent intent = new Intent(this, EditDreamActivity.class);
        intent.putExtra("dream_id", currentDream.getId());
        startActivity(intent);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление сна")
                .setMessage("Вы уверены, что хотите удалить этот сон? Это действие нельзя отменить.")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Удалить", (dialog, which) -> deleteDream())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteDream() {
        executor.execute(() -> {
            dreamDao.delete(currentDream);

            runOnUiThread(() -> {
                Toast.makeText(this, "Сон удален", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагружаем данные при возврате из редактирования
        if (currentDream != null) {
            loadDreamData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}