package com.example.scplayer.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scplayer.R;
import com.example.scplayer.adapters.SearchResultAdapter;
import com.example.scplayer.api.ApiClient;
import com.example.scplayer.api.SoundCloudApi;
import com.example.scplayer.models.SearchResponse;
import com.example.scplayer.models.Track;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements SearchResultAdapter.OnTrackClickListener {

    private EditText searchInput;
    private ImageButton btnClearSearch;
    private RecyclerView recyclerViewResults;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private SearchResultAdapter adapter;
    private SoundCloudApi api;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 500;

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
        searchInput = view.findViewById(R.id.searchInput);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        recyclerViewResults = view.findViewById(R.id.recyclerViewResults);
        emptyState = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new SearchResultAdapter(this);
        recyclerViewResults.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewResults.setAdapter(adapter);

        api = ApiClient.getSoundCloudApi();
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (s.length() > 0) {
                    searchRunnable = () -> performSearch(s.toString());
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                } else {
                    adapter.clearTracks();
                    showEmptyState(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            adapter.clearTracks();
            showEmptyState(true);
        });
    }

    private void performSearch(String query) {
        showLoading(true);
        showEmptyState(false);

        Call<SearchResponse> call = api.searchTracks(query.trim(), 20, 0);

        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = response.body().getCollection();

                    if (tracks != null && !tracks.isEmpty()) {
                        adapter.setTracks(tracks);
                        showEmptyState(false);
                    } else {
                        adapter.clearTracks();
                        showEmptyState(true);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "No tracks found", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    adapter.clearTracks();
                    showEmptyState(true);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Search failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showLoading(false);
                showEmptyState(true);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyState != null) {
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onTrackClick(Track track, int position) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Playing: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
