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

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private List<Long> likedTrackIds = new ArrayList<>();
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int pos);
        void onLikeClick(Track track, int pos, boolean isLiked);
    }

    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    public void setLikedTrackIds(List<Long> likedTrackIds) {
        this.likedTrackIds = likedTrackIds;
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

    private int findTrackPosition(long trackId) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId() == trackId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        Track track = tracks.get(pos);
        boolean isLiked = likedTrackIds.contains(track.getId());
        holder.bind(track, listener, isLiked);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView cover;
        private TextView title;
        private TextView artist;
        private TextView duration;
        private ImageButton btnLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.trackCover);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.artistName);
            duration = itemView.findViewById(R.id.trackDuration);
            btnLike = itemView.findViewById(R.id.btnLike);
        }

        public void bind(Track track, OnTrackClickListener listener, boolean isLiked) {
            title.setText(track.getTitle());
            artist.setText(track.getUser() != null ? track.getUser().getUsername() : "Unknown");
            duration.setText(formatDuration(track.getDuration()));

            String artwork = track.getArtworkUrl();
            if (artwork != null) {
                artwork = artwork.replace("large", "t300x300");
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

        private String formatDuration(long ms) {
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
