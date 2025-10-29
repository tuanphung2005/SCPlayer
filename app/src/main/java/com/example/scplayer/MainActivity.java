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
import com.example.scplayer.utils.NavigationHelper;
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
        
        ApiClient.initialize(this);
        auth = new AuthManager(this);
        
        if (!auth.isLoggedIn()) {
            NavigationHelper.navigateToLogin(this);
            return;
        }
        
        // to home
        NavigationHelper.navigateToHome(this);
    }
}