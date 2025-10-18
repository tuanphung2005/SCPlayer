package com.example.scplayer.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.scplayer.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "https://api.soundcloud.com/";
    private static Retrofit retrofit = null;
    private static SoundCloudApi api = null;
    private static Context ctx;

    public static void initialize(Context context) {
        ctx = context.getApplicationContext();
    }

    public static SoundCloudApi getSoundCloudApi() {
        if (api == null) {
            api = getClient().create(SoundCloudApi.class);
        }
        return api;
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .addInterceptor(chain -> {
                        Request req = chain.request();
                        Request.Builder builder = req.newBuilder();
                        
                        // authorization header
                        String token = getAccessToken();
                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "OAuth " + token);
                        } else {
                            builder.header("Authorization", "OAuth " + getClientId());
                        }
                        
                        Request newReq = builder.build();
                        return chain.proceed(newReq);
                    })
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

    private static String getAccessToken() {
        if (ctx == null) return null;
        SharedPreferences prefs = ctx.getSharedPreferences("SoundCloudAuth", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null);
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

