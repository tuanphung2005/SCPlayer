package com.example.scplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.adapters.TrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.auth.AuthManager;
import com.example.scplayer.models.SearchResponse;
import com.example.scplayer.models.Track;
import com.example.scplayer.player.MusicPlayerService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TrackAdapter.OnTrackClickListener {

    private static final String TAG = "MainActivity";

    // auth
    private AuthManager authManager;
    private Button btnLogout;
    
    // search
    private TextInputEditText searchInput;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private TrackAdapter trackAdapter;
    
    private MusicPlayerService playerService;
    
    private SoundCloudApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        authManager = new AuthManager(this);
        
        if (!authManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        initializeViews();
        setupAuth();
        setupSearch();
        
        playerService = MusicPlayerService.getInstance(this);
    }
    
    private void initializeViews() {
        btnLogout = findViewById(R.id.btnLogout);
        
        searchInput = findViewById(R.id.searchInput);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerView = findViewById(R.id.recyclerView);
    }
    
    private void setupAuth() {
        api = ApiClient.getSoundCloudApi();
        
        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void setupSearch() {
        trackAdapter = new TrackAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(trackAdapter);
        
        btnSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchTracks(query);
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void searchTracks(String query) {
        Call<SearchResponse> call = api.searchTracks(query, ApiClient.getClientId(), 20, 0);
        
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = response.body().getCollection();
                    runOnUiThread(() -> {
                        trackAdapter.setTracks(tracks);
                        if (tracks.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No tracks found", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Search failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    @Override
    public void onPlayClick(Track track, int position) {
        playerService.playTrack(track);
    }
    
    @Override
    public void onLikeClick(Track track, int position) {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to like tracks", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = authManager.getAccessToken();
        Call<Void> call = api.likeTrack(track.getId(), token);
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Track liked!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to like track", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}