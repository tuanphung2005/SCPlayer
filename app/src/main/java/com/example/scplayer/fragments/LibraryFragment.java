package com.example.scplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
                // TODO: Open liked songs fragment
                Toast.makeText(getContext(), "Opening Liked Songs (" + likedTracks.size() + " tracks)", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Open playlist detail fragment
                Toast.makeText(getContext(), "Opening: " + playlist.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
        playlistsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns
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
                        Playlist likedSongsPlaylist = new Playlist();
                        likedSongsPlaylist.setId(-1);
                        likedSongsPlaylist.setTitle("Liked Songs");
                        likedSongsPlaylist.setTrackCount(likedTracks.size());
                        likedSongsPlaylist.setArtworkUrl(likedTracks.get(0).getArtworkUrl());
                        
                        List<Playlist> allPlaylists = new ArrayList<>();
                        allPlaylists.add(likedSongsPlaylist);
                        playlistsAdapter.setPlaylists(allPlaylists);
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
                    Toast.makeText(getContext(), "Failed to load playlists", Toast.LENGTH_SHORT).show();
                    checkIfEmpty();
                }
            }

            @Override
            public void onFailure(Call<List<Playlist>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            Playlist likedSongs = new Playlist();
            likedSongs.setId(-1);
            likedSongs.setTitle("Liked Songs");
            likedSongs.setTrackCount(likedTracks.size());
            likedSongs.setArtworkUrl(likedTracks.get(0).getArtworkUrl());
            allPlaylists.add(likedSongs);
        }
        
        allPlaylists.addAll(userPlaylists);
        playlistsAdapter.setPlaylists(allPlaylists);
        
        if (allPlaylists.isEmpty()) {
            showEmpty(true);
        }
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void checkIfEmpty() {
        showEmpty(playlistsAdapter.getItemCount() == 0);
    }
}
