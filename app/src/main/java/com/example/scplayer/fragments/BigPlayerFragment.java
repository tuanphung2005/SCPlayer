package com.example.scplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.scplayer.R;
import com.example.scplayer.adapters.BaseTrackAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.models.Track;
import com.example.scplayer.playback.PlaybackService;
import com.example.scplayer.utils.ImageUtils;
import com.example.scplayer.utils.MiniPlayer;
import com.example.scplayer.utils.TimeUtils;

import java.util.List;

public class BigPlayerFragment extends BaseTrackFragment implements 
        MiniPlayer.ErrorListener,
        MiniPlayer.ShuffleRepeatListener {

    private ImageButton btnMinimize;
    private ImageView ivAlbumCover;
    private TextView tvSongTitle;
    private TextView tvArtistName;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private ImageButton btnShuffle;
    private ImageButton btnPreviousBig;
    private ImageButton btnPlayPauseBig;
    private ImageButton btnNextBig;
    private ImageButton btnRepeat;
    private ImageButton btnLike;
    private ImageButton btnShare;

    private MiniPlayer miniPlayer;
    private Handler handler;
    private boolean isShuffleEnabled = false;
    private boolean isRepeatEnabled = false;
    private boolean isLiked = false;

    @Nullable
    @Override
    protected BaseTrackAdapter getAdapter() {
        return null; // BigPlayer doesn't use an adapter
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_big_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        miniPlayer = MiniPlayer.getInstance();
        initializeLikeManagement(); // From BaseTrackFragment
        handler = new Handler(Looper.getMainLooper());

        initViews(view);
        setupListeners();
        loadLikedTracks(); // From BaseTrackFragment
        updateUI();
        startSeekBarUpdate();
        hideMiniPlayer();
        registerMiniPlayerListener(); // From BaseTrackFragment
    }

    @Override
    protected void onLikedTracksLoaded(List<Long> likedTrackIds) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Track track = miniPlayer.getCurrentTrack();
                if (track != null) {
                    updateLikeButton(track);
                }
            });
        }
    }

    private void hideMiniPlayer() {
        Fragment miniPlayerFragment = requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.miniPlayerContainer);
        if (miniPlayerFragment instanceof MiniPlayerFragment) {
            ((MiniPlayerFragment) miniPlayerFragment).hideMiniPlayer();
        }
    }

    private void showMiniPlayer() {
        Fragment miniPlayerFragment = requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.miniPlayerContainer);
        if (miniPlayerFragment instanceof MiniPlayerFragment) {
            ((MiniPlayerFragment) miniPlayerFragment).showMiniPlayer();
        }
    }

    private void initViews(View view) {
        btnMinimize = view.findViewById(R.id.btnMinimize);
        ivAlbumCover = view.findViewById(R.id.ivAlbumCover);
        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        seekBar = view.findViewById(R.id.seekBar);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvDuration = view.findViewById(R.id.tvDuration);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnPreviousBig = view.findViewById(R.id.btnPreviousBig);
        btnPlayPauseBig = view.findViewById(R.id.btnPlayPauseBig);
        btnNextBig = view.findViewById(R.id.btnNextBig);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnLike = view.findViewById(R.id.btnLike);
        btnShare = view.findViewById(R.id.btnShare);
        
        isShuffleEnabled = miniPlayer.isShuffleEnabled();
        isRepeatEnabled = miniPlayer.isRepeatEnabled();
        btnShuffle.setAlpha(isShuffleEnabled ? 1.0f : 0.5f);
        btnRepeat.setAlpha(isRepeatEnabled ? 1.0f : 0.5f);
    }

    private void setupListeners() {
        btnMinimize.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        btnPlayPauseBig.setOnClickListener(v -> miniPlayer.togglePlayPause());
        btnPreviousBig.setOnClickListener(v -> miniPlayer.previous());
        btnNextBig.setOnClickListener(v -> miniPlayer.next());

        btnShuffle.setOnClickListener(v -> {
            isShuffleEnabled = !isShuffleEnabled;
            miniPlayer.setShuffleEnabled(isShuffleEnabled);
            btnShuffle.setAlpha(isShuffleEnabled ? 1.0f : 0.5f);
        });

        btnRepeat.setOnClickListener(v -> {
            isRepeatEnabled = !isRepeatEnabled;
            miniPlayer.setRepeatEnabled(isRepeatEnabled);
            btnRepeat.setAlpha(isRepeatEnabled ? 1.0f : 0.5f);
        });

        btnLike.setOnClickListener(v -> {
            Track track = miniPlayer.getCurrentTrack();
            if (track != null) {
                toggleLike(track, isLiked);
            }
        });

        btnShare.setOnClickListener(v -> shareTrack());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(TimeUtils.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PlaybackService service = miniPlayer.getPlaybackService();
                if (service != null) {
                    service.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void updateUI() {
        if (miniPlayer.hasTrack()) {
            Track track = miniPlayer.getCurrentTrack();
            updateTrackInfo(track);
            updatePlayPauseButton(miniPlayer.isPlaying());
            updateLikeButton(track);
        }
    }

    private void updateTrackInfo(Track track) {
        if (track == null || getContext() == null) return;

        tvSongTitle.setText(track.getTitle());
        tvSongTitle.setSelected(true);
        tvArtistName.setText(track.getUser() != null ? track.getUser().getUsername() : "Unknown Artist");

        ImageUtils.loadArtwork(requireContext(), track.getHighQualityArtworkUrl(), ivAlbumCover);

        long durationMs = track.getDuration();
        seekBar.setMax((int) durationMs);
        tvDuration.setText(TimeUtils.formatDuration(durationMs));
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPauseBig.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateLikeButton(Track track) {
        if (track == null) return;
        isLiked = likeManager.isLiked(track.getId());
        btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void shareTrack() {
        Track track = miniPlayer.getCurrentTrack();
        if (track == null || getContext() == null) return;

        String shareText = "Check out \"" + track.getTitle() + "\" by " + 
            (track.getUser() != null ? track.getUser().getUsername() : "Unknown Artist") + 
            "\n" + track.getPermalinkUrl();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share track"));
    }

    private void startSeekBarUpdate() {
        handler.post(updateSeekBar);
    }

    private void updateProgress() {
        PlaybackService service = miniPlayer.getPlaybackService();
        if (service != null && service.isPlaying()) {
            long currentPosition = service.getCurrentPosition();
            seekBar.setProgress((int) currentPosition);
            tvCurrentTime.setText(TimeUtils.formatDuration(currentPosition));
        }
    }

    @Override
    public void onTrackChanged(Track track) {
        updateTrackInfo(track);
        updateLikeButton(track);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
    }

    @Override
    public void onPlaybackError(String message) {
        Toast.makeText(requireContext(), "Playback error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShuffleRepeatChanged(boolean shuffleEnabled, boolean repeatEnabled) {
        requireActivity().runOnUiThread(() -> {
            isShuffleEnabled = shuffleEnabled;
            isRepeatEnabled = repeatEnabled;
            btnShuffle.setAlpha(isShuffleEnabled ? 1.0f : 0.5f);
            btnRepeat.setAlpha(isRepeatEnabled ? 1.0f : 0.5f);
        });
    }

    @Override
    public void onLikeChanged(long trackId, boolean liked) {
        Track currentTrack = miniPlayer.getCurrentTrack();
        if (currentTrack != null && currentTrack.getId() == trackId) {
            requireActivity().runOnUiThread(() -> {
                isLiked = liked;
                btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView(); // This calls unregisterMiniPlayerListener from base
        handler.removeCallbacks(updateSeekBar);
        showMiniPlayer();
    }
}
