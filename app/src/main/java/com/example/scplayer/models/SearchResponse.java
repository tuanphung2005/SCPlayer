package com.example.scplayer.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {
    @SerializedName("collection")
    private List<Track> collection;
    
    @SerializedName("next_href")
    private String nextHref;

    public List<Track> getCollection() {
        return collection;
    }

    public void setCollection(List<Track> collection) {
        this.collection = collection;
    }

    public String getNextHref() {
        return nextHref;
    }

    public void setNextHref(String nextHref) {
        this.nextHref = nextHref;
    }
}
