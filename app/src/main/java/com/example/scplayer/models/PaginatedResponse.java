package com.example.scplayer.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaginatedResponse<T> {
    @SerializedName("collection")
    private List<T> collection;
    public List<T> getCollection() {
        return collection;
    }
}
