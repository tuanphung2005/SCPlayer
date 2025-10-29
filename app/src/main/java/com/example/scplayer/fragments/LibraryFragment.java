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
import com.example.scplayer.models.PaginatedResponse;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.ApiConstants;
import com.example.scplayer.utils.CollectionUtils;
import com.example.scplayer.utils.PlaylistManager;

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
    private PlaylistManager playlistManager;
    private List<Track> liked = new ArrayList<>();
    private List<Playlist> cachedPlaylists = new ArrayList<>();

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

    // refresh on resume/hidden change fix
    @Override
    public void onResume() {
        super.onResume();
        loadLibraryData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadLibraryData();
        }
    }

    private void initializeViews(View view) {
        recycler = view.findViewById(R.id.playlistsRecycler);
        empty = view.findViewById(R.id.empty);
        api = ApiClient.getSoundCloudApi();
        playlistManager = new PlaylistManager(api);
    }

    private void setupRecyclers() {
        adapter = new PlaylistAdapter(p -> {
            if (p.getId() == ApiConstants.LIKED_SONGS_PLAYLIST_ID) {
                openPlaylist(p, liked);
            } else {
                openPlaylist(p, null);
            }
        });
        recycler.setLayoutManager(new GridLayoutManager(getContext(), ApiConstants.PLAYLIST_GRID_COLUMNS));
        recycler.setAdapter(adapter);
        
        // read cached playlists
        if (!cachedPlaylists.isEmpty()) {
            adapter.setPlaylists(cachedPlaylists);
            showEmpty(false);
        }
    }
    
    private void openPlaylist(Playlist p, List<Track> tracks) {
        PlaylistDetailFragment fragment = PlaylistDetailFragment.newInstance(p, tracks);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadLibraryData() {
        showEmpty(false);
        loadLikedTracks();
    }

    private void loadLikedTracks() {
        api.getLikedTracks(ApiConstants.MAX_LIKED_TRACKS, 0).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    List<Track> newLiked = res.body();
                    boolean likedChanged = !CollectionUtils.areTracksEqual(liked, newLiked);
                    
                    if (likedChanged) {
                        liked = newLiked;
                    }
                    loadPlaylists(likedChanged);
                } else {
                    loadPlaylists(false);
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                loadPlaylists(false);
            }
        });
    }

    private void loadPlaylists(boolean likedChanged) {
        playlistManager.loadUserPlaylists(playlists -> {
            boolean playlistsChanged = !areUserPlaylistsEqual(cachedPlaylists, playlists);
            
            if (likedChanged || playlistsChanged) {
                fetchArtwork(playlists, 0);
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
        
        if (!liked.isEmpty()) {
            all.add(createLiked());
        }
        
        all.addAll(user);
        cachedPlaylists = all;
        adapter.setPlaylists(all);
        
        if (all.isEmpty()) {
            showEmpty(true);
        }
    }
    
    private Playlist createLiked() {
        Playlist p = new Playlist();
        p.setId(ApiConstants.LIKED_SONGS_PLAYLIST_ID);
        p.setTitle("Liked Songs");
        p.setTrackCount(liked.size());
        p.setArtworkUrl(liked.get(0).getArtworkUrl());
        return p;
    }

    private boolean areUserPlaylistsEqual(List<Playlist> oldPlaylists, List<Playlist> newPlaylists) {
        if (oldPlaylists.isEmpty()) {
            return false;
        }

        // skip the "Liked Songs" synthetic playlist if it exists in oldPlaylists
        int startIndex = (!liked.isEmpty()) ? 1 : 0;
        int oldCount = oldPlaylists.size() - startIndex;
        
        if (oldCount != newPlaylists.size()) {
            return false;
        }

        // compare user playlists by ID
        for (int i = 0; i < newPlaylists.size(); i++) {
            int oldIndex = startIndex + i;
            if (oldIndex >= oldPlaylists.size() || 
                oldPlaylists.get(oldIndex).getId() != newPlaylists.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void checkIfEmpty() {
        showEmpty(adapter.getItemCount() == 0);
    }
}
