package com.example.scplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.scplayer.R;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTrackAdapter extends RecyclerView.Adapter<BaseTrackAdapter.TrackViewHolder> {

    protected List<Track> tracks = new ArrayList<>();
    protected List<Long> likedTrackIds = new ArrayList<>();
    protected OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int pos);
        void onLikeClick(Track track, int pos, boolean isLiked);
    }

    public BaseTrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    public void setLikedTrackIds(List<Long> likedTrackIds) {
        this.likedTrackIds.clear();
        if (likedTrackIds != null) {
            this.likedTrackIds.addAll(likedTrackIds);
        }
        notifyDataSetChanged();
    }

    public void addLikedTrack(long trackId) {
        if (!likedTrackIds.contains(trackId)) {
            likedTrackIds.add(trackId);
            notifyItemChanged(findTrackPosition(trackId));
        }
    }

    public void removeLikedTrack(long trackId) {
        likedTrackIds.remove(trackId);
        notifyItemChanged(findTrackPosition(trackId));
    }

    protected int findTrackPosition(long trackId) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId() == trackId) {
                return i;
            }
        }
        return -1;
    }

    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int pos) {
        Track track = tracks.get(pos);
        boolean isLiked = likedTrackIds.contains(track.getId());
        holder.bind(track, listener, isLiked);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    protected static class TrackViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView artist;
        private final TextView duration;
        private final ImageButton btnLike;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.trackCover);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.artistName);
            duration = itemView.findViewById(R.id.trackDuration);
            btnLike = itemView.findViewById(R.id.btnLike);
        }

        public void bind(Track track, OnTrackClickListener listener, boolean isLiked) {
            title.setText(track.getTitle());
            artist.setText(track.getUser() != null ? track.getUser().getUsername() : "Unknown Artist");
            duration.setText(TimeUtils.formatDuration(track.getDuration()));

            String artwork = track.getHighQualityArtworkUrl();
            if (artwork != null && !artwork.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artwork)
                        .placeholder(R.drawable.ic_library)
                        .into(cover);
            } else {
                cover.setImageResource(R.drawable.ic_library);
            }

            btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, getAdapterPosition());
                }
            });

            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(track, getAdapterPosition(), isLiked);
                }
            });
        }
    }
}
