package com.example.scplayer.api;

import com.example.scplayer.models.AccessToken;
import com.example.scplayer.models.PaginatedResponse;
import com.example.scplayer.models.Playlist;
import com.example.scplayer.models.SearchResponse;
import com.example.scplayer.models.Track;
import com.example.scplayer.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SoundCloudApi {
    
    // authentication
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<AccessToken> getAccessToken(
            @Field("grant_type") String grant,
            @Field("client_id") String id,
            @Field("client_secret") String secret,
            @Field("redirect_uri") String redirect,
            @Field("code") String code
    );
    
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<AccessToken> refreshToken(
            @Field("grant_type") String grant,
            @Field("client_id") String id,
            @Field("client_secret") String secret,
            @Field("refresh_token") String refresh
    );
    
    // tracks - return array [{}]
    @GET("tracks")
    Call<List<Track>> searchTracks(
            @Query("q") String q,
            @Query("limit") int limit,
            @Query("offset") int offset
    );
    // track
    @GET("me/likes/tracks")
    Call<List<Track>> getLikedTracks(
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // playlist
    @GET("me/likes/playlists")
    Call<List<Playlist>> getLikedPlaylistsV2(
            @Query("limit") int limit,
            @Query("offset") int offset
    );
    
    @GET("playlists/{id}/tracks")
    Call<List<Track>> getPlaylistTracks(
            @Path("id") String playlistUrn,
            @Query("limit") int limit
    );
}
