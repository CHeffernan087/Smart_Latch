package com.example.smart_latch_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Created by CJ on 26/6/2017.
 */

public class AuthenticationInterceptor implements Interceptor {
    Context applicationContext = MainActivity.getContextOfApplication();

    //--- HTTP Response codes relative constants
    private static final int RESPONSE_UNAUTHORIZED_401 = 401;
    private static final int RESPONSE_HTTP_RANK_2XX = 2;
    private static final int RESPONSE_HTTP_CLIENT_ERROR = 4;
    private static final int RESPONSE_HTTP_SERVER_ERROR = 5;
    //--- My backend params
    private static final String BODY_PARAM_KEY_GRANT_TYPE = "grant_type";
    private static final String BODY_PARAM_VALUE_GRANT_TYPE = "refresh_token";
    private static final String BODY_PARAM_KEY_REFRESH_TOKEN = "refresh_token";
    SharedPreferences.Editor editor;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    String responseString = "";
    String token = "";
    JSONObject jObj = null;

    int refreshResponse = 0;
    private AtomicBoolean processed = new AtomicBoolean(true) ;

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();                  //<<< Original Request

        //Build new request-----------------------------
        Request.Builder builder = request.newBuilder();
        builder.header("Accept", "application/json");       //if necessary...

        editor = prefs.edit();

        String defaultToken = "defaultTokenValue";
        token = prefs.getString("token", defaultToken);//Save token of this request for future
        builder.addHeader("x-auth-token", token);

        request = builder.build();                          //Overwrite the original request

        System.out
                .println(
                "2. >>> Sending Request >>>\n"
                        +"To: "+request.url()+"\n"
                        +"Headers:"+request.headers()+"\n"
                        );   //Shows the magic...

        //------------------------------------------------------------------------------------------
        Response response = chain.proceed(request);         // Sends the request (Original w/ Auth.)
        //------------------------------------------------------------------------------------------

        System.out.println(
                "2. <<< Receiving Request response <<<\n"
                        +"To: "+response.request().url()+"\n"
                        +"Headers: "+response.headers()+"\n"
                        +"Code: "+response.code()+"\n"
                        );  //Shows the magic...



        //------------------- 401 --- 401 --- UNAUTHORIZED --- 401 --- 401 -------------------------
        System.out.println("2. Original Request responses code: "+response.code());
        if (response.code() == RESPONSE_UNAUTHORIZED_401) { //If unauthorized (Token expired)...
            response.close();
            synchronized (processed) {                           // Gets all 401 in sync blocks,
                // to avoid multiply token updates...

                String currentToken = prefs.getString("token", defaultToken);
                System.out.println("3. Inside sync bit of 401 section. Token pulled here is " + currentToken);

                //Compares current token with token that was stored before,
                // if it was not updated - do update..

                if(currentToken != null && currentToken.equals(token)) {
                    try {
                        // --- REFRESHING TOKEN --- --- REFRESHING TOKEN --- --- REFRESHING TOKEN ------
                        String refreshToken = prefs.getString("refreshToken", defaultToken);
                        String email = prefs.getString("email", "email");
                        int code = refreshToken(refreshToken, email) / 100;
                        processed.wait(); //Refactor resp. cod ranking

                        System.out.println("4. Now we have returned from refresh token with the code: " + code);
                        if(code != RESPONSE_HTTP_RANK_2XX) {                // If refresh token failed

                            if(code == RESPONSE_HTTP_CLIENT_ERROR           // If failed by error 4xx...
                                    ||
                                    code == RESPONSE_HTTP_SERVER_ERROR ){   // If failed by error 5xx...

                                logout();                                   // ToDo GoTo login screen
                                return response;                            // Todo Shows auth error to user
                            }
                        }   // <<--------------------------------------------New Auth. Token acquired --
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                }   // <<-----------------------------------New Auth. Token acquired double check --


                // --- --- RETRYING ORIGINAL REQUEST --- --- RETRYING ORIGINAL REQUEST --- --------|
                System.out.println("5. Retrying");
                if(prefs.getString("token", "defaultToken") != null) {
                    String retryToken = prefs.getString("token", "defaultToken");
//                    setAuthHeader(builder, retryToken);   // Add Current Auth. Token
                    builder.removeHeader("x-auth-token");
                    builder.addHeader("x-auth-token", retryToken);
                    request = builder.build();                          // O/w the original request

                    System.out.println(
                            "5. >>> Retrying original Request >>>\n"
                                    +"To: "+request.url()+"\n"
                                    +"Headers:"+request.headers()+"\n"
                                    );  //Shows the magic...

//                    response.close();
                    //-----------------------------------------------------------------------------|
                    Response responseRetry = chain.proceed(request);// Sends request (w/ New Auth.)
                    //-----------------------------------------------------------------------------|


                    System.out.println(
                            "5. <<< Receiving Retried Request response <<<\n"
                                    +"To: "+responseRetry.request().url()+"\n"
                                    +"Headers: "+responseRetry.headers()+"\n"
                                    +"Code: "+responseRetry.code()+"\n"
                                    );  //Shows the magic.

                    return responseRetry;
                }

            }
        }else {
            //------------------- 200 --- 200 --- AUTHORIZED --- 200 --- 200 -----------------------
            System.out.println("Request responses code: "+response.code());
        }

        return response;

    }


    // Sets/Adds the authentication header to current request builder.-----------------------------|
    private void setAuthHeader(Request.Builder builder, String updatedToken) {
        System.out.println("MISC: Setting authentication header to this: " + updatedToken);
        if (token != null){
            builder.addHeader("x-auth-token", updatedToken);
        }
    }

    // Refresh/renew Synchronously Authentication Token & refresh token----------------------------|
    private int refreshToken(String refreshToken, String email) {
        //Refresh token, synchronously, save it, and return result code
        //you might use retrofit here
        System.out.println("3. Going to hit RefreshToken endpoint");

        OkHttpClient refreshClient = new OkHttpClient();

        String hostName = this.applicationContext.getString(R.string.smart_latch_url);
        String url = hostName + "/refreshToken?refreshToken=" + refreshToken + "&email=" + email;
        System.out.println("3. REFRESHING AT this endpoint: " + url);
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
                    System.out.println("3. RESPONSE AUTH REFRESH: " + responseString);
                    try {
                        jObj = new JSONObject(responseString);
                        token = jObj.getString("token");

                        // store tokens
                        System.out.println("3. PUT TOKEN INTO PREFS: " + token);
                        editor.putString("token", token);
                        editor.apply();
                        refreshResponse = 200;
                        // call repeat request
                        processed.notify();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        return refreshResponse;
    }

    private int logout() {
        System.out.println("go to logout");
        //logout your user
        return 0; //TODO...
    }
}
