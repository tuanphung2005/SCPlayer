package com.example.scplayer.utils;

public class ApiConstants {
    // API Limits
    public static final int MAX_LIKED_TRACKS = 50;
    public static final int PLAYLIST_TRACKS_LIMIT = 50;
    public static final int SEARCH_RESULTS_LIMIT = 20;
    public static final int USER_PLAYLISTS_LIMIT = 50;
    public static final int LIKED_PLAYLISTS_LIMIT = 50;
    public static final int PLAYLIST_GRID_COLUMNS = 2;

    public static final long LIKED_SONGS_PLAYLIST_ID = -1;

    public static final int SEARCH_DEBOUNCE_DELAY_MS = 500;
    
    // SharedPreferences
    public static final String PREFS_NAME = "SoundCloudAuth";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_TOKEN_EXPIRY = "token_expiry";


}
