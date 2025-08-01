package com.lionido.dreams_track.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.utils.GeminiAnalyzer;
import com.lionido.dreams_track.utils.NLPAnalyzer;
import com.lionido.dreams_track.utils.SpeechHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordDreamActivity extends AppCompatActivity implements SpeechHelper.OnSpeechResultListener {

    private static final String PREFS_NAME = "DreamTrackPrefs";
    private static final String PREF_GEMINI_API_KEY = "gemini_api_key";
    private static final String PREF_USE_GEMINI = "use_gemini";

    private ToggleButton toggleInputMode;
    private Button recordButton;
    private EditText editTextDream;
    private TextView statusText;
    private TextView transcriptText;
    private Button analyzeButton;
    private Button saveButton;
    private Button settingsButton;
    private ProgressBar progressBar;

    private SpeechHelper speechHelper;
    private NLPAnalyzer nlpAnalyzer;
    private EmotionDetector emotionDetector;
    private GeminiAnalyzer geminiAnalyzer;

    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;
    private SharedPreferences prefs;

    private String currentTranscript = "";
    private String currentInputMethod = "voice";
    private List<Symbol> analyzedSymbols;
    private String analyzedEmotion;
    private String geminiInterpretation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dream_enhanced);

        initializePreferences();
        initializeDatabase();
        initializeViews();
        initializeHelpers();
        checkGeminiSetup();
    }

    private void initializePreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializeViews() {
        toggleInputMode = findViewById(R.id.toggle_input_mode);
        recordButton = findViewById(R.id.btn_record);
        editTextDream = findViewById(R.id.edit_dream_text);
        statusText = findViewById(R.id.tv_status);
        transcriptText = findViewById(R.id.tv_transcript);
        analyzeButton = findViewById(R.id.btn_analyze);
        saveButton = findViewById(R.id.btn_save);
        settingsButton = findViewById(R.id.btn_settings);
        progressBar = findViewById(R.id.progress_bar);

        // Переключение между голосовым и текстовым вводом
        toggleInputMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToTextMode();
            } else {
                switchToVoiceMode();
            }
        });

        recordButton.setOnClickListener(v -> {
            if (recordButton.getText().equals("Начать запись")) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        // Отслеживание изменений в текстовом поле
        editTextDream.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentTranscript = s.toString();
                analyzeButton.setEnabled(!currentTranscript.trim().isEmpty());
                transcriptText.setText(currentTranscript);
            }
        });

        analyzeButton.setOnClickListener(v -> analyzeDream());
        saveButton.setOnClickListener(v -> saveDream());
        settingsButton.setOnClickListener(v -> showGeminiSettings());

        // Изначально кнопки анализа и сохранения неактивны
        analyzeButton.setEnabled(false);
        saveButton.setEnabled(false);

        // По умолчанию голосовой режим
        switchToVoiceMode();
    }

    private void initializeHelpers() {
        speechHelper = new SpeechHelper(this);
        speechHelper.setOnSpeechResultListener(this);
        nlpAnalyzer = new NLPAnalyzer(this);
        emotionDetector = new EmotionDetector();

        // Инициализация Gemini если API ключ настроен
        String apiKey = prefs.getString(PREF_GEMINI_API_KEY, "");
        if (GeminiAnalyzer.isApiKeyValid(apiKey)) {
            geminiAnalyzer = new GeminiAnalyzer(this, apiKey);
        }
    }

    private void checkGeminiSetup() {
        boolean useGemini = prefs.getBoolean(PREF_USE_GEMINI, false);
        String apiKey = prefs.getString(PREF_GEMINI_API_KEY, "");

        if (useGemini && !GeminiAnalyzer.isApiKeyValid(apiKey)) {
            showGeminiSetupDialog();
        }

        updateAnalyzeButtonText();
    }

    private void updateAnalyzeButtonText() {
        boolean useGemini = prefs.getBoolean(PREF_USE_GEMINI, false);
        String apiKey = prefs.getString(PREF_GEMINI_API_KEY, "");

        if (useGemini && GeminiAnalyzer.isApiKeyValid(apiKey)) {
            analyzeButton.setText("🤖 Анализ с ИИ");
        } else {
            analyzeButton.setText("🔍 Базовый анализ");
        }
    }

    private void switchToVoiceMode() {
        currentInputMethod = "voice";
        recordButton.setVisibility(View.VISIBLE);
        findViewById(R.id.scroll_text_input).setVisibility(View.GONE);
        findViewById(R.id.scroll_transcript).setVisibility(View.VISIBLE);
        statusText.setText("Нажмите кнопку для начала записи");
        toggleInputMode.setText("Голос");
    }

    private void switchToTextMode() {
        currentInputMethod = "text";
        recordButton.setVisibility(View.GONE);
        findViewById(R.id.scroll_text_input).setVisibility(View.VISIBLE);
        findViewById(R.id.scroll_transcript).setVisibility(View.VISIBLE);
        statusText.setText("Введите текст сна");
        toggleInputMode.setText("Текст");
        editTextDream.requestFocus();
    }

    private void startRecording() {
        recordButton.setText("Остановить запись");
        statusText.setText("Говорите...");
        speechHelper.startListening();
    }

    private void stopRecording() {
        recordButton.setText("Начать запись");
        statusText.setText("Запись остановлена");
        speechHelper.stopListening();
    }

    @Override
    public void onSpeechResult(String text) {
        currentTranscript = text;
        transcriptText.setText(text);
        statusText.setText("Речь распознана");
        analyzeButton.setEnabled(true);
    }

    @Override
    public void onSpeechError(String error) {
        statusText.setText("Ошибка: " + error);
        recordButton.setText("Начать запись");
    }

    @Override
    public void onSpeechReady() {
        statusText.setText("Готов к записи");
    }

    @Override
    public void onSpeechStarted() {
        statusText.setText("Запись...");
    }

    @Override
    public void onSpeechEnded() {
        statusText.setText("Запись завершена");
    }

    private void analyzeDream() {
        if (currentTranscript.trim().isEmpty()) {
            Toast.makeText(this, "Введите или запишите сон", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean useGemini = prefs.getBoolean(PREF_USE_GEMINI, false);
        String apiKey = prefs.getString(PREF_GEMINI_API_KEY, "");

        if (useGemini && GeminiAnalyzer.isApiKeyValid(apiKey) && geminiAnalyzer != null) {
            analyzeWithGemini();
        } else {
            analyzeWithLocalNLP();
        }
    }

    private void analyzeWithGemini() {
        statusText.setText("🤖 ИИ анализирует сон...");
        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        geminiAnalyzer.analyzeDreamAsync(currentTranscript, new GeminiAnalyzer.OnAnalysisCompleteListener() {
            @Override
            public void onSuccess(List<Symbol> symbols, String emotion, String interpretation) {
                runOnUiThread(() -> {
                    analyzedSymbols = symbols;
                    analyzedEmotion = emotion;
                    geminiInterpretation = interpretation;

                    showGeminiAnalysisResults();

                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    statusText.setText("✨ Анализ ИИ завершен");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    statusText.setText("❌ Ошибка ИИ, используется базовый анализ");

                    // Fallback к локальному анализу
                    analyzeWithLocalNLP();
                });
            }
        });
    }

    private void analyzeWithLocalNLP() {
        statusText.setText("🔍 Анализирую сон...");
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            // Локальный анализ
            analyzedSymbols = nlpAnalyzer.findSymbolsAdvanced(currentTranscript);
            analyzedEmotion = nlpAnalyzer.detectEmotionAdvanced(currentTranscript);
            geminiInterpretation = ""; // Нет интерпретации от ИИ

            runOnUiThread(() -> {
                showLocalAnalysisResults();
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                statusText.setText("📊 Базовый анализ завершен");
            });
        });
    }

    private void showGeminiAnalysisResults() {
        StringBuilder result = new StringBuilder();
        result.append("🤖 Анализ ИИ\n\n");
        result.append("📝 Текст сна:\n").append(currentTranscript).append("\n\n");

        result.append("😊 Эмоция: ").append(emotionDetector.getEmotionDisplayName(analyzedEmotion)).append("\n\n");

        if (analyzedSymbols != null && !analyzedSymbols.isEmpty()) {
            result.append("🔮 Найденные символы:\n");
            for (Symbol symbol : analyzedSymbols) {
                result.append("• ").append(symbol.getKeyword()).append(": ").append(symbol.getInterpretation()).append("\n");
            }
            result.append("\n");
        }

        if (!geminiInterpretation.isEmpty()) {
            result.append("🧠 Общая интерпретация:\n").append(geminiInterpretation);
        }

        transcriptText.setText(result.toString());
    }

    private void showLocalAnalysisResults() {
        StringBuilder result = new StringBuilder();
        result.append("📊 Базовый анализ\n\n");
        result.append("📝 Текст сна:\n").append(currentTranscript).append("\n\n");

        result.append("😊 Эмоция: ").append(emotionDetector.getEmotionDisplayName(analyzedEmotion)).append("\n\n");

        if (analyzedSymbols != null && !analyzedSymbols.isEmpty()) {
            result.append("🔍 Найденные символы:\n");
            for (Symbol symbol : analyzedSymbols) {
                result.append("• ").append(symbol.getKeyword()).append(": ").append(symbol.getInterpretation()).append("\n");
            }
        } else {
            result.append("Символы не найдены");
        }

        transcriptText.setText(result.toString());
    }

    private void showGeminiSettings() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gemini_settings, null);
        EditText editApiKey = dialogView.findViewById(R.id.edit_api_key);
        TextView tvInfo = dialogView.findViewById(R.id.tv_gemini_info);

        editApiKey.setText(prefs.getString(PREF_GEMINI_API_KEY, ""));
        tvInfo.setText(GeminiAnalyzer.getUsageInfo());

        new AlertDialog.Builder(this)
                .setTitle("🤖 Настройки ИИ анализа")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String apiKey = editApiKey.getText().toString().trim();
                    prefs.edit()
                            .putString(PREF_GEMINI_API_KEY, apiKey)
                            .putBoolean(PREF_USE_GEMINI, GeminiAnalyzer.isApiKeyValid(apiKey))
                            .apply();

                    if (GeminiAnalyzer.isApiKeyValid(apiKey)) {
                        geminiAnalyzer = new GeminiAnalyzer(this, apiKey);
                        Toast.makeText(this, "✅ Gemini ИИ активирован!", Toast.LENGTH_SHORT).show();
                    } else {
                        geminiAnalyzer = null;
                        Toast.makeText(this, "❌ Неверный API ключ", Toast.LENGTH_SHORT).show();
                    }
                    updateAnalyzeButtonText();
                })
                .setNegativeButton("Отмена", null)
                .setNeutralButton("Как получить ключ?", (dialog, which) -> showApiKeyHelp())
                .show();
    }

    private void showGeminiSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🤖 Улучшенный анализ с ИИ")
                .setMessage("Хотите использовать Google Gemini для более точного анализа снов?\n\n" +
                        "✨ Преимущества:\n" +
                        "• Более точный анализ символов\n" +
                        "• Психологическая интерпретация\n" +
                        "• Учет контекста и нюансов\n\n" +
                        "🆓 Полностью бесплатно!")
                .setPositiveButton("Настроить", (dialog, which) -> showGeminiSettings())
                .setNegativeButton("Позже", null)
                .show();
    }

    private void showApiKeyHelp() {
        new AlertDialog.Builder(this)
                .setTitle("🔑 Как получить API ключ")
                .setMessage("1. Перейдите на https://makersuite.google.com/app/apikey\n\n" +
                        "2. Войдите в Google аккаунт\n\n" +
                        "3. Нажмите 'Create API key'\n\n" +
                        "4. Скопируйте ключ и вставьте в поле\n\n" +
                        "⚠️ Ключ должен начинаться с 'AIza'")
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void saveDream() {
        if (currentTranscript.trim().isEmpty()) {
            Toast.makeText(this, "Нет данных для сохранения", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText("💾 Сохраняю сон...");

        executor.execute(() -> {
            DreamEntity dreamEntity = new DreamEntity(currentTranscript, null, currentInputMethod);
            dreamEntity.setSymbols(analyzedSymbols);
            dreamEntity.setEmotion(analyzedEmotion);

            long dreamId = dreamDao.insert(dreamEntity);

            runOnUiThread(() -> {
                if (dreamId > 0) {
                    Toast.makeText(this, "✅ Сон сохранен", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "❌ Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    statusText.setText("Ошибка сохранения");
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}