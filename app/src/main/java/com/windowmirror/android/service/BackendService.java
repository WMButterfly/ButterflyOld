package com.windowmirror.android.service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.windowmirror.android.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class BackendService {
    private static BackendService sBackendService;
    private BackendApi mApi;

    public static BackendService getInstance() {
        if (sBackendService == null) {
            sBackendService = new BackendService();
        }
        return sBackendService;
    }

    private BackendService() {
        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ?
                HttpLoggingInterceptor.Level.BODY :
                HttpLoggingInterceptor.Level.NONE);

        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(getGsonFactory())
                .client(httpClient)
                .build();

        mApi = retrofit.create(BackendApi.class);
    }

    private GsonConverterFactory getGsonFactory() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        return GsonConverterFactory.create(gson);
    }
}
