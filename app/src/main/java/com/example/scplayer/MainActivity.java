package com.example.scplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.scplayer.adapters.TrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.auth.AuthManager;
import com.example.scplayer.models.SearchResponse;
import com.example.scplayer.models.Track;
import com.example.scplayer.player.MusicPlayerService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    
    // player
    private MusicPlayerService playerService;
    private View playerControls;
    private ImageView playerArtwork;
    private TextView playerTrackTitle;
    private TextView playerTrackArtist;
    private SeekBar playerSeekBar;
    private TextView playerCurrentTime;
    private TextView playerTotalTime;
    private ImageButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    
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
        setupPlayer();
        setupSearch();
    }
    
    private void initializeViews() {
        btnLogout = findViewById(R.id.btnLogout);
        
        searchInput = findViewById(R.id.searchInput);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerView = findViewById(R.id.recyclerView);
        
        playerControls = findViewById(R.id.playerControls);
        playerArtwork = findViewById(R.id.playerArtwork);
        playerTrackTitle = findViewById(R.id.playerTrackTitle);
        playerTrackArtist = findViewById(R.id.playerTrackArtist);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        playerCurrentTime = findViewById(R.id.playerCurrentTime);
        playerTotalTime = findViewById(R.id.playerTotalTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
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
    
    private void setupPlayer() {
        playerService = MusicPlayerService.getInstance(this);
        
        playerService.setPlayerListener(new MusicPlayerService.PlayerListener() {
            @Override
            public void onTrackChanged(Track track) {
                runOnUiThread(() -> updatePlayerUI(track));
            }
            
            @Override
            public void onPlaybackStateChanged(int state) {
                runOnUiThread(() -> updatePlayPauseButton());
            }
            
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    updatePlayPauseButton();
                    if (isPlaying) {
                        startSeekBarUpdate();
                    } else {
                        stopSeekBarUpdate();
                    }
                });
            }
        });
        
        btnPlayPause.setOnClickListener(v -> {
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.play();
            }
        });
        
        btnPrevious.setOnClickListener(v -> playerService.playPrevious());
        btnNext.setOnClickListener(v -> playerService.playNext());
        
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerService.seekTo(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        setupSeekBarUpdate();
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
        playerControls.setVisibility(View.VISIBLE);
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
    
    private void updatePlayerUI(Track track) {
        if (track == null) return;
        
        playerTrackTitle.setText(track.getTitle());
        playerTrackArtist.setText(track.getUser() != null ? track.getUser().getUsername() : "Unknown Artist");
        
        String artworkUrl = track.getHighQualityArtworkUrl();
        if (artworkUrl != null) {
            Glide.with(this)
                    .load(artworkUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(playerArtwork);
        }
        
        long duration = playerService.getDuration();
        if (duration > 0) {
            playerSeekBar.setMax((int) duration);
            playerTotalTime.setText(formatTime(duration));
        }
    }
    
    private void updatePlayPauseButton() {
        if (playerService.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }
    
    private void setupSeekBarUpdate() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (playerService != null && playerService.isPlaying()) {
                    long currentPosition = playerService.getCurrentPosition();
                    playerSeekBar.setProgress((int) currentPosition);
                    playerCurrentTime.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }
    
    private void startSeekBarUpdate() {
        handler.post(updateSeekBar);
    }
    
    private void stopSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBar);
    }
    
    private String formatTime(long milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdate();
    }
}