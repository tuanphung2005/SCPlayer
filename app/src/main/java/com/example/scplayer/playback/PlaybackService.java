package com.example.scplayer.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.scplayer.HomeActivity;
import com.example.scplayer.R;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Track;
import com.example.scplayer.models.TrackStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaybackService extends Service {

    private static final String TAG = "PlaybackService";
    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private ExoPlayer player;
    private SoundCloudApi api;
    private final IBinder binder = new PlaybackBinder();
    private PlaybackListener listener;
    private Track currentTrack;

    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this)
                .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)
                .build();
        api = ApiClient.getSoundCloudApi();
        
        createNotificationChannel();
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (listener != null) {
                    listener.onPlaybackStateChanged(state == Player.STATE_READY && player.getPlayWhenReady());
                }
                updateNotification();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "ACTION_STOP".equals(intent.getAction())) {
            Log.d(TAG, "Notification dismissed - stopping playback");
            pause();
            if (listener != null) {
                listener.onPlaybackStateChanged(false);
            }
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Shows currently playing music");
            channel.setShowBadge(true);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    private void startForeground() {
        Notification notification = buildNotification();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        Log.d(TAG, "Started foreground service with notification");
    }

    private void updateNotification() {
        if (currentTrack != null) {
            Notification notification = buildNotification();
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
                Log.d(TAG, "Notification updated");
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent deleteIntent = new Intent(this, PlaybackService.class);
        deleteIntent.setAction("ACTION_STOP");
        PendingIntent deletePendingIntent = PendingIntent.getService(
                this, 1, deleteIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String title = currentTrack != null ? currentTrack.getTitle() : "No track";
        String artist = currentTrack != null && currentTrack.getUser() != null 
                ? currentTrack.getUser().getUsername() 
                : "Unknown Artist";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_play)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setOngoing(false)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        Log.d(TAG, "Building notification for: " + title + " by " + artist);
        
        return builder.build();
    }

    public void playTrack(Track track) {
        if (track == null) return;
        
        this.currentTrack = track;
        startForeground();
        
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
                        updateNotification();
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
            updateNotification();
        }
    }

    public void resume() {
        if (player != null) {
            player.play();
            updateNotification();
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
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
        stopForeground(true);
    }

    public interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onError(String message);
    }
}
