package com.lionido.dreams_track;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.core.content.FileProvider;
import com.lionido.dreams_track.utils.ExportUtils;
import java.io.File;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.lionido.dreams_track.activity.RecordDreamActivity;
import com.lionido.dreams_track.adapter.DreamAdapter;
import com.lionido.dreams_track.database.AppDatabase;
import com.lionido.dreams_track.database.DreamDao;
import com.lionido.dreams_track.database.DreamEntity;
import com.lionido.dreams_track.model.Dream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private Button recordButton;
    private ImageButton atlasButton;
    private RecyclerView dreamsRecyclerView;
    private DreamAdapter dreamAdapter;
    private List<Dream> dreamsList;

    private AppDatabase database;
    private DreamDao dreamDao;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeDatabase();
        initializeViews();
        setupRecyclerView();
        checkPermissions();
    }

    private void initializeDatabase() {
        database = AppDatabase.getDatabase(this);
        dreamDao = database.dreamDao();
        executor = Executors.newFixedThreadPool(2);
    }

    private void initializeViews() {
        recordButton = findViewById(R.id.btn_record_dream);
        atlasButton = findViewById(R.id.btn_atlas);

        recordButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startRecordActivity();
            } else {
                requestPermissions();
            }
        });

        atlasButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.lionido.dreams_track.activity.AtlasActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        dreamsList = new ArrayList<>();
        dreamAdapter = new DreamAdapter(dreamsList, dream -> {
            // Переход к деталям сна
            Intent intent = new Intent(this, com.lionido.dreams_track.activity.DreamDetailActivity.class);
            intent.putExtra("dream_id", dream.getId());
            startActivity(intent);
        });
        dreamAdapter.setOnDreamDeleteListener((dream, position) -> {
            deleteDreamFromDb(dream, position);
        });
        dreamsRecyclerView = findViewById(R.id.recycler_dreams);
        dreamsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dreamsRecyclerView.setAdapter(dreamAdapter);
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Dream dream = dreamsList.get(position);
                deleteDreamFromDb(dream, position);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(dreamsRecyclerView);
    }

    private void startRecordActivity() {
        Intent intent = new Intent(this, RecordDreamActivity.class);
        startActivity(intent);
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecordActivity();
            } else {
                Toast.makeText(this, "Разрешение на запись аудио необходимо для работы приложения",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDreams();
    }

    private void loadDreams() {
        executor.execute(() -> {
            List<DreamEntity> dreamEntities = dreamDao.getAllDreams();

            runOnUiThread(() -> {
                dreamsList.clear();
                for (DreamEntity entity : dreamEntities) {
                    Dream dream = convertEntityToDream(entity);
                    dreamsList.add(dream);
                }
                dreamAdapter.notifyDataSetChanged();
            });
        });
    }

    private Dream convertEntityToDream(DreamEntity entity) {
        Dream dream = new Dream(entity.getText(), entity.getAudioPath());
        dream.setId(entity.getId());
        dream.setTimestamp(entity.getTimestamp());
        dream.setSymbols(entity.getSymbols());
        dream.setEmotion(entity.getEmotion());
        return dream;
    }

    private void deleteDreamFromDb(Dream dream, int position) {
        executor.execute(() -> {
            dreamDao.delete(new DreamEntity(dream.getText(), dream.getAudioPath(), ""));
            runOnUiThread(() -> {
                dreamAdapter.removeDream(position);
                Toast.makeText(this, "Сон удалён", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_txt) {
            exportDreams("txt");
            return true;
        } else if (item.getItemId() == R.id.action_export_json) {
            exportDreams("json");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void exportDreams(String type) {
        executor.execute(() -> {
            try {
                List<DreamEntity> dreamEntities = dreamDao.getAllDreams();
                File file = type.equals("txt") ? ExportUtils.exportDreamsToTxt(this, dreamEntities) : ExportUtils.exportDreamsToJson(this, dreamEntities);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Экспортировано: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}