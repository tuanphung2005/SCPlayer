package com.example.scplayer.api;

import com.example.scplayer.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "https://api.soundcloud.com/";
    private static Retrofit retrofit = null;
    private static SoundCloudApi soundCloudApi = null;

    public static SoundCloudApi getSoundCloudApi() {
        if (soundCloudApi == null) {
            soundCloudApi = getClient().create(SoundCloudApi.class);
        }
        return soundCloudApi;
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static String getClientId() {
        return BuildConfig.SOUNDCLOUD_CLIENT_ID;
    }

    public static String getClientSecret() {
        return BuildConfig.SOUNDCLOUD_CLIENT_SECRET;
    }

    public static String getRedirectUri() {
        return BuildConfig.SOUNDCLOUD_REDIRECT_URI;
    }
}
