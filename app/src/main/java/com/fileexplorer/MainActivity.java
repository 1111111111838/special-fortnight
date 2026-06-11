package com.fileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements FileAdapter.FileClickListener {

    private static final int REQUEST_PERMISSION = 1001;
    private static final int REQUEST_MANAGE_STORAGE = 1002;

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private TextView tvCurrentPath;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private FloatingActionButton fabHome;

    private File currentDirectory;
    private final Stack<File> backStack = new Stack<>();
    private List<FileItem> allFiles = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        checkPermissions();
    }

    private void setupViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        fabHome = findViewById(R.id.fabHome);

        fileAdapter = new FileAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileAdapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadDirectory(currentDirectory);
            swipeRefresh.setRefreshing(false);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim();
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterFiles();
            }
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        fabHome.setOnClickListener(v -> {
            backStack.clear();
            loadDirectory(Environment.getExternalStorageDirectory());
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                    .setTitle("הרשאת גישה לקבצים")
                    .setMessage("האפליקציה צריכה גישה מלאה לאחסון כדי לגלוש בקבצים שלך.")
                    .setPositiveButton("אשר", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                    })
                    .setNegativeButton("ביטול", (d, w) -> finish())
                    .show();
            } else {
                startExplorer();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
            } else {
                startExplorer();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                startExplorer();
            } else {
                Toast.makeText(this, "נדרשת הרשאה לגישה לקבצים", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startExplorer();
        } else {
            Toast.makeText(this, "נדרשת הרשאה לגישה לקבצים", Toast.LENGTH_LONG).show();
        }
    }

    private void startExplorer() {
        currentDirectory = Environment.getExternalStorageDirectory();
        loadDirectory(currentDirectory);
    }

    private void loadDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        currentDirectory = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());

        allFiles = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : files) {
                if (!f.getName().startsWith(".")) {
                    allFiles.add(new FileItem(f));
                }
            }
        }
        filterFiles();
    }

    private void filterFiles() {
        List<FileItem> filtered = new ArrayList<>();
        if (searchQuery.isEmpty()) {
            filtered.addAll(allFiles);
        } else {
            String q = searchQuery.toLowerCase();
            for (FileItem item : allFiles) {
                if (item.getFile().getName().toLowerCase().contains(q)) {
                    filtered.add(item);
                }
            }
        }
        fileAdapter.setFiles(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFileClick(FileItem fileItem) {
        File file = fileItem.getFile();
        if (file.isDirectory()) {
            backStack.push(currentDirectory);
            loadDirectory(file);
        } else {
            openFile(file);
        }
    }

    @Override
    public void onFileLongClick(FileItem fileItem) {
        showFileOptions(fileItem.getFile());
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "*/*";
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "פתח עם..."));
        } catch (Exception e) {
            Toast.makeText(this, "לא ניתן לפתוח קובץ זה", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFileOptions(File file) {
        String[] options = {"העתק נתיב", "מחק", "שנה שם"};
        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0: copyPath(file); break;
                    case 1: confirmDelete(file); break;
                    case 2: renameFile(file); break;
                }
            })
            .show();
    }

    private void copyPath(File file) {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("path", file.getAbsolutePath()));
        Toast.makeText(this, "הנתיב הועתק", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
            .setTitle("מחיקה")
            .setMessage("האם למחוק את " + file.getName() + "?")
            .setPositiveButton("מחק", (d, w) -> {
                if (file.delete()) {
                    loadDirectory(currentDirectory);
                    Toast.makeText(this, "נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("ביטול", null)
            .show();
    }

    private void renameFile(File file) {
        EditText et = new EditText(this);
        et.setText(file.getName());
        et.selectAll();
        new AlertDialog.Builder(this)
            .setTitle("שנה שם")
            .setView(et)
            .setPositiveButton("שמור", (d, w) -> {
                String newName = et.getText().toString().trim();
                if (!newName.isEmpty()) {
                    File newFile = new File(file.getParent(), newName);
                    if (file.renameTo(newFile)) {
                        loadDirectory(currentDirectory);
                        Toast.makeText(this, "השם שונה", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "שגיאה בשינוי שם", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("ביטול", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (!searchQuery.isEmpty()) {
            etSearch.setText("");
        } else if (!backStack.isEmpty()) {
            loadDirectory(backStack.pop());
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort_name) {
            Collections.sort(allFiles, (a, b) -> a.getFile().getName().compareToIgnoreCase(b.getFile().getName()));
            filterFiles();
            return true;
        } else if (item.getItemId() == R.id.action_sort_size) {
            Collections.sort(allFiles, (a, b) -> Long.compare(b.getFile().length(), a.getFile().length()));
            filterFiles();
            return true;
        } else if (item.getItemId() == R.id.action_sort_date) {
            Collections.sort(allFiles, (a, b) -> Long.compare(b.getFile().lastModified(), a.getFile().lastModified()));
            filterFiles();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
