package com.example.scplayer.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.R;
import com.example.scplayer.adapters.BaseTrackAdapter;
import com.example.scplayer.adapters.SearchResultAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.Track;
import com.example.scplayer.utils.ApiConstants;
import com.example.scplayer.utils.TrackLikeManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends BaseTrackFragment implements SearchResultAdapter.OnTrackClickListener {

    private EditText input;
    private ImageButton clear;
    private RecyclerView results;
    private LinearLayout empty;
    private SearchResultAdapter adapter;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Nullable
    @Override
    protected BaseTrackAdapter getAdapter() {
        return adapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupSearch();
    }

    private void initializeViews(View view) {
        input = view.findViewById(R.id.searchInput);
        clear = view.findViewById(R.id.btnClearSearch);
        results = view.findViewById(R.id.recyclerViewResults);
        empty = view.findViewById(R.id.emptyState);

        adapter = new SearchResultAdapter(this);
        results.setLayoutManager(new LinearLayoutManager(getContext()));
        results.setAdapter(adapter);

        initializeLikeManagement();
        registerMiniPlayerListener();
        loadLikedTracks();
    }

    private void setupSearch() {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }

                if (s.length() > 0) {
                    runnable = () -> performSearch(s.toString());
                    handler.postDelayed(runnable, ApiConstants.SEARCH_DEBOUNCE_DELAY_MS);
                } else {
                    adapter.clearTracks();
                    showEmpty(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clear.setOnClickListener(v -> {
            input.setText("");
            adapter.clearTracks();
            showEmpty(true);
        });
    }

    private void performSearch(String q) {
        showEmpty(false);

        Call<List<Track>> call = api.searchTracks(q.trim(), ApiConstants.SEARCH_RESULTS_LIMIT, 0);

        call.enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    List<Track> tracks = res.body();

                    if (!tracks.isEmpty()) {
                        adapter.setTracks(tracks);
                        showEmpty(false);
                    } else {
                        adapter.clearTracks();
                        showEmpty(true);
                        Log.d("SearchFragment", "No tracks found");
                    }
                } else {
                    adapter.clearTracks();
                    showEmpty(true);
                    Log.d("SearchFragment", "Search failed: " + res.code());
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                showEmpty(true);
                Log.d("SearchFragment", "Network error: " + t.getMessage());
            }
        });
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTrackClick(Track track, int position) {
        Log.d("SearchFragment", "Playing: " + track.getTitle());
        com.example.scplayer.utils.MiniPlayer.getInstance().setPlaylist(adapter.getTracks(), position);
    }

    @Override
    public void onLikeClick(Track track, int position, boolean isLiked) {
        toggleLike(track, isLiked);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(runnable);
    }


}
