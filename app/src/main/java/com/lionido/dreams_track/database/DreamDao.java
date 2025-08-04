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

    @Query("DELETE FROM dreams WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM dreams ORDER BY timestamp DESC")
    List<DreamEntity> getAllDreams();

    @Query("SELECT * FROM dreams WHERE id = :id")
    DreamEntity getDreamById(int id);

    @Query("SELECT * FROM dreams WHERE text LIKE :searchQuery ORDER BY timestamp DESC")
    List<DreamEntity> searchDreams(String searchQuery);

    @Query("SELECT COUNT(*) FROM dreams")
    int getDreamCount();

    @Query("SELECT * FROM dreams WHERE emotion = :emotion ORDER BY timestamp DESC")
    List<DreamEntity> getDreamsByEmotion(String emotion);
}