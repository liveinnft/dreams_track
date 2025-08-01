package com.lionido.dreams_track.utils;

import java.util.HashMap;
import java.util.Map;

public class EmotionDetector {

    private Map<String, String> emotionKeywords;

    public EmotionDetector() {
        initializeEmotionKeywords();
    }

    private void initializeEmotionKeywords() {
        emotionKeywords = new HashMap<>();
        
        // Страх
        emotionKeywords.put("страх", "fear");
        emotionKeywords.put("бояться", "fear");
        emotionKeywords.put("ужас", "fear");
        emotionKeywords.put("паника", "fear");
        emotionKeywords.put("тревога", "fear");
        emotionKeywords.put("бежать", "fear");
        emotionKeywords.put("спрятаться", "fear");
        emotionKeywords.put("опасность", "fear");
        
        // Радость
        emotionKeywords.put("радость", "joy");
        emotionKeywords.put("счастье", "joy");
        emotionKeywords.put("веселье", "joy");
        emotionKeywords.put("смех", "joy");
        emotionKeywords.put("улыбка", "joy");
        emotionKeywords.put("праздник", "joy");
        emotionKeywords.put("подарок", "joy");
        
        // Грусть
        emotionKeywords.put("грусть", "sadness");
        emotionKeywords.put("печаль", "sadness");
        emotionKeywords.put("тоска", "sadness");
        emotionKeywords.put("плач", "sadness");
        emotionKeywords.put("одиночество", "sadness");
        emotionKeywords.put("потеря", "sadness");
        
        // Гнев
        emotionKeywords.put("злость", "anger");
        emotionKeywords.put("гнев", "anger");
        emotionKeywords.put("ярость", "anger");
        emotionKeywords.put("раздражение", "anger");
        emotionKeywords.put("ненависть", "anger");
        
        // Удивление
        emotionKeywords.put("удивление", "surprise");
        emotionKeywords.put("шок", "surprise");
        emotionKeywords.put("неожиданность", "surprise");
        emotionKeywords.put("странно", "surprise");
        
        // Спокойствие
        emotionKeywords.put("спокойствие", "calm");
        emotionKeywords.put("мир", "calm");
        emotionKeywords.put("тишина", "calm");
        emotionKeywords.put("гармония", "calm");
        emotionKeywords.put("умиротворение", "calm");
        
        // Любовь
        emotionKeywords.put("любовь", "love");
        emotionKeywords.put("нежность", "love");
        emotionKeywords.put("объятия", "love");
        emotionKeywords.put("поцелуй", "love");
        emotionKeywords.put("романтика", "love");
        
        // Смущение
        emotionKeywords.put("смущение", "shame");
        emotionKeywords.put("стыд", "shame");
        emotionKeywords.put("вина", "shame");
        emotionKeywords.put("нагота", "shame");
        
        // Отчаяние
        emotionKeywords.put("отчаяние", "despair");
        emotionKeywords.put("безнадежность", "despair");
        emotionKeywords.put("ловушка", "despair");
        emotionKeywords.put("безвыходность", "despair");
    }

    public String detectEmotion(String text) {
        text = text.toLowerCase();
        Map<String, Integer> scores = new HashMap<>();

        for (String word : emotionKeywords.keySet()) {
            if (text.contains(word)) {
                String emo = emotionKeywords.get(word);
                scores.put(emo, scores.getOrDefault(emo, 0) + 1);
            }
        }

        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
    }

    public String getEmotionDisplayName(String emotion) {
        switch (emotion) {
            case "fear": return "Страх";
            case "joy": return "Радость";
            case "sadness": return "Грусть";
            case "anger": return "Гнев";
            case "surprise": return "Удивление";
            case "calm": return "Спокойствие";
            case "love": return "Любовь";
            case "shame": return "Смущение";
            case "despair": return "Отчаяние";
            default: return "Нейтрально";
        }
    }
}