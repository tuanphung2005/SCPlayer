package com.example.scplayer.utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.scplayer.R;
import com.example.scplayer.fragments.MiniPlayerFragment;

public class MiniPlayerHelper {

    public static void attachMiniPlayer(AppCompatActivity activity, int containerViewId) {
        FragmentManager fm = activity.getSupportFragmentManager();
        
        if (fm.findFragmentByTag("mini_player") == null) {
            MiniPlayerFragment miniPlayerFragment = new MiniPlayerFragment();
            fm.beginTransaction()
                    .add(containerViewId, miniPlayerFragment, "mini_player")
                    .commit();
        }
    }

    public static void detachMiniPlayer(AppCompatActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        MiniPlayerFragment fragment = (MiniPlayerFragment) fm.findFragmentByTag("mini_player");
        
        if (fragment != null) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }
}
