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
        void onSuccess(List<Symbol> symbols, String emotion, String interpretation);
        void onError(String error);
    }

    public static class DreamAnalysisResult {
        public List<Symbol> symbols;
        public String emotion;
        public String interpretation;
        
        public DreamAnalysisResult() {
            symbols = new ArrayList<>();
            emotion = "neutral";
            interpretation = "";
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
                listener.onSuccess(result.symbols, result.emotion, result.interpretation);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка анализа сна с OpenRouter", e);
                logOpenRouterInteraction("Ошибка анализа", "dreamText: " + dreamText, "error: " + e.getMessage());
                // Fallback к базовому анализу
                DreamAnalysisResult fallbackResult = getFallbackAnalysis(dreamText);
                listener.onSuccess(fallbackResult.symbols, fallbackResult.emotion, fallbackResult.interpretation);
            }
        });
    }

    private DreamAnalysisResult analyzeDream(String dreamText) throws IOException, JSONException {
        String prompt = createAnalysisPrompt(dreamText);
        String response = sendOpenRouterRequest(prompt);
        logOpenRouterInteraction("Запрос отправлен", "prompt: " + prompt, "response: " + response);
        return parseOpenRouterResponse(response);
    }

    // Сделаем метод публичным для использования из других классов
    public String createAnalysisPrompt(String dreamText) {
        // Загружаем dream_symbols.json для предоставления контекста модели
        String symbolsContext = loadDreamSymbolsContext();
        
        return "Проанализируй следующий сон и верни результат СТРОГО в формате JSON:\n\n" +
                "Текст сна: \"" + dreamText + "\"\n\n" +
                "Известные символы сна (из dream_symbols.json):\n" + symbolsContext + "\n\n" +
                "ЗАДАЧИ:\n" +
                "1. Найди в сне символы, которые есть в списке выше\n" +
                "2. Найди дополнительные символы, которых нет в списке, но которые важны для понимания сна\n" +
                "3. Если слово отсутствует в списке известных символов, найди его в тексте и дай объяснение сна в 1-2 предложениях\n" +
                "4. При анализе учитывай контекст и взаимосвязи между элементами сна\n" +
                "5. ДАЖЕ ЕСЛИ В СНЕ НЕ УПОМИНАЮТСЯ ЧУВСТВА, ПОПЫТАЙСЯ ПРЕДПОЛОЖИТЬ, ПОЧЕМУ ЧЕЛОВЕКУ ПРИСНИЛСЯ ЭТОТ СОН\n" +
                "6. УЧИТЫВАЙ ВОЗМОЖНЫЕ СВЯЗИ С РЕАЛЬНОЙ ЖИЗНЬЮ ЧЕЛОВЕКА\n" +
                "7. ОБЯЗАТЕЛЬНО НАЙДИ НЕ МЕНЕЕ 3 СИМВОЛОВ ИЗ ТЕКСТА СНА, ДАЖЕ ЕСЛИ ОНИ НЕ ПРЕДСТАВЛЕНЫ В dream_symbols.json\n\n" +
                "ОБЯЗАТЕЛЬНЫЕ ТРЕБОВАНИЯ К ОТВЕТУ:\n" +
                "- Ответ должен быть в формате JSON\n" +
                "- JSON должен содержать ТОЛЬКО следующие поля:\n" +
                "{\n" +
                "  \"symbols\": [\n" +
                "    {\n" +
                "      \"keyword\": \"ключевое_слово_из_сна_на_русском\",\n" +
                "      \"symbol\": \"символ_на_английском\",\n" +
                "      \"interpretation\": \"подробная_интерпретация_на_русском_языке_не_более_15_слов\",\n" +
                "      \"emotion\": \"одна_из_эмоций_fear_joy_sadness_anger_surprise_calm_love_shame_despair_neutral\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"dominant_emotion\": \"одна_из_эмоций_fear_joy_sadness_anger_surprise_calm_love_shame_despair_neutral\",\n" +
                "  \"interpretation\": \"общая_интерпретация_сна_на_русском_языке_в_2_3_предложениях_с_предположением_о_причинах_сна\"\n" +
                "}\n\n" +
                "ПРИМЕР ПРАВИЛЬНОГО ОТВЕТА:\n" +
                "{\n" +
                "  \"symbols\": [\n" +
                "    {\n" +
                "      \"keyword\": \"вода\",\n" +
                "      \"symbol\": \"water\",\n" +
                "      \"interpretация\": \"Символ эмоций, подсознания, очищения. Спокойная вода — гармония.\",\n" +
                "      \"emotion\": \"calm\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"dominant_emotion\": \"calm\",\n" +
                "  \"interpretation\": \"Сон отражает ваше стремление к внутренней гармонии и эмоциональному балансу. Возможно, вы переживаете период спокойствия в жизни или стремитесь к нему. Такие сны часто приходят, когда человеку нужно восстановить душевные силы.\"\n" +
                "}\n\n" +
                "ВАЖНО:\n" +
                "- НЕ добавляй пояснения к JSON\n" +
                "- НЕ используйте код\n" +
                "- НЕ добавляйте дополнительные поля\n" +
                "- Ответ должен содержать ТОЛЬКО JSON структуру\n" +
                "- Если не можешь найти подходящие символы, верни пустой массив [] для поля symbols\n" +
                "- Все поля обязательны к заполнению\n" +
                "- Не используй спецсимволы и экранирование в JSON\n" +
                "- Используй двойные кавычки для строк\n" +
                "- Не добавляй комментарии в JSON\n" +
                "- В поле interpretation ОБЯЗАТЕЛЬНО включи предположение о причинах сна, даже если чувства не упомянуты\n" +
                "- В интерпретации указывай возможные связи с реальной жизнью человека\n" +
                "- ОБЯЗАТЕЛЬНО НАЙДИ НЕ МЕНЕЕ 3 СИМВОЛОВ ИЗ ТЕКСТА СНА\n" +
                "- Для каждого найденного символа в поле interpretation должно быть не более 15 слов\n" +
                "Найди символы из категорий: вода, огонь, животные, люди, места, объекты, действия, эмоции.\n" +
                "Дай психологическую интерпретацию на основе работ Юнга и Фрейда.\n" +
                "При формулировке интерпретации ПОМНИ:\n" +
                "1. Люди часто видят сны об обработке дневных переживаний\n" +
                "2. Сны могут отражать подавленные желания или страхи\n" +
                "3. Сны могут быть способом работы подсознания с проблемами\n" +
                "4. Сны могут указывать на внутренние конфликты\n" +
                "5. Сны могут отражать фазу жизненного цикла человека";
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
            
            // Парсим JSON и создаем текстовое представление символов
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
        
        // Настраиваем параметры подключения
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + (apiKey != null ? apiKey : ""));
        if (context != null) {
            connection.setRequestProperty("HTTP-Referer", context.getPackageName());
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000); // 30 секунд на подключение
        connection.setReadTimeout(60000);    // 60 секунд на чтение
        
        // Создаем JSON тело запроса
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "tngtech/deepseek-r1t2-chimera:free");
        
        JSONArray messagesArray = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты - эксперт по интерпретации снов. Ты должен отвечать строго в формате JSON, как указано в инструкциях.");
        messagesArray.put(systemMessage);
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.put(userMessage);
        
        requestBody.put("messages", messagesArray);
        
        // Отправляем запрос
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        
        // Читаем ответ
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
            // Обрабатываем ошибку
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

        // Извлекаем текст ответа из структуры OpenRouter API
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        String text = message.getString("content");

        // Очищаем текст от кода разметки и других нежелательных символов
        text = text.replaceAll("```json", "")
                   .replaceAll("```", "")
                   .replaceAll("\\\\\"", "\"") // Убираем экранирование кавычек
                   .replaceAll("^\\s+", "") // Убираем пробелы в начале
                   .replaceAll("\\s+$", "") // Убираем пробелы в конце
                   .trim();

        // Пытаемся найти JSON в тексте (иногда модель добавляет пояснения)
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }

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
        result.interpretation = "Резервный анализ: " + dreamText.substring(0, Math.min(100, dreamText.length())) + "...";
        
        return result;
    }

    public void logOpenRouterInteraction(String action, String request, String response) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s\nRequest: %s\nResponse: %s\n\n", timestamp, action, request, response);
            
            File logFile = new File(context != null ? context.getFilesDir() : new File("."), LOG_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(logFile, true); // true для добавления в конец файла
            fos.write(logEntry.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при записи лога OpenRouter", e);
        }
    }

    // Метод для получения логов OpenRouter
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

    // Метод для очистки логов OpenRouter
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
        return "Для использования функции анализа сна с помощью ИИ используется OpenRouter API с моделью tngtech/deepseek-r1t2-chimera:free.";
    }

    // Метод для ограничения количества слов в тексте
    private static String limitWords(String text, int maxWords) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) result.append(" ");
            result.append(words[i]);
        }
        return result.toString();
    }
}