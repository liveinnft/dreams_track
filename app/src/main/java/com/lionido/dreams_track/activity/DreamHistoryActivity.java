package com.lionido.dreams_track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lionido.dreams_track.R;
import com.lionido.dreams_track.adapter.DreamAdapter;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Dream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DreamHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DreamAdapter dreamAdapter;
    private List<Dream> dreamList;
    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;
    private View emptyView;
    private Button btnAddFirstDream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dream_history);

        initializeDatabase();
        initializeViews();
        setupRecyclerView();
        loadDreams();
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recycler_dreams);
        emptyView = findViewById(R.id.layout_empty);
        btnAddFirstDream = findViewById(R.id.btn_add_first_dream);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(DreamHistoryActivity.this, RecordDreamActivity.class);
                startActivity(intent);
            });
        }

        if (btnAddFirstDream != null) {
            btnAddFirstDream.setOnClickListener(v -> {
                Intent intent = new Intent(DreamHistoryActivity.this, RecordDreamActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        dreamList = new ArrayList<>();
        dreamAdapter = new DreamAdapter(dreamList, dream -> {
            Intent intent = new Intent(DreamHistoryActivity.this, DreamDetailActivity.class);
            intent.putExtra("dream_id", dream.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(dreamAdapter);
    }

    private void loadDreams() {
        executor.execute(() -> {
            try {
                List<DreamEntity> dreams = dreamDao.getAllDreams();

                runOnUiThread(() -> {
                    dreamList.clear();
                    // Преобразуем DreamEntity в Dream
                    for (DreamEntity entity : dreams) {
                        String dreamText = entity.getText() != null ? entity.getText() : "";
                        String audioPath = entity.getAudioPath() != null ? entity.getAudioPath() : "";

                        Dream dream = new Dream(dreamText, audioPath);
                        dream.setId(entity.getId());
                        dream.setTimestamp(entity.getTimestamp());
                        dream.setSymbols(entity.getSymbols());
                        dream.setEmotion(entity.getEmotion());
                        dreamList.add(dream);
                    }
                    dreamAdapter.notifyDataSetChanged();

                    if (dreams.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        // Показать ошибку или пустое состояние
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагружаем данные при возврате в активность
        loadDreams();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}