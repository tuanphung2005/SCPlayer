package com.example.scplayer.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.AccessToken;
import com.example.scplayer.utils.ApiConstants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthManager {
    private static final String TAG = "AuthManager";
    
    private static final String AUTH_URL = "https://soundcloud.com/connect";
    private static final String SCOPE = "non-expiring";
    
    private final Context ctx;
    private final SharedPreferences prefs;
    private final SoundCloudApi api;
    
    public AuthManager(Context context) {
        this.ctx = context;
        this.prefs = context.getSharedPreferences(ApiConstants.PREFS_NAME, Context.MODE_PRIVATE);
        this.api = ApiClient.getSoundCloudApi();
    }
    
    public String getAuthorizationUrl() {
        Uri.Builder builder = Uri.parse(AUTH_URL).buildUpon();
        builder.appendQueryParameter("client_id", ApiClient.getClientId());
        builder.appendQueryParameter("redirect_uri", ApiClient.getRedirectUri());
        builder.appendQueryParameter("response_type", "code");
        builder.appendQueryParameter("scope", SCOPE);
        
        String url = builder.build().toString();
        Log.d(TAG, "=== Authorization URL Generated ===");
        Log.d(TAG, "Client ID: " + ApiClient.getClientId());
        Log.d(TAG, "Redirect URI: " + ApiClient.getRedirectUri());
        Log.d(TAG, "Response Type: code");
        Log.d(TAG, "Scope: " + SCOPE);
        Log.d(TAG, "Full Auth URL: " + url);
        Log.d(TAG, "===================================");
        
        return url;
    }
    
    public void handleAuthorizationResponse(Uri uri, AuthCallback callback) {
        Log.d(TAG, "=== handleAuthorizationResponse ===");
        Log.d(TAG, "URI: " + uri.toString());
        
        String code = uri.getQueryParameter("code");
        String err = uri.getQueryParameter("error");
        String desc = uri.getQueryParameter("error_description");
        
        Log.d(TAG, "Authorization code: " + code);
        Log.d(TAG, "Error: " + err);
        Log.d(TAG, "Error description: " + desc);
        
        if (err != null) {
            String msg = "Authorization failed: " + err;
            if (desc != null) {
                msg += " - " + desc;
            }
            Log.e(TAG, msg);
            callback.onError(msg);
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
        String secret = ApiClient.getClientSecret();
        Log.d(TAG, "Client Secret: " + (secret != null ? "***" + secret.substring(Math.max(0, secret.length() - 4)) : "null"));
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
            public void onResponse(Call<AccessToken> call, Response<AccessToken> res) {
                Log.d(TAG, "=== Token Exchange Response ===");
                Log.d(TAG, "Response code: " + res.code());
                Log.d(TAG, "Response successful: " + res.isSuccessful());
                
                if (res.isSuccessful() && res.body() != null) {
                    AccessToken token = res.body();
                    Log.d(TAG, "✅ Token received successfully!");
                    Log.d(TAG, "Access token: " + token.getAccessToken().substring(0, Math.min(10, token.getAccessToken().length())) + "...");
                    Log.d(TAG, "Token type: " + token.getTokenType());
                    Log.d(TAG, "Expires in: " + token.getExpiresIn());
                    Log.d(TAG, "Scope: " + token.getScope());
                    
                    saveToken(token);
                    callback.onSuccess(token.getAccessToken());
                } else {
                    String msg = "Failed to get access token: " + res.code();
                    try {
                        if (res.errorBody() != null) {
                            String body = res.errorBody().string();
                            Log.e(TAG, "Error body: " + body);
                            msg += " - " + body;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, msg);
                    callback.onError(msg);
                }
            }
            
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String msg = "Network error: " + t.getMessage();
                Log.e(TAG, msg, t);
                callback.onError(msg);
            }
        });
    }
    

    private void saveToken(AccessToken token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ApiConstants.KEY_ACCESS_TOKEN, token.getAccessToken());
        editor.putString(ApiConstants.KEY_REFRESH_TOKEN, token.getRefreshToken());
        
        long expiry = System.currentTimeMillis() + (token.getExpiresIn() * 1000);
        editor.putLong(ApiConstants.KEY_TOKEN_EXPIRY, expiry);
        
        editor.apply();
    }
    
    public String getAccessToken() {
        return prefs.getString(ApiConstants.KEY_ACCESS_TOKEN, null);
    }
    
    public boolean isTokenExpired() {
        long expiry = prefs.getLong(ApiConstants.KEY_TOKEN_EXPIRY, 0);
        return System.currentTimeMillis() >= expiry;
    }
    
    public boolean isLoggedIn() {
        return getAccessToken() != null && !isTokenExpired();
    }
    
    public void logout() {
        prefs.edit().clear().apply();
    }
    
    public interface AuthCallback {
        void onSuccess(String token);
        void onError(String error);
    }
}
