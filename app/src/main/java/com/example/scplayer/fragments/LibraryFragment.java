package com.example.scplayer.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.R;
import com.example.scplayer.adapters.PlaylistAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {

    private RecyclerView playlistsRecycler;
    private View empty;
    
    private PlaylistAdapter playlistsAdapter;
    private SoundCloudApi api;
    
    private List<Track> likedTracks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclers();
        loadLibraryData();
    }

    private void initializeViews(View view) {
        playlistsRecycler = view.findViewById(R.id.playlistsRecycler);
        empty = view.findViewById(R.id.empty);
        
        api = ApiClient.getSoundCloudApi();
    }

    private void setupRecyclers() {
        playlistsAdapter = new PlaylistAdapter(playlist -> {
            if (playlist.getId() == -1) {
                Log.d("LibraryFragment", "Opening Liked Songs (" + likedTracks.size() + " tracks)");
            } else {
                Log.d("LibraryFragment", "Opening: " + playlist.getTitle());
            }
        });
        playlistsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        playlistsRecycler.setAdapter(playlistsAdapter);
    }

    private void loadLibraryData() {
        showEmpty(false);

        loadLikedTracks();
        loadPlaylists();
    }

    private void loadLikedTracks() {
        Call<List<Track>> call = api.getLikedTracks(50, 0);
        
        call.enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    likedTracks = res.body();
                    if (!likedTracks.isEmpty()) {
                        playlistsAdapter.setPlaylists(List.of(createLikedPlaylist()));
                    }
                    loadPlaylists();
                } else {
                    loadPlaylists();
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                loadPlaylists();
            }
        });
    }

    private void loadPlaylists() {
        Call<List<Playlist>> call = api.getLikedPlaylistsV2(50, 0);
        
        call.enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    fetchPlaylistArtwork(res.body(), 0);
                } else {
                    Log.d("LibraryFragment", "Failed to load playlists: " + res.code());
                    checkIfEmpty();
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Log.d("LibraryFragment", "Error: " + t.getMessage());
                checkIfEmpty();
            }
        });
    }
    
    private void fetchPlaylistArtwork(List<Playlist> playlists, int index) {
        if (index >= playlists.size()) {
            displayPlaylists(playlists);
            return;
        }
        
        Playlist playlist = playlists.get(index);

        if (playlist.getArtworkUrl() != null) {
            fetchPlaylistArtwork(playlists, index + 1);
            return;
        }

        String urn = playlist.getUrn();
        if (urn != null) {
            api.getPlaylistTracks(urn, 1).enqueue(new Callback<List<Track>>() {
                @Override
                public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                    if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                        playlist.setArtworkUrl(res.body().get(0).getArtworkUrl());
                    }
                    fetchPlaylistArtwork(playlists, index + 1);
                }

                @Override
                public void onFailure(Call<List<Track>> call, Throwable t) {
                    fetchPlaylistArtwork(playlists, index + 1);
                }
            });
        } else {
            fetchPlaylistArtwork(playlists, index + 1);
        }
    }
    
    private void displayPlaylists(List<Playlist> userPlaylists) {
        List<Playlist> allPlaylists = new ArrayList<>();
        
        if (playlistsAdapter.getItemCount() > 0) {
            allPlaylists.add(createLikedSongsPlaylist());
        }
        
        allPlaylists.addAll(userPlaylists);
        playlistsAdapter.setPlaylists(allPlaylists);
        
        if (allPlaylists.isEmpty()) {
            showEmpty(true);
        }
    }
    
    private Playlist createLikedPlaylist() {
        Playlist likedSongs = new Playlist();
        likedSongs.setId(-1);
        likedSongs.setTitle("Liked Songs");
        likedSongs.setTrackCount(likedTracks.size());
        likedSongs.setArtworkUrl(likedTracks.get(0).getArtworkUrl());
        return likedSongs;
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void checkIfEmpty() {
        showEmpty(playlistsAdapter.getItemCount() == 0);
    }
}
