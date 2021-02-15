package com.example.smart_latch_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

        OkHttpClient client = new OkHttpClient();

        String hostUrl = getString(R.string.smart_latch_url);

        // === OPEN ===
        view.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = hostUrl + "/toggleLatch?state=1";

                Request request = new Request.Builder().url(url).build();


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
                System.out.println("BUTTON CLICKED" + view.getId());
                String url = hostUrl + "/toggleLatch?state=0";

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
