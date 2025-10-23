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

    public String getHttpMp3128Url() {
        return httpMp3128Url;
    }

    public void setHttpMp3128Url(String httpMp3128Url) {
        this.httpMp3128Url = httpMp3128Url;
    }

    public String getHlsMp3128Url() {
        return hlsMp3128Url;
    }

    public void setHlsMp3128Url(String hlsMp3128Url) {
        this.hlsMp3128Url = hlsMp3128Url;
    }

    public String getHlsOpus64Url() {
        return hlsOpus64Url;
    }

    public void setHlsOpus64Url(String hlsOpus64Url) {
        this.hlsOpus64Url = hlsOpus64Url;
    }

    public String getPreviewMp3128Url() {
        return previewMp3128Url;
    }

    public void setPreviewMp3128Url(String previewMp3128Url) {
        this.previewMp3128Url = previewMp3128Url;
    }

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
