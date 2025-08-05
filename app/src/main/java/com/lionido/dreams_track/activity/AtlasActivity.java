package com.lionido.dreams_track.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.lionido.dreams_track.BaseActivity;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.view.WordCloudView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AtlasActivity extends BaseActivity {
    private WordCloudView wordCloudView;
    private Spinner emotionFilterSpinner;
    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;
    private List<DreamEntity> allDreams;
    private EmotionDetector emotionDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atlas);

        initializeViews();
        initializeDatabase();
        setupEmotionFilter();
        loadDreamsAndShowCloud(null);
    }

    private void initializeViews() {
        wordCloudView = findViewById(R.id.word_cloud_view);
        emotionFilterSpinner = findViewById(R.id.spinner_emotion_filter);

        if (wordCloudView == null) {
            Toast.makeText(this, "Ошибка инициализации WordCloudView", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newSingleThreadExecutor();
        emotionDetector = new EmotionDetector();
        allDreams = new ArrayList<>();
    }

    private void setupEmotionFilter() {
        if (emotionFilterSpinner == null) return;

        String[] emotions = {
                "Все",
                "Страх",
                "Радость",
                "Грусть",
                "Гнев",
                "Удивление",
                "Спокойствие",
                "Любовь",
                "Смущение",
                "Отчаяние"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                emotions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionFilterSpinner.setAdapter(adapter);

        emotionFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = position == 0 ? null : mapEmotionNameToCode(emotions[position]);
                loadDreamsAndShowCloud(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    private String mapEmotionNameToCode(String emotionName) {
        switch (emotionName) {
            case "Страх": return "fear";
            case "Радость": return "joy";
            case "Грусть": return "sadness";
            case "Гнев": return "anger";
            case "Удивление": return "surprise";
            case "Спокойствие": return "calm";
            case "Любовь": return "love";
            case "Смущение": return "shame";
            case "Отчаяние": return "despair";
            default: return "neutral";
        }
    }

    private void loadDreamsAndShowCloud(String emotionFilter) {
        if (executor == null || wordCloudView == null) return;

        executor.execute(() -> {
            try {
                allDreams = dreamDao.getAllDreams();
                Map<String, Integer> symbolFrequency = new HashMap<>();

                for (DreamEntity dream : allDreams) {
                    // Проверяем фильтр эмоций
                    if (emotionFilter == null ||
                            (dream.getEmotion() != null && dream.getEmotion().equals(emotionFilter))) {

                        List<Symbol> symbols = dream.getSymbols();
                        if (symbols != null) {
                            for (Symbol symbol : symbols) {
                                if (symbol != null && symbol.getKeyword() != null && !symbol.getKeyword().trim().isEmpty()) {
                                    String keyword = symbol.getKeyword().trim();
                                    symbolFrequency.put(keyword,
                                            symbolFrequency.getOrDefault(keyword, 0) + 1);
                                }
                            }
                        }

                        // Также анализируем текст сна на ключевые слова
                        if (dream.getText() != null && !dream.getText().trim().isEmpty()) {
                            analyzeTextForKeywords(dream.getText(), symbolFrequency);
                        }
                    }
                }

                List<WordCloudView.Word> words = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : symbolFrequency.entrySet()) {
                    if (entry.getValue() > 0) { // Показываем только символы с частотой > 0
                        words.add(new WordCloudView.Word(entry.getKey(), entry.getValue()));
                    }
                }

                runOnUiThread(() -> {
                    if (wordCloudView != null) {
                        if (words.isEmpty()) {
                            // Если нет символов, показываем сообщение
                            words.add(new WordCloudView.Word("Нет данных", 1));
                        }
                        wordCloudView.setWords(words);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(AtlasActivity.this, "Ошибка загрузки данных: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void analyzeTextForKeywords(String text, Map<String, Integer> symbolFrequency) {
        if (text == null || text.trim().isEmpty()) return;

        String lowerText = text.toLowerCase();

        // Список ключевых слов для анализа
        String[] keywords = {
                "вода", "море", "река", "дождь", "океан",
                "огонь", "пламя", "жар", "костер",
                "животное", "кот", "собака", "птица", "змея",
                "дом", "здание", "комната", "дверь",
                "летать", "полет", "падать", "бежать",
                "страх", "радость", "грусть", "любовь",
                "смерть", "жизнь", "свет", "темнота",
                "дерево", "лес", "гора", "небо",
                "человек", "люди", "ребенок", "мать", "отец"
        };

        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                symbolFrequency.put(keyword, symbolFrequency.getOrDefault(keyword, 0) + 1);
            }
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