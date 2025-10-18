package com.example.scplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.auth.AuthManager;
import com.example.scplayer.models.SearchResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AuthManager auth;
    private Button logout;
    private TextInputEditText input;
    private Button search;
    private SoundCloudApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ApiClient.initialize(this);
        auth = new AuthManager(this);
        
        if (!auth.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        
        initializeViews();
        setupAuth();
        setupSearch();
    }
    
    private void initializeViews() {
        logout = findViewById(R.id.btnLogout);
        input = findViewById(R.id.searchInput);
        search = findViewById(R.id.btnSearch);
    }
    
    private void setupAuth() {
        api = ApiClient.getSoundCloudApi();
        
        logout.setOnClickListener(v -> {
            auth.logout();
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
        search.setOnClickListener(v -> {
            String q = input.getText().toString().trim();
            if (!q.isEmpty()) {
                searchTracks(q);
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void searchTracks(String q) {
        Call<SearchResponse> call = api.searchTracks(q, 20, 0);
        
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> res) {
                if (res.isSuccessful() && res.body() != null) {
                    int count = res.body().getCollection().size();
                    runOnUiThread(() -> {
                        if (count == 0) {
                            Toast.makeText(MainActivity.this, "No tracks found", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Found " + count + " tracks", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Search failed: " + res.code(), Toast.LENGTH_SHORT).show();
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
}