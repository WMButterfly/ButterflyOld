package com.windowmirror.android.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.result.Credentials;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.windowmirror.android.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class BackendService {
    private static BackendService sBackendService;
    private static Credentials sCredentials;
    private static CredentialsManager sCredentialsManager;
    private BackendApi mApi;

    public static BackendService getInstance() {
        if (sBackendService == null) {
            sBackendService = new BackendService();
        }
        return sBackendService;
    }

    public BackendApi getApi() {
        return mApi;
    }

    public static void setCredentials(@NonNull Context context,
                                      @NonNull Credentials credentials) {
        getCredentialsManager(context).saveCredentials(credentials);
        sCredentials = credentials;
    }

    public static void clearCredentials(@NonNull Context context) {
        getCredentialsManager(context).clearCredentials();
        sCredentials = null;
    }

    public static boolean hasCredentials(@NonNull Context context) {
        return getCredentialsManager(context).hasValidCredentials();
    }

    public static CredentialsManager getCredentialsManager(@NonNull Context context) {
        if (sCredentialsManager == null) {
            Auth0 auth0 = new Auth0(context);
            AuthenticationAPIClient apiClient = new AuthenticationAPIClient(auth0);
            sCredentialsManager = new CredentialsManager(apiClient, new SharedPreferencesStorage(context));
        }
        return sCredentialsManager;
    }

    private BackendService() {
        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ?
                HttpLoggingInterceptor.Level.BODY :
                HttpLoggingInterceptor.Level.NONE);

        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(getAuthInterceptor())
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(getGsonFactory())
                .client(httpClient)
                .build();

        mApi = retrofit.create(BackendApi.class);
    }

    private Interceptor getAuthInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + (sCredentials == null ? "" : sCredentials.getAccessToken()))
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            }
        };
    }

    private GsonConverterFactory getGsonFactory() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        return GsonConverterFactory.create(gson);
    }
}
