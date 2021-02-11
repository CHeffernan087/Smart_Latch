package com.example.smart_latch_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextLinks;
import android.widget.EditText;
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
    EditText mEdit;

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

        mTextViewResult = view.findViewById(R.id.textview2);
        mEdit = view.findViewById(R.id.textInput);

        OkHttpClient client = new OkHttpClient();
        String testUrl = "https://www.sci.utah.edu/~macleod/docs/txt2html/sample.txt";

        // onClick handler for button 1
        view.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hostName = mEdit.getText().toString();
                String url = "https://" + hostName + "/toggleLatch?state=1";
                System.out.println("> Clicked OPEN");

                Request request = new Request.Builder().url(url).build();
                String builtUrl = "The URL built is: " + url;
                System.out.println(builtUrl);
                mTextViewResult.setText(builtUrl);

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("> Error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        System.out.println("> Success, response:");
                        System.out.println(response.body().string());
                    }
                });
            }
        });

        // onClick handler for button 2
        view.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hostName = mEdit.getText().toString();
                String url = "https://" + hostName + "/toggleLatch?state=0";
                System.out.println("Clicked CLOSE");
                String builtUrl = "The URL built is: " + url;
                System.out.println(builtUrl);
                mTextViewResult.setText(builtUrl);

                Request request2 = new Request.Builder().url(url).build();

                client.newCall(request2).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("> Error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        System.out.println("> Success");
                    }
                });
            }
        });
    }
}