package com.example.smart_latch_app;

import androidx.fragment.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

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


public class AddDoorFragment extends DialogFragment {

    public static final String TAG = AddDoorFragment.class.getSimpleName();
    String responseString = "";
    String responseMessage = "";
    JSONObject jObj = null;

    public static AddDoorFragment newInstance() {
        return new AddDoorFragment();
    }

    private TextView doorIdText;
    private Listener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_door,container,false);
        doorIdText = (TextView) view.findViewById(R.id.doorIdText);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MyDoorsActivity) getActivity()).onDialogDisplayed();
        mListener = (MyDoorsActivity)context;
        mListener.onDialogDisplayed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.onDialogDismissed();
    }

    public void onNfcDetected(String doorId, String nfcTagId){
        Log.v(TAG,"NFC ID: " + doorId);
        Log.v(TAG,"Sending Request to add door.");
        System.out.println("> onNfcDetected");
        sendAddDoorReq(doorId, nfcTagId);
//        ((MyDoorsActivity) getActivity()).appendDoor(doorId);
    }


    private void sendAddDoorReq(String doorId, String nfcTagId) {

        OkHttpClient client = new OkHttpClient()
                .newBuilder()
//                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String hostUrl = getString(R.string.smart_latch_url) + "/registerDoor" ;

        String email ="";
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(((MyDoorsActivity) getActivity()));
        if (acct != null) {
            email = acct.getEmail();
        }
        Log.v(TAG,hostUrl);
        RequestBody formBody = new FormBody.Builder()
                .add("doorId", doorId)
                .add("email",  email)
                .add("nfcId", nfcTagId)
                .build();
        Request request = new Request.Builder()
                .url(hostUrl)
                .post(formBody)
                .build();

        System.out.println("SENDING THE NFCID IN THE REQUEST: " + nfcTagId);
        // sending request to add door for scanned NFC ID
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                System.out.println(responseString);
                try {
                    jObj = new JSONObject(responseString);
                    responseMessage = jObj.getString("message");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}