package com.example.smart_latch_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by CJ on 26/6/2017.
 */

public class AuthInt implements Interceptor {
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

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();                  //<<< Original Request

        //Build new request-----------------------------
        Request.Builder builder = request.newBuilder();
        builder.header("Accept", "application/json");       //if necessary...

        editor = prefs.edit();

        String defaultToken = "defaultTokenValue";
        token = prefs.getString("token", defaultToken);//Save token of this request for future
        setAuthHeader(builder, token);                      //Add Current Authentication Token..

        request = builder.build();                          //Overwrite the original request

        System.out
                .println(
                ">>> Sending Request >>>\n"
                        +"To: "+request.url()+"\n"
                        +"Headers:"+request.headers()+"\n"
                        +"Body: "+bodyToString(request));   //Shows the magic...

        //------------------------------------------------------------------------------------------
        Response response = chain.proceed(request);         // Sends the request (Original w/ Auth.)
        //------------------------------------------------------------------------------------------

        System.out.println(
                "<<< Receiving Request response <<<\n"
                        +"To: "+response.request().url()+"\n"
                        +"Headers: "+response.headers()+"\n"
                        +"Code: "+response.code()+"\n"
                        +"Body: "+bodyToString(response.request()));  //Shows the magic...



        //------------------- 401 --- 401 --- UNAUTHORIZED --- 401 --- 401 -------------------------

        if (response.code() == RESPONSE_UNAUTHORIZED_401) { //If unauthorized (Token expired)...
            System.out.println("Request responses code: "+response.code());

            synchronized (this) {                           // Gets all 401 in sync blocks,
                // to avoid multiply token updates...

                String currentToken = prefs.getString("token", defaultToken);

                //Compares current token with token that was stored before,
                // if it was not updated - do update..

                if(currentToken != null && currentToken.equals(token)) {

                    // --- REFRESHING TOKEN --- --- REFRESHING TOKEN --- --- REFRESHING TOKEN ------
                    String refreshToken = prefs.getString("refreshToken", defaultToken);
                    String email = prefs.getString("email", "email");
                    int code = refreshToken(refreshToken, email) / 100;                    //Refactor resp. cod ranking


                    if(code != RESPONSE_HTTP_RANK_2XX) {                // If refresh token failed

                        if(code == RESPONSE_HTTP_CLIENT_ERROR           // If failed by error 4xx...
                                ||
                                code == RESPONSE_HTTP_SERVER_ERROR ){   // If failed by error 5xx...

                            logout();                                   // ToDo GoTo login screen
                            return response;                            // Todo Shows auth error to user
                        }
                    }   // <<--------------------------------------------New Auth. Token acquired --
                }   // <<-----------------------------------New Auth. Token acquired double check --


                // --- --- RETRYING ORIGINAL REQUEST --- --- RETRYING ORIGINAL REQUEST --- --------|

                if(prefs.getString("token", defaultToken) != null) {                  // Checks new Auth. Token
                    setAuthHeader(builder, prefs.getString("token", defaultToken));   // Add Current Auth. Token
                    request = builder.build();                          // O/w the original request

                    System.out.println(
                            ">>> Retrying original Request >>>\n"
                                    +"To: "+request.url()+"\n"
                                    +"Headers:"+request.headers()+"\n"
                                    +"Body: "+bodyToString(request));  //Shows the magic...


                    //-----------------------------------------------------------------------------|
                    Response responseRetry = chain.proceed(request);// Sends request (w/ New Auth.)
                    //-----------------------------------------------------------------------------|


                    System.out.println(
                            "<<< Receiving Retried Request response <<<\n"
                                    +"To: "+responseRetry.request().url()+"\n"
                                    +"Headers: "+responseRetry.headers()+"\n"
                                    +"Code: "+responseRetry.code()+"\n"
                                    +"Body: "+bodyToString(response.request()));  //Shows the magic.

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
    private void setAuthHeader(Request.Builder builder, String token) {
        System.out.println("Setting authentication header...");
        if (token != null){
            builder.header("Authorization", String.format("Bearer %s", token));
        }

        System.out.println("Current Auth Token = "+ prefs.getString("token", "defaultToken"));
        System.out.println("Current Refresh Token = "+ prefs.getString("token", "defaultToken"));
    }

    // Refresh/renew Synchronously Authentication Token & refresh token----------------------------|
    private int refreshToken(String refreshToken, String email) {
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

                } catch (JSONException e) {
                    e.printStackTrace();
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

    //----------------------------------------------------------------------------------------------
    @Deprecated
    private static String bodyToString(final Request request){
        /*
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            Log.w(TAG_ALIEN+TAG_THIS,"Error while trying to get body to string.");
            return "Null";
        }*/
        return "Nullix";
    }

}
