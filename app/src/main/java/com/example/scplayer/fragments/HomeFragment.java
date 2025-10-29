package com.example.scplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.adapters.PlaylistAdapter;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.utils.TrackLikeManager;
import com.example.scplayer.utils.NavigationHelper;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.models.PaginatedResponse;
import com.example.scplayer.models.Track;

import java.util.List;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.scplayer.LoginActivity;
import com.example.scplayer.R;
import com.example.scplayer.auth.AuthManager;

public class HomeFragment extends Fragment {

    private AuthManager auth;
    private PlaylistAdapter adapter;
    private TrackLikeManager likeManager;
    private Playlist relatedPlaylist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = new AuthManager(requireContext());

        TextView title = view.findViewById(R.id.titleText);
        title.setText("Home");

        Button logout = view.findViewById(R.id.btnLogout);
        logout.setOnClickListener(v -> {
            auth.logout();
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
            NavigationHelper.navigateToLogin(requireContext());
        });

        RecyclerView rv = view.findViewById(R.id.recyclerRelated);
        adapter = new PlaylistAdapter(playlist -> {
            openPlaylistDetail(playlist);
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        fetchLatestLikedRelatedTracks();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Fragment is now visible, refresh related tracks
            fetchLatestLikedRelatedTracks();
        }
    }

    private void fetchLatestLikedRelatedTracks() {
        SoundCloudApi api = ApiClient.getSoundCloudApi();
        // Get liked tracks (limit 1, latest)
        api.getLikedTracks(1, 0).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Track latest = response.body().get(0);
                    String urn = "soundcloud:tracks:" + latest.getId();
                    fetchRelatedTracks(urn);
                } else {
                    Toast.makeText(requireContext(), "No liked tracks found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Toast.makeText(requireContext(), "Failed to fetch liked tracks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRelatedTracks(String trackUrn) {
        SoundCloudApi api = ApiClient.getSoundCloudApi();
        api.getRelatedTracks(trackUrn, "playable,preview", 20, true).enqueue(new Callback<PaginatedResponse<Track>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Track>> call, Response<PaginatedResponse<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> related = response.body().getCollection();
                    relatedPlaylist = new Playlist();
                    relatedPlaylist.setTitle("Related");
                    relatedPlaylist.setTracks(related);
                    relatedPlaylist.setTrackCount(related.size());
                    // Set artwork to first track's artwork if available
                    if (related != null && !related.isEmpty() && related.get(0).getArtworkUrl() != null) {
                        relatedPlaylist.setArtworkUrl(related.get(0).getArtworkUrl());
                    }
                    // Show as a single playlist card
                    List<Playlist> playlists = new ArrayList<>();
                    playlists.add(relatedPlaylist);
                    adapter.setPlaylists(playlists);
                } else {
                    Toast.makeText(requireContext(), "No related tracks found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Track>> call, Throwable t) {
                Toast.makeText(requireContext(), "Failed to fetch related tracks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openPlaylistDetail(Playlist playlist) {
        PlaylistDetailFragment fragment = PlaylistDetailFragment.newInstance(playlist, playlist.getTracks());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void onLikeClick(Track track, int pos, boolean isLiked) {
        likeManager.toggleLike(track, isLiked, new TrackLikeManager.LikeCallback() {
            public void onSuccess(boolean nowLiked) {
                // Like state is managed in PlaylistDetailFragment
            }
            public void onError(int code, Throwable t) {
                Toast.makeText(requireContext(), "Failed to update like", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
