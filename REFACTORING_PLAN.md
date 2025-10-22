# SCPlayer Code Review & Refactoring Plan

## Executive Summary
Overall, the codebase is clean and well-structured. Here's a detailed analysis with recommendations for simplification and preparation for streaming implementation.

---

## 1. CODE DUPLICATION ISSUES

### 1.1 Duplicate Like/Unlike Logic ⚠️ HIGH PRIORITY
**Location:** `SearchFragment.java` and `PlaylistDetailFragment.java`

**Problem:**
```java
// DUPLICATED in both fragments:
String trackUrn = "soundcloud:tracks:" + track.getId();
Call<Void> call = isLiked ? api.unlikeTrack(trackUrn) : api.likeTrack(trackUrn);
call.enqueue(new Callback<Void>() {
    // Similar logic...
});
```

**Solution:** Create a `TrackLikeManager` utility class:
```java
public class TrackLikeManager {
    private SoundCloudApi api;
    private List<Long> likedTrackIds;
    
    public void toggleLike(Track track, boolean isLiked, LikeCallback callback) {
        String trackUrn = "soundcloud:tracks:" + track.getId();
        Call<Void> call = isLiked ? api.unlikeTrack(trackUrn) : api.likeTrack(trackUrn);
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    if (isLiked) {
                        likedTrackIds.remove(track.getId());
                    } else {
                        likedTrackIds.add(track.getId());
                    }
                    callback.onSuccess(track.getId(), !isLiked);
                } else {
                    callback.onError("HTTP " + r.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
    
    public interface LikeCallback {
        void onSuccess(long trackId, boolean nowLiked);
        void onError(String message);
    }
}
```

### 1.2 Duplicate Adapter Code ⚠️ MEDIUM PRIORITY
**Location:** `TrackAdapter.java` and `SearchResultAdapter.java`

**Problem:** Both adapters have nearly identical:
- `setLikedTrackIds()`, `addLikedTrack()`, `removeLikedTrack()`, `findTrackPosition()`
- `ViewHolder` bind logic
- Duration formatting

**Solution:** Create base `BaseTrackAdapter`:
```java
public abstract class BaseTrackAdapter extends RecyclerView.Adapter<BaseTrackAdapter.ViewHolder> {
    protected List<Track> tracks = new ArrayList<>();
    protected List<Long> likedTrackIds = new ArrayList<>();
    protected OnTrackClickListener listener;
    
    // Shared methods
    public void setTracks(List<Track> tracks) { /*...*/ }
    public void setLikedTrackIds(List<Long> ids) { /*...*/ }
    public void addLikedTrack(long trackId) { /*...*/ }
    public void removeLikedTrack(long trackId) { /*...*/ }
    protected int findTrackPosition(long trackId) { /*...*/ }
    protected String formatDuration(long ms) { /*...*/ }
    
    // ViewHolder with shared bind logic
    class ViewHolder extends RecyclerView.ViewHolder {
        // Shared implementation
    }
}
```

Then `TrackAdapter` and `SearchResultAdapter` extend it with only their unique differences.

### 1.3 Duplicate loadLikedTracks() ⚠️ MEDIUM PRIORITY
**Location:** Multiple fragments

**Problem:**
```java
// DUPLICATED in SearchFragment, PlaylistDetailFragment, LibraryFragment
api.getLikedTracks(500, 0).enqueue(new Callback<List<Track>>() { /*...*/ });
```

**Solution:** Move to `TrackLikeManager` or create a repository pattern.

---

## 2. CODE SIMPLIFICATION OPPORTUNITIES

### 2.1 LibraryFragment - Simplify Artwork Fetching
**Current:** Recursive `fetchArtwork()` with callback hell

**Simpler approach:**
```java
private void loadPlaylists() {
    playlistManager.loadUserPlaylists(playlists -> {
        // Use RxJava or Kotlin Coroutines if possible
        // Or at minimum, use CountDownLatch/CompletableFuture
        for (Playlist p : playlists) {
            if (p.getArtworkUrl() == null && p.getUrn() != null) {
                // Load artwork async but don't block display
                loadArtworkAsync(p);
            }
        }
        display(playlists); // Display immediately with placeholders
    });
}
```

### 2.2 Remove Unused Methods
**TrackAdapter:**
- `addLikedTrack()` and `removeLikedTrack()` are only called from SearchFragment
- TrackAdapter doesn't use them (only `setLikedTrackIds()`)
- Can be removed from TrackAdapter

