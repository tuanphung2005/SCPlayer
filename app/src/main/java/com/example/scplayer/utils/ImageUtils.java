package com.example.scplayer.utils;

public class ImageUtils {
    
    private ImageUtils() {
    }

    public static String getHighQualityArtworkUrl(String artworkUrl) {
        if (artworkUrl != null) {
            return artworkUrl.replace("large", "t500x500");
        }
        return null;
    }
    
    public static String getMediumQualityArtworkUrl(String artworkUrl) {
        if (artworkUrl != null) {
            return artworkUrl.replace("large", "t300x300")
                    .replace("t500x500", "t300x300")
                    .replace("t250x250", "t300x300");
        }
        return null;
    }
}
