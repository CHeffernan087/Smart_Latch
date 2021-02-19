package com.example.smart_latch_app;

import android.content.Intent;
import android.os.Bundle;
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
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;


public class LoginActivity extends AppCompatActivity {
    SignInButton signinButton;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int SIGN_IN = 1;

    String responseString = "";
    JSONObject jObj = null;
    String userIsVerified = "";

    HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestProfile()
                .requestEmail().build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signinButton = findViewById(R.id.sign_in_btn);
        signinButton.setVisibility(View.GONE);
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
        } else {
            signinButton.setVisibility(View.VISIBLE);
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

                System.out.println("Hi " + name);
                System.out.println("HERE IS YOUR SENSITIVE ID TOKEN LOL: " + idToken);

                // REGISTER REQUEST
                validateTokenOnServer(idToken);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("USER_NAME", name);
                startActivity(intent);
                finish();


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

    private void validateTokenOnServer(String idToken) {
        String hostUrl = getString(R.string.smart_latch_url);
        String url = hostUrl + "/verifyUser" + "?idToken=" + idToken;

        System.out.println("Start validation: " + url);

//        RequestBody formBody = new FormBody.Builder()
//                .add("idToken", idToken)
//                .build();

        Request request = new Request.Builder()
                .url(url)
//                .post(formBody)
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
                    userIsVerified = jObj.getString("success");
                    System.out.println("IS USER VERIFiED? " + userIsVerified.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }


//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTextViewResult.setText(currentStatus + " " + doorStates[state]);
//                    }
//                });

            }
        });
    }
}
