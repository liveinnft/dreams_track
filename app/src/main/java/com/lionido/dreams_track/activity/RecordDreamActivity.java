package com.lionido.dreams_track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lionido.dreams_track.R;
import com.lionido.dreams_track.model.Dream;
import com.lionido.dreams_track.model.Symbol;
import com.lionido.dreams_track.utils.EmotionDetector;
import com.lionido.dreams_track.utils.SpeechHelper;
import com.lionido.dreams_track.utils.SymbolAnalyzer;

import java.util.List;

public class RecordDreamActivity extends AppCompatActivity implements SpeechHelper.OnSpeechResultListener {

    private Button recordButton;
    private TextView statusText;
    private TextView transcriptText;
    private Button analyzeButton;
    private Button saveButton;

    private SpeechHelper speechHelper;
    private SymbolAnalyzer symbolAnalyzer;
    private EmotionDetector emotionDetector;
    private String currentTranscript = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_dream);

        initializeViews();
        initializeHelpers();
    }

    private void initializeViews() {
        recordButton = findViewById(R.id.btn_record);
        statusText = findViewById(R.id.tv_status);
        transcriptText = findViewById(R.id.tv_transcript);
        analyzeButton = findViewById(R.id.btn_analyze);
        saveButton = findViewById(R.id.btn_save);

        recordButton.setOnClickListener(v -> {
            if (recordButton.getText().equals("Начать запись")) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        analyzeButton.setOnClickListener(v -> analyzeDream());
        saveButton.setOnClickListener(v -> saveDream());

        // Изначально кнопки анализа и сохранения неактивны
        analyzeButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void initializeHelpers() {
        speechHelper = new SpeechHelper(this);
        speechHelper.setOnSpeechResultListener(this);
        symbolAnalyzer = new SymbolAnalyzer(this);
        emotionDetector = new EmotionDetector();
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
        if (currentTranscript.isEmpty()) {
            Toast.makeText(this, "Сначала запишите сон", Toast.LENGTH_SHORT).show();
            return;
        }

        // Анализируем символы
        List<Symbol> symbols = symbolAnalyzer.findSymbolsInText(currentTranscript);
        
        // Определяем эмоцию
        String emotion = emotionDetector.detectEmotion(currentTranscript);

        // Создаем объект сна
        Dream dream = new Dream(currentTranscript, null);
        dream.setSymbols(symbols);
        dream.setEmotion(emotion);

        // Показываем результаты анализа
        showAnalysisResults(dream);
        
        saveButton.setEnabled(true);
    }

    private void showAnalysisResults(Dream dream) {
        StringBuilder result = new StringBuilder();
        result.append("Текст сна:\n").append(dream.getText()).append("\n\n");
        
        result.append("Эмоция: ").append(emotionDetector.getEmotionDisplayName(dream.getEmotion())).append("\n\n");
        
        if (!dream.getSymbols().isEmpty()) {
            result.append("Найденные символы:\n");
            for (Symbol symbol : dream.getSymbols()) {
                result.append("• ").append(symbol.getKeyword()).append(": ").append(symbol.getInterpretation()).append("\n");
            }
        } else {
            result.append("Символы не найдены");
        }

        transcriptText.setText(result.toString());
    }

    private void saveDream() {
        // TODO: Сохранить сон в базу данных
        Toast.makeText(this, "Сон сохранен", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
    }
}