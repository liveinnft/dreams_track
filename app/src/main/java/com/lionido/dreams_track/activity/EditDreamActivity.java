package com.lionido.dreams_track.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.NLPAnalyzer;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.utils.SymbolAnalyzer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditDreamActivity extends AppCompatActivity {

    private EditText editDreamText;
    private TextView tvAnalysisResults;
    private Button btnReanalyze;
    private Button btnSave;
    private Button btnCancel;

    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;

    private NLPAnalyzer nlpAnalyzer;
    private EmotionDetector emotionDetector;
    private SymbolAnalyzer symbolAnalyzer;

    private DreamEntity currentDream;
    private List<Symbol> analyzedSymbols;
    private String analyzedEmotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dream);

        initializeDatabase();
        initializeViews();
        initializeHelpers();
        loadDreamData();
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializeViews() {
        editDreamText = findViewById(R.id.edit_dream_text);
        tvAnalysisResults = findViewById(R.id.tv_analysis_results);
        btnReanalyze = findViewById(R.id.btn_reanalyze);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Отслеживание изменений в тексте
        editDreamText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                btnReanalyze.setEnabled(!s.toString().trim().isEmpty());
            }
        });

        btnReanalyze.setOnClickListener(v -> reanalyzeDream());
        btnSave.setOnClickListener(v -> saveDream());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initializeHelpers() {
        nlpAnalyzer = new NLPAnalyzer(this);
        emotionDetector = new EmotionDetector();
        symbolAnalyzer = new SymbolAnalyzer(this);
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
                    editDreamText.setText(currentDream.getText());
                    analyzedSymbols = currentDream.getSymbols();
                    analyzedEmotion = currentDream.getEmotion();
                    displayAnalysisResults();
                } else {
                    Toast.makeText(this, "Сон не найден", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void reanalyzeDream() {
        String text = editDreamText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите текст сна", Toast.LENGTH_SHORT).show();
            return;
        }

        btnReanalyze.setEnabled(false);
        btnReanalyze.setText("Анализирую...");

        executor.execute(() -> {
            // Выполняем расширенный анализ
            analyzedSymbols = nlpAnalyzer.findSymbolsAdvanced(text);
            analyzedEmotion = nlpAnalyzer.detectEmotionAdvanced(text);

            // Дополняем базовым анализом
            List<Symbol> basicSymbols = symbolAnalyzer.findSymbolsInText(text);
            String basicEmotion = emotionDetector.detectEmotion(text);

            // Объединяем результаты
            for (Symbol symbol : basicSymbols) {
                if (!containsSymbol(analyzedSymbols, symbol)) {
                    analyzedSymbols.add(symbol);
                }
            }

            if (analyzedEmotion.equals("neutral") && !basicEmotion.equals("neutral")) {
                analyzedEmotion = basicEmotion;
            }

            runOnUiThread(() -> {
                displayAnalysisResults();
                btnReanalyze.setEnabled(true);
                btnReanalyze.setText("🔍 Переанализировать");
            });
        });
    }

    private boolean containsSymbol(List<Symbol> symbols, Symbol target) {
        for (Symbol symbol : symbols) {
            if (symbol.getKeyword().equals(target.getKeyword())) {
                return true;
            }
        }
        return false;
    }

    private void displayAnalysisResults() {
        StringBuilder result = new StringBuilder();

        result.append("Эмоция: ").append(emotionDetector.getEmotionDisplayName(analyzedEmotion)).append("\n\n");

        if (analyzedSymbols != null && !analyzedSymbols.isEmpty()) {
            result.append("Найденные символы:\n");
            for (Symbol symbol : analyzedSymbols) {
                result.append("• ").append(symbol.getKeyword()).append(": ").append(symbol.getInterpretation()).append("\n");
            }
        } else {
            result.append("Символы не найдены");
        }

        tvAnalysisResults.setText(result.toString());
    }

    private void saveDream() {
        String text = editDreamText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите текст сна", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Сохраняю...");

        executor.execute(() -> {
            currentDream.setText(text);
            currentDream.setSymbols(analyzedSymbols);
            currentDream.setEmotion(analyzedEmotion);

            dreamDao.update(currentDream);

            runOnUiThread(() -> {
                Toast.makeText(this, "Сон обновлен", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}