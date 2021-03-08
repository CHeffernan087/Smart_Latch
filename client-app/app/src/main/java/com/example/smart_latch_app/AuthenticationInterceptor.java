package com.example.smart_latch_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {
    Context applicationContext = MainActivity.getContextOfApplication();
    String responseString = "";
    JSONObject jObj = null;
    String token = "";
    int refreshResponse = 0;
    OkHttpClient client = new OkHttpClient();

    SharedPreferences.Editor editor;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        //Build new request
        Request.Builder builder = request.newBuilder();
        builder.header("Accept", "application/json"); //if necessary, say to consume JSON

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        editor = prefs.edit();

        String defaultToken = "defaultTokenValue";
        token = prefs.getString("token", defaultToken);
        System.out.println("PULL OUT TOKEN: "+  token);

        request = builder.build(); //overwrite old request
        Response response = chain.proceed(request); //perform request, here original request will be executed

        if (response.code() == 401) { //if unauthorized
            System.out.println("HERES THE FLOW WE WANNA TEST");
            synchronized (this) { //perform all 401 in sync blocks, to avoid multiply token updates
                String currentToken = prefs.getString("token", defaultToken);

                if(currentToken != null && currentToken.equals(token)) { //compare current token with token that was stored before, if it was not updated - do update
                    String refreshToken = prefs.getString("refreshToken", defaultToken);
                    String email = prefs.getString("email", "email");
                    int code = refreshToken(refreshToken, email, builder, chain, request) / 100; //refresh token
                    System.out.println("CODE: " + code);
                }
//                if (currentToken  != null) {
//                    setAuthHeader(builder, currentToken);
//                    request = builder.build();
//                    return chain.proceed(request);
//                }

            }
        }

        return response;
    }

    private void setAuthHeader(Request.Builder builder, String token) {
        if (token != null) //Add Auth token to each request if authorized
            builder.header("x-auth-token", token);
    }

    private int refreshToken(String refreshToken, String email,  Request.Builder builder, Chain chain, Request request) {
        //Refresh token, synchronously, save it, and return result code
        //you might use retrofit here
        System.out.println("Refresh");
        OkHttpClient refreshClient = new OkHttpClient();

        String hostName = this.applicationContext.getString(R.string.test_url);
        String url = hostName + "/refreshToken?refreshToken=" + refreshToken + "&email=" + email;
        System.out.println("REFRESHING AT this endpoint: " + url);
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
                responseString = response.body().string();
                System.out.println("RESPONSE AUTH REFRESH: " + responseString);
                try {
                    jObj = new JSONObject(responseString);
                    token = jObj.getString("token");

                    // store tokens
                    System.out.println("PUT TOKEN INTO PREFS: " + token);
                    editor.putString("token", token);
                    editor.apply();
                    refreshResponse = 200;
                    // call repeat request
                    retry(token, chain.request(), builder, chain);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return refreshResponse;
    }

    private Response retry (String updatedToken, Request request, Request.Builder builder, Chain chain) throws IOException {
        System.out.println("Retry");
        if (updatedToken  != null) {
            setAuthHeader(builder, updatedToken);
            request = builder.build();
            return chain.proceed(request);
        }
        return null;
    }

    private int logout() {
        //logout your user
        System.out.println("Logout");
        return -1;
    }
}