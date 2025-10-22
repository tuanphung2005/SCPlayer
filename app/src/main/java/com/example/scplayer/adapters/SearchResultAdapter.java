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
import java.util.concurrent.TimeUnit;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<Track> tracks = new ArrayList<>();
    private List<Long> likedTrackIds = new ArrayList<>();
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int pos);
        void onLikeClick(Track track, int pos, boolean isLiked);
    }

    public SearchResultAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    public void setLikedTracks(List<Track> likedTracks) {
        this.likedTrackIds.clear();
        if (likedTracks != null) {
            for (Track track : likedTracks) {
                this.likedTrackIds.add(track.getId());
            }
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

    private int findTrackPosition(long trackId) {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId() == trackId) {
                return i;
            }
        }
        return -1;
    }

    public void clearTracks() {
        this.tracks.clear();
        notifyDataSetChanged();
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
        holder.bind(tracks.get(pos), pos);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView trackCover;
        private TextView trackTitle;
        private TextView artistName;
        private TextView trackDuration;
        private ImageButton btnLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            trackCover = itemView.findViewById(R.id.trackCover);
            trackTitle = itemView.findViewById(R.id.trackTitle);
            artistName = itemView.findViewById(R.id.artistName);
            trackDuration = itemView.findViewById(R.id.trackDuration);
            btnLike = itemView.findViewById(R.id.btnLike);
        }

        public void bind(Track track, int pos) {
            trackTitle.setText(track.getTitle());

            if (track.getUser() != null && track.getUser().getUsername() != null) {
                artistName.setText(track.getUser().getUsername());
            } else {
                artistName.setText("Unknown Artist");
            }

            long durationMs = track.getDuration();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
            trackDuration.setText(String.format("%d:%02d", minutes, seconds));

            String artworkUrl = track.getHighQualityArtworkUrl();
            if (artworkUrl != null && !artworkUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artworkUrl)
                        .placeholder(R.drawable.ic_library)
                        .into(trackCover);
            } else {
                trackCover.setImageResource(R.drawable.ic_library);
            }

            boolean isLiked = likedTrackIds.contains(track.getId());
            btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, pos);
                }
            });

            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(track, pos, isLiked);
                }
            });
        }
    }
}
