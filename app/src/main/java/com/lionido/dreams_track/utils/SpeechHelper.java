package com.lionido.dreams_track.utils;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechHelper {

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private OnSpeechResultListener listener;

    public interface OnSpeechResultListener {
        void onSpeechResult(String text);
        void onSpeechError(String error);
        void onSpeechReady();
        void onSpeechStarted();
        void onSpeechEnded();
    }

    public SpeechHelper(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(android.os.Bundle bundle) {
                if (listener != null) {
                    listener.onSpeechReady();
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                if (listener != null) {
                    listener.onSpeechStarted();
                }
            }

            @Override
            public void onRmsChanged(float v) {
                // Можно использовать для визуализации звуковой волны
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
                if (listener != null) {
                    listener.onSpeechEnded();
                }
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                if (listener != null) {
                    listener.onSpeechError(errorMessage);
                }
            }

            @Override
            public void onResults(android.os.Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String transcript = matches.get(0); // Лучший результат
                    if (listener != null) {
                        listener.onSpeechResult(transcript);
                    }
                }
            }

            @Override
            public void onPartialResults(android.os.Bundle bundle) {
            }

            @Override
            public void onEvent(int i, android.os.Bundle bundle) {
            }
        });
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Ошибка аудио";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Ошибка клиента";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Недостаточно разрешений";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Ошибка сети";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Таймаут сети";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Речь не распознана";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Распознаватель занят";
            case SpeechRecognizer.ERROR_SERVER:
                return "Ошибка сервера";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Таймаут речи";
            default:
                return "Неизвестная ошибка";
        }
    }

    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Расскажи свой сон...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void setOnSpeechResultListener(OnSpeechResultListener listener) {
        this.listener = listener;
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}