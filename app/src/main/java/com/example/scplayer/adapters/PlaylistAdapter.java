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
import com.example.scplayer.models.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, listener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private ImageView artwork;
        private TextView title;
        private TextView trackCount;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            artwork = itemView.findViewById(R.id.playlistArtwork);
            title = itemView.findViewById(R.id.playlistTitle);
            trackCount = itemView.findViewById(R.id.playlistTrackCount);
        }

        public void bind(Playlist playlist, OnPlaylistClickListener listener) {
            title.setText(playlist.getTitle());
            trackCount.setText(playlist.getTrackCount() + " tracks");

            // Load artwork for all playlists (including Liked Songs)
            artwork.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            artwork.setBackgroundColor(0x00000000); // Transparent
            artwork.setPadding(0, 0, 0, 0);
            
            String artworkUrl = playlist.getArtworkUrl();
            if (artworkUrl != null && !artworkUrl.isEmpty()) {
                // Replace 'large' with 't300x300' for playlist thumbnails
                artworkUrl = artworkUrl.replace("large", "t300x300");
                // Also handle other size formats
                artworkUrl = artworkUrl.replace("t500x500", "t300x300");
                artworkUrl = artworkUrl.replace("t250x250", "t300x300");
                
                Glide.with(itemView.getContext())
                        .load(artworkUrl)
                        .placeholder(R.drawable.ic_library)
                        .error(R.drawable.ic_library)
                        .into(artwork);
            } else {
                artwork.setImageResource(R.drawable.ic_library);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
        }
    }
}
