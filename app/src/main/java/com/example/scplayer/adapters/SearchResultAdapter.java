package com.example.scplayer.adapters;

public class SearchResultAdapter extends BaseTrackAdapter {

    public SearchResultAdapter(OnTrackClickListener listener) {
        super(listener);
    }

    public void clearTracks() {
        this.tracks.clear();
        notifyDataSetChanged();
    }
}
