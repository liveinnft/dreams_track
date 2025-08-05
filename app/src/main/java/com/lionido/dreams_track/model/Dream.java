package com.lionido.dreams_track.model;

import java.util.Date;
import java.util.List;

public class Dream {
    private int id;
    private String text;
    private String audioPath;
    private long timestamp;
    private List<Symbol> symbols;
    private String emotion;
    private String interpretation;
    private Date date;

    public Dream() {
        this.timestamp = System.currentTimeMillis();
    }

    public Dream(String text, String audioPath) {
        this.text = text;
        this.audioPath = audioPath;
        this.timestamp = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<Symbol> symbols) {
        this.symbols = symbols;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public String getInterpretation() {
        return interpretation;
    }
    
    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
}