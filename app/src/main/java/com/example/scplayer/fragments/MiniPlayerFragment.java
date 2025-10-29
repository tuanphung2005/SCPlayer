package com.example.scplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.scplayer.R;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.ImageUtils;
import com.example.scplayer.utils.MiniPlayer;

import android.widget.Toast;

public class MiniPlayerFragment extends Fragment implements MiniPlayer.StateListener, MiniPlayer.ErrorListener {

    private CardView miniPlayerCard;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;

    private MiniPlayer miniPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_mini_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        miniPlayer = MiniPlayer.getInstance();
        miniPlayer.initialize(requireContext());
        initViews(view);
        setupListeners();
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (miniPlayer.hasTrack()) {
            miniPlayerCard.setVisibility(View.VISIBLE);
        }
    }

    private void initViews(View view) {
        miniPlayerCard = view.findViewById(R.id.miniPlayerCard);
        miniPlayerCover = view.findViewById(R.id.miniPlayerCover);
        miniPlayerTitle = view.findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = view.findViewById(R.id.miniPlayerArtist);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
    }

    private void setupListeners() {
        miniPlayer.addListener(this);

        miniPlayerCard.setOnClickListener(v -> openBigPlayer());

        btnPlayPause.setOnClickListener(v -> miniPlayer.togglePlayPause());
        btnPrevious.setOnClickListener(v -> miniPlayer.previous());
        btnNext.setOnClickListener(v -> miniPlayer.next());
    }

    private void openBigPlayer() {
        BigPlayerFragment bigPlayerFragment = new BigPlayerFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom
                )
                .add(android.R.id.content, bigPlayerFragment)
                .addToBackStack(null)
                .commit();
    }

    public void hideMiniPlayer() {
        miniPlayerCard.setVisibility(View.GONE);
    }

    public void showMiniPlayer() {
        if (miniPlayer.hasTrack()) {
            miniPlayerCard.setVisibility(View.VISIBLE);
        }
    }

    private void updateUI() {
        if (miniPlayer.hasTrack()) {
            updateMiniPlayer(miniPlayer.getCurrentTrack());
            updatePlayPauseButton(miniPlayer.isPlaying());
            miniPlayerCard.setVisibility(View.VISIBLE);
        } else {
            miniPlayerCard.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTrackChanged(Track track) {
        updateMiniPlayer(track);
        if (miniPlayerCard.getVisibility() != View.VISIBLE) {
            miniPlayerCard.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlayPauseButton(isPlaying);
    }

    private void updateMiniPlayer(Track track) {
        if (track == null || getContext() == null) return;

        miniPlayerTitle.setText(track.getTitle());
        miniPlayerArtist.setText(track.getUser() != null ? track.getUser().getUsername() : "Unknown Artist");

        ImageUtils.loadArtwork(requireContext(), track.getHighQualityArtworkUrl(), miniPlayerCover);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void onPlaybackError(String message) {
        Toast.makeText(requireContext(), "Playback error: " + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        miniPlayer.removeListener(this);
    }
}
