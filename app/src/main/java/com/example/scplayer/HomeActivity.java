package com.example.scplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.scplayer.fragments.HomeFragment;
import com.example.scplayer.fragments.LibraryFragment;
import com.example.scplayer.fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private FragmentManager fm;
    private Fragment home;
    private Fragment search;
    private Fragment library;
    private Fragment active;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted
                } else {
                    // Permission denied - notifications won't work
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        requestNotificationPermission();

        fm = getSupportFragmentManager();
        
        home = new HomeFragment();
        search = new SearchFragment();
        library = new LibraryFragment();

        fm.beginTransaction()
            .add(R.id.fragmentContainer, home, "home")
            .add(R.id.fragmentContainer, search, "search")
            .add(R.id.fragmentContainer, library, "library")
            .hide(search)
            .hide(library)
            .commit();
        
        active = home;

        setupBottomNavigation();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_home);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                switchFragment(home);
                return true;
            } else if (id == R.id.nav_search) {
                switchFragment(search);
                return true;
            } else if (id == R.id.nav_library) {
                switchFragment(library);
                return true;
            }
            
            return false;
        });
    }
    
    private void switchFragment(Fragment fragment) {
        if (fragment != active) {
            // clear stack
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            
            fm.beginTransaction()
                .hide(active)
                .show(fragment)
                .commit();
            active = fragment;
        }
    }
}
