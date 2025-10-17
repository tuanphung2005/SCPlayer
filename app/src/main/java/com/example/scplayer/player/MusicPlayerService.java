package com.example.scplayer.player;

import android.content.Context;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.datasource.DefaultDataSource;

import com.example.scplayer.api.ApiClient;
import com.example.scplayer.models.Track;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService {
    private static MusicPlayerService instance;
    private ExoPlayer player;
    private Context context;
    private List<Track> playlist;
    private int currentTrackIndex = -1;
    private PlayerListener playerListener;
    
    private MusicPlayerService(Context context) {
        this.context = context.getApplicationContext();
        this.playlist = new ArrayList<>();
        initializePlayer();
    }
    
    public static synchronized MusicPlayerService getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerService(context);
        }
        return instance;
    }
    
    private void initializePlayer() {
        player = new ExoPlayer.Builder(context).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playerListener != null) {
                    playerListener.onPlaybackStateChanged(playbackState);
                }
                
                if (playbackState == Player.STATE_ENDED) {
                    playNext();
                }
            }
            
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (playerListener != null) {
                    playerListener.onIsPlayingChanged(isPlaying);
                }
            }
        });
    }
    
    @OptIn(markerClass = UnstableApi.class)
    public void playTrack(Track track) {
        if (track == null || track.getStreamUrl() == null) return;
        
        String streamUrl = track.getStreamUrl() + "?client_id=" + ApiClient.getClientId();
        
        MediaItem mediaItem = MediaItem.fromUri(streamUrl);
        
        DefaultDataSource.Factory dataSourceFactory = 
                new DefaultDataSource.Factory(context);
        
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
        
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
        
        if (playerListener != null) {
            playerListener.onTrackChanged(track);
        }
    }
    
    public void playPlaylist(List<Track> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty()) return;
        
        this.playlist = new ArrayList<>(tracks);
        this.currentTrackIndex = startIndex;
        
        if (startIndex >= 0 && startIndex < tracks.size()) {
            playTrack(tracks.get(startIndex));
        }
    }
    
    public void play() {
        if (player != null) {
            player.play();
        }
    }
    
    public void pause() {
        if (player != null) {
            player.pause();
        }
    }
    
    public void playNext() {
        if (playlist.isEmpty()) return;
        
        currentTrackIndex++;
        if (currentTrackIndex >= playlist.size()) {
            currentTrackIndex = 0; // loop to start
        }
        
        playTrack(playlist.get(currentTrackIndex));
    }
    
    public void playPrevious() {
        if (playlist.isEmpty()) return;
        
        currentTrackIndex--;
        if (currentTrackIndex < 0) {
            currentTrackIndex = playlist.size() - 1; // loop to end
        }
        
        playTrack(playlist.get(currentTrackIndex));
    }
    
    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }
    
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }
    
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }
    
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }
    
    public Track getCurrentTrack() {
        if (currentTrackIndex >= 0 && currentTrackIndex < playlist.size()) {
            return playlist.get(currentTrackIndex);
        }
        return null;
    }
    
    public ExoPlayer getPlayer() {
        return player;
    }
    
    public void setPlayerListener(PlayerListener listener) {
        this.playerListener = listener;
    }
    
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    public interface PlayerListener {
        void onTrackChanged(Track track);
        void onPlaybackStateChanged(int state);
        void onIsPlayingChanged(boolean isPlaying);
    }
}
