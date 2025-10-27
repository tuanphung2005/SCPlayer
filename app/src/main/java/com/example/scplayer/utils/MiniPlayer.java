package com.example.scplayer.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.example.scplayer.models.Track;
import com.example.scplayer.playback.PlaybackService;

import java.util.ArrayList;
import java.util.List;

public class MiniPlayer implements PlaybackService.PlaybackListener {
    private static MiniPlayer instance;
    private Track currentTrack;
    private List<Track> playlist;
    private int currentIndex;
    private boolean isPlaying;
    private final List<StateListener> listeners;
    
    private PlaybackService playbackService;
    private boolean serviceBound = false;
    private Context appContext;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
            playbackService = binder.getService();
            playbackService.setPlaybackListener(MiniPlayer.this);
            serviceBound = true;
            
            boolean serviceIsPlaying = playbackService.isPlaying();
            if (isPlaying != serviceIsPlaying) {
                isPlaying = serviceIsPlaying;
                notifyPlaybackStateChanged();
            }
            
            if (currentTrack != null && !serviceIsPlaying) {
                notifyPlaybackStateChanged();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            playbackService = null;
            isPlaying = false;
            notifyPlaybackStateChanged();
        }
    };

    private MiniPlayer() {
        this.playlist = new ArrayList<>();
        this.currentIndex = -1;
        this.isPlaying = false;
        this.listeners = new ArrayList<>();
    }

    public static synchronized MiniPlayer getInstance() {
        if (instance == null) {
            instance = new MiniPlayer();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
            Intent intent = new Intent(appContext, PlaybackService.class);
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void playTrack(Track track) {
        this.currentTrack = track;
        this.isPlaying = true;
        
        if (serviceBound && playbackService != null) {
            playbackService.playTrack(track);
        }
        
        notifyTrackChanged();
        notifyPlaybackStateChanged();
    }

    public void setPlaylist(List<Track> playlist, int position) {
        this.playlist = new ArrayList<>(playlist);
        this.currentIndex = position;
        if (position >= 0 && position < playlist.size()) {
            playTrack(playlist.get(position));
        }
    }

    public void togglePlayPause() {
        isPlaying = !isPlaying;
        
        if (serviceBound && playbackService != null) {
            if (isPlaying) {
                playbackService.resume();
            } else {
                playbackService.pause();
            }
        }
        
        notifyPlaybackStateChanged();
    }

    public void next() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlist.size();
        playTrack(playlist.get(currentIndex));
    }

    public void previous() {
        if (playlist.isEmpty()) return;
        currentIndex = currentIndex - 1;
        if (currentIndex < 0) {
            currentIndex = playlist.size() - 1;
        }
        playTrack(playlist.get(currentIndex));
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean hasTrack() {
        return currentTrack != null;
    }

    public void addListener(StateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void notifyTrackChanged() {
        for (StateListener listener : listeners) {
            listener.onTrackChanged(currentTrack);
        }
    }

    private void notifyPlaybackStateChanged() {
        for (StateListener listener : listeners) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        this.isPlaying = playing;
        notifyPlaybackStateChanged();
    }

    @Override
    public void onError(String message) {
        for (StateListener listener : listeners) {
            if (listener instanceof ErrorListener) {
                ((ErrorListener) listener).onPlaybackError(message);
            }
        }
    }

    public void release() {
        if (appContext != null && serviceBound) {
            appContext.unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    public PlaybackService getPlaybackService() {
        return playbackService;
    }

    public interface StateListener {
        void onTrackChanged(Track track);
        void onPlaybackStateChanged(boolean isPlaying);
    }

    public interface ErrorListener {
        void onPlaybackError(String message);
    }
}
