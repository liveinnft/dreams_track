package com.lionido.dreams_track.utils;

import android.content.Context;
import android.util.Log;

import com.lionido.dreams_track.model.Symbol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class OpenRouterAnalyzer {
    private static final String TAG = "OpenRouterAnalyzer";
    private static final String API_BASE_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String LOG_FILE_NAME = "openrouter_logs.txt";
    private static final String API_KEY_FILE = "openrouter_api_key.txt";

    private String apiKey;
    private Context context;

    public interface OnAnalysisCompleteListener {
        void onSuccess(List<Symbol> symbols, String emotion, String interpretation, String personalGrowth, String actionableAdvice);
        void onError(String error);
    }

    public static class DreamAnalysisResult {
        public List<Symbol> symbols;
        public String emotion;
        public String interpretation;
        public String personalGrowth;
        public String actionableAdvice;

        public DreamAnalysisResult() {
            symbols = new ArrayList<>();
            emotion = "neutral";
            interpretation = "";
            personalGrowth = "";
            actionableAdvice = "";
        }
    }

    public OpenRouterAnalyzer(Context context) {
        this.context = context;
        this.apiKey = loadApiKeyFromFile();
    }

    private String loadApiKeyFromFile() {
        try {
            InputStream inputStream = context.getAssets().open(API_KEY_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder apiKey = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                apiKey.append(line);
            }
            reader.close();
            inputStream.close();
            return apiKey.toString().trim();
        } catch (IOException e) {
            Log.e(TAG, "Не удалось загрузить API ключ из файла", e);
            return null;
        }
    }

    public void analyzeDreamAsync(String dreamText, OnAnalysisCompleteListener listener) {
        CompletableFuture.runAsync(() -> {
            try {
                DreamAnalysisResult result = analyzeDream(dreamText);
                listener.onSuccess(result.symbols, result.emotion, result.interpretation, result.personalGrowth, result.actionableAdvice);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка анализа сна с OpenRouter", e);
                logOpenRouterInteraction("Ошибка анализа", "dreamText: " + dreamText, "error: " + e.getMessage());
                // Fallback к базовому анализу
                DreamAnalysisResult fallbackResult = getFallbackAnalysis(dreamText);
                listener.onSuccess(fallbackResult.symbols, fallbackResult.emotion, fallbackResult.interpretation, fallbackResult.personalGrowth, fallbackResult.actionableAdvice);
            }
        });
    }

    private DreamAnalysisResult analyzeDream(String dreamText) throws IOException, JSONException {
        String prompt = createAnalysisPrompt(dreamText);
        String response = sendOpenRouterRequest(prompt);
        logOpenRouterInteraction("Запрос отправлен", "prompt: " + prompt, "response: " + response);
        return parseOpenRouterResponse(response);
    }

    public String createAnalysisPrompt(String dreamText) {
        String symbolsContext = loadDreamSymbolsContext();

        return "Ты - опытный психоаналитик, специализирующийся на интерпретации снов согласно методам Карла Юнга, Зигмунда Фрейда и современной психологии сна. Проанализируй следующий сон с точки зрения глубинной психологии.\n\n" +
                "ТЕКСТ СНА: \"" + dreamText + "\"\n\n" +
                "КОНТЕКСТ СИМВОЛОВ ИЗ БАЗЫ ЗНАНИЙ:\n" + symbolsContext + "\n\n" +
                "МЕТОДОЛОГИЧЕСКИЙ ПОДХОД:\n" +
                "1. ЮНГИАНСКИЙ АНАЛИЗ: Рассматривай архетипы (Анима/Анимус, Тень, Самость, Мудрый Старец), коллективное бессознательное, процесс индивидуации\n" +
                "2. ФРЕЙДИСТСКИЙ АНАЛИЗ: Учитывай механизмы сгущения, смещения, символизации, латентное содержание за манифестным\n" +
                "3. СОВРЕМЕННАЯ ПСИХОЛОГИЯ: Рассматривай сон как обработку эмоций, консолидацию памяти, решение проблем\n" +
                "4. ЭКЗИСТЕНЦИАЛЬНЫЙ ПОДХОД: Выявляй темы свободы, ответственности, смысла, аутентичности\n\n" +
                "КРИТЕРИИ АНАЛИЗА СИМВОЛОВ:\n" +
                "- Контекст символа в сне (как он появляется, взаимодействия)\n" +
                "- Эмоциональная окраска символа\n" +
                "- Личные ассоциации (предполагаемые)\n" +
                "- Архетипическое значение\n" +
                "- Культурное значение\n" +
                "- Возможная компенсаторная функция\n\n" +
                "ОПРЕДЕЛЕНИЕ ДОМИНИРУЮЩЕЙ ЭМОЦИИ:\n" +
                "Анализируй не только явные эмоции, но и:\n" +
                "- Скрытые эмоциональные темы\n" +
                "- Защитные механизмы\n" +
                "- Эмоциональные конфликты\n" +
                "- Подавленные чувства\n\n" +
                "ЗАДАЧИ АНАЛИЗА:\n" +
                "1. Найди 3-7 наиболее значимых символов в сне\n" +
                "2. Определи доминирующую эмоцию с учетом скрытых аспектов\n" +
                "3. Дай психоаналитическую интерпретацию\n" +
                "4. Определи возможное значение для личностного роста\n" +
                "5. Предложи практические рекомендации\n\n" +
                "СТРОГИЙ ФОРМАТ ОТВЕТА (ТОЛЬКО JSON):\n" +
                "{\n" +
                "  \"symbols\": [\n" +
                "    {\n" +
                "      \"keyword\": \"ключевое_слово_из_сна\",\n" +
                "      \"symbol\": \"symbol_name_english\",\n" +
                "      \"interpretation\": \"Психологическая интерпретация символа в контексте сна (до 25 слов)\",\n" +
                "      \"emotion\": \"fear|joy|sadness|anger|surprise|calm|love|shame|despair|anxiety|confusion|empowerment|neutral\",\n" +
                "      \"archetype\": \"связанный_архетип_или_комплекс\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"dominant_emotion\": \"основная_эмоция_сна\",\n" +
                "  \"emotional_undertone\": \"скрытая_эмоциональная_тема\",\n" +
                "  \"interpretation\": \"Глубокая психоаналитическая интерпретация сна: что он говорит о внутреннем мире человека, какие бессознательные процессы отражает, какие внутренние конфликты может выражать (3-4 предложения)\",\n" +
                "  \"personal_growth\": \"Что этот сон может означать для личностного развития и самопознания человека. Какие аспекты личности требуют внимания (2-3 предложения)\",\n" +
                "  \"actionable_advice\": \"Конкретные практические рекомендации на основе анализа сна: что можно сделать в реальной жизни, на что обратить внимание, какие вопросы себе задать (2-3 предложения)\"\n" +
                "}\n\n" +
                "ПРИНЦИПЫ ИНТЕРПРЕТАЦИИ:\n" +
                "- Каждый символ имеет множественные значения\n" +
                "- Сон компенсирует однобокость сознательной установки\n" +
                "- Обращай внимание на противоположности и парадоксы\n" +
                "- Учитывай стадию жизни и возрастные задачи\n" +
                "- Ищи связи с процессом индивидуации\n" +
                "- Рассматривай защитные механизмы и сопротивления\n\n" +
                "КРИТИЧЕСКИ ВАЖНО:\n" +
                "- Отвечай ТОЛЬКО в формате JSON\n" +
                "- Не добавляй объяснения вне JSON\n" +
                "- Интерпретация должна быть профессиональной, но понятной\n" +
                "- Избегай категоричных утверждений, используй \"возможно\", \"может указывать на\"\n" +
                "- Фокусируйся на потенциале роста, а не на патологии\n" +
                "- Каждое поле JSON должно быть заполнено\n" +
                "- Символы должны быть действительно значимыми для понимания сна\n" +
                "- В поле archetype указывай юнгианские архетипы, комплексы или психологические темы\n\n" +
                "ПРИМЕРЫ АРХЕТИПОВ/КОМПЛЕКСОВ:\n" +
                "- Анима/Анимус (внутренний образ противоположного пола)\n" +
                "- Тень (отвергаемые аспекты личности)\n" +
                "- Самость (целостность личности)\n" +
                "- Мудрый Старец/Великая Мать (руководящие принципы)\n" +
                "- Материнский/Отцовский комплекс\n" +
                "- Комплекс неполноценности\n" +
                "- Комплекс власти\n" +
                "- Герой (преодоление препятствий)\n" +
                "- Трикстер (нарушение правил, хаос)\n\n" +
                "Помни: сон - это письмо от бессознательного к сознанию. Твоя задача - расшифровать это послание с максимальной психологической точностью.";
    }

    private String loadDreamSymbolsContext() {
        try {
            InputStream inputStream = context != null ?
                    context.getAssets().open("dream_symbols.json") :
                    getClass().getClassLoader().getResourceAsStream("assets/dream_symbols.json");

            if (inputStream == null) {
                Log.e(TAG, "Не удалось загрузить dream_symbols.json");
                return "Файл dream_symbols.json не найден";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            inputStream.close();

            JSONArray symbolsArray = new JSONArray(jsonString.toString());
            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < symbolsArray.length(); i++) {
                JSONObject symbolObj = symbolsArray.getJSONObject(i);
                contextBuilder.append("- ")
                        .append(symbolObj.getString("word"))
                        .append(" (")
                        .append(symbolObj.getString("symbol"))
                        .append("): ")
                        .append(symbolObj.getString("interpretation"))
                        .append(" [")
                        .append(symbolObj.getString("emotion"))
                        .append("]\n");
            }

            return contextBuilder.toString();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Ошибка при загрузке dream_symbols.json", e);
            return "Ошибка при загрузке контекста символов";
        }
    }

    public String sendOpenRouterRequest(String prompt) throws IOException, JSONException {
        URL url = new URL(API_BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + (apiKey != null ? apiKey : ""));
        if (context != null) {
            connection.setRequestProperty("HTTP-Referer", context.getPackageName());
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(90000); // Увеличен timeout для более сложного анализа

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "tngtech/deepseek-r1t2-chimera:free");

        JSONArray messagesArray = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты - профессиональный психоаналитик с глубокими знаниями в области психологии снов. Твоя задача - дать точную, профессиональную, но понятную интерпретацию снов, основанную на принципах глубинной психологии. Отвечай строго в формате JSON без дополнительных комментариев.");
        messagesArray.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.put(userMessage);

        requestBody.put("messages", messagesArray);
        requestBody.put("temperature", 0.7); // Баланс между креативностью и точностью
        requestBody.put("max_tokens", 2000); // Увеличено для более подробного анализа

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            return response.toString();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            reader.close();
            connection.disconnect();
            throw new IOException("OpenRouter API error " + responseCode + ": " + errorResponse.toString());
        }
    }

    public DreamAnalysisResult parseOpenRouterResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        String text = message.getString("content");

        // Очистка текста
        text = text.replaceAll("```json", "")
                .replaceAll("```", "")
                .replaceAll("\\\\\"", "\"")
                .replaceAll("^\\s+", "")
                .replaceAll("\\s+$", "")
                .trim();

        // Поиск JSON в тексте
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }

        JSONObject analysisJson = new JSONObject(text);
        DreamAnalysisResult result = new DreamAnalysisResult();

        // Парсинг символов
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
                // Добавляем архетип, если он есть
                if (symbolObj.has("archetype")) {
                    symbol.setArchetype(symbolObj.getString("archetype"));
                }
                result.symbols.add(symbol);
            }
        }

        result.emotion = analysisJson.optString("dominant_emotion", "neutral");
        result.interpretation = analysisJson.optString("interpretation", "Интерпретация недоступна");
        result.personalGrowth = analysisJson.optString("personal_growth", "");
        result.actionableAdvice = analysisJson.optString("actionable_advice", "");

        return result;
    }

    public DreamAnalysisResult getFallbackAnalysis(String dreamText) {
        Log.i(TAG, "Используется резервный анализ");

        DreamAnalysisResult result = new DreamAnalysisResult();
        result.symbols = new ArrayList<>();
        result.emotion = "neutral";
        result.interpretation = "Не удалось получить анализ от ИИ. Попробуйте еще раз.";
        result.personalGrowth = "Ведение дневника снов само по себе способствует самопознанию.";
        result.actionableAdvice = "Запишите свои первые впечатления от сна и подумайте о связях с вашей текущей жизненной ситуацией.";

        return result;
    }

    // Остальные методы остаются без изменений
    public void logOpenRouterInteraction(String action, String request, String response) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s\nRequest: %s\nResponse: %s\n\n", timestamp, action, request, response);

            File logFile = new File(context != null ? context.getFilesDir() : new File("."), LOG_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(logEntry.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при записи лога OpenRouter", e);
        }
    }

    public String getOpenRouterLogs() {
        try {
            File logFile = new File(context != null ? context.getFilesDir() : new File("."), LOG_FILE_NAME);
            if (!logFile.exists()) {
                return "Логи отсутствуют";
            }

            Scanner scanner = new Scanner(logFile);
            StringBuilder logs = new StringBuilder();
            while (scanner.hasNextLine()) {
                logs.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return logs.toString();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при чтении логов OpenRouter", e);
            return "Ошибка при чтении логов: " + e.getMessage();
        }
    }

    public void clearOpenRouterLogs() {
        try {
            File logFile = new File(context != null ? context.getFilesDir() : new File("."), LOG_FILE_NAME);
            if (logFile.exists()) {
                logFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке логов OpenRouter", e);
        }
    }

    public static String getUsageInfo() {
        return "Для анализа снов используется OpenRouter API с психоаналитическим подходом на основе методов Юнга и Фрейда.";
    }
}