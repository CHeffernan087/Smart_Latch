package com.example.smart_latch_app;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ComponentActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FirstFragment extends Fragment {

    private TextView mTextViewResult;
    private TextView mTextViewUrl;
    public static String responseString = "";

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

        mTextViewResult = view.findViewById(R.id.textview_result);
        mTextViewUrl = view.findViewById(R.id.textview_url);

        OkHttpClient client = new OkHttpClient();

        String herokuHostUrl = "https://smart-latch.herokuapp.com";

        // === OPEN ===
        view.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = herokuHostUrl + "/toggleLatch?state=1";

                Request request = new Request.Builder().url(url).build();
                String builtUrl = "The endpoint URL is: " + url;
                System.out.println(builtUrl);
                mTextViewUrl.setText(builtUrl);

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("> Error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        responseString = response.body().string();
                        System.out.println("> Response received: " + responseString);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextViewResult.setText("Response: " + responseString);
                            }
                        });

                    }
                });

            }
        });

        // === CLOSE ===
        view.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = herokuHostUrl + "/toggleLatch?state=0";

                String builtUrl = "The endpoint URL is: " + url;
                System.out.println(builtUrl);
                mTextViewUrl.setText(builtUrl);

                Request request2 = new Request.Builder().url(url).build();

                client.newCall(request2).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("> Error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        responseString = response.body().string();
                        System.out.println("> Response received: " + responseString);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextViewResult.setText("Response: " + responseString);
                            }
                        });
                    }
                });


            }
        });
    }
}
