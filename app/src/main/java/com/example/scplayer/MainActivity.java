package com.example.scplayer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.auth.AuthManager;
import com.example.scplayer.utils.NavigationHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ApiClient.initialize(this);
        AuthManager auth = new AuthManager(this);
        
        if (!auth.isLoggedIn()) {
            NavigationHelper.navigateToLogin(this);
            return;
        }
        
        // to home
        NavigationHelper.navigateToHome(this);
    }
}