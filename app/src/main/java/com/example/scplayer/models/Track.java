package com.example.scplayer.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Track implements Serializable {
    @SerializedName("id")
    private long id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("duration")
    private long duration;
    
    @SerializedName("artwork_url")
    private String artworkUrl;
    
    @SerializedName("stream_url")
    private String streamUrl;
    
    @SerializedName("waveform_url")
    private String waveformUrl;
    
    @SerializedName("user")
    private User user;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("genre")
    private String genre;
    
    @SerializedName("playback_count")
    private long playbackCount;
    
    @SerializedName("likes_count")
    private long likesCount;
    
    @SerializedName("uri")
    private String uri;
    
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getWaveformUrl() {
        return waveformUrl;
    }

    public void setWaveformUrl(String waveformUrl) {
        this.waveformUrl = waveformUrl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public long getPlaybackCount() {
        return playbackCount;
    }

    public void setPlaybackCount(long playbackCount) {
        this.playbackCount = playbackCount;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }
    
    public String getHighQualityArtworkUrl() {
        if (artworkUrl != null) {
            return artworkUrl.replace("large", "t500x500");
        }
        return null;
    }
}
