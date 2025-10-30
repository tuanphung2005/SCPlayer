package com.example.scplayer.models;

import com.google.gson.annotations.SerializedName;

public class TrackStream {
    @SerializedName("http_mp3_128_url")
    private String httpMp3128Url;
    
    @SerializedName("hls_mp3_128_url")
    private String hlsMp3128Url;
    
    @SerializedName("hls_opus_64_url")
    private String hlsOpus64Url;
    
    @SerializedName("preview_mp3_128_url")
    private String previewMp3128Url;
    public String getBestStreamUrl() {
        if (httpMp3128Url != null && !httpMp3128Url.isEmpty()) {
            return httpMp3128Url;
        }
        if (hlsMp3128Url != null && !hlsMp3128Url.isEmpty()) {
            return hlsMp3128Url;
        }
        if (hlsOpus64Url != null && !hlsOpus64Url.isEmpty()) {
            return hlsOpus64Url;
        }
        return previewMp3128Url;
    }
}
