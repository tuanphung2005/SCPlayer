package com.example.scplayer.api;

import com.example.scplayer.models.AccessToken;
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
    
    // user
    @GET("me")
    Call<User> getCurrentUser();
    
    @GET("users/{id}")
    Call<User> getUser(@Path("id") long id);
    
    // tracks
    @GET("tracks")
    Call<SearchResponse> searchTracks(
            @Query("q") String q,
            @Query("limit") int limit,
            @Query("offset") int offset
    );
    
    @GET("tracks/{id}")
    Call<Track> getTrack(@Path("id") long id);
    
    @GET("me/favorites")
    Call<List<Track>> getFavoriteTracks();
    
    @PUT("me/favorites/{id}")
    Call<Void> likeTrack(@Path("id") long id);
    
    @DELETE("me/favorites/{id}")
    Call<Void> unlikeTrack(@Path("id") long id);
    
    // playlists
    @GET("playlists")
    Call<List<Playlist>> searchPlaylists(
            @Query("q") String q,
            @Query("limit") int limit
    );
    
    @GET("playlists/{id}")
    Call<Playlist> getPlaylist(@Path("id") long id);
    
    @GET("me/playlists")
    Call<List<Playlist>> getUserPlaylists();
    
    @GET("me/playlist_likes")
    Call<List<Playlist>> getLikedPlaylists();
    
    @PUT("me/playlist_likes/{id}")
    Call<Void> likePlaylist(@Path("id") long id);
    
    @DELETE("me/playlist_likes/{id}")
    Call<Void> unlikePlaylist(@Path("id") long id);
    
    // following
    @GET("me/followings")
    Call<List<User>> getFollowings();
    
    @PUT("me/followings/{id}")
    Call<Void> followUser(@Path("id") long id);
    
    @DELETE("me/followings/{id}")
    Call<Void> unfollowUser(@Path("id") long id);
    
    // stream
    @GET("tracks/{id}/stream")
    Call<Void> getStreamUrl(@Path("id") long trackId);
}
