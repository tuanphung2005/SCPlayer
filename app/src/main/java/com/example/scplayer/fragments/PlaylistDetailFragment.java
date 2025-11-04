package com.example.scplayer.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.R;
import com.example.scplayer.adapters.BaseTrackAdapter;
import com.example.scplayer.adapters.TrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.ApiConstants;
import com.example.scplayer.utils.TrackLikeManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailFragment extends BaseTrackFragment {

    private static final String ARG_PLAYLIST = "playlist";
    private static final String ARG_TRACKS = "tracks";

    private RecyclerView recycler;
    private View empty;
    private TextView titleView;
    private ImageButton btnBack;
    private TrackAdapter adapter;
    
    private Playlist playlist;
    private List<Track> tracks;
    private List<Long> likedTrackIds = new ArrayList<>();

    @Nullable
    @Override
    protected BaseTrackAdapter getAdapter() {
        return adapter;
    }

    public static PlaylistDetailFragment newInstance(Playlist playlist, List<Track> tracks) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYLIST, playlist);
        args.putSerializable(ARG_TRACKS, (ArrayList<Track>) tracks);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            playlist = (Playlist) getArguments().getSerializable(ARG_PLAYLIST);
            tracks = (List<Track>) getArguments().getSerializable(ARG_TRACKS);
        }

        initViews(view);
        initializeLikeManagement();
        registerMiniPlayerListener();
        setupRecycler();
        loadLikedTracks();
        loadTracks();
    }

    private void initViews(View view) {
        recycler = view.findViewById(R.id.tracksRecycler);
        empty = view.findViewById(R.id.empty);
        titleView = view.findViewById(R.id.playlistTitle);
        btnBack = view.findViewById(R.id.btnBack);
        
        if (playlist != null) {
            titleView.setText(playlist.getTitle());
        }

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    protected void onLikedTracksLoaded(List<Long> likedTrackIds) {
        this.likedTrackIds = likedTrackIds;
        super.onLikedTracksLoaded(likedTrackIds);
    }

    private void setupRecycler() {

        boolean isLikedPlaylist = playlist != null && playlist.getId() == ApiConstants.LIKED_SONGS_PLAYLIST_ID;
        
        adapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Track track, int pos) {
                Log.d("PlaylistDetail", "Playing: " + track.getTitle());
                com.example.scplayer.utils.MiniPlayer.getInstance().setPlaylist(adapter.getTracks(), pos);
            }

            @Override
            public void onLikeClick(Track track, int pos, boolean isLiked) {
                // Use base method for common logic
                toggleLike(track, isLiked);
                
                // Special case: if unliking from a "liked tracks" playlist, remove from view
                if (!isLiked && isLikedPlaylist) {
                    tracks.remove(pos);
                    adapter.setTracks(tracks);
                    showEmpty(tracks.isEmpty());
                }
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);
    }

    private void loadTracks() {
        if (tracks != null) {
            adapter.setTracks(tracks);
            showEmpty(tracks.isEmpty());
        } else if (playlist != null && playlist.getUrn() != null) {
            api.getPlaylistTracks(playlist.getUrn(), ApiConstants.PLAYLIST_TRACKS_LIMIT).enqueue(new Callback<List<Track>>() {
                @Override
                public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                    if (res.isSuccessful() && res.body() != null) {
                        adapter.setTracks(res.body());
                        showEmpty(res.body().isEmpty());
                    } else {
                        Log.d("PlaylistDetail", "Failed to load tracks: " + res.code());
                        showEmpty(true);
                    }
                }

                @Override
                public void onFailure(Call<List<Track>> call, Throwable t) {
                    Log.d("PlaylistDetail", "Error: " + t.getMessage());
                    showEmpty(true);
                }
            });
        } else {
            showEmpty(true);
        }
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
