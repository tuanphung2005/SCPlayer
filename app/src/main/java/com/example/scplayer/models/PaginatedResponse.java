package com.example.scplayer.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaginatedResponse<T> {
    @SerializedName("collection")
    private List<T> collection;
    
    @SerializedName("next_href")
    private String nextHref;

    public List<T> getCollection() {
        return collection;
    }

    public void setCollection(List<T> collection) {
        this.collection = collection;
    }

    public String getNextHref() {
        return nextHref;
    }

    public void setNextHref(String nextHref) {
        this.nextHref = nextHref;
    }
}
