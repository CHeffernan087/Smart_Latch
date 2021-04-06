package com.example.smart_latch_app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Selfie extends Activity {
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private Bitmap photo = null;
    public static Selfie instance = null;

    @Override
    public void onResume() {
        super.onResume();
        instance = this;
    }

    @Override
    public void onPause() {
        super.onPause();
        instance = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie);
        this.imageView = (ImageView)this.findViewById(R.id.imageView);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Button photoButton = (Button) this.findViewById(R.id.takePhotoBtn);
        Button uploadButton = (Button) this.findViewById(R.id.uploadBtn);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_selfie);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMainActivity();
            }
        });
        photoButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = prefs.getString("email", "email");

                if (photo == null) {
                    Toast.makeText(getApplicationContext(), "You need to take a photo before uploading.", Toast.LENGTH_LONG).show();
                } else {
                    byte[] encodedImage = convertImageToByteArray(photo);
                    try {
                        postImage(encodedImage, email);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private byte[] convertImageToByteArray (Bitmap img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        return b;
    }

    private void postImage(byte[] imageAsByteArray, String email) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://recognition.smart-latchxyz.xyz/register";

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("email", email)
                .addFormDataPart("image", "image.jpg", RequestBody.create(MediaType.parse("image/*jpg"), imageAsByteArray))
                .build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseString = response.body().string();
                String errorMessage = null;
                JSONObject jObj = null;
                try {
                    jObj = new JSONObject(responseString);

                    String responseMessage = jObj.getString("Message");
                    Selfie.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), responseMessage, Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();

                    try {
                        errorMessage = jObj.getString("Error");
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                    String finalErrorMessage = errorMessage;
                    Selfie.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "There's something wrong with the photo you took.", Toast.LENGTH_SHORT).show();
                            System.out.println("Error uploading image: " + finalErrorMessage);
                        }
                    });
                }

            }
        });

    }

    private void gotoMainActivity() {
        startActivity(new Intent(Selfie.this, MainActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }
}