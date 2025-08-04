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