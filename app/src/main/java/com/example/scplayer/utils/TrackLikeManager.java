package com.example.scplayer.utils;

import android.util.Log;

import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Track;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackLikeManager {
    private static final String TAG = "TrackLikeManager";
    
    private final SoundCloudApi api;
    private final List<Long> likedTrackIds;
    
    public TrackLikeManager(SoundCloudApi api) {
        this.api = api;
        this.likedTrackIds = new ArrayList<>();
    }
    public boolean isLiked(long trackId) {
        return likedTrackIds.contains(trackId);
    }
    
    public void loadLikedTracks(int limit, LoadCallback callback) {
        api.getLikedTracks(limit, 0).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    likedTrackIds.clear();
                    for (Track track : response.body()) {
                        likedTrackIds.add(track.getId());
                    }
                    callback.onLoaded(new ArrayList<>(likedTrackIds));
                } else {
                    callback.onError("Failed to load: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Log.e(TAG, "Failed to load liked tracks", t);
                callback.onError(t.getMessage());
            }
        });
    }
    
    public void toggleLike(Track track, boolean isCurrentlyLiked, LikeCallback callback) {
        String trackUrn = "soundcloud:tracks:" + track.getId();
        Call<Void> call = isCurrentlyLiked ? api.unlikeTrack(trackUrn) : api.likeTrack(trackUrn);
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (isCurrentlyLiked) {
                        likedTrackIds.remove(track.getId());
                    } else {
                        likedTrackIds.add(track.getId());
                    }
                    callback.onSuccess(!isCurrentlyLiked);
                } else {
                    callback.onError(response.code(), null);
                }
            }
            
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                Log.e(TAG, "Failed to toggle like", t);
                callback.onError(-1, t);
            }
        });
    }
    
    public interface LoadCallback {
        void onLoaded(List<Long> likedTrackIds);
        void onError(String error);
    }
    
    public interface LikeCallback {
        void onSuccess(boolean nowLiked);
        void onError(int code, Throwable t);
    }
}
