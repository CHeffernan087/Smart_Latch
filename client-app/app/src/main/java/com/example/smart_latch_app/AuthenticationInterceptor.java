package com.example.smart_latch_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {
    Context applicationContext = MainActivity.getContextOfApplication();

    //--- HTTP Response codes relative constants
    private static final int RESPONSE_UNAUTHORIZED_401 = 401;
    private static final int RESPONSE_HTTP_RANK_2XX = 2;
    private static final int RESPONSE_HTTP_CLIENT_ERROR = 4;
    private static final int RESPONSE_HTTP_SERVER_ERROR = 5;

    SharedPreferences.Editor editor;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    String responseString = "";
    String token = "";
    JSONObject jObj = null;

    int refreshResponse = 0;
    private AtomicBoolean processed = new AtomicBoolean(true) ;

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        Request.Builder builder = request.newBuilder();

        editor = prefs.edit();

        String defaultToken = "defaultTokenValue";
        token = prefs.getString("token", defaultToken);
        builder.addHeader("x-auth-token", token);

        request = builder.build();

        System.out.println(
                "2. >>> Retrying original Request >>>\n"
                        +"To: "+request.url()+"\n"
                        +"Headers:"+request.headers()+"\n"
        );

        Response response = chain.proceed(request);

        System.out.println(
                "2. <<< Receiving Request response <<<\n"
                        +"To: "+response.request().url()+"\n"
                        +"Headers: "+response.headers()+"\n"
                        +"Code: "+response.code()+"\n"
                        );

        //------------------- 401 UNAUTHORIZED
        System.out.println("2. Original Request responses code: "+response.code());
        if (response.code() == RESPONSE_UNAUTHORIZED_401) {
            response.close();
            synchronized (processed) {

                String currentToken = prefs.getString("token", defaultToken);
                System.out.println("3. Inside sync bit of 401 section. Token pulled here is " + currentToken);

                if(currentToken != null && currentToken.equals(token)) {
                    try {
                        // --- REFRESHING TOKEN
                        String refreshToken = prefs.getString("refreshToken", defaultToken);
                        String email = prefs.getString("email", "email");
                        int code = refreshToken(refreshToken, email) / 100;
                        processed.wait(); //Refactor resp. cod ranking

                        System.out.println("4. Now we have returned from refresh token with the code: " + code);
                        if(code != RESPONSE_HTTP_RANK_2XX) {                // If refresh token failed

                            if(code == RESPONSE_HTTP_CLIENT_ERROR           // If failed by error 4xx...
                                    ||
                                    code == RESPONSE_HTTP_SERVER_ERROR){   // If failed by error 5xx...

                                logout();

                                Toast.makeText(applicationContext, "Token refreshing failed.", Toast.LENGTH_SHORT).show();

                                return response;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                }

                // --- --- RETRYING ORIGINAL REQUEST
                System.out.println("5. Retrying the original request");
                if(prefs.getString("token", "defaultToken") != null) {
                    String retryToken = prefs.getString("token", "defaultToken");

                    builder.removeHeader("x-auth-token");
                    builder.addHeader("x-auth-token", retryToken);
                    request = builder.build();

                    System.out.println(
                            "5. >>> Retrying original Request >>>\n"
                                    +"To: "+request.url()+"\n"
                                    +"Headers:"+request.headers()+"\n"
                                    );

                    Response responseRetry = chain.proceed(request);
                    System.out.println(
                            "5. <<< Receiving Retried Request response <<<\n"
                                    +"To: "+responseRetry.request().url()+"\n"
                                    +"Headers: "+responseRetry.headers()+"\n"
                                    +"Code: "+responseRetry.code()+"\n"
                                    );

                    return responseRetry;
                }

            }
        } else {
            System.out.println("Request responses code: "+response.code());
        }

        return response;

    }

    private int refreshToken(String refreshToken, String email) {
        OkHttpClient refreshClient = new OkHttpClient();

        String hostName = this.applicationContext.getString(R.string.smart_latch_url);
        String url = hostName + "/refreshToken?refreshToken=" + refreshToken + "&email=" + email;
        System.out.println("3. Refreshing: " + url);
        Request refreshRequest = new Request.Builder()
                .url(url)
                .build();

        refreshClient.newCall(refreshRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                refreshResponse = 400;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                synchronized (processed) {
                    responseString = response.body().string();
                    System.out.println("3. Response to refreshToken request: " + responseString);
                    try {
                        jObj = new JSONObject(responseString);
                        token = jObj.getString("token");

                        // store tokens
                        System.out.println("3. Put the new token in prefs: " + token);
                        editor.putString("token", token);
                        editor.apply();
                        refreshResponse = 200;
                        // call repeat request
                        processed.notify(); // allow thread execution to continue

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        return refreshResponse;
    }

    private int logout() {
        Intent intent = new Intent();
        intent.setClass(applicationContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("SILENT_SIGN_IN", false);

        applicationContext.startActivity(intent);
        ((Activity)applicationContext).finish();
        return 0;
    }
}
