package com.lionido.dreams_track.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.utils.NLPAnalyzer;
import com.lionido.dreams_track.utils.OpenRouterAnalyzer;
import com.lionido.dreams_track.utils.SpeechHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordDreamActivity extends AppCompatActivity implements SpeechHelper.OnSpeechResultListener {
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    private EditText editDreamText;
    private FloatingActionButton btnRecord;
    private Button btnAnalyze;
    private Button btnSave;
    private TextView tvStatus;
    private TextView tvTranscript;
    private ProgressBar progressBar;
    private ChipGroup chipGroupInputMode;
    private Chip chipVoice, chipText;
    private LinearLayout layoutVoiceInput, layoutTextInput;

    private SpeechHelper speechHelper;
    private NLPAnalyzer nlpAnalyzer;
    private EmotionDetector emotionDetector;
    private OpenRouterAnalyzer openRouterAnalyzer;
    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;

    private String currentTranscript = "";
    private String currentInputMethod = "voice"; // "voice" или "text"
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dream_new);

        initializeHelpers();
        initializeViews();
        setupClickListeners();
        checkPermissions();
    }

    private void initializeHelpers() {
        speechHelper = new SpeechHelper(this);
        speechHelper.setOnSpeechResultListener(this);
        nlpAnalyzer = new NLPAnalyzer(this);
        emotionDetector = new EmotionDetector();
        openRouterAnalyzer = new OpenRouterAnalyzer(this);
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializeViews() {
        editDreamText = findViewById(R.id.edit_dream_text);
        btnRecord = findViewById(R.id.btn_record);
        btnAnalyze = findViewById(R.id.btn_analyze);
        btnSave = findViewById(R.id.btn_save);
        tvStatus = findViewById(R.id.tv_status);
        tvTranscript = findViewById(R.id.tv_transcript);
        progressBar = findViewById(R.id.progress_bar);
        chipGroupInputMode = findViewById(R.id.chip_group_input_mode);
        chipVoice = findViewById(R.id.chip_voice);
        chipText = findViewById(R.id.chip_text);
        layoutVoiceInput = findViewById(R.id.layout_voice_input);
        layoutTextInput = findViewById(R.id.layout_text_input);
    }

    private void setupClickListeners() {
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnAnalyze.setOnClickListener(v -> analyzeDream());
        btnSave.setOnClickListener(v -> saveDream());
        
        // Обработчик выбора режима ввода
        chipGroupInputMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(chipVoice.getId())) {
                layoutVoiceInput.setVisibility(View.VISIBLE);
                layoutTextInput.setVisibility(View.GONE);
                currentInputMethod = "voice";
            } else if (checkedIds.contains(chipText.getId())) {
                layoutVoiceInput.setVisibility(View.GONE);
                layoutTextInput.setVisibility(View.VISIBLE);
                currentInputMethod = "text";
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatus.setText("Распознавание речи недоступно");
            return;
        }

        speechHelper.startListening();
        isRecording = true;
        btnRecord.setImageResource(R.drawable.ic_mic);
        tvStatus.setText("Говорите...");
        currentInputMethod = "voice";
    }

    private void stopRecording() {
        speechHelper.stopListening();
        isRecording = false;
        btnRecord.setImageResource(R.drawable.ic_mic);
        tvStatus.setText("Обработка...");
    }

    @Override
    public void onSpeechResult(String result) {
        currentTranscript = result;
        editDreamText.setText(result);
        tvTranscript.setText(result);
        tvStatus.setText("Распознавание завершено");
        btnAnalyze.setEnabled(true);
        btnSave.setEnabled(true);
    }

    @Override
    public void onSpeechError(String error) {
        tvStatus.setText("Ошибка: " + error);
        isRecording = false;
        btnRecord.setImageResource(R.drawable.ic_mic);
    }

    @Override
    public void onSpeechReady() {
        tvStatus.setText("Готов к записи. Нажмите кнопку для начала.");
    }
    
    @Override
    public void onSpeechStarted() {
        tvStatus.setText("Запись...");
    }
    
    @Override
    public void onSpeechEnded() {
        tvStatus.setText("Обработка...");
    }

    private void analyzeDream() {
        String dreamText;
        if ("voice".equals(currentInputMethod)) {
            dreamText = currentTranscript;
        } else {
            dreamText = editDreamText.getText().toString().trim();
        }
        
        if (dreamText.isEmpty()) {
            tvStatus.setText("Введите текст сна или запишите голосом");
            return;
        }

        currentTranscript = dreamText;
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);
        tvStatus.setText("Анализ...");

        executor.execute(() -> {
            try {
                // Получаем последние 10 снов для контекста
                List<DreamEntity> recentDreams = dreamDao.getRecentDreams(10);
                StringBuilder contextBuilder = new StringBuilder();
                for (DreamEntity dream : recentDreams) {
                    contextBuilder.append("- ").append(dream.getText()).append("\n");
                }

                // Проводим локальный NLP-анализ
                List<Symbol> localSymbols = nlpAnalyzer.extractSymbols(currentTranscript);

                // Получаем результат анализа с контекстом
                OpenRouterAnalyzer.DreamAnalysisResult result = analyzeDreamWithContext(
                        currentTranscript, contextBuilder.toString(), localSymbols.size());

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                    tvStatus.setText("Анализ завершен");

                    // Отображаем результаты анализа
                    StringBuilder analysisResult = new StringBuilder();
                    analysisResult.append("Анализ сна:\n\n");

                    if (result.symbols != null && !result.symbols.isEmpty()) {
                        analysisResult.append("Найденные символы:\n");
                        for (Symbol symbol : result.symbols) {
                            analysisResult.append("- ").append(symbol.getKeyword())
                                    .append(" (").append(symbol.getSymbol()).append("): ")
                                    .append(symbol.getInterpretation()).append("\n");
                        }
                    } else {
                        analysisResult.append("Символы не найдены\n");
                    }

                    if (result.emotion != null && !result.emotion.isEmpty()) {
                        String emotionDisplay = emotionDetector.getEmotionDisplayName(result.emotion);
                        analysisResult.append("\nЭмоция: ").append(emotionDisplay).append("\n");
                    }

                    if (result.interpretation != null && !result.interpretation.isEmpty()) {
                        analysisResult.append("\nИнтерпретация:\n").append(result.interpretation).append("\n");
                    }

                    tvTranscript.setText(analysisResult.toString());
                    btnSave.setEnabled(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                    tvStatus.setText("Ошибка анализа: " + e.getMessage());
                });
            }
        });
    }

    private void saveDream() {
        String dreamText;
        if ("voice".equals(currentInputMethod)) {
            dreamText = currentTranscript;
        } else {
            dreamText = editDreamText.getText().toString().trim();
        }
        
        if (dreamText.isEmpty()) {
            tvStatus.setText("Введите текст сна или запишите голосом");
            return;
        }

        currentTranscript = dreamText;

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        tvStatus.setText("Анализ и сохранение...");

        executor.execute(() -> {
            try {
                // Получаем последние 10 снов для контекста
                List<DreamEntity> recentDreams = dreamDao.getRecentDreams(10);
                StringBuilder contextBuilder = new StringBuilder();
                for (DreamEntity dream : recentDreams) {
                    contextBuilder.append("- ").append(dream.getText()).append("\n");
                }

                // Проводим локальный NLP-анализ
                List<Symbol> localSymbols = nlpAnalyzer.extractSymbols(currentTranscript);

                // Получаем результат анализа с контекстом
                OpenRouterAnalyzer.DreamAnalysisResult result = analyzeDreamWithContext(
                        currentTranscript, contextBuilder.toString(), localSymbols.size());

                // Создаем сущность сна
                DreamEntity dream = new DreamEntity(currentTranscript, "", currentInputMethod);

                // Устанавливаем символы (из анализа с контекстом)
                if (result.symbols != null && !result.symbols.isEmpty()) {
                    dream.setSymbols(result.symbols);
                } else if (localSymbols != null && !localSymbols.isEmpty()) {
                    dream.setSymbols(localSymbols);
                }

                // Устанавливаем эмоцию
                if (result.emotion != null && !result.emotion.isEmpty()) {
                    dream.setEmotion(result.emotion);
                } else {
                    // Определяем эмоцию локально, если не получили от ИИ
                    String emotion = emotionDetector.detectEmotion(currentTranscript);
                    dream.setEmotion(emotion);
                }

                // Устанавливаем интерпретацию
                if (result.interpretation != null && !result.interpretation.isEmpty()) {
                    dream.setInterpretation(result.interpretation);
                } else {
                    // Генерируем локальную интерпретацию, если не получили от ИИ
                    String interpretation = generateLocalInterpretation(currentTranscript,
                            dream.getSymbols() != null ? dream.getSymbols() : new ArrayList<>());
                    dream.setInterpretation(interpretation);
                }

                // Устанавливаем анализ
                String analysis = generateAnalysis(currentTranscript,
                        dream.getSymbols() != null ? dream.getSymbols() : new ArrayList<>(),
                        dream.getEmotion());
                dream.setAnalysis(analysis);

                // Сохраняем сон в базе данных
                dreamDao.insert(dream);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    tvStatus.setText("Сон успешно сохранен!");
                    // Закрываем активность через 1 секунду
                    new Handler().postDelayed(this::finish, 1000);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    tvStatus.setText("Ошибка сохранения: " + e.getMessage());
                });
            }
        });
    }

    // Метод для генерации локальной интерпретации (резервный вариант)
    private String generateLocalInterpretation(String originalText, List<Symbol> symbols) {
        StringBuilder interpretation = new StringBuilder();
        interpretation.append("Локальный анализ: ");

        if (symbols != null && !symbols.isEmpty()) {
            // Если есть символы, анализируем их
            interpretation.append("Найдены следующие символы: ");
            for (int i = 0; i < Math.min(3, symbols.size()); i++) {
                Symbol symbol = symbols.get(i);
                interpretation.append(symbol.getKeyword()).append(" - ").append(symbol.getInterpretation()).append("; ");
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
        }

        // Добавляем заключение
        interpretation.append("Помните, что значение сна индивидуально и зависит от вашего личного контекста и переживаний.");
        return interpretation.toString();
    }

    // Метод для генерации анализа сна
    private String generateAnalysis(String originalText, List<Symbol> symbols, String emotion) {
        StringBuilder result = new StringBuilder();
        result.append("Анализ сна:\n\n");

        // Добавляем информацию о символах
        if (symbols != null && !symbols.isEmpty()) {
            result.append("Найденные символы:\n");
            for (Symbol symbol : symbols) {
                result.append("- ").append(symbol.getKeyword())
                        .append(" (").append(symbol.getSymbol()).append("): ")
                        .append(symbol.getInterpretation()).append("\n");
            }
        } else {
            result.append("Символы не найдены\n");
        }

        // Добавляем информацию об эмоциях
        result.append("\n");
        if (emotion != null && !emotion.isEmpty()) {
            String emotionDisplay = emotionDetector.getEmotionDisplayName(emotion);
            result.append("Эмоциональный фон: ").append(emotionDisplay).append("\n");
        } else {
            result.append("Эмоциональный фон не определен\n");
        }

        // Добавляем анализ текста
        result.append("\nАнализ текста:\n");
        String lowerText = originalText.toLowerCase();

        // Анализируем длину сна
        int wordCount = originalText.split("\\s+").length;
        result.append("- Длина сна: ").append(wordCount).append(" слов\n");

        // Анализируем ключевые темы
        if (lowerText.contains("летать") || lowerText.contains("полет") || lowerText.contains("летел")) {
            result.append("- Тема полета: указывает на стремление к свободе\n");
        }
        if (lowerText.contains("вода") || lowerText.contains("море") || lowerText.contains("река") || lowerText.contains("озеро")) {
            result.append("- Тема воды: связано с эмоциональным состоянием\n");
        }
        if (lowerText.contains("погоня") || lowerText.contains("преследуют") || lowerText.contains("преследовать")) {
            result.append("- Тема погони: может указывать на избегание чего-то\n");
        }

        return result.toString();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText("Нет разрешения на запись аудио");
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