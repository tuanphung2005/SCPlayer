package com.example.scplayer.utils;

public class TimeUtils {
    
    private TimeUtils() {
    }

    public static String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
