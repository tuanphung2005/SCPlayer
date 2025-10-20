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

    private EditText searchInput;
    private ImageButton btnClearSearch;
    private RecyclerView recyclerViewResults;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private SearchResultAdapter adapter;
    private SoundCloudApi api;
    
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 500; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupSearch();
        setupBottomNavigation();
    }

    private void initializeViews() {
        searchInput = findViewById(R.id.searchInput);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        adapter = new SearchResultAdapter(this);
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewResults.setAdapter(adapter);

        // API
        api = ApiClient.getSoundCloudApi();
    }

    private void setupSearch() {
        // Text change listener with debounce
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // Remove previous search callback
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Schedule new search
                if (s.length() > 0) {
                    searchRunnable = () -> performSearch(s.toString());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                } else {
                    // Clear results if search is empty
                    adapter.clearTracks();
                    showEmptyState(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            adapter.clearTracks();
            showEmptyState(true);
        });
    }

    private void performSearch(String query) {
        showLoading(true);
        showEmptyState(false);

        Call<SearchResponse> call = api.searchTracks(query.trim(), 20, 0);
        
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = response.body().getCollection();
                    
                    if (tracks != null && !tracks.isEmpty()) {
                        adapter.setTracks(tracks);
                        showEmptyState(false);
                    } else {
                        adapter.clearTracks();
                        showEmptyState(true);
                        Toast.makeText(SearchActivity.this, "No tracks found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    adapter.clearTracks();
                    showEmptyState(true);
                    Toast.makeText(SearchActivity.this, "Search failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showLoading(false);
                showEmptyState(true);
                Toast.makeText(SearchActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTrackClick(Track track, int position) {
        Toast.makeText(this, "Playing: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Implement play functionality
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_search);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_library) {
                startActivity(new Intent(this, LibraryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                return true;
            }
            
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
