package com.windowmirror.android.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.result.Credentials;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Wrapper aroundRetrofit callback that knows how to handle token refresh.
 */
public abstract class BackendApiCallback<T> implements Callback<T> {
    private static final int UNAUTHORIZED = 401;

    /**
     * Context used to refresh auth tokens.
     */
    public abstract Context getContext();

    /**
     * Called when server call was a success.
     */
    public abstract void onSuccess(@NonNull T data);

    /**
     * Called when the server call resulted in an error
     */
    public abstract void onError(@Nullable String error);

    /**
     * Called when the user no longer has a valid token and token refresh failed.
     */
    public abstract void onAuthenticationLost();

    /**
     * true when at an attempt has been made to refresh the token, already.
     */
    private boolean mIsTokenRefreshComplete = false;

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (response.isSuccessful()) {
            T body = response.body();
            if (body != null) {
                onSuccess(body);
            } else {
                onError(null);
            }
        } else if (response.code() == UNAUTHORIZED) {
            refreshTokensAndRetry(call);
        } else {
            onError(null);
        }
    }

    /**
     * Network exception occurred or some other unexpected error from Retrofit.
     */
    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        onError(t.toString());
    }

    private void refreshTokensAndRetry(final Call<T> call) {
        final Context context;
        if (mIsTokenRefreshComplete || (context = getContext()) == null) {
            onAuthenticationLost();
            return;
        }
        mIsTokenRefreshComplete = true;
        BackendService.getCredentialsManager(context)
                .getCredentials(new BaseCallback<Credentials, CredentialsManagerException>() {
                    @Override
                    public void onSuccess(Credentials payload) {
                        call.clone().enqueue(BackendApiCallback.this); // Retry service call
                    }

                    @Override
                    public void onFailure(CredentialsManagerException error) {
                        onAuthenticationLost();
                    }
                });
    }
}
