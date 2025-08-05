package com.lionido.dreams_track.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.lionido.dreams_track.model.Symbol;
import java.util.List;

@Entity(tableName = "dreams")
@TypeConverters({Converters.class})
public class DreamEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String text;
    private String audioPath;
    private long timestamp;
    private List<Symbol> symbols;
    private String emotion;
    private String inputMethod; // "voice" или "text"
    private String interpretation; // Интерпретация сна
    private String analysis; // Анализ сна

    public DreamEntity() {
        this.timestamp = System.currentTimeMillis();
    }

    public DreamEntity(String text, String audioPath, String inputMethod) {
        this.text = text;
        this.audioPath = audioPath;
        this.inputMethod = inputMethod;
        this.timestamp = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<Symbol> getSymbols() { return symbols; }
    public void setSymbols(List<Symbol> symbols) { this.symbols = symbols; }

    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }

    public String getInputMethod() { return inputMethod; }
    public void setInputMethod(String inputMethod) { this.inputMethod = inputMethod; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
}