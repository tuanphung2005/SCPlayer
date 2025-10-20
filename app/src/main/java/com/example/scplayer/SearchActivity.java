package com.example.scplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.adapters.SearchResultAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.SearchResponse;
import com.example.scplayer.models.Track;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SearchResultAdapter.OnTrackClickListener {

    private EditText input;
    private ImageButton clear;
    private RecyclerView results;
    private LinearLayout empty;
    private ProgressBar loading;
    private SearchResultAdapter adapter;
    private SoundCloudApi api;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private static final int DELAY = 500; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupSearch();
        setupBottomNavigation();
    }

    private void initializeViews() {
        input = findViewById(R.id.searchInput);
        clear = findViewById(R.id.btnClearSearch);
        results = findViewById(R.id.recyclerViewResults);
        empty = findViewById(R.id.emptyState);
        loading = findViewById(R.id.progressBar);

        // recycler
        adapter = new SearchResultAdapter(this);
        results.setLayoutManager(new LinearLayoutManager(this));
        results.setAdapter(adapter);

        // api
        api = ApiClient.getSoundCloudApi();
    }

    private void setupSearch() {
        // search listener
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                if (s.length() > 0) {
                    runnable = () -> performSearch(s.toString());
                    handler.postDelayed(runnable, DELAY);
                } else {
                    adapter.clearTracks();
                    showEmpty(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // clear button
        clear.setOnClickListener(v -> {
            input.setText("");
            adapter.clearTracks();
            showEmpty(true);
        });
    }

    private void performSearch(String q) {
        showLoading(true);
        showEmpty(false);

        Call<List<Track>> call = api.searchTracks(q.trim(), 20, 0);
        
        call.enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                showLoading(false);
                
                if (res.isSuccessful() && res.body() != null) {
                    List<Track> tracks = res.body();
                    
                    if (!tracks.isEmpty()) {
                        adapter.setTracks(tracks);
                        showEmpty(false);
                    } else {
                        adapter.clearTracks();
                        showEmpty(true);
                        Toast.makeText(SearchActivity.this, "No tracks found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    adapter.clearTracks();
                    showEmpty(true);
                    Toast.makeText(SearchActivity.this, "Search failed: " + res.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                showLoading(false);
                showEmpty(true);
                Toast.makeText(SearchActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        loading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTrackClick(Track track, int position) {
        Toast.makeText(this, "Playing: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: play track
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_search);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(this, LibraryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                return true;
            }
            
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
