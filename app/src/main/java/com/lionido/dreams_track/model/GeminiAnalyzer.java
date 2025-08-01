package com.lionido.dreams_track.utils;

import android.content.Context;
import android.util.Log;

import com.lionido.dreams_track.model.Symbol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class GeminiAnalyzer {
    private static final String TAG = "GeminiAnalyzer";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    private String apiKey;
    private Context context;

    public interface OnAnalysisCompleteListener {
        void onSuccess(List<Symbol> symbols, String emotion, String interpretation);
        void onError(String error);
    }

    public GeminiAnalyzer(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    public void analyzeDreamAsync(String dreamText, OnAnalysisCompleteListener listener) {
        CompletableFuture.runAsync(() -> {
            try {
                DreamAnalysisResult result = analyzeDream(dreamText);
                listener.onSuccess(result.symbols, result.emotion, result.interpretation);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка анализа сна с Gemini", e);
                listener.onError("Ошибка анализа: " + e.getMessage());
            }
        });
    }

    private DreamAnalysisResult analyzeDream(String dreamText) throws IOException, JSONException {
        String prompt = createAnalysisPrompt(dreamText);
        String response = sendGeminiRequest(prompt);
        return parseGeminiResponse(response);
    }

    private String createAnalysisPrompt(String dreamText) {
        return String.format(
                "Проанализируй следующий сон и верни результат СТРОГО в JSON формате:\n\n" +
                        "Текст сна: \"%s\"\n\n" +
                        "Верни JSON с полями:\n" +
                        "{\n" +
                        "  \"symbols\": [\n" +
                        "    {\n" +
                        "      \"keyword\": \"найденное_слово\",\n" +
                        "      \"symbol\": \"символ_на_английском\",\n" +
                        "      \"interpretation\": \"подробная_интерпретация_на_русском\",\n" +
                        "      \"emotion\": \"связанная_эмоция\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"dominant_emotion\": \"fear|joy|sadness|anger|surprise|calm|love|shame|despair|neutral\",\n" +
                        "  \"interpretation\": \"общая_интерпретация_сна_на_русском\"\n" +
                        "}\n\n" +
                        "Найди символы из категорий: вода, огонь, животные, люди, места, объекты, действия, эмоции.\n" +
                        "Дай психологическую интерпретацию на основе работ Юнга и Фрейда.\n" +
                        "Отвечай ТОЛЬКО JSON, без дополнительного текста!",
                dreamText
        );
    }

    private String sendGeminiRequest(String prompt) throws IOException, JSONException {
        URL url = new URL(API_BASE_URL + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Создаем JSON запрос
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Настройки генерации
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 1000);
        requestBody.put("generationConfig", generationConfig);

        // Отправляем запрос
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Читаем ответ
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP error code: " + responseCode);
        }

        Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        return response.toString();
    }

    private DreamAnalysisResult parseGeminiResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        // Извлекаем текст ответа из структуры Gemini API
        JSONArray candidates = jsonResponse.getJSONArray("candidates");
        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String text = parts.getJSONObject(0).getString("text");

        // Очищаем текст от markdown разметки
        text = text.replaceAll("```json", "").replaceAll("```", "").trim();

        // Парсим JSON ответ
        JSONObject analysisJson = new JSONObject(text);

        DreamAnalysisResult result = new DreamAnalysisResult();

        // Парсим символы
        result.symbols = new ArrayList<>();
        if (analysisJson.has("symbols")) {
            JSONArray symbolsArray = analysisJson.getJSONArray("symbols");
            for (int i = 0; i < symbolsArray.length(); i++) {
                JSONObject symbolObj = symbolsArray.getJSONObject(i);
                Symbol symbol = new Symbol(
                        symbolObj.getString("keyword"),
                        symbolObj.getString("symbol"),
                        symbolObj.getString("interpretation"),
                        symbolObj.getString("emotion")
                );
                result.symbols.add(symbol);
            }
        }

        // Парсим эмоцию
        result.emotion = analysisJson.optString("dominant_emotion", "neutral");

        // Парсим общую интерпретацию
        result.interpretation = analysisJson.optString("interpretation", "Интерпретация недоступна");

        return result;
    }

    // Резервный анализ на случай ошибки API
    public DreamAnalysisResult getFallbackAnalysis(String dreamText) {
        Log.i(TAG, "Используется резервный анализ");

        DreamAnalysisResult result = new DreamAnalysisResult();
        result.symbols = new ArrayList<>();
        result.emotion = "neutral";
        result.interpretation = "Анализ временно недоступен. Попробуйте позже.";

        // Простой анализ ключевых слов
        String lowerText = dreamText.toLowerCase();

        if (lowerText.contains("вода") || lowerText.contains("море") || lowerText.contains("река")) {
            result.symbols.add(new Symbol("вода", "water",
                    "Символ эмоций и подсознания", "emotional_flow"));
        }

        if (lowerText.contains("падать") || lowerText.contains("падение")) {
            result.symbols.add(new Symbol("падение", "falling",
                    "Потеря контроля или страх неудачи", "fear"));
            result.emotion = "fear";
        }

        if (lowerText.contains("летать") || lowerText.contains("полет")) {
            result.symbols.add(new Symbol("полет", "flying",
                    "Свобода и духовный подъем", "joy"));
            result.emotion = "joy";
        }

        if (lowerText.contains("страх") || lowerText.contains("боюсь") || lowerText.contains("ужас")) {
            result.emotion = "fear";
        }

        return result;
    }

    // Вспомогательный класс для результата анализа
    private static class DreamAnalysisResult {
        List<Symbol> symbols;
        String emotion;
        String interpretation;
    }

    // Проверка доступности API
    public static boolean isApiKeyValid(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() &&
                apiKey.startsWith("AIza") && apiKey.length() > 30;
    }

    // Получение информации о лимитах
    public static String getUsageInfo() {
        return "Google Gemini Free Tier:\n" +
                "• 15 запросов в минуту\n" +
                "• 1M токенов в день\n" +
                "• 32K токенов на запрос\n\n" +
                "Для снов этого более чем достаточно!";
    }
}