package com.lionido.dreams_track.utils;

import android.content.Context;
import android.os.Environment;
import com.lionido.dreams_track.database.DreamEntity;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportUtils {
    public static File exportDreamsToTxt(Context context, List<DreamEntity> dreams) throws IOException {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "dreams_export.txt");
        FileWriter writer = new FileWriter(file);
        for (DreamEntity dream : dreams) {
            writer.write("Сон от: " + dream.getTimestamp() + "\n");
            writer.write(dream.getText() + "\n");
            writer.write("Эмоция: " + dream.getEmotion() + "\n");
            writer.write("Символы: " + (dream.getSymbols() != null ? dream.getSymbols().toString() : "") + "\n");
            writer.write("---\n");
        }
        writer.close();
        return file;
    }
    public static File exportDreamsToJson(Context context, List<DreamEntity> dreams) throws IOException {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "dreams_export.json");
        FileWriter writer = new FileWriter(file);
        writer.write(new Gson().toJson(dreams));
        writer.close();
        return file;
    }
}