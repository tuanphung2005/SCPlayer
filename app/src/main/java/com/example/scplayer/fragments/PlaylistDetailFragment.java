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
import com.example.scplayer.utils.ApiConstants;
import com.example.scplayer.utils.TrackLikeManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailFragment extends Fragment implements 
    com.example.scplayer.utils.MiniPlayer.StateListener,
    com.example.scplayer.utils.MiniPlayer.LikeChangeListener {

    private static final String ARG_PLAYLIST = "playlist";
    private static final String ARG_TRACKS = "tracks";

    private RecyclerView recycler;
    private View empty;
    private TextView titleView;
    private ImageButton btnBack;
    private TrackAdapter adapter;
    private SoundCloudApi api;
    private TrackLikeManager likeManager;
    
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

        com.example.scplayer.utils.MiniPlayer.getInstance().addListener(this);
    }

    private void initViews(View view) {
        recycler = view.findViewById(R.id.tracksRecycler);
        empty = view.findViewById(R.id.empty);
        titleView = view.findViewById(R.id.playlistTitle);
        btnBack = view.findViewById(R.id.btnBack);

        api = ApiClient.getSoundCloudApi();
        likeManager = new TrackLikeManager(api);
        
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
        likeManager.loadLikedTracks(ApiConstants.MAX_LIKED_TRACKS, new TrackLikeManager.LoadCallback() {
            @Override
            public void onLoaded(List<Long> ids) {
                likedTrackIds = ids;
                if (adapter != null) {
                    adapter.setLikedTrackIds(likedTrackIds);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("PlaylistDetail", "Failed to load liked tracks: " + error);
            }
        });
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
                likeManager.toggleLike(track, isLiked, new TrackLikeManager.LikeCallback() {
                    @Override
                    public void onSuccess(boolean nowLiked) {
                        Toast.makeText(getContext(), nowLiked ? "Track liked!" : "Track unliked", Toast.LENGTH_SHORT).show();
                        
                        if (nowLiked) {
                            likedTrackIds.add(track.getId());
                        } else {
                            likedTrackIds.remove(track.getId());
                        }
                        
                        if (!nowLiked && isLikedPlaylist) {
                            tracks.remove(pos);
                            adapter.setTracks(tracks);
                            showEmpty(tracks.isEmpty());
                        }

                        adapter.setLikedTrackIds(likedTrackIds);
                    }

                    @Override
                    public void onError(int code, Throwable t) {
                        if (t != null) {
                            Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed (HTTP " + code + ")", Toast.LENGTH_SHORT).show();
                        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        com.example.scplayer.utils.MiniPlayer.getInstance().removeListener(this);
    }

    @Override
    public void onTrackChanged(Track track) {
        // no-op
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        // no-op
    }

    @Override
    public void onLikeChanged(long trackId, boolean isLiked) {
        if (adapter != null) {
            if (isLiked) adapter.addLikedTrack(trackId); else adapter.removeLikedTrack(trackId);
        }
    }
}
