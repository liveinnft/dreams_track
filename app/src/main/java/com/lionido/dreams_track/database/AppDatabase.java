package com.lionido.dreams_track.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(entities = {DreamEntity.class}, version = 2, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract DreamDao dreamDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "dream_database")
                            // Добавляем миграцию для обновления с версии 1 до 2
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    // Простая миграция с версии 1 до 2
    static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override
        public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            // Добавляем недостающие столбцы в таблицу dreams только если они еще не существуют
            addColumnIfNotExists(database, "emotion", "TEXT");
            addColumnIfNotExists(database, "inputMethod", "TEXT");
            addColumnIfNotExists(database, "interpretation", "TEXT");
            addColumnIfNotExists(database, "analysis", "TEXT");
        }
        
        private void addColumnIfNotExists(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database, 
                                          String columnName, 
                                          String columnType) {
            try {
                // Попробуем добавить столбец - если он уже существует, будет исключение
                database.execSQL("ALTER TABLE dreams ADD COLUMN " + columnName + " " + columnType);
            } catch (Exception e) {
                // Если столбец уже существует, просто игнорируем ошибку
            }
        }
    };
}