package com.example.scplayer.utils;

import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.PaginatedResponse;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistManager {
    private final SoundCloudApi api;

    public interface PlaylistsLoadCallback {
        void onPlaylistsLoaded(List<Playlist> playlists);
    }

    public PlaylistManager(SoundCloudApi api) {
        this.api = api;
    }

    public void loadUserPlaylists(PlaylistsLoadCallback callback) {
        api.getUserPlaylists(ApiConstants.USER_PLAYLISTS_LIMIT, true).enqueue(new Callback<PaginatedResponse<Playlist>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Playlist>> call, Response<PaginatedResponse<Playlist>> res) {
                if (res.isSuccessful() && res.body() != null && res.body().getCollection() != null) {
                    List<Playlist> userPlaylists = res.body().getCollection();
                    
                    api.getLikedPlaylistsV2(ApiConstants.LIKED_PLAYLISTS_LIMIT, 0).enqueue(new Callback<List<Playlist>>() {
                        @Override
                        public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> res) {
                            if (res.isSuccessful() && res.body() != null) {
                                mergePlaylists(userPlaylists, res.body());
                            }
                            callback.onPlaylistsLoaded(userPlaylists);
                        }

                        @Override
                        public void onFailure(Call<List<Playlist>> call, Throwable t) {
                            callback.onPlaylistsLoaded(userPlaylists);
                        }
                    });
                } else {
                    loadLikedPlaylistsOnly(callback);
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Playlist>> call, Throwable t) {
                loadLikedPlaylistsOnly(callback);
            }
        });
    }

    private void loadLikedPlaylistsOnly(PlaylistsLoadCallback callback) {
        api.getLikedPlaylistsV2(ApiConstants.LIKED_PLAYLISTS_LIMIT, 0).enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    callback.onPlaylistsLoaded(res.body());
                } else {
                    callback.onPlaylistsLoaded(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                callback.onPlaylistsLoaded(new ArrayList<>());
            }
        });
    }

    private void mergePlaylists(List<Playlist> userPlaylists, List<Playlist> likedPlaylists) {
        for (Playlist liked : likedPlaylists) {
            boolean exists = false;
            for (Playlist user : userPlaylists) {
                if (user.getId() == liked.getId()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                userPlaylists.add(liked);
            }
        }
    }
}
