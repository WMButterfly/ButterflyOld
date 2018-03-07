package com.windowmirror.android.auth;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.windowmirror.android.R;
import com.windowmirror.android.controller.MainActivity;
import com.windowmirror.android.service.BackendService;

/**
 * Activity used when the user needs to authenticate.
 */
public class AuthActivity extends Activity implements AuthCallback {
    private static final String AUTH0_SCOPE = "write:recording read:recording openid profile email offline_access";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        findViewById(R.id.button_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginClick();
            }
        });
    }

    private void onLoginClick() {
        Auth0 auth0 = new Auth0(this);
        auth0.setOIDCConformant(true);
        WebAuthProvider.init(auth0)
                .withScope(AUTH0_SCOPE)
                .start(this, this);
    }

    @Override
    public void onFailure(@NonNull Dialog dialog) {
        // Show error Dialog to user
        dialog.show();
    }

    @Override
    public void onFailure(AuthenticationException exception) {
        // Show error to user
        Toast.makeText(AuthActivity.this, exception.getDescription(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSuccess(@NonNull Credentials credentials) {
        BackendService.setCredentials(this, credentials);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
