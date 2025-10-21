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

    private RecyclerView recycler;
    private View empty;
    private PlaylistAdapter adapter;
    private SoundCloudApi api;
    private List<Track> liked = new ArrayList<>();

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
        recycler = view.findViewById(R.id.playlistsRecycler);
        empty = view.findViewById(R.id.empty);
        api = ApiClient.getSoundCloudApi();
    }

    private void setupRecyclers() {
        adapter = new PlaylistAdapter(p -> {
            if (p.getId() == -1) {
                Log.d("LibraryFragment", "Opening Liked Songs (" + liked.size() + " tracks)");
            } else {
                Log.d("LibraryFragment", "Opening: " + p.getTitle());
            }
        });
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recycler.setAdapter(adapter);
    }

    private void loadLibraryData() {
        showEmpty(false);
        loadLikedTracks();
        loadPlaylists();
    }

    private void loadLikedTracks() {
        api.getLikedTracks(50, 0).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    liked = res.body();
                    if (!liked.isEmpty()) {
                        adapter.setPlaylists(List.of(createLiked()));
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
        api.getLikedPlaylistsV2(50, 0).enqueue(new Callback<List<Playlist>>() {
            @Override
            public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    fetchArtwork(res.body(), 0);
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
    
    private void fetchArtwork(List<Playlist> playlists, int i) {
        if (i >= playlists.size()) {
            display(playlists);
            return;
        }
        
        Playlist p = playlists.get(i);

        if (p.getArtworkUrl() != null) {
            fetchArtwork(playlists, i + 1);
            return;
        }

        String urn = p.getUrn();
        if (urn != null) {
            api.getPlaylistTracks(urn, 1).enqueue(new Callback<List<Track>>() {
                @Override
                public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                    if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                        p.setArtworkUrl(res.body().get(0).getArtworkUrl());
                    }
                    fetchArtwork(playlists, i + 1);
                }

                @Override
                public void onFailure(Call<List<Track>> call, Throwable t) {
                    fetchArtwork(playlists, i + 1);
                }
            });
        } else {
            fetchArtwork(playlists, i + 1);
        }
    }
    
    private void display(List<Playlist> user) {
        List<Playlist> all = new ArrayList<>();
        
        if (adapter.getItemCount() > 0) {
            all.add(createLiked());
        }
        
        all.addAll(user);
        adapter.setPlaylists(all);
        
        if (all.isEmpty()) {
            showEmpty(true);
        }
    }
    
    private Playlist createLiked() {
        Playlist p = new Playlist();
        p.setId(-1);
        p.setTitle("Liked Songs");
        p.setTrackCount(liked.size());
        p.setArtworkUrl(liked.get(0).getArtworkUrl());
        return p;
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void checkIfEmpty() {
        showEmpty(adapter.getItemCount() == 0);
    }
}
