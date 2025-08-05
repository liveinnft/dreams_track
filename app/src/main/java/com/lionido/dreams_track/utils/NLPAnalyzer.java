package com.lionido.dreams_track.utils;

import android.content.Context;
import android.util.Log;

import com.lionido.dreams_track.model.Symbol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NLPAnalyzer {

    private Context context;
    private List<Symbol> symbols;
    private Map<String, List<String>> synonyms;
    private Set<String> stopWords;
    private Map<String, Double> emotionWeights;

    public NLPAnalyzer(Context context) {
        this.context = context;
        this.symbols = loadSymbolsFromAsset();
        initializeSynonyms();
        initializeStopWords();
        initializeEmotionWeights();
    }

    private List<Symbol> loadSymbolsFromAsset() {
        List<Symbol> list = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("dream_symbols.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Symbol symbol = new Symbol(
                        obj.getString("word"),
                        obj.getString("symbol"),
                        obj.getString("interpretation"),
                        obj.getString("emotion")
                );
                list.add(symbol);
            }
        } catch (IOException | JSONException e) {
            Log.e("NLPAnalyzer", "Ошибка загрузки символов", e);
        }
        return list;
    }

    private void initializeSynonyms() {
        synonyms = new HashMap<>();

        // Вода и связанные понятия
        synonyms.put("вода", Arrays.asList("река", "озеро", "море", "океан", "дождь", "ливень", "поток", "волна", "течение"));
        synonyms.put("падать", Arrays.asList("падение", "упасть", "сваливаться", "рухнуть", "скатиться", "провалиться"));
        synonyms.put("летать", Arrays.asList("полет", "лететь", "парить", "взлетать", "планировать", "витать"));
        synonyms.put("огонь", Arrays.asList("пламя", "костер", "пожар", "горение", "жар", "жжение"));
        synonyms.put("дом", Arrays.asList("жилище", "квартира", "здание", "постройка", "кров", "обитель"));
        synonyms.put("смерть", Arrays.asList("кончина", "гибель", "умирание", "конец", "погибель"));
        synonyms.put("страх", Arrays.asList("ужас", "боязнь", "трепет", "паника", "испуг", "тревога", "боюсь"));
        synonyms.put("преследование", Arrays.asList("погоня", "охота", "гонка", "преследовать", "гнаться", "следовать"));
        synonyms.put("темнота", Arrays.asList("тьма", "мрак", "темно", "черный", "непроглядный"));
        synonyms.put("свет", Arrays.asList("яркость", "освещение", "блеск", "сияние", "луч"));

        // Добавляем обратные связи
        Map<String, List<String>> reverseSynonyms = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            String mainWord = entry.getKey();
            for (String synonym : entry.getValue()) {
                if (!reverseSynonyms.containsKey(synonym)) {
                    reverseSynonyms.put(synonym, new ArrayList<>());
                }
                reverseSynonyms.get(synonym).add(mainWord);
            }
        }
        synonyms.putAll(reverseSynonyms);
    }

    private void initializeStopWords() {
        stopWords = new HashSet<>(Arrays.asList(
                "и", "в", "на", "с", "по", "для", "от", "до", "при", "за", "над", "под", "между",
                "я", "мы", "ты", "вы", "он", "она", "оно", "они", "мой", "твой", "его", "её", "наш", "ваш", "их",
                "это", "то", "что", "где", "когда", "как", "почему", "который", "которая", "которое",
                "был", "была", "было", "были", "есть", "буду", "будешь", "будет", "будем", "будете", "будут",
                "а", "но", "или", "если", "хотя", "потому", "что", "чтобы", "так", "тот", "тем", "том"
        ));
    }

    private void initializeEmotionWeights() {
        emotionWeights = new HashMap<>();

        // Страх
        emotionWeights.put("страх", 3.0);
        emotionWeights.put("ужас", 4.0);
        emotionWeights.put("боюсь", 3.5);
        emotionWeights.put("паника", 4.5);
        emotionWeights.put("кошмар", 4.0);
        emotionWeights.put("тревога", 2.5);

        // Радость
        emotionWeights.put("радость", 3.0);
        emotionWeights.put("счастье", 3.5);
        emotionWeights.put("веселье", 2.5);
        emotionWeights.put("восторг", 4.0);

        // Грусть
        emotionWeights.put("грусть", 3.0);
        emotionWeights.put("печаль", 3.5);
        emotionWeights.put("тоска", 4.0);
        emotionWeights.put("депрессия", 4.5);

        // Злость
        emotionWeights.put("злость", 3.0);
        emotionWeights.put("гнев", 4.0);
        emotionWeights.put("ярость", 4.5);
        emotionWeights.put("бешенство", 5.0);
    }

    public List<Symbol> findSymbolsAdvanced(String text) {
        List<Symbol> foundSymbols = new ArrayList<>();
        String processedText = preprocessText(text);

        // Создаем карту для отслеживания найденных символов и их весов
        Map<Symbol, Double> symbolWeights = new HashMap<>();

        for (Symbol symbol : symbols) {
            double weight = calculateSymbolWeight(processedText, symbol);
            if (weight > 0) {
                symbolWeights.put(symbol, weight);
            }
        }

        // Сортируем по весу и берем наиболее релевантные
        symbolWeights.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10) // Ограничиваем количество символов
                .forEach(entry -> foundSymbols.add(entry.getKey()));

        return foundSymbols;
    }

    private String preprocessText(String text) {
        // Приводим к нижнему регистру и убираем лишние символы
        text = text.toLowerCase()
                .replaceAll("[^а-яё\\s]", " ") // Оставляем только русские буквы и пробелы
                .replaceAll("\\s+", " ") // Убираем множественные пробелы
                .trim();

        return text;
    }

    private double calculateSymbolWeight(String text, Symbol symbol) {
        double weight = 0.0;
        String keyword = symbol.getKeyword().toLowerCase();

        // Прямое совпадение ключевого слова
        if (text.contains(keyword)) {
            weight += 2.0;

            // Дополнительный вес за точное словоразмерное совпадение
            Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
            Matcher matcher = wordPattern.matcher(text);
            if (matcher.find()) {
                weight += 1.0;
            }
        }

        // Проверяем синонимы
        if (synonyms.containsKey(keyword)) {
            for (String synonym : synonyms.get(keyword)) {
                if (text.contains(synonym.toLowerCase())) {
                    weight += 1.5;
                }
            }
        }

        // Контекстуальный анализ - ищем связанные слова
        weight += analyzeContext(text, symbol);

        return weight;
    }

    private double analyzeContext(String text, Symbol symbol) {
        double contextWeight = 0.0;

        // Определяем контекстуальные слова для каждого типа символа
        Map<String, List<String>> contextWords = new HashMap<>();

        contextWords.put("water", Arrays.asList("мокрый", "плавать", "тонуть", "брызги", "капли", "течь"));
        contextWords.put("falling", Arrays.asList("высота", "глубина", "вниз", "пропасть", "обрыв", "лететь"));
        contextWords.put("flying", Arrays.asList("небо", "облака", "крылья", "высоко", "парить", "взлетать"));
        contextWords.put("fire", Arrays.asList("жар", "дым", "горячий", "сжигать", "пепел", "искры"));
        contextWords.put("chase", Arrays.asList("бежать", "убегать", "догонять", "быстро", "скорость", "спасаться"));
        contextWords.put("death", Arrays.asList("гроб", "похороны", "умирать", "мертвый", "кладбище", "душа"));
        contextWords.put("darkness", Arrays.asList("ночь", "черный", "мрак", "тени", "не видно", "слепой"));
        contextWords.put("light", Arrays.asList("солнце", "лампа", "яркий", "светлый", "освещать", "сияние"));

        if (contextWords.containsKey(symbol.getSymbol())) {
            for (String contextWord : contextWords.get(symbol.getSymbol())) {
                if (text.contains(contextWord)) {
                    contextWeight += 0.5;
                }
            }
        }

        return contextWeight;
    }

    public String detectEmotionAdvanced(String text) {
        Map<String, Double> emotionScores = new HashMap<>();
        String processedText = preprocessText(text);

        // Анализируем эмоциональные слова с весами
        for (Map.Entry<String, Double> entry : emotionWeights.entrySet()) {
            String emotionWord = entry.getKey();
            Double weight = entry.getValue();

            if (processedText.contains(emotionWord)) {
                String emotionCategory = getEmotionCategory(emotionWord);
                emotionScores.put(emotionCategory,
                        emotionScores.getOrDefault(emotionCategory, 0.0) + weight);
            }
        }

        // Анализируем символы для определения эмоций
        List<Symbol> symbols = findSymbolsAdvanced(text);
        for (Symbol symbol : symbols) {
            String symbolEmotion = symbol.getEmotion();
            String emotionCategory = mapSymbolEmotionToCategory(symbolEmotion);
            emotionScores.put(emotionCategory,
                    emotionScores.getOrDefault(emotionCategory, 0.0) + 1.0);
        }

        // Контекстуальный анализ эмоций
        emotionScores = analyzeEmotionalContext(processedText, emotionScores);

        // Возвращаем эмоцию с наибольшим весом
        return emotionScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");
    }

    private String getEmotionCategory(String emotionWord) {
        if (Arrays.asList("страх", "ужас", "боюсь", "паника", "кошмар", "тревога").contains(emotionWord)) {
            return "fear";
        } else if (Arrays.asList("радость", "счастье", "веселье", "восторг").contains(emotionWord)) {
            return "joy";
        } else if (Arrays.asList("грусть", "печаль", "тоска", "депрессия").contains(emotionWord)) {
            return "sadness";
        } else if (Arrays.asList("злость", "гнев", "ярость", "бешенство").contains(emotionWord)) {
            return "anger";
        }
        return "neutral";
    }

    private String mapSymbolEmotionToCategory(String symbolEmotion) {
        switch (symbolEmotion) {
            case "fear":
            case "anxiety":
            case "despair":
                return "fear";
            case "joy":
            case "hope":
            case "love":
                return "joy";
            case "sadness":
            case "shame":
                return "sadness";
            case "anger":
                return "anger";
            default:
                return "neutral";
        }
    }

    private Map<String, Double> analyzeEmotionalContext(String text, Map<String, Double> emotionScores) {
        // Анализируем контекстуальные индикаторы эмоций

        // Страх
        if (text.contains("не мог") || text.contains("не могу") || text.contains("помощь") ||
                text.contains("спасаться") || text.contains("убегать")) {
            emotionScores.put("fear", emotionScores.getOrDefault("fear", 0.0) + 1.5);
        }

        // Радость
        if (text.contains("красиво") || text.contains("прекрасно") || text.contains("удивительно") ||
                text.contains("чудесно") || text.contains("восхитительно")) {
            emotionScores.put("joy", emotionScores.getOrDefault("joy", 0.0) + 1.5);
        }

        // Грусть
        if (text.contains("потерял") || text.contains("потеряла") || text.contains("одиноко") ||
                text.contains("пусто") || text.contains("грустно")) {
            emotionScores.put("sadness", emotionScores.getOrDefault("sadness", 0.0) + 1.5);
        }

        // Гнев
        if (text.contains("злой") || text.contains("сердитый") || text.contains("раздражает") ||
                text.contains("бесит") || text.contains("ненавижу")) {
            emotionScores.put("anger", emotionScores.getOrDefault("anger", 0.0) + 1.5);
        }

        return emotionScores;
    }

    // Дополнительные методы для расширенного анализа

    public List<String> extractKeyPhrases(String text) {
        List<String> keyPhrases = new ArrayList<>();
        String processedText = preprocessText(text);

        // Простой алгоритм выделения ключевых фраз
        String[] sentences = processedText.split("[.!?]+");

        for (String sentence : sentences) {
            String[] words = sentence.trim().split("\\s+");
            if (words.length >= 2 && words.length <= 4) {
                // Фильтруем стоп-слова
                StringBuilder phrase = new StringBuilder();
                for (String word : words) {
                    if (!stopWords.contains(word) && word.length() > 2) {
                        if (phrase.length() > 0) phrase.append(" ");
                        phrase.append(word);
                    }
                }
                if (phrase.length() > 0) {
                    keyPhrases.add(phrase.toString());
                }
            }
        }

        return keyPhrases;
    }

    public double calculateTextComplexity(String text) {
        String processedText = preprocessText(text);
        String[] words = processedText.split("\\s+");
        String[] sentences = processedText.split("[.!?]+");

        if (sentences.length == 0) return 0.0;

        double avgWordsPerSentence = (double) words.length / sentences.length;
        double avgCharsPerWord = processedText.replace(" ", "").length() / (double) words.length;

        // Простая формула сложности
        return (avgWordsPerSentence * 0.6) + (avgCharsPerWord * 0.4);
    }

    public Map<String, Integer> getWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        String processedText = preprocessText(text);
        String[] words = processedText.split("\\s+");

        for (String word : words) {
            if (!stopWords.contains(word) && word.length() > 2) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }

        return frequency;
    }
    
    // Добавляем метод extractSymbols как псевдоним для findSymbolsAdvanced
    public List<Symbol> extractSymbols(String text) {
        return findSymbolsAdvanced(text);
    }
}