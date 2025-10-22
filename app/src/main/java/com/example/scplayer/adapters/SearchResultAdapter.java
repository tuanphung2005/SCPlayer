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
import java.util.concurrent.TimeUnit;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<Track> tracks = new ArrayList<>();
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    public SearchResultAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track, position);
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            trackCover = itemView.findViewById(R.id.trackCover);
            trackTitle = itemView.findViewById(R.id.trackTitle);
            artistName = itemView.findViewById(R.id.artistName);
            trackDuration = itemView.findViewById(R.id.trackDuration);
        }

        public void bind(Track track, int position) {
            // Set track title
            trackTitle.setText(track.getTitle());

            // Set artist name
            if (track.getUser() != null && track.getUser().getUsername() != null) {
                artistName.setText(track.getUser().getUsername());
            } else {
                artistName.setText("Unknown Artist");
            }

            // Set duration
            long durationMs = track.getDuration();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
            trackDuration.setText(String.format("%d:%02d", minutes, seconds));

            // Load album cover
            String artworkUrl = track.getHighQualityArtworkUrl();
            if (artworkUrl != null && !artworkUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(artworkUrl)
                        .placeholder(R.drawable.ic_library)
                        .into(trackCover);
            } else {
                trackCover.setImageResource(R.drawable.ic_library);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, position);
                }
            });
        }
    }
}
