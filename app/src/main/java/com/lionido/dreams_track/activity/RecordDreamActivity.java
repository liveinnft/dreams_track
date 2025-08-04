package com.lionido.dreams_track.activity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.SpeechHelper;
import com.lionido.dreams_track.utils.NLPAnalyzer;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.utils.OpenRouterAnalyzer;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RecordDreamActivity extends AppCompatActivity implements SpeechHelper.OnSpeechResultListener {
    private static final String PREFS_NAME = "DreamPrefs";
    private static final String PREF_GEMINI_API_KEY = "gemini_api_key";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    private SpeechHelper speechHelper;
    private NLPAnalyzer nlpAnalyzer;
    private EmotionDetector emotionDetector;
    private OpenRouterAnalyzer openRouterAnalyzer;
    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;

    private SharedPreferences prefs;
    private String currentInputMethod = "voice";
    private String currentTranscript = "";

    // UI элементы
    private MaterialToolbar toolbar;
    private Chip chipVoice;
    private Chip chipText;
    private LinearLayout layoutVoiceInput;
    private LinearLayout layoutTextInput;
    private TextView statusText;
    private FloatingActionButton recordButton;
    private EditText editTextDream;
    private TextView transcriptText;
    private Button analyzeButton;
    private Button saveButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dream_new); // Используем новый layout

        initializePreferences();
        initializeDatabase();
        initializeViews();
        initializeHelpers();
        checkGeminiSetup();
        
        // Проверка разрешений
        checkAudioPermission();
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        chipVoice = findViewById(R.id.chip_voice);
        chipText = findViewById(R.id.chip_text);
        layoutVoiceInput = findViewById(R.id.layout_voice_input);
        layoutTextInput = findViewById(R.id.layout_text_input);
        statusText = findViewById(R.id.tv_status);
        recordButton = findViewById(R.id.btn_record);
        editTextDream = findViewById(R.id.edit_dream_text);
        transcriptText = findViewById(R.id.tv_transcript);
        analyzeButton = findViewById(R.id.btn_analyze);
        saveButton = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);

        // Установка обработчиков событий
        chipVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToVoiceMode();
            }
        });

        chipText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchToTextMode();
            }
        });

        recordButton.setOnClickListener(v -> {
            if (statusText.getText().equals(getString(R.string.status_ready)) || 
                statusText.getText().equals(getString(R.string.status_processing))) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        editTextDream.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                currentTranscript = s.toString();
                analyzeButton.setEnabled(!currentTranscript.trim().isEmpty());
                transcriptText.setText(currentTranscript);
            }
        });

        analyzeButton.setOnClickListener(v -> analyzeDream());
        saveButton.setOnClickListener(v -> saveDream());

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

        // Инициализация OpenRouterAnalyzer
        openRouterAnalyzer = new OpenRouterAnalyzer(this);
    }

    private void checkGeminiSetup() {
        // OpenRouter не требует проверки ключа, так как использует встроенный ключ    }
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        }
    }

    private void switchToVoiceMode() {
        currentInputMethod = "voice";
        layoutVoiceInput.setVisibility(View.VISIBLE);
        layoutTextInput.setVisibility(View.GONE);
        statusText.setText(R.string.status_ready);
    }

    private void switchToTextMode() {
        currentInputMethod = "text";
        layoutVoiceInput.setVisibility(View.GONE);
        layoutTextInput.setVisibility(View.VISIBLE);
        statusText.setText(R.string.describe_your_dream);
        editTextDream.requestFocus();
    }

    private void startRecording() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechHelper.startListening();
            statusText.setText(R.string.status_recording);
            recordButton.setImageResource(R.drawable.ic_mic);
        } else {
            statusText.setText("Распознавание речи недоступно");
        }
    }

    private void stopRecording() {
        speechHelper.stopListening();
        statusText.setText(R.string.status_processing);
        recordButton.setImageResource(R.drawable.ic_mic);
    }

    @Override
    public void onSpeechResult(String result) {
        currentTranscript = result;
        transcriptText.setText(result);
        analyzeButton.setEnabled(!result.trim().isEmpty());
        statusText.setText(R.string.status_ready);
    }

    @Override
    public void onSpeechError(String error) {
        statusText.setText("Ошибка: " + error);
    }
    
    @Override
    public void onSpeechReady() {
        // Готовность к записи
    }
    
    @Override
    public void onSpeechStarted() {
        // Начало записи
    }
    
    @Override
    public void onSpeechEnded() {
        // Окончание записи
    }

    private void analyzeDream() {
        if (currentTranscript.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);

        executor.execute(() -> {
            try {
                // Получаем последние 10 снов для контекста
                List<DreamEntity> recentDreams = dreamDao.getAllDreams().stream()
                        .limit(10)
                        .collect(Collectors.toList());
                
                // Анализ символов с помощью NLP
                List<Symbol> localSymbols = nlpAnalyzer.findSymbolsAdvanced(currentTranscript);
                
                // Определение эмоции локальным детектором
                String localEmotion = emotionDetector.detectEmotion(currentTranscript);
                
                // Получение интерпретации от OpenRouter (если доступно)
                String openRouterInterpretation = "";
                List<Symbol> openRouterSymbols = new ArrayList<>();
                String openRouterEmotion = "neutral";
                
                if (openRouterAnalyzer != null) {
                    Log.d("RecordDreamActivity", "OpenRouter API инициализирован, начинаем анализ");
                    // Создаем промпт с контекстом последних снов
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append("Прошлые сны:\n");
                    for (int i = 0; i < Math.min(5, recentDreams.size()); i++) {
                        DreamEntity dream = recentDreams.get(i);
                        contextBuilder.append((i + 1)).append(". ").append(dream.getText()).append("\n");
                    }
                    
                    // Используем асинхронный метод анализа с контекстом
                    OpenRouterAnalyzer.DreamAnalysisResult result = analyzeDreamWithContext(currentTranscript, contextBuilder.toString(), localSymbols.size());
                    if (result != null) {
                        openRouterInterpretation = result.interpretation;
                        openRouterSymbols = result.symbols;
                        openRouterEmotion = result.emotion;
                        Log.d("RecordDreamActivity", "Анализ OpenRouter завершен. Найдено символов: " + openRouterSymbols.size());
                    } else {
                        Log.e("RecordDreamActivity", "Результат анализа OpenRouter равен null");
                        openRouterInterpretation = "Интерпретация недоступна";
                        openRouterSymbols = new ArrayList<>();
                        openRouterEmotion = "neutral";
                    }
                } else {
                    Log.d("RecordDreamActivity", "OpenRouter API не инициализирован");
                    openRouterInterpretation = "Интерпретация недоступна";
                    openRouterSymbols = new ArrayList<>();
                    openRouterEmotion = "neutral";
                }
                
                // Комбинируем символы из локального анализа и OpenRouter
                List<Symbol> combinedSymbols = new ArrayList<>(localSymbols);
                for (Symbol openRouterSymbol : openRouterSymbols) {
                    // Добавляем символы из OpenRouter, которых нет в локальном анализе
                    boolean exists = false;
                    for (Symbol localSymbol : localSymbols) {
                        if (localSymbol.getKeyword().equals(openRouterSymbol.getKeyword())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        combinedSymbols.add(openRouterSymbol);
                    }
                }

                String finalOpenRouterInterpretation = openRouterInterpretation;
                List<Symbol> finalSymbols = combinedSymbols;
                String finalEmotion = !openRouterEmotion.equals("neutral") ? openRouterEmotion : localEmotion;
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    
                    // Обновляем UI с результатами анализа
                    if (finalOpenRouterInterpretation != null && !finalOpenRouterInterpretation.isEmpty()) {
                        updateUIWithAnalysisResults(finalSymbols, finalEmotion, finalOpenRouterInterpretation, currentTranscript);
                    } else {
                        updateUIWithAnalysisResults(finalSymbols, finalEmotion, "Не удалось получить интерпретацию сна", currentTranscript);
                    }
                });
            } catch (Exception e) {
                Log.e("RecordDreamActivity", "Ошибка анализа сна", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    statusText.setText("Ошибка анализа: " + e.getMessage());
                });
            }
        });
    }

    // Метод для обновления UI с результатами анализа
    private void updateUIWithAnalysisResults(List<Symbol> symbols, String emotion, String interpretation, String originalText) {
        // Показываем результаты в transcriptText
        StringBuilder result = new StringBuilder();
        result.append("Анализ сна:\n\n");
        
        result.append("Эмоциональный фон: ").append(getEmotionDisplayName(emotion)).append("\n\n");
        
        result.append("Найденные символы:\n");
        if (symbols.isEmpty()) {
            result.append("  Символы не найдены\n");
        } else {
            for (Symbol symbol : symbols) {
                result.append("  • ").append(symbol.getKeyword()).append(": ").append(symbol.getInterpretation()).append("\n");
            }
        }
        
        result.append("\n");
        
        if (!interpretation.isEmpty() && !interpretation.equals("Интерпретация недоступна") && !interpretation.equals("Анализ выполнен базовыми алгоритмами.")) {
            result.append("Интерпретация: ").append(interpretation).append("\n");
        } else {
            // Создаем базовую интерпретацию на основе найденных символов
            result.append("Интерпретация: ").append(createBasicInterpretation(symbols, emotion, originalText)).append("\n");
        }
        
        transcriptText.setText(result.toString());
    }
    
    // Метод для получения отображаемого имени эмоции
    private String getEmotionDisplayName(String emotionCode) {
        switch (emotionCode) {
            case "fear": return "Страх";
            case "joy": return "Радость";
            case "sadness": return "Грусть";
            case "anger": return "Гнев";
            case "surprise": return "Удивление";
            case "calm": return "Спокойствие";
            case "love": return "Любовь";
            case "shame": return "Стыд";
            case "despair": return "Отчаяние";
            default: return "Нейтральное";
        }
    }
    
    // Метод для создания базовой интерпретации на основе найденных символов
    private String createBasicInterpretation(List<Symbol> symbols, String emotion, String originalText) {
        StringBuilder interpretation = new StringBuilder();
        
        if (!symbols.isEmpty()) {
            // Если найдены символы, создаем интерпретацию на основе них
            interpretation.append("В вашем сне присутствуют следующие ключевые символы: ");
            for (int i = 0; i < Math.min(3, symbols.size()); i++) {
                if (i > 0) interpretation.append(", ");
                interpretation.append(symbols.get(i).getKeyword());
            }
            interpretation.append(". ");
            
            // Анализируем преобладающую эмоцию
            switch (emotion) {
                case "fear":
                    interpretation.append("Это может указывать на ваши тревоги и страхи в реальной жизни. ");
                    break;
                case "joy":
                    interpretation.append("Это отражает ваше внутреннее спокойствие и удовлетворенность жизнью. ");
                    break;
                case "sadness":
                    interpretation.append("Это может быть связано с переживаниями утраты или неудовлетворенности. ");
                    break;
                case "anger":
                    interpretation.append("Это может указывать на внутреннее напряжение или подавленное раздражение. ");
                    break;
                case "surprise":
                    interpretation.append("Это может отражать неожиданные события или перемены в вашей жизни. ");
                    break;
                case "calm":
                    interpretation.append("Это указывает на ваше стремление к гармонии и внутреннему балансу. ");
                    break;
                case "love":
                    interpretation.append("Это отражает потребность в любви, близости или эмоциональной связи. ");
                    break;
                case "shame":
                    interpretation.append("Это может быть связано с чувством вины или стыда за что-то. ");
                    break;
                case "despair":
                    interpretation.append("Это может указывать на чувство безысходности или глубокую утрату. ");
                    break;
                default:
                    interpretation.append("Эмоциональный фон сна требует вашего личного осмысления. ");
                    break;
            }
            
            // Добавляем анализ каждого символа
            interpretation.append("В частности: ");
            for (int i = 0; i < Math.min(2, symbols.size()); i++) {
                Symbol symbol = symbols.get(i);
                interpretation.append(symbol.getKeyword()).append(" - ").append(symbol.getInterpretation().toLowerCase()).append("; ");
            }
        } else {
            // Если символы не найдены, анализируем по ключевым словам в тексте
            String lowerText = originalText.toLowerCase();
            
            // Анализируем ключевые слова и создаем интерпретацию
            if (lowerText.contains("летать") || lowerText.contains("полет") || lowerText.contains("летел")) {
                interpretation.append("Сон о полете часто указывает на стремление к свободе, независимости или желание выйти за рамки текущих ограничений. ");
            } else if (lowerText.contains("падать") || lowerText.contains("падение") || lowerText.contains("упал")) {
                interpretation.append("Сны о падении могут отражать чувство неуверенности, потери контроля или тревогу по поводу неудачи. ");
            } else if (lowerText.contains("вода") || lowerText.contains("море") || lowerText.contains("река") || lowerText.contains("озеро")) {
                interpretation.append("Вода в снах символизирует эмоции и подсознание. Спокойная вода указывает на внутреннюю гармонию, а бурная - на эмоциональные потрясения. ");
            } else if (lowerText.contains("огонь") || lowerText.contains("гореть") || lowerText.contains("пламя")) {
                interpretation.append("Огонь может символизировать страсть, разрушение или очищение. Возможно, в вашей жизни происходят важные перемены. ");
            } else if (lowerText.contains("деньги") || lowerText.contains("денег") || lowerText.contains("заработ") || lowerText.contains("платить")) {
                interpretation.append("Сны о деньгах могут отражать ваши заботы о материальном благополучии или чувство собственной ценности. ");
            } else if (lowerText.contains("зубы") || lowerText.contains("зуб")) {
                interpretation.append("Зубы в снах часто связаны со страхом старения, потери привлекательности или чувства уязвимости. ");
            } else if (lowerText.contains("экзамен") || lowerText.contains("сдавать") || lowerText.contains("учиться") || lowerText.contains("учеба")) {
                interpretation.append("Сны об экзамене отражают стресс, связанный с проверкой знаний или способностей, а также страх не оправдать ожиданий. ");
            } else if (lowerText.contains("смерть") || lowerText.contains("умер") || lowerText.contains("умирает")) {
                interpretation.append("Смерть в снах редко предвещает реальную смерть. Чаще это символ окончания этапа жизни и начала нового. ");
            } else if (lowerText.contains("преследуют") || lowerText.contains("преследовать") || lowerText.contains("бегать")) {
                interpretation.append("Сны о погоне могут указывать на избегание какой-то ситуации или проблемы в реальной жизни. ");
            } else if (lowerText.contains("потеря") || lowerText.contains("потерять") || lowerText.contains("искать")) {
                interpretation.append("Сны о потере или поиске чего-то могут отражать чувство нехватки чего-то важного в жизни. ");
            } else {
                // Если не найдено специфических ключевых слов, даем общий анализ
                interpretation.append("В вашем сне не выделены явные символы, но обратите внимание на общее эмоциональное состояние, которое вы испытывали во сне. ");
                interpretation.append("Попробуйте вспомнить, какие события предшествовали сну и какие чувства он вызвал. ");
            }
            
            // Добавляем общие рекомендации
            interpretation.append("Старайтесь вспомнить свои чувства во сне - они могут дать важные подсказки о его значении. ");
        }
        
        // Добавляем заключение
        interpretation.append("Помните, что значение сна индивидуально и зависит от вашего личного контекста и переживаний.");
        return interpretation.toString();
    }

    // Метод для анализа сна с контекстом последних снов
    private OpenRouterAnalyzer.DreamAnalysisResult analyzeDreamWithContext(String dreamText, String context, int localSymbolsCount) {
        if (openRouterAnalyzer != null) {
            try {
                // Создаем специальный промпт для анализа с контекстом
                String additionalInstruction = "";
                if (localSymbolsCount < 3) {
                    int symbolsNeeded = 3 - localSymbolsCount;
                    additionalInstruction = "ВАЖНО: В локальном анализе найдено только " + localSymbolsCount + " символа(ов). " +
                            "Обязательно найди еще " + symbolsNeeded + " символа(ов) из текста сна и добавь их в результат. " +
                            "Общее количество символов должно быть не менее 3.\n\n";
                }
                
                String prompt = "Проанализируй следующий сон с учетом контекста прошлых снов:\n\n" +
                        "Контекст прошлых снов:\n" + context + "\n\n" +
                        "Текущий сон для анализа: \"" + dreamText + "\"\n\n" +
                        additionalInstruction +
                        "Найди символы, которые есть в файле dream_symbols.json, а также дополнительные символы, " +
                        "которых там может не быть, но которые важны для понимания сна.\n\n" +
                        "Если слово отсутствует в dream_symbols.json, найди его в тексте и дай объяснение сна в 1-2 предложениях.\n\n" +
                        "ДАЖЕ ЕСЛИ В СНЕ НЕ УПОМИНАЮТСЯ ЧУВСТВА, ПОПЫТАЙСЯ ПРЕДПОЛОЖИТЬ, ПОЧЕМУ ЧЕЛОВЕКУ ПРИСНИЛСЯ ЭТОТ СОН.\n\n" +
                        "УЧИТЫВАЙ ВОЗМОЖНЫЕ СВЯЗИ С РЕАЛЬНОЙ ЖИЗНЬЮ ЧЕЛОВЕКА И КОНТЕКСТ ПРОШЛЫХ СНОВ.\n\n" +
                        "Верни результат СТРОГО в формате JSON:\n" +
                        "{\n" +
                        "  \"symbols\": [\n" +
                        "    {\n" +
                        "      \"keyword\": \"ключевое_слово_из_сна_на_русском\",\n" +
                        "      \"symbol\": \"символ_на_английском\",\n" +
                        "      \"interpretation\": \"подробная_интерпретация_на_русском_языке\",\n" +
                        "      \"emotion\": \"одна_из_эмоций_fear_joy_sadness_anger_surprise_calm_love_shame_despair_neutral\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"dominant_emotion\": \"одна_из_эмоций_fear_joy_sadness_anger_surprise_calm_love_shame_despair_neutral\",\n" +
                        "  \"interpretation\": \"общая_интерпретация_сна_на_русском_языке_в_2_3_предложениях_с_предположением_о_причинах_сна\"\n" +
                        "}\n\n" +
                        "ВАЖНО:\n" +
                        "- Ответ должен содержать ТОЛЬКО JSON структуру\n" +
                        "- Все поля обязательны к заполнению\n" +
                        "- Не используйте код и пояснения\n" +
                        "- Используй двойные кавычки для строк\n" +
                        "- В поле interpretation ОБЯЗАТЕЛЬНО включи предположение о причинах сна, даже если чувства не упомянуты\n" +
                        "- В интерпретации указывай возможные связи с реальной жизнью человека и контекстом прошлых снов\n";
                
                // Отправляем запрос напрямую через OpenRouterAnalyzer
                String response = openRouterAnalyzer.sendOpenRouterRequest(prompt);
                openRouterAnalyzer.logOpenRouterInteraction("Анализ сна с контекстом", prompt, response);
                return openRouterAnalyzer.parseOpenRouterResponse(response);
            } catch (Exception e) {
                // В случае ошибки используем резервный анализ
                openRouterAnalyzer.logOpenRouterInteraction("Ошибка анализа с контекстом", 
                    "dreamText: " + dreamText + ", context: " + context, 
                    "error: " + e.getMessage());
                return openRouterAnalyzer.getFallbackAnalysis(dreamText);
            }
        } else {
            // Если OpenRouter не доступен, используем резервный анализ
            OpenRouterAnalyzer.DreamAnalysisResult result = new OpenRouterAnalyzer.DreamAnalysisResult();
            result.symbols = new ArrayList<>();
            result.emotion = "neutral";
            result.interpretation = "Анализ выполнен базовыми алгоритмами.";
            return result;
        }
    }

    private void saveDream() {
        if (currentTranscript.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        executor.execute(() -> {
            try {
                DreamEntity dream = new DreamEntity(currentTranscript, "", currentInputMethod);
                dreamDao.insert(dream);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Показать сообщение об успешном сохранении
                    // finish(); // Закрыть активность и вернуться к предыдущей
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    statusText.setText("Ошибка сохранения: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Нет разрешения на запись аудио");
            }
        }
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