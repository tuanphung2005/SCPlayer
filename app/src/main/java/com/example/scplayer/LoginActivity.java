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
        Log.d(TAG, "onNewIntent called");
        handleIntent(intent);
    }
    
    private void initializeViews() {
        welcome = findViewById(R.id.welcomeText);
        login = findViewById(R.id.btnLogin);
    }
    
    private void setupAuth() {
        auth = new AuthManager(this);
        
        if (auth.isLoggedIn()) {
            navigateToMain();
            return;
        }
        
        login.setOnClickListener(v -> {
            String url = auth.getAuthorizationUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
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
        
        Uri uri = intent.getData();
        Log.d(TAG, "Intent data URI: " + uri);
        
        if (uri == null) {
            Log.d(TAG, "No URI data in intent");
            return;
        }
        
        Log.d(TAG, "URI scheme: " + uri.getScheme());
        Log.d(TAG, "URI host: " + uri.getHost());
        Log.d(TAG, "URI path: " + uri.getPath());
        Log.d(TAG, "URI query: " + uri.getQuery());
        Log.d(TAG, "URI fragment: " + uri.getFragment());
        Log.d(TAG, "Full URI string: " + uri.toString());
        
        String redirect = ApiClient.getRedirectUri();
        Log.d(TAG, "Expected redirect URI: " + redirect);
        Log.d(TAG, "URI starts with redirect? " + uri.toString().startsWith(redirect));
        
        if (uri.toString().startsWith(redirect)) {
            Log.d(TAG, "Redirect URI matched! Processing OAuth response...");
            
            String code = uri.getQueryParameter("code");
            String err = uri.getQueryParameter("error");
            String desc = uri.getQueryParameter("error_description");
            
            Log.d(TAG, "Authorization code: " + code);
            Log.d(TAG, "Error: " + err);
            Log.d(TAG, "Error description: " + desc);
            
            String fragment = uri.getFragment();
            if (fragment != null) {
                Log.d(TAG, "Fragment found: " + fragment);
                if (fragment.contains("access_token=")) {
                    Log.d(TAG, "Access token found in fragment!");
                }
            }
            
            auth.handleAuthorizationResponse(uri, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess(String token) {
                    Log.d(TAG, "Auth SUCCESS! Token: " + token.substring(0, Math.min(10, token.length())) + "...");
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_LONG).show();
                        navigateToMain();
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Auth ERROR: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "`Login failed: " + error, Toast.LENGTH_LONG).show();
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
