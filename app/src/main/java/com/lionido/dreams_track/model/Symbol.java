package com.lionido.dreams_track.model;

public class Symbol {
    private String keyword;
    private String symbol;
    private String interpretation;
    private String emotion;

    public Symbol(String keyword, String symbol, String interpretation, String emotion) {
        this.keyword = keyword;
        this.symbol = symbol;
        this.interpretation = interpretation;
        this.emotion = emotion;
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
}