package com.example.scplayer.fragments;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scplayer.adapters.BaseTrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.ApiConstants;
import com.example.scplayer.utils.MiniPlayer;
import com.example.scplayer.utils.TrackLikeManager;

import java.util.List;

public abstract class BaseTrackFragment extends Fragment 
    implements MiniPlayer.StateListener, MiniPlayer.LikeChangeListener {

    protected SoundCloudApi api;
    protected TrackLikeManager likeManager;
    @Nullable
    protected abstract BaseTrackAdapter getAdapter();

    protected void onLikedTracksLoaded(List<Long> likedTrackIds) {
        BaseTrackAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.setLikedTrackIds(likedTrackIds);
        }
    }

    protected void onLikedTracksError(String error) {
        Log.e(getClass().getSimpleName(), "Failed to load liked tracks: " + error);
    }

    //onCreateView() loadLikedTracks()
    protected void initializeLikeManagement() {
        api = ApiClient.getSoundCloudApi();
        likeManager = new TrackLikeManager(api);
    }

    // onCreateView() initializeLikeManagement()
    protected void loadLikedTracks() {
        likeManager.loadLikedTracks(ApiConstants.MAX_LIKED_TRACKS, new TrackLikeManager.LoadCallback() {
            @Override
            public void onLoaded(List<Long> likedTrackIds) {
                onLikedTracksLoaded(likedTrackIds);
            }

            @Override
            public void onError(String error) {
                onLikedTracksError(error);
            }
        });
    }

    // onCreateView()
    protected void registerMiniPlayerListener() {
        MiniPlayer.getInstance().addListener(this);
    }

    protected void unregisterMiniPlayerListener() {
        MiniPlayer.getInstance().removeListener(this);
    }

    // TOGGLE LIKE
    protected void toggleLike(Track track, boolean isCurrentlyLiked) {
        if (track == null) return;

        likeManager.toggleLike(track, isCurrentlyLiked, new TrackLikeManager.LikeCallback() {
            @Override
            public void onSuccess(boolean nowLiked) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {

                        BaseTrackAdapter adapter = getAdapter();
                        if (adapter != null) {
                            if (nowLiked) {
                                adapter.addLikedTrack(track.getId());
                            } else {
                                adapter.removeLikedTrack(track.getId());
                            }
                        }
                        
                        // notify mini player
                        MiniPlayer.getInstance().notifyTrackLikeChanged(track.getId(), nowLiked);
                        showLikeToast(nowLiked);
                    });
                }
            }

            @Override
            public void onError(int code, Throwable t) {
                if (getContext() != null) {
                    String message = t != null ? "Network error" : "Failed to update like status";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected void showLikeToast(boolean isLiked) {
        if (getContext() != null) {
            Toast.makeText(getContext(), 
                isLiked ? "Added to likes" : "Removed from likes", 
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterMiniPlayerListener();
    }

    @Override
    public void onLikeChanged(long trackId, boolean isLiked) {
        BaseTrackAdapter adapter = getAdapter();
        if (adapter != null) {
            if (isLiked) {
                adapter.addLikedTrack(trackId);
            } else {
                adapter.removeLikedTrack(trackId);
            }
        }
    }

    @Override
    public void onTrackChanged(Track track) {
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
    }
}
