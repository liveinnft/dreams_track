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
import java.util.List;

public class SymbolAnalyzer {

    private Context context;
    private List<Symbol> symbols;

    public SymbolAnalyzer(Context context) {
        this.context = context;
        this.symbols = loadSymbolsFromAsset();
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
            Log.e("SymbolAnalyzer", "Ошибка загрузки символов", e);
        }
        return list;
    }

    public List<Symbol> findSymbolsInText(String text) {
        List<Symbol> found = new ArrayList<>();
        text = text.toLowerCase();

        for (Symbol symbol : symbols) {
            if (text.contains(symbol.getKeyword())) {
                found.add(symbol);
            }
        }
        return found;
    }

    public List<Symbol> getAllSymbols() {
        return new ArrayList<>(symbols);
    }
}