package com.example.scplayer.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.AccessToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "SoundCloudAuth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    
    private static final String AUTHORIZATION_URL = "https://soundcloud.com/connect";
    private static final String SCOPE = "non-expiring";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final SoundCloudApi api;
    
    public AuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.api = ApiClient.getSoundCloudApi();
    }
    
    public String getAuthorizationUrl() {
        Uri.Builder builder = Uri.parse(AUTHORIZATION_URL).buildUpon();
        builder.appendQueryParameter("client_id", ApiClient.getClientId());
        builder.appendQueryParameter("redirect_uri", ApiClient.getRedirectUri());
        builder.appendQueryParameter("response_type", "code");
        builder.appendQueryParameter("scope", SCOPE);
        
        String authUrl = builder.build().toString();
        Log.d(TAG, "=== Authorization URL Generated ===");
        Log.d(TAG, "Client ID: " + ApiClient.getClientId());
        Log.d(TAG, "Redirect URI: " + ApiClient.getRedirectUri());
        Log.d(TAG, "Response Type: code");
        Log.d(TAG, "Scope: " + SCOPE);
        Log.d(TAG, "Full Auth URL: " + authUrl);
        Log.d(TAG, "===================================");
        
        return authUrl;
    }
    
    public void handleAuthorizationResponse(Uri uri, AuthCallback callback) {
        Log.d(TAG, "=== handleAuthorizationResponse ===");
        Log.d(TAG, "URI: " + uri.toString());
        
        String code = uri.getQueryParameter("code");
        String error = uri.getQueryParameter("error");
        String errorDescription = uri.getQueryParameter("error_description");
        
        Log.d(TAG, "Authorization code: " + code);
        Log.d(TAG, "Error: " + error);
        Log.d(TAG, "Error description: " + errorDescription);
        
        if (error != null) {
            String errorMsg = "Authorization failed: " + error;
            if (errorDescription != null) {
                errorMsg += " - " + errorDescription;
            }
            Log.e(TAG, errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        if (code != null) {
            Log.d(TAG, "Authorization code received, exchanging for token...");
            exchangeCodeForToken(code, callback);
        } else {
            Log.e(TAG, "No authorization code received");
            callback.onError("No authorization code received");
        }
    }
    
    private void exchangeCodeForToken(String code, AuthCallback callback) {
        Log.d(TAG, "=== exchangeCodeForToken ===");
        Log.d(TAG, "Code: " + code);
        Log.d(TAG, "Client ID: " + ApiClient.getClientId());
        Log.d(TAG, "Client Secret: " + (ApiClient.getClientSecret() != null ? "***" + ApiClient.getClientSecret().substring(Math.max(0, ApiClient.getClientSecret().length() - 4)) : "null"));
        Log.d(TAG, "Redirect URI: " + ApiClient.getRedirectUri());
        
        Call<AccessToken> call = api.getAccessToken(
                "authorization_code",
                ApiClient.getClientId(),
                ApiClient.getClientSecret(),
                ApiClient.getRedirectUri(),
                code
        );
        
        Log.d(TAG, "Making token exchange request...");
        
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                Log.d(TAG, "=== Token Exchange Response ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response successful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    AccessToken token = response.body();
                    Log.d(TAG, "✅ Token received successfully!");
                    Log.d(TAG, "Access token: " + token.getAccessToken().substring(0, Math.min(10, token.getAccessToken().length())) + "...");
                    Log.d(TAG, "Token type: " + token.getTokenType());
                    Log.d(TAG, "Expires in: " + token.getExpiresIn());
                    Log.d(TAG, "Scope: " + token.getScope());
                    
                    saveToken(token);
                    callback.onSuccess(token.getAccessToken());
                } else {
                    String errorMsg = "Failed to get access token: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                            errorMsg += " - " + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    public void refreshAccessToken(RefreshCallback callback) {
        String refreshToken = getRefreshToken();
        if (refreshToken == null) {
            callback.onError("No refresh token available");
            return;
        }
        
        Call<AccessToken> call = api.refreshToken(
                "refresh_token",
                ApiClient.getClientId(),
                ApiClient.getClientSecret(),
                refreshToken
        );
        
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccessToken token = response.body();
                    saveToken(token);
                    callback.onSuccess(token.getAccessToken());
                } else {
                    callback.onError("Failed to refresh token: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void saveToken(AccessToken token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, token.getAccessToken());
        editor.putString(KEY_REFRESH_TOKEN, token.getRefreshToken());
        
        long expiryTime = System.currentTimeMillis() + (token.getExpiresIn() * 1000);
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTime);
        
        editor.apply();
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    public boolean isTokenExpired() {
        long expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        return System.currentTimeMillis() >= expiryTime;
    }
    
    public boolean isLoggedIn() {
        return getAccessToken() != null && !isTokenExpired();
    }
    
    public void logout() {
        prefs.edit().clear().apply();
    }
    
    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onError(String error);
    }
    
    public interface RefreshCallback {
        void onSuccess(String accessToken);
        void onError(String error);
    }
}
