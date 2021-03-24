package com.example.smart_latch_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FirstFragment extends Fragment {

    private TextView mTextViewResult;
    private TextView doorIdTitle;
    private ImageButton backBtn;
    private Button initialiseButton;

    String responseString = "";

    String doorID;

    JSONObject jObj = null;
    Integer state = 0;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        OkHttpClient client = (OkHttpClient) new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String hostUrl = getString(R.string.smart_latch_url);
        String[] doorStates = {getString(R.string.door_state_locked), getString(R.string.door_state_open)};
        mTextViewResult = view.findViewById(R.id.textview_result);
        doorIdTitle = view.findViewById(R.id.textview_doorid);
        backBtn = view.findViewById(R.id.back_nav);

        if (getArguments() != null) {
            doorID = getArguments().getString("doorID");
            doorIdTitle.setText(doorID);
        }

        backBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                gotoMyDoorActivity();
            }
        });
        // === OPEN ===
        view.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = hostUrl + "/toggleLatch?state=1?doorId=" + doorID;

                Request request = new Request.Builder().url(url).build();

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
                            state = jObj.getInt("newDoorState");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String currentStatus = getString(R.string.current_door_status_hint);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextViewResult.setText(currentStatus + " " + doorStates[state]);
                            }
                        });

                    }
                });

            }
        });

        // === CLOSE ===
        view.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = hostUrl + "/toggleLatch?state=0?doorId=" + doorID;

                Request request2 = new Request.Builder().url(url).build();

                client.newCall(request2).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        responseString = response.body().string();

                        try {
                            jObj = new JSONObject(responseString);
                            state = jObj.getInt("newDoorState");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String currentStatus = getString(R.string.current_door_status_hint);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextViewResult.setText(currentStatus + " " + doorStates[state]);
                            }
                        });
                    }
                });


            }
        });

    }

    private void gotoMyDoorActivity() {
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }
}
