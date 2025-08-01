// DreamEntity.java
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
}

// Converters.java
package com.lionido.dreams_track.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lionido.dreams_track.model.Symbol;
import java.lang.reflect.Type;
import java.util.List;

public class Converters {
    private static Gson gson = new Gson();

    @TypeConverter
    public static String fromSymbolList(List<Symbol> symbols) {
        if (symbols == null) {
            return null;
        }
        return gson.toJson(symbols);
    }

    @TypeConverter
    public static List<Symbol> toSymbolList(String symbolsString) {
        if (symbolsString == null) {
            return null;
        }
        Type listType = new TypeToken<List<Symbol>>(){}.getType();
        return gson.fromJson(symbolsString, listType);
    }
}

// DreamDao.java
package com.lionido.dreams_track.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DreamDao {
    @Insert
    long insert(DreamEntity dream);

    @Update
    void update(DreamEntity dream);

    @Delete
    void delete(DreamEntity dream);

    @Query("SELECT * FROM dreams ORDER BY timestamp DESC")
    List<DreamEntity> getAllDreams();

    @Query("SELECT * FROM dreams WHERE id = :id")
    DreamEntity getDreamById(int id);

    @Query("SELECT * FROM dreams WHERE text LIKE :searchQuery ORDER BY timestamp DESC")
    List<DreamEntity> searchDreams(String searchQuery);

    @Query("SELECT COUNT(*) FROM dreams")
    int getDreamCount();
}

// AppDatabase.java
package com.lionido.dreams_track.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {DreamEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DreamDao dreamDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "dream_database")
                            .allowMainThreadQueries() // Только для простоты, в продакшене использовать AsyncTask
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}