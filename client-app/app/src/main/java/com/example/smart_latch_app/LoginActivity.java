package com.example.smart_latch_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;


public class LoginActivity extends AppCompatActivity {
    SignInButton signinButton;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int SIGN_IN = 1;

    String responseString = "";
    String token = "";
    String refreshToken = "";

    JSONObject jObj = null;
    Boolean userIsVerified = false;

    HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestProfile()
                .requestEmail().build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signinButton = findViewById(R.id.sign_in_btn);

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, SIGN_IN);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn
                .getLastSignedInAccount(this);
        if (account != null) {
            startSignInIntent();
            Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                String name = account.getGivenName();
                String email = account.getEmail();

                editor.putString("email", email);
                validateTokenOnServer(idToken, name);

            } catch (ApiException e) {
                e.printStackTrace();
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSignInIntent () {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, SIGN_IN);
    }

    private void gotoMainActivity (String userName) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        finish();
    }

    private void validateTokenOnServer(String idToken, String userName) {
        String hostName = getString(R.string.smart_latch_url);
        String url = hostName + "/verifyUser?idToken=" + idToken;
        System.out.println("ID TOKEN: " + idToken);
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();

                try {
                    jObj = new JSONObject(responseString);
                    userIsVerified = jObj.getBoolean("success");
                    token = jObj.getString("token");
                    refreshToken = jObj.getString("refreshToken");
                    // store tokens
                    System.out.println("REFRESH TOKEN: " + refreshToken);
                    String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRrZWxseTJAdGNkLmllIiwiZmlyc3ROYW1lIjoiVGhvbWFzIiwibGFzdE5hbWUiOiJLZWxseSIsImlkIjoiMTExNjUzMDM4ODU5Mjc5NTU2ODI1IiwiaWF0IjoxNjE1MDQwOTk3LCJleHAiOjE2MTUwNDQ1OTd9.dDTe87DQnf1tqF_vB8Mp-NPu1yFm-FwsVgk4X6EzigU";
                    editor.putString("token", testToken);
                    editor.putString("refreshToken", refreshToken);
                    editor.apply();
                    if (userIsVerified == true) {
                        gotoMainActivity(userName);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
