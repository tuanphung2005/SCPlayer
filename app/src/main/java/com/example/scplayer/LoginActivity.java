package com.example.scplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.auth.AuthManager;
import com.example.scplayer.utils.NavigationHelper;

public class LoginActivity extends AppCompatActivity {
    private AuthManager auth;
    private TextView welcome;
    private Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        ApiClient.initialize(this);
        
        initializeViews();
        setupAuth();
        
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void initializeViews() {
        welcome = findViewById(R.id.welcomeText);
        login = findViewById(R.id.btnLogin);
    }
    
    private void setupAuth() {
        auth = new AuthManager(this);
        
        if (auth.isLoggedIn()) {
            NavigationHelper.navigateToMain(this);
            return;
        }
        
        login.setOnClickListener(v -> {
            String url = auth.getAuthorizationUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }
    
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        
        Uri uri = intent.getData();
        String redirect = ApiClient.getRedirectUri();
        
        if (uri.toString().startsWith(redirect)) {
            auth.handleAuthorizationResponse(uri, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess(String token) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_LONG).show();
                        NavigationHelper.navigateToMain(LoginActivity.this);
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }
}
