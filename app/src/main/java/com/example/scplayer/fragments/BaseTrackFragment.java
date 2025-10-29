package com.example.scplayer.fragments;

import android.util.Log;

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
