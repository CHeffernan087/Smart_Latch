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

    public void onNfcDetected(String doorId){
        Log.v(TAG,"NFC ID: " + doorId);
        Log.v(TAG,"Sending Request to add door.");
        sendAddDoorReq(doorId);
        ((MyDoorsActivity) getActivity()).appendDoor(doorId);
    }


    private void sendAddDoorReq(String doorId) {

        OkHttpClient client = new OkHttpClient();
        String hostUrl = getString(R.string.smart_latch_url) + "/registerDoor2" ;

        Log.v(TAG,hostUrl);
        RequestBody formBody = new FormBody.Builder()
                .add("doorId", doorId)
                .add("userId", "1234")
                .build();
        Request request = new Request.Builder()
                .url(hostUrl)
                .post(formBody)
                .build();

        // sending request to add door for scanned NFC ID
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
                    responseMessage = jObj.getString("message");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.v(TAG,"NFC ID: 0x" + doorId);
//                        doorIdText.setText("Door ID: 0x" + doorId);
//
//                    }
//                });
            }
        });
    }
}