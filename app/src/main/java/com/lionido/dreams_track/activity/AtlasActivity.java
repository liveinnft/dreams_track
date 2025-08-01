package com.lionido.dreams_track.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
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

public class AtlasActivity extends AppCompatActivity {
    private WordCloudView wordCloudView;
    private Spinner emotionFilterSpinner;
    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;
    private List<DreamEntity> allDreams;
    private List<Symbol> allSymbols;
    private EmotionDetector emotionDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atlas);
        wordCloudView = findViewById(R.id.word_cloud_view);
        emotionFilterSpinner = findViewById(R.id.spinner_emotion_filter);
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newSingleThreadExecutor();
        emotionDetector = new EmotionDetector();
        setupEmotionFilter();
        loadDreamsAndShowCloud(null);
    }

    private void setupEmotionFilter() {
        String[] emotions = {"Все", "Страх", "Радость", "Грусть", "Гнев", "Удивление", "Спокойствие", "Любовь", "Смущение", "Отчаяние"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, emotions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionFilterSpinner.setAdapter(adapter);
        emotionFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = position == 0 ? null : emotionDetector.detectEmotion(emotions[position].toLowerCase());
                loadDreamsAndShowCloud(selected);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDreamsAndShowCloud(String emotionFilter) {
        executor.execute(() -> {
            allDreams = dreamDao.getAllDreams();
            Map<String, Integer> symbolFrequency = new HashMap<>();
            for (DreamEntity dream : allDreams) {
                if (emotionFilter == null || (dream.getEmotion() != null && dream.getEmotion().equals(emotionFilter))) {
                    List<Symbol> symbols = dream.getSymbols();
                    if (symbols != null) {
                        for (Symbol symbol : symbols) {
                            symbolFrequency.put(symbol.getKeyword(), symbolFrequency.getOrDefault(symbol.getKeyword(), 0) + 1);
                        }
                    }
                }
            }
            List<WordCloudView.Word> words = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : symbolFrequency.entrySet()) {
                words.add(new WordCloudView.Word(entry.getKey(), entry.getValue()));
            }
            runOnUiThread(() -> wordCloudView.setWords(words));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}