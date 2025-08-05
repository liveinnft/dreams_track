package com.lionido.dreams_track.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.lionido.dreams_track.BaseActivity;
import com.lionido.dreams_track.R;

import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.model.Dream;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.OpenRouterAnalyzer;
import com.lionido.dreams_track.utils.SpeechHelper;
import com.lionido.dreams_track.utils.SymbolAnalyzer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordDreamActivity extends BaseActivity {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 100;

    // UI элементы
    private ChipGroup chipGroupInputMode;
    private Chip chipVoice, chipText;
    private MaterialCardView layoutVoiceInput, layoutTextInput;
    private ImageButton btnRecord;
    private Button btnAnalyze, btnSave;
    private LinearLayout progressBar;
    private TextView tvStatus;
    private TextInputEditText editDreamText;

    // Карточки результатов
    private MaterialCardView cardTranscript, cardSymbols, cardEmotion,
            cardInterpretation, cardPersonalGrowth, cardActionableAdvice;

    // TextView для результатов
    private TextView tvTranscript, tvEmotionIcon, tvEmotionName, tvEmotionDescription,
            tvInterpretation, tvPersonalGrowth, tvActionableAdvice;

    // ChipGroup для символов
    private ChipGroup chipGroupSymbols;

    // Утилиты
    private SpeechHelper speechHelper;
    private OpenRouterAnalyzer openRouterAnalyzer;

    // Данные
    private String currentDreamText = "";
    private List<Symbol> currentSymbols = new ArrayList<>();
    private String currentEmotion = "neutral";
    private String currentInterpretation = "";
    private String currentPersonalGrowth = "";
    private String currentActionableAdvice = "";

    private boolean isRecording = false;
    private boolean isVoiceMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dream_new);

        initializeViews();
        initializeUtils();
        setupListeners();
        setupChipGroup();
        checkPermissions();
    }

    private void initializeViews() {
        // Режим ввода
        chipGroupInputMode = findViewById(R.id.chip_group_input_method);
        chipVoice = findViewById(R.id.chip_voice);
        chipText = findViewById(R.id.chip_text);

        // Области ввода
        layoutVoiceInput = findViewById(R.id.card_voice_input);
        layoutTextInput = findViewById(R.id.card_text_input);
        btnRecord = findViewById(R.id.btn_voice_record);
        tvStatus = findViewById(R.id.tv_voice_status);
        editDreamText = findViewById(R.id.et_dream_text);

        // Кнопки действий
        btnAnalyze = findViewById(R.id.btn_edit_transcript);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.layout_analysis_progress);

        // Карточки результатов
        cardTranscript = findViewById(R.id.card_transcript);
        cardSymbols = findViewById(R.id.card_analysis);
        cardEmotion = findViewById(R.id.card_analysis);
        cardInterpretation = findViewById(R.id.card_analysis);
        cardPersonalGrowth = findViewById(R.id.card_analysis);
        cardActionableAdvice = findViewById(R.id.card_analysis);

        // TextView результатов
        tvTranscript = findViewById(R.id.tv_transcript);
        tvEmotionIcon = findViewById(R.id.tv_emotion);
        tvEmotionName = findViewById(R.id.tv_emotion);
        tvEmotionDescription = findViewById(R.id.tv_emotion);
        tvInterpretation = findViewById(R.id.tv_interpretation);
        tvPersonalGrowth = findViewById(R.id.tv_interpretation);
        tvActionableAdvice = findViewById(R.id.tv_interpretation);

        // ChipGroup для символов
        chipGroupSymbols = findViewById(R.id.chip_group_symbols);
    }

    private void initializeUtils() {
        speechHelper = new SpeechHelper(this);
        openRouterAnalyzer = new OpenRouterAnalyzer(this);
    }

    private void setupChipGroup() {
        // Инициализация ChipGroup для символов
        // Символы будут добавляться динамически при анализе
    }

    private void setupListeners() {
        // Переключение режима ввода
        chipGroupInputMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_voice)) {
                switchToVoiceMode();
            } else if (checkedIds.contains(R.id.chip_text)) {
                switchToTextMode();
            }
        });

        // Кнопка записи
        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        // Кнопка анализа
        btnAnalyze.setOnClickListener(v -> {
            String dreamText = isVoiceMode ? currentDreamText : editDreamText.getText().toString().trim();
            if (!dreamText.isEmpty()) {
                analyzeDream(dreamText);
            } else {
                Toast.makeText(this, "Сначала введите текст сна", Toast.LENGTH_SHORT).show();
            }
        });

        // Кнопка сохранения
        btnSave.setOnClickListener(v -> saveDream());

        // Обработчики SpeechHelper
        speechHelper.setOnSpeechResultListener(new SpeechHelper.OnSpeechResultListener() {
            @Override
            public void onSpeechResult(String text) {
                currentDreamText = text;
                showTranscript(text);
                btnAnalyze.setEnabled(true);
                tvStatus.setText("Запись завершена. Текст распознан.");
            }

            @Override
            public void onSpeechError(String error) {
                tvStatus.setText("Ошибка распознавания: " + error);
                isRecording = false;
                updateRecordButton();
            }

            @Override
            public void onSpeechReady() {
                tvStatus.setText("Говорите...");
            }

            @Override
            public void onSpeechStarted() {
                tvStatus.setText("Запись...");
            }
            
            @Override
            public void onSpeechEnded() {
                tvStatus.setText("Обработка речи...");
                isRecording = false;
                updateRecordButton();
            }
        });
    }

    private void switchToVoiceMode() {
        isVoiceMode = true;
        layoutVoiceInput.setVisibility(View.VISIBLE);
        layoutTextInput.setVisibility(View.GONE);
        btnAnalyze.setEnabled(!currentDreamText.isEmpty());
    }

    private void switchToTextMode() {
        isVoiceMode = false;
        layoutVoiceInput.setVisibility(View.GONE);
        layoutTextInput.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(!editDreamText.getText().toString().trim().isEmpty());

        // Добавляем слушатель для текстового поля
        editDreamText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnAnalyze.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
            return;
        }

        isRecording = true;
        updateRecordButton();
        tvStatus.setText("Подготовка к записи...");
        speechHelper.startListening();
    }

    private void stopRecording() {
        isRecording = false;
        updateRecordButton();
        speechHelper.stopListening();
        tvStatus.setText("Остановка записи...");
    }

    private void updateRecordButton() {
        if (isRecording) {
            btnRecord.setImageResource(R.drawable.ic_stop);
            btnRecord.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.error_dark));
        } else {
            btnRecord.setImageResource(R.drawable.ic_mic);
            btnRecord.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.accent_dark));
        }
    }

    private void showTranscript(String text) {
        tvTranscript.setText(text);
        cardTranscript.setVisibility(View.VISIBLE);

        // Плавная анимация появления карточки
        cardTranscript.setAlpha(0f);
        cardTranscript.animate().alpha(1f).setDuration(300).start();
    }

    private void analyzeDream(String dreamText) {
        showProgress(true);
        hideResultCards();

        openRouterAnalyzer.analyzeDreamAsync(dreamText,
                new OpenRouterAnalyzer.OnAnalysisCompleteListener() {
                    @Override
                    public void onSuccess(List<Symbol> symbols, String emotion, String interpretation,
                                          String personalGrowth, String actionableAdvice) {
                        runOnUiThread(() -> {
                            showProgress(false);
                            displayAnalysisResults(symbols, emotion, interpretation,
                                    personalGrowth, actionableAdvice);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showProgress(false);
                            Toast.makeText(RecordDreamActivity.this,
                                    "Ошибка анализа: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void displayAnalysisResults(List<Symbol> symbols, String emotion, String interpretation,
                                        String personalGrowth, String actionableAdvice) {
        // Сохраняем результаты
        currentSymbols = symbols;
        currentEmotion = emotion;
        currentInterpretation = interpretation;
        currentPersonalGrowth = personalGrowth;
        currentActionableAdvice = actionableAdvice;

        // Показываем символы
        if (symbols != null && !symbols.isEmpty()) {
            displaySymbols(symbols);
            showCardWithAnimation(cardSymbols);
        }

        // Показываем эмоцию
        displayEmotion(emotion);
        showCardWithAnimation(cardEmotion);

        // Показываем интерпретацию
        if (interpretation != null && !interpretation.isEmpty()) {
            tvInterpretation.setText(interpretation);
            showCardWithAnimation(cardInterpretation);
        }

        // Показываем личностный рост
        if (personalGrowth != null && !personalGrowth.isEmpty()) {
            tvPersonalGrowth.setText(personalGrowth);
            showCardWithAnimation(cardPersonalGrowth);
        }

        // Показываем практические рекомендации
        if (actionableAdvice != null && !actionableAdvice.isEmpty()) {
            tvActionableAdvice.setText(actionableAdvice);
            showCardWithAnimation(cardActionableAdvice);
        }

        // Активируем кнопку сохранения
        btnSave.setEnabled(true);
    }

    private void displaySymbols(List<Symbol> symbols) {
        chipGroupSymbols.removeAllViews();
        for (Symbol symbol : symbols) {
            Chip chip = new Chip(this);
            chip.setText(symbol.getName());
            chip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
            chip.setTextColor(getResources().getColor(android.R.color.white));
            chipGroupSymbols.addView(chip);
        }
    }

    private void displayEmotion(String emotion) {
        String emotionIcon = getEmotionIcon(emotion);
        String emotionName = getEmotionDisplayName(emotion);
        String emotionDescription = getEmotionDescription(emotion);

        tvEmotionIcon.setText(emotionIcon);
        tvEmotionName.setText(emotionName);
        tvEmotionDescription.setText(emotionDescription);
    }

    private void showCardWithAnimation(MaterialCardView card) {
        card.setVisibility(View.VISIBLE);
        card.setAlpha(0f);
        card.setTranslationY(50f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void hideResultCards() {
        cardSymbols.setVisibility(View.GONE);
        cardEmotion.setVisibility(View.GONE);
        cardInterpretation.setVisibility(View.GONE);
        cardPersonalGrowth.setVisibility(View.GONE);
        cardActionableAdvice.setVisibility(View.GONE);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAnalyze.setEnabled(!show);
    }

    private void saveDream() {
        String dreamText = isVoiceMode ? currentDreamText : editDreamText.getText().toString().trim();

        if (dreamText.isEmpty()) {
            Toast.makeText(this, "Нет текста для сохранения", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем объект сна
        Dream dream = new Dream();
        dream.setText(dreamText);
        dream.setDate(new Date());
        dream.setEmotion(currentEmotion);
        dream.setInterpretation(currentInterpretation);

        // TODO: Сохранение в базу данных через Room
        // dreamRepository.insert(dream);

        Toast.makeText(this, "Сон сохранен!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на запись предоставлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Разрешение на запись необходимо для работы приложения",
                        Toast.LENGTH_LONG).show();
                chipText.setChecked(true); // Переключаемся на текстовый режим
            }
        }
    }

    private String getEmotionIcon(String emotion) {
        switch (emotion.toLowerCase()) {
            case "fear": return "😨";
            case "joy": return "😊";
            case "sadness": return "😢";
            case "anger": return "😠";
            case "surprise": return "😮";
            case "calm": return "😌";
            case "love": return "❤️";
            case "shame": return "😳";
            case "despair": return "😰";
            case "anxiety": return "😟";
            case "confusion": return "😕";
            case "empowerment": return "💪";
            default: return "😐";
        }
    }

    private String getEmotionDisplayName(String emotion) {
        switch (emotion.toLowerCase()) {
            case "fear": return "Страх";
            case "joy": return "Радость";
            case "sadness": return "Грусть";
            case "anger": return "Гнев";
            case "surprise": return "Удивление";
            case "calm": return "Спокойствие";
            case "love": return "Любовь";
            case "shame": return "Стыд";
            case "despair": return "Отчаяние";
            case "anxiety": return "Тревога";
            case "confusion": return "Смятение";
            case "empowerment": return "Вдохновение";
            default: return "Нейтральная";
        }
    }

    private String getEmotionDescription(String emotion) {
        switch (emotion.toLowerCase()) {
            case "fear": return "Сон отражает внутренние страхи и тревоги";
            case "joy": return "Сон наполнен позитивными эмоциями и радостью";
            case "sadness": return "Сон может указывать на печаль или потерю";
            case "anger": return "Сон выражает подавленный гнев или фрустрацию";
            case "surprise": return "Сон содержит неожиданные элементы";
            case "calm": return "Сон отражает внутреннее спокойствие";
            case "love": return "Сон наполнен любовью и теплыми чувствами";
            case "shame": return "Сон может отражать чувство вины или стыда";
            case "despair": return "Сон выражает глубокие переживания";
            case "anxiety": return "Сон отражает беспокойство и напряжение";
            case "confusion": return "Сон содержит противоречивые элементы";
            case "empowerment": return "Сон дает силу и уверенность";
            default: return "Эмоционально нейтральный сон";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
    }
}