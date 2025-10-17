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
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("redirect_uri") String redirectUri,
            @Field("code") String code
    );
    
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<AccessToken> refreshToken(
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("refresh_token") String refreshToken
    );
    
    // user
    @GET("me")
    Call<User> getCurrentUser(@Query("oauth_token") String token);
    
    @GET("users/{id}")
    Call<User> getUser(@Path("id") long userId, @Query("client_id") String clientId);
    
    // tracks
    @GET("tracks")
    Call<SearchResponse> searchTracks(
            @Query("q") String query,
            @Query("client_id") String clientId,
            @Query("limit") int limit,
            @Query("offset") int offset
    );
    
    @GET("tracks/{id}")
    Call<Track> getTrack(@Path("id") long trackId, @Query("client_id") String clientId);
    
    @GET("me/favorites")
    Call<List<Track>> getFavoriteTracks(@Query("oauth_token") String token);
    
    @PUT("me/favorites/{id}")
    Call<Void> likeTrack(@Path("id") long trackId, @Query("oauth_token") String token);
    
    @DELETE("me/favorites/{id}")
    Call<Void> unlikeTrack(@Path("id") long trackId, @Query("oauth_token") String token);
    
    // playlists
    @GET("playlists")
    Call<List<Playlist>> searchPlaylists(
            @Query("q") String query,
            @Query("client_id") String clientId,
            @Query("limit") int limit
    );
    
    @GET("playlists/{id}")
    Call<Playlist> getPlaylist(@Path("id") long playlistId, @Query("client_id") String clientId);
    
    @GET("me/playlists")
    Call<List<Playlist>> getUserPlaylists(@Query("oauth_token") String token);
    
    @GET("me/playlist_likes")
    Call<List<Playlist>> getLikedPlaylists(@Query("oauth_token") String token);
    
    @PUT("me/playlist_likes/{id}")
    Call<Void> likePlaylist(@Path("id") long playlistId, @Query("oauth_token") String token);
    
    @DELETE("me/playlist_likes/{id}")
    Call<Void> unlikePlaylist(@Path("id") long playlistId, @Query("oauth_token") String token);
    
    // following
    @GET("me/followings")
    Call<List<User>> getFollowings(@Query("oauth_token") String token);
    
    @PUT("me/followings/{id}")
    Call<Void> followUser(@Path("id") long userId, @Query("oauth_token") String token);
    
    @DELETE("me/followings/{id}")
    Call<Void> unfollowUser(@Path("id") long userId, @Query("oauth_token") String token);
    
    // stream
    @GET("tracks/{id}/stream")
    Call<Void> getStreamUrl(@Path("id") long trackId, @Query("client_id") String clientId);
}
