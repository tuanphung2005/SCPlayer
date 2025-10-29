package com.example.scplayer.utils;

import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.Track;

import java.util.List;

public class CollectionUtils {

    public static boolean areTracksEqual(List<Track> oldTracks, List<Track> newTracks) {
        if (oldTracks == null || newTracks == null) {
            return oldTracks == newTracks;
        }
        
        if (oldTracks.size() != newTracks.size()) {
            return false;
        }
        
        for (int i = 0; i < oldTracks.size(); i++) {
            if (oldTracks.get(i).getId() != newTracks.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    public static boolean arePlaylistsEqual(List<Playlist> oldPlaylists, List<Playlist> newPlaylists) {
        if (oldPlaylists == null || newPlaylists == null) {
            return oldPlaylists == newPlaylists;
        }
        
        if (oldPlaylists.size() != newPlaylists.size()) {
            return false;
        }
        
        for (int i = 0; i < oldPlaylists.size(); i++) {
            if (oldPlaylists.get(i).getId() != newPlaylists.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean areListsEqualById(List<T> oldList, List<T> newList, IdExtractor<T> idExtractor) {
        if (oldList == null || newList == null) {
            return oldList == newList;
        }
        
        if (oldList.size() != newList.size()) {
            return false;
        }
        
        for (int i = 0; i < oldList.size(); i++) {
            if (idExtractor.getId(oldList.get(i)) != idExtractor.getId(newList.get(i))) {
                return false;
            }
        }
        return true;
    }

    public interface IdExtractor<T> {
        long getId(T item);
    }
}