### 2.3 Simplify Fragment Navigation
**Current:** Every fragment has:
```java
getActivity().getSupportFragmentManager()
    .beginTransaction()
    .replace(R.id.fragmentContainer, fragment)
    .addToBackStack(null)
    .commit();
```

**Solution:** Add navigation helper to base fragment or activity:
```java
// In HomeActivity
public void navigateToFragment(Fragment fragment) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .addToBackStack(null)
        .commit();
}
```

### 2.4 Simplify PlaylistManager
**Current:** Complex merging logic with callbacks

**Simpler:**
```java
public void loadUserPlaylists(PlaylistsLoadCallback callback) {
    // Use CompletableFuture or RxJava to combine calls
    CompletableFuture.supplyAsync(() -> fetchUserPlaylists())
        .thenCombine(CompletableFuture.supplyAsync(() -> fetchLikedPlaylists()), 
            (user, liked) -> mergePlaylists(user, liked))
        .thenAccept(callback::onPlaylistsLoaded);
}
```

---

## 3. ARCHITECTURE IMPROVEMENTS

### 3.1 Add Repository Layer 🎯 RECOMMENDED
**Current:** Fragments call API directly
**Problem:** No caching, repeated API calls, hard to test

**Solution:** Create repository classes:
```java
public class TrackRepository {
    private SoundCloudApi api;
    private List<Track> cachedLikedTracks;
    private long lastFetch;
    
    public LiveData<List<Track>> getLikedTracks(boolean forceRefresh) {
        if (!forceRefresh && isCacheValid()) {
            return LiveData.of(cachedLikedTracks);
        }
        // Fetch from API and cache
    }
}
```

### 3.2 Add ViewModel Layer 🎯 RECOMMENDED
**Benefit:** Survive configuration changes, separate UI from business logic

```java
public class PlaylistViewModel extends ViewModel {
    private TrackRepository trackRepo;
    private PlaylistRepository playlistRepo;
    
    private MutableLiveData<List<Track>> likedTracks = new MutableLiveData<>();
    private MutableLiveData<List<Playlist>> playlists = new MutableLiveData<>();
    
    public LiveData<List<Track>> getLikedTracks() {
        return likedTracks;
    }
    
    public void loadLikedTracks() {
        trackRepo.getLikedTracks(false).observeForever(likedTracks::setValue);
    }
}
```

### 3.3 Move Shared Logic to Utilities
**Create:**
- `TrackUtils.java` - formatDuration(), getHighQualityArtwork()
- `TrackLikeManager.java` - All like/unlike logic
- `NavigationHelper.java` - Fragment navigation
- `ImageLoader.java` - Centralize Glide calls

---

## 4. STREAMING PREPARATION 🎵

### 4.1 Current State
- ✅ ExoPlayer dependencies already added
- ✅ Track model has `streamUrl` field
- ❌ No playback service
- ❌ No player UI
- ❌ No playback state management

### 4.2 Required Changes for Streaming

#### A. Create PlaybackService
```java
public class PlaybackService extends Service {
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    
    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        setupMediaSession();
    }
    
    public void playTrack(Track track) {
        String streamUrl = track.getStreamUrl() + "?client_id=" + ApiClient.getClientId();
        MediaItem mediaItem = MediaItem.fromUri(streamUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
}
```

#### B. Add PlaybackManager (Singleton)
```java
public class PlaybackManager {
    private static PlaybackManager instance;
    private PlaybackService service;
    private Track currentTrack;
    private List<PlaybackListener> listeners = new ArrayList<>();
    
    public void playTrack(Context ctx, Track track) {
        this.currentTrack = track;
        Intent intent = new Intent(ctx, PlaybackService.class);
        intent.putExtra("track", track);
        ctx.startService(intent);
        notifyListeners();
    }
    
    public interface PlaybackListener {
        void onTrackChanged(Track track);
        void onPlaybackStateChanged(boolean isPlaying);
    }
}
```

#### C. Add Mini Player UI
Create `fragment_mini_player.xml`:
```xml
<LinearLayout android:id="@+id/miniPlayer">
    <ImageView android:id="@+id/artwork"/>
    <TextView android:id="@+id/trackTitle"/>
    <ImageButton android:id="@+id/playPause"/>
</LinearLayout>
```

Include in `activity_home.xml` above bottom navigation.

#### D. Update Click Handlers
In all `onTrackClick()` methods:
```java
@Override
public void onTrackClick(Track track, int position) {
    PlaybackManager.getInstance().playTrack(getContext(), track);
}
```

