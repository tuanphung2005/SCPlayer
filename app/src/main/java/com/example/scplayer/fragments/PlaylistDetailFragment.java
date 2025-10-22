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
import com.example.scplayer.adapters.TrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST = "playlist";
    private static final String ARG_TRACKS = "tracks";

    private RecyclerView recycler;
    private View empty;
    private TextView titleView;
    private ImageButton btnBack;
    private TrackAdapter adapter;
    private SoundCloudApi api;
    
    private Playlist playlist;
    private List<Track> tracks;
    private List<Long> likedTrackIds = new ArrayList<>();

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
        setupRecycler();
        loadLikedTracks();
        loadTracks();
    }

    private void initViews(View view) {
        recycler = view.findViewById(R.id.tracksRecycler);
        empty = view.findViewById(R.id.empty);
        titleView = view.findViewById(R.id.playlistTitle);
        btnBack = view.findViewById(R.id.btnBack);

        api = ApiClient.getSoundCloudApi();
        
        if (playlist != null) {
            titleView.setText(playlist.getTitle());
        }

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void loadLikedTracks() {
        api.getLikedTracks(500, 0).enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    likedTrackIds.clear();
                    for (Track track : res.body()) {
                        likedTrackIds.add(track.getId());
                    }
                    if (adapter != null) {
                        adapter.setLikedTrackIds(likedTrackIds);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Log.e("PlaylistDetail", "Failed to load liked tracks", t);
            }
        });
    }

    private void setupRecycler() {

        boolean isLikedPlaylist = playlist != null && playlist.getId() == -1;
        
        adapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Track track, int pos) {
                Log.d("PlaylistDetail", "Playing: " + track.getTitle());
            }

            @Override
            public void onLikeClick(Track track, int pos, boolean isLiked) {
                String trackUrn = "soundcloud:tracks:" + track.getId();
                Call<Void> call = isLiked ? api.unlikeTrack(trackUrn) : api.likeTrack(trackUrn);
                
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> res) {
                        if (res.isSuccessful()) {
                            Toast.makeText(getContext(), isLiked ? "Track unliked" : "Track liked!", Toast.LENGTH_SHORT).show();
                            
                            // Update liked state
                            if (isLiked) {
                                likedTrackIds.remove(track.getId());
                            } else {
                                likedTrackIds.add(track.getId());
                            }
                            
                            // If unliked in Liked Songs playlist, remove track from list
                            if (isLiked && isLikedPlaylist) {
                                tracks.remove(pos);
                                adapter.setTracks(tracks);
                                showEmpty(tracks.isEmpty());
                            }
                            
                            // Update adapter with new liked state
                            adapter.setLikedTrackIds(likedTrackIds);
                        } else {
                            Toast.makeText(getContext(), "Failed (HTTP " + res.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
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
            api.getPlaylistTracks(playlist.getUrn(), 50).enqueue(new Callback<List<Track>>() {
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
