package com.example.scplayer.models;

import com.example.scplayer.utils.ImageUtils;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Track implements Serializable {
    @SerializedName("id")
    private long id;
    
    @SerializedName("title")
    private String title;
    @SerializedName("duration")
    private long duration;
    
    @SerializedName("artwork_url")
    private String artworkUrl;
    @SerializedName("user")
    private User user;
    @SerializedName("permalink_url")
    private String permalinkUrl;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }
    public String getArtworkUrl() {
        return artworkUrl;
    }
    public User getUser() {
        return user;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }
    public String getHighQualityArtworkUrl() {
        return ImageUtils.getHighQualityArtworkUrl(artworkUrl);
    }
}