### 4.3 Streaming Implementation Checklist
- [ ] Create `PlaybackService` extending `Service`
- [ ] Create `PlaybackManager` singleton
- [ ] Add mini player layout
- [ ] Create `MiniPlayerFragment`
- [ ] Update all `onTrackClick()` implementations
- [ ] Add playback controls (play/pause/next/previous)
- [ ] Add notification with media controls
- [ ] Handle audio focus
- [ ] Add queue management
- [ ] Persist playback state
- [ ] Add loading states for streaming

---

## 5. CLEAN CODE RECOMMENDATIONS

### 5.1 Use Constants
**Instead of:**
```java
api.getLikedTracks(500, 0)
```

**Use:**
```java
public class ApiConstants {
    public static final int MAX_LIKED_TRACKS = 500;
    public static final int DEFAULT_OFFSET = 0;
    public static final int PLAYLIST_TRACKS_LIMIT = 50;
}
```

### 5.2 Extract String Resources
**Move to `strings.xml`:**
```xml
<string name="track_liked">Track liked!</string>
<string name="track_unliked">Track unliked</string>
<string name="network_error">Network error</string>
<string name="loading">Loading...</string>
```

### 5.3 Use Meaningful Variable Names
**Current:**
- `c`, `r`, `t` in callbacks
- `pos` for position

**Better:**
- `call`, `response`, `throwable`
- `position`

**Note:** You asked to simplify var names, but for streaming implementation, descriptive names will be more maintainable.

### 5.4 Add Error Handling
**Current:** Many empty `onFailure()` blocks

**Better:**
```java
@Override
public void onFailure(Call<Void> call, Throwable t) {
    Log.e(TAG, "API call failed", t);
    if (getContext() != null) {
        Toast.makeText(getContext(), "Connection error: " + t.getMessage(), 
            Toast.LENGTH_SHORT).show();
    }
}
```

---

## 6. REFACTORING PRIORITY

### Immediate (Before Streaming):
1. ✅ **Create `TrackLikeManager`** - Eliminate duplication
2. ✅ **Add `BaseTrackAdapter`** - Consolidate adapter logic
3. ✅ **Extract constants** - Make code more maintainable

### Short-term (During Streaming):
4. ✅ **Add Repository layer** - Cache liked tracks, prepare for offline
5. ✅ **Create utility classes** - TrackUtils, ImageLoader
6. ✅ **Implement PlaybackService** - Core streaming functionality

### Long-term (After Streaming):
7. 🔄 **Add ViewModel** - Better architecture
8. 🔄 **Migrate to Kotlin** - More concise, modern
9. 🔄 **Add Room database** - Offline support, caching

---

## 7. FILE STRUCTURE RECOMMENDATION

### Current:
```
app/src/main/java/com/example/scplayer/
├── adapters/
├── api/
├── auth/
├── fragments/
├── models/
├── utils/
└── activities
```

### Recommended (for streaming):
```
app/src/main/java/com/example/scplayer/
├── adapters/
├── api/
├── auth/
├── data/
│   ├── repository/
│   └── local/ (Room DB)
├── fragments/
├── models/
├── playback/  ← NEW
│   ├── PlaybackService.java
│   ├── PlaybackManager.java
│   ├── PlaybackNotification.java
│   └── queue/
├── ui/
│   ├── player/ ← NEW
│   └── common/
├── utils/
│   ├── Constants.java
│   ├── TrackLikeManager.java
│   └── TrackUtils.java
└── activities/
```

---

## 8. SUMMARY

### What's Good ✅:
- Clean separation of concerns (API, models, UI)
- Proper use of RecyclerView adapters
- OAuth flow implemented correctly
- Good use of fragments for navigation

### What Needs Work ⚠️:
- Code duplication in like/unlike logic
- No caching layer
- Direct API calls from fragments
- Complex nested callbacks

### Critical for Streaming 🎵:
1. Create `PlaybackService` with ExoPlayer
2. Add `PlaybackManager` singleton
3. Implement mini player UI
4. Update all track click handlers
5. Add media session for notifications

### Estimated Refactoring Time:
- **TrackLikeManager + BaseAdapter**: 2-3 hours
- **Repository layer**: 3-4 hours
- **Streaming implementation**: 8-12 hours
- **Total**: ~15-20 hours

---

## 9. NEXT STEPS

1. Start with `TrackLikeManager` to eliminate duplication
2. Create `BaseTrackAdapter` to consolidate adapters
3. Add constants file for magic numbers
4. Begin streaming implementation with `PlaybackService`
5. Test thoroughly before adding queue/notification features

Would you like me to implement any of these refactorings now?
