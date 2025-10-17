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

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    
    private List<Track> tracks = new ArrayList<>();
    private OnTrackClickListener listener;
    
    public interface OnTrackClickListener {
        void onPlayClick(Track track, int position);
        void onLikeClick(Track track, int position);
    }
    
    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }
    
    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }
    
    public void addTracks(List<Track> newTracks) {
        int startPosition = tracks.size();
        tracks.addAll(newTracks);
        notifyItemRangeInserted(startPosition, newTracks.size());
    }
    
    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track, position);
    }
    
    @Override
    public int getItemCount() {
        return tracks.size();
    }
    
    class TrackViewHolder extends RecyclerView.ViewHolder {
        private ImageView trackArtwork;
        private TextView trackTitle;
        private TextView trackArtist;
        private TextView trackDuration;
        private ImageButton btnPlay;
        private ImageButton btnLike;
        
        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            trackArtwork = itemView.findViewById(R.id.trackArtwork);
            trackTitle = itemView.findViewById(R.id.trackTitle);
            trackArtist = itemView.findViewById(R.id.trackArtist);
            trackDuration = itemView.findViewById(R.id.trackDuration);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
        
        public void bind(Track track, int position) {
            trackTitle.setText(track.getTitle());
            
            if (track.getUser() != null) {
                trackArtist.setText(track.getUser().getUsername());
            } else {
                trackArtist.setText("Unknown Artist");
            }
            
            long durationMs = track.getDuration();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
            trackDuration.setText(String.format("%d:%02d", minutes, seconds));
            
            String artworkUrl = track.getHighQualityArtworkUrl();
            if (artworkUrl != null) {
                Glide.with(itemView.getContext())
                        .load(artworkUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(trackArtwork);
            } else {
                trackArtwork.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            btnPlay.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayClick(track, position);
                }
            });
            
            btnLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(track, position);
                }
            });
        }
    }
}
