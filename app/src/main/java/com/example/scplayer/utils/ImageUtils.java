package com.example.scplayer.utils;

public class ImageUtils {
    
    private ImageUtils() {
        // Prevent instantiation
    }
    
    /**
     * Converts artwork URL to high quality version (t500x500)
     * @param artworkUrl Original artwork URL
     * @return High quality artwork URL or null if input is null
     */
    public static String getHighQualityArtworkUrl(String artworkUrl) {
        if (artworkUrl != null) {
            return artworkUrl.replace("large", "t500x500");
        }
        return null;
    }
    
    /**
     * Converts artwork URL to medium quality version (t300x300)
     * Useful for playlist covers and grid views
     * @param artworkUrl Original artwork URL
     * @return Medium quality artwork URL or null if input is null
     */
    public static String getMediumQualityArtworkUrl(String artworkUrl) {
        if (artworkUrl != null) {
            return artworkUrl.replace("large", "t300x300")
                    .replace("t500x500", "t300x300")
                    .replace("t250x250", "t300x300");
        }
        return null;
    }
}
