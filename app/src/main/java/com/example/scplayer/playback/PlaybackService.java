package com.example.scplayer.playback;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Track;
import com.example.scplayer.models.TrackStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaybackService extends Service {

    private static final String TAG = "PlaybackService";
    
    private ExoPlayer player;
    private SoundCloudApi api;
    private final IBinder binder = new PlaybackBinder();
    private PlaybackListener listener;

    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        api = ApiClient.getSoundCloudApi();
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (listener != null) {
                    listener.onPlaybackStateChanged(state == Player.STATE_READY && player.getPlayWhenReady());
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage());
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }

    public void playTrack(Track track) {
        if (track == null) return;
        
        String trackUrn = "soundcloud:tracks:" + track.getId();
        
        api.getTrackStreams(trackUrn, null).enqueue(new Callback<TrackStream>() {
            @Override
            public void onResponse(Call<TrackStream> call, Response<TrackStream> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String streamUrl = response.body().getBestStreamUrl();
                    
                    if (streamUrl != null && !streamUrl.isEmpty()) {
                        MediaItem mediaItem = MediaItem.fromUri(streamUrl);
                        player.setMediaItem(mediaItem);
                        player.prepare();
                        player.play();
                        Log.d(TAG, "Playing: " + track.getTitle() + " from " + streamUrl);
                    } else {
                        Log.e(TAG, "No stream URL available");
                        if (listener != null) {
                            listener.onError("No stream available");
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get streams: " + response.code());
                    if (listener != null) {
                        listener.onError("Failed to load stream");
                    }
                }
            }

            @Override
            public void onFailure(Call<TrackStream> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                if (listener != null) {
                    listener.onError("Network error");
                }
            }
        });
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void resume() {
        if (player != null) {
            player.play();
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onError(String message);
    }
}
