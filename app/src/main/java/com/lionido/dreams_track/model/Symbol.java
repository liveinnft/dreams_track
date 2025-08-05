package com.lionido.dreams_track.model;

public class Symbol {
    private String keyword;
    private String symbol;
    private String interpretation;
    private String emotion;
    private String archetype; // Новое поле для архетипов/комплексов

    public Symbol(String keyword, String symbol, String interpretation, String emotion) {
        this.keyword = keyword;
        this.symbol = symbol;
        this.interpretation = interpretation;
        this.emotion = emotion;
        this.archetype = "";
    }

    public Symbol(String keyword, String symbol, String interpretation, String emotion, String archetype) {
        this.keyword = keyword;
        this.symbol = symbol;
        this.interpretation = interpretation;
        this.emotion = emotion;
        this.archetype = archetype != null ? archetype : "";
    }

    // Геттеры
    public String getKeyword() {
        return keyword;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getArchetype() {
        return archetype;
    }

    // Сеттеры
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public void setArchetype(String archetype) {
        this.archetype = archetype != null ? archetype : "";
    }

    // Вспомогательные методы
    public boolean hasArchetype() {
        return archetype != null && !archetype.trim().isEmpty();
    }

    public String getEmotionDisplayName() {
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
            case "neutral": return "Нейтральная";
            default: return emotion;
        }
    }

    public String getArchetypeDisplayName() {
        if (!hasArchetype()) return "";

        switch (archetype.toLowerCase()) {
            case "anima": return "Анима";
            case "animus": return "Анимус";
            case "shadow": return "Тень";
            case "self": return "Самость";
            case "wise_old_man": return "Мудрый Старец";
            case "great_mother": return "Великая Мать";
            case "hero": return "Герой";
            case "trickster": return "Трикстер";
            case "mother_complex": return "Материнский комплекс";
            case "father_complex": return "Отцовский комплекс";
            case "inferiority_complex": return "Комплекс неполноценности";
            case "power_complex": return "Комплекс власти";
            default: return archetype;
        }
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "keyword='" + keyword + '\'' +
                ", symbol='" + symbol + '\'' +
                ", interpretation='" + interpretation + '\'' +
                ", emotion='" + emotion + '\'' +
                ", archetype='" + archetype + '\'' +
                '}';
    }
}