package com.example.scplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int pos);
    }

    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
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
        holder.bind(tracks.get(pos), listener);
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.trackCover);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.artistName);
            duration = itemView.findViewById(R.id.trackDuration);
        }

        public void bind(Track track, OnTrackClickListener listener) {
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

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, getAdapterPosition());
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
