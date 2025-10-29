package com.example.scplayer.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

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

    public static void loadArtwork(Context context, String artworkUrl, ImageView imageView) {
        if (artworkUrl != null) {
            Glide.with(context)
                    .load(artworkUrl)
                    .placeholder(android.R.color.darker_gray)
                    .into(imageView);
        } else {
            imageView.setImageResource(android.R.color.darker_gray);
        }
    }
}
