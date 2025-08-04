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
    private static final String DEFAULT_API_KEY = "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";

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
        this.apiKey = DEFAULT_API_KEY;
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
                "      \"interpretation\": \"подробная_интерпретация_на_русском_языке\",\n" +
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
                "      \"interpretация\": \"Символ эмоций, подсознания, очищения. Спокойная вода — гармония, бурная — внутренний конфликт.\",\n" +
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
            InputStream inputStream = context.getAssets().open("dream_symbols.json");
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки dream_symbols.json", e);
            return "Не удалось загрузить контекст символов";
        }
    }

    // Сделаем метод публичным для использования из других классов
    public String sendOpenRouterRequest(String prompt) throws IOException, JSONException {
        URL url = new URL(API_BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("HTTP-Referer", "https://dreams-track.lionido.com");
        conn.setRequestProperty("X-Title", "Dreams Track");
        conn.setDoOutput(true);

        // Создаем JSON запрос
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "google/gemini-2.5-flash-lite");
        
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты эксперт по интерпретации снов. Твоя задача - анализировать сны и предоставлять подробную интерпретацию.");
        messages.put(systemMessage);
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        
        requestBody.put("messages", messages);

        // Настройки генерации
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.2); // Снижаем температуру для более предсказуемых ответов
        generationConfig.put("max_tokens", 1000);
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

        // Используем BufferedReader вместо Scanner для совместимости с API 26
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    // Сделаем метод публичным для использования из других классов
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
        
        // Простой анализ ключевых слов
        String lowerText = dreamText.toLowerCase();

        if (lowerText.contains("вода") || lowerText.contains("море") || lowerText.contains("река")) {
            result.symbols.add(new Symbol("вода", "water",
                    "Символ эмоций и подсознания", "calm"));
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
        
        if (lowerText.contains("огонь") || lowerText.contains("гореть")) {
            result.symbols.add(new Symbol("огонь", "fire",
                    "Страсть, разрушение или очищение", "anger"));
            result.emotion = "anger";
        }
        
        if (lowerText.contains("деньги") || lowerText.contains("денег") || lowerText.contains("заработ")) {
            result.symbols.add(new Symbol("деньги", "money",
                    "Материальные заботы и ценности", "anxiety"));
        }
        
        if (lowerText.contains("зубы") || lowerText.contains("зуб")) {
            result.symbols.add(new Symbol("зубы", "teeth",
                    "Страх старения или потери привлекательности", "anxiety"));
        }
        
        if (lowerText.contains("экзамен") || lowerText.contains("сдавать") || lowerText.contains("учиться")) {
            result.symbols.add(new Symbol("экзамен", "exam",
                    "Страх несоответствия ожиданиям", "anxiety"));
        }
        
        if (lowerText.contains("смерть") || lowerText.contains("умер")) {
            result.symbols.add(new Symbol("смерть", "death",
                    "Конец этапа жизни или трансформация", "fear"));
            result.emotion = "fear";
        }

        // Создаем более точную интерпретацию на основе найденных символов
        if (!result.symbols.isEmpty()) {
            StringBuilder interpretation = new StringBuilder();
            
            // Начинаем с общего описания
            interpretation.append("В вашем сне обнаружены следующие символы: ");
            
            for (int i = 0; i < result.symbols.size(); i++) {
                if (i > 0) interpretation.append(", ");
                interpretation.append(result.symbols.get(i).getKeyword());
            }
            
            interpretation.append(". ");
            
            // Анализируем преобладающую эмоцию
            switch (result.emotion) {
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
            for (int i = 0; i < result.symbols.size(); i++) {
                Symbol symbol = result.symbols.get(i);
                interpretation.append(symbol.getKeyword()).append(" - ").append(symbol.getInterpretation().toLowerCase()).append("; ");
            }
            
            // Добавляем возможные причины сна
            interpretation.append("Возможные причины такого сна: ");
            if (result.emotion.equals("fear")) {
                interpretation.append("вы можете испытывать тревогу или беспокойство по поводу каких-то ситуаций в жизни; ");
            } else if (result.emotion.equals("joy")) {
                interpretation.append("вы, возможно, переживаете период удовлетворенности жизнью или стремитесь к ней; ");
            } else if (result.emotion.equals("anger")) {
                interpretation.append("в вашей жизни могут быть ситуации, вызывающие раздражение или гнев; ");
            }
            
            // Общие причины снов
            if (lowerText.contains("вода")) {
                interpretation.append("возможно, вы переживаете эмоциональный период или нуждаетесь в очищении; ");
            } else if (lowerText.contains("летать")) {
                interpretation.append("вы можете стремиться к свободе или выходу за рамки текущих ограничений; ");
            } else if (lowerText.contains("падать")) {
                interpretation.append("возможно, вы чувствуете неуверенность или страх потери контроля; ");
            } else if (lowerText.contains("огонь")) {
                interpretation.append("в вашей жизни могут происходить важные перемены или вы испытываете сильные чувства; ");
            } else if (lowerText.contains("деньги")) {
                interpretation.append("вы можете переживать материальные заботы или вопросы самооценки; ");
            } else if (lowerText.contains("зубы")) {
                interpretation.append("возможно, вы беспокоитесь о своей внешности или чувствуете уязвимость; ");
            } else if (lowerText.contains("экзамен")) {
                interpretation.append("вы можете испытывать стресс из-за проверки знаний или способностей; ");
            } else if (lowerText.contains("смерть")) {
                interpretation.append("возможно, вы переживаете окончание какого-то этапа жизни; ");
            }
            
            interpretation.append("Обратите внимание на свои чувства при вспоминании этого сна. ");
            interpretation.append("Помните, что значение сна индивидуально и зависит от вашего личного контекста.");
            result.interpretation = interpretation.toString();
        } else {
            // Если символы не найдены, анализируем по ключевым словам
            StringBuilder interpretation = new StringBuilder();
            
            // Анализируем ключевые слова и создаем интерпретацию
            if (lowerText.contains("летать") || lowerText.contains("полет") || lowerText.contains("летел")) {
                interpretation.append("Сон о полете часто указывает на стремление к свободе, независимости или желание выйти за рамки текущих ограничений. ");
                interpretation.append("Возможная причина: вы можете испытывать потребность в большей свободе или независимости в реальной жизни. ");
            } else if (lowerText.contains("падать") || lowerText.contains("падение") || lowerText.contains("упал")) {
                interpretation.append("Сны о падении могут отражать чувство неуверенности, потери контроля или тревогу по поводу неудачи. ");
                interpretation.append("Возможная причина: вы можете чувствовать нестабильность в какой-то сфере жизни или беспокоиться о возможной неудаче. ");
            } else if (lowerText.contains("вода") || lowerText.contains("море") || lowerText.contains("река") || lowerText.contains("озеро")) {
                interpretation.append("Вода в снах символизирует эмоции и подсознание. Спокойная вода указывает на внутреннюю гармонию, а бурная - на эмоциональные потрясения. ");
                interpretation.append("Возможная причина: вы можете переживать эмоциональный период или нуждаетесь в очищении и восстановлении. ");
            } else if (lowerText.contains("огонь") || lowerText.contains("гореть") || lowerText.contains("пламя")) {
                interpretation.append("Огонь может символизировать страсть, разрушение или очищение. Возможно, в вашей жизни происходят важные перемены. ");
                interpretation.append("Возможная причина: вы можете испытывать сильные чувства или переживать важные жизненные изменения. ");
            } else if (lowerText.contains("деньги") || lowerText.contains("денег") || lowerText.contains("заработ") || lowerText.contains("платить")) {
                interpretation.append("Сны о деньгах могут отражать ваши заботы о материальном благополучии или чувство собственной ценности. ");
                interpretation.append("Возможная причина: вы можете переживать материальные заботы или вопросы самооценки. ");
            } else if (lowerText.contains("зубы") || lowerText.contains("зуб")) {
                interpretation.append("Зубы в снах часто связаны со страхом старения, потери привлекательности или чувства уязвимости. ");
                interpretation.append("Возможная причина: вы можете беспокоиться о своей внешности или чувствовать уязвимость в какой-то ситуации. ");
            } else if (lowerText.contains("экзамен") || lowerText.contains("сдавать") || lowerText.contains("учиться") || lowerText.contains("учеба")) {
                interpretation.append("Сны об экзамене отражают стресс, связанный с проверкой знаний или способностей, а также страх не оправдать ожиданий. ");
                interpretation.append("Возможная причина: вы можете испытывать стресс из-за предстоящей проверки или оценки ваших способностей. ");
            } else if (lowerText.contains("смерть") || lowerText.contains("умер") || lowerText.contains("умирает")) {
                interpretation.append("Смерть в снах редко предвещает реальную смерть. Чаще это символ окончания этапа жизни и начала нового. ");
                interpretation.append("Возможная причина: вы можете переживать окончание какого-то важного этапа жизни. ");
            } else if (lowerText.contains("преследуют") || lowerText.contains("преследовать") || lowerText.contains("бегать")) {
                interpretation.append("Сны о погоне могут указывать на избегание какой-то ситуации или проблемы в реальной жизни. ");
                interpretation.append("Возможная причина: вы можете избегать какой-то ситуации или проблемы, которая требует вашего внимания. ");
            } else if (lowerText.contains("потеря") || lowerText.contains("потерять") || lowerText.contains("искать")) {
                interpretation.append("Сны о потере или поиске чего-то могут отражать чувство нехватки чего-то важного в жизни. ");
                interpretation.append("Возможная причина: вы можете ощущать недостаток чего-то важного в своей жизни. ");
            } else {
                // Если не найдено специфических ключевых слов, даем общий анализ
                interpretation.append("В вашем сне не выделены явные символы, но обратите внимание на общее эмоциональное состояние, которое вы испытывали во сне. ");
                interpretation.append("Возможная причина: сон может быть связан с переработкой дневных впечатлений или подсознательными переживаниями. ");
            }
            
            // Добавляем общие рекомендации
            interpretation.append("Старайтесь вспомнить свои чувства во сне - они могут дать важные подсказки о его значении. ");
            interpretation.append("Помните, что значение сна индивидуально и зависит от вашего личного контекста и переживаний.");
            result.interpretation = interpretation.toString();
        }

        return result;
    }

    // Метод для логирования взаимодействия с OpenRouter
    public void logOpenRouterInteraction(String action, String request, String response) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s\nRequest: %s\nResponse: %s\n\n", timestamp, action, request, response);
            
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
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
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
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
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            if (logFile.exists()) {
                logFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке логов OpenRouter", e);
        }
    }

    public static String getUsageInfo() {
        return "Для использования функции анализа сна с помощью ИИ используется OpenRouter API с моделью Google Gemini 2.5 Flash Lite.";
    }
}