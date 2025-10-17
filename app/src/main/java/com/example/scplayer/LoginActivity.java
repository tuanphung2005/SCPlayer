package com.example.scplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.auth.AuthManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private AuthManager authManager;
    private TextView welcomeText;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initializeViews();
        setupAuth();
        
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        handleIntent(intent);
    }
    
    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        btnLogin = findViewById(R.id.btnLogin);
    }
    
    private void setupAuth() {
        authManager = new AuthManager(this);
        
        if (authManager.isLoggedIn()) {
            navigateToMain();
            return;
        }
        
        btnLogin.setOnClickListener(v -> {
            String authUrl = authManager.getAuthorizationUrl();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            startActivity(browserIntent);
        });
    }
    
    private void handleIntent(Intent intent) {
        Log.d(TAG, "=== handleIntent START ===");
        
        if (intent == null) {
            Log.d(TAG, "Intent is null");
            return;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "Intent action: " + action);
        
        Uri data = intent.getData();
        Log.d(TAG, "Intent data URI: " + data);
        
        if (data == null) {
            Log.d(TAG, "No URI data in intent");
            return;
        }
        
        Log.d(TAG, "URI scheme: " + data.getScheme());
        Log.d(TAG, "URI host: " + data.getHost());
        Log.d(TAG, "URI path: " + data.getPath());
        Log.d(TAG, "URI query: " + data.getQuery());
        Log.d(TAG, "URI fragment: " + data.getFragment());
        Log.d(TAG, "Full URI string: " + data.toString());
        
        String redirectUri = ApiClient.getRedirectUri();
        Log.d(TAG, "Expected redirect URI: " + redirectUri);
        Log.d(TAG, "URI starts with redirect? " + data.toString().startsWith(redirectUri));
        
        if (data.toString().startsWith(redirectUri)) {
            Log.d(TAG, "Redirect URI matched! Processing OAuth response...");
            
            String code = data.getQueryParameter("code");
            String error = data.getQueryParameter("error");
            String errorDescription = data.getQueryParameter("error_description");
            
            Log.d(TAG, "Authorization code: " + code);
            Log.d(TAG, "Error: " + error);
            Log.d(TAG, "Error description: " + errorDescription);
            
            String fragment = data.getFragment();
            if (fragment != null) {
                Log.d(TAG, "Fragment found: " + fragment);
                if (fragment.contains("access_token=")) {
                    Log.d(TAG, "Access token found in fragment!");
                }
            }
            
            authManager.handleAuthorizationResponse(data, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess(String accessToken) {
                    Log.d(TAG, "Auth SUCCESS! Token: " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "✅ Login successful!", Toast.LENGTH_LONG).show();
                        navigateToMain();
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Auth ERROR: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "❌ Login failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            Log.d(TAG, "URI does not match redirect URI - ignoring");
        }
        
        Log.d(TAG, "=== handleIntent END ===");
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
