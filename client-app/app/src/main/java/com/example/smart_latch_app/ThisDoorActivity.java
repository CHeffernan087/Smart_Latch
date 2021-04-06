package com.example.smart_latch_app;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ThisDoorActivity extends AppCompatActivity {
    private TextView mTextViewResult;
    private TextView doorIdTitle;
    private ImageButton backBtn;
    private ImageButton openBtn;
    private ImageButton closeBtn;
    private ImageButton grantAccessBtn;
    private String dialogTextBox = "";

    // NFC setup
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    String responseString = "";

    String doorID;
    String[] doors = new String[] {};
    JSONObject doorDetails;

    JSONObject jObj = null;
    Integer state = 0;

    // variables for /2fa endpoint
    String responseString2fa = "";
    JSONObject jObj2fa = null;
    String responseMessage = "";

    @Override
    protected void onResume () {
        super.onResume();
        assert nfcAdapter != null;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            detectTagData(tag);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_this_door);
        OkHttpClient client = (OkHttpClient) new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String hostUrl = getString(R.string.smart_latch_url);
        String[] doorStates = {getString(R.string.door_state_locked), getString(R.string.door_state_open)};
        mTextViewResult = (TextView) findViewById(R.id.textview_result);
        doorIdTitle = (TextView) findViewById(R.id.textview_doorid);
        backBtn = (ImageButton) findViewById(R.id.back_nav);
        openBtn = (ImageButton) findViewById(R.id.button2);
        closeBtn = (ImageButton) findViewById(R.id.button1);
        grantAccessBtn = (ImageButton) findViewById(R.id.grant_access);

        Bundle b = getIntent().getExtras();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String email = prefs.getString("email", "email");

        if (b != null) {
            doorID = b.getString("doorId");
            doors = b.getStringArray("DOORS");
            try {
                doorDetails = new JSONObject(b.getString("DETAILS"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            doorIdTitle.setText(doorID);
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        backBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                gotoMyDoorActivity();
            }
        });

        // === OPEN ===
        openBtn.setOnClickListener(new View.OnClickListener() {
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

                        ThisDoorActivity.this.runOnUiThread(new Runnable() {
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
        closeBtn.setOnClickListener(new View.OnClickListener() {
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
                        ThisDoorActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextViewResult.setText(currentStatus + " " + doorStates[state]);
                            }
                        });
                    }
                });


            }
        });

        grantAccessBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                try {
                    if(email.equals(doorDetails.getJSONObject(doorID).getJSONObject("Admin").getJSONObject("_path").getJSONArray("segments").get(1))) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(ThisDoorActivity.this);
                        builder.setTitle("Grant access to another user:");

                        final EditText input = new EditText(ThisDoorActivity.this);

                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialogTextBox = input.getText().toString();
                                grantAccess(dialogTextBox, doorID);
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    } else {

                        System.out.println("Get onto your door admin");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void gotoMyDoorActivity() {
        Intent i = new Intent(ThisDoorActivity.this, MyDoorsActivity.class);
        i.putExtra("DOORS", doors);
        i.putExtra("DETAILS", doorDetails.toString());
        startActivity(i);
    }

    private void grantAccess(String emailOfPersonToGiveAccess, String doorId) {
        // Todo: grant access to another user
    }

    private void detectTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append(toHex(id));
        String nfcId = sb.toString();
        Toast.makeText(this, "NFC authenticated!", Toast.LENGTH_SHORT).show();

        OkHttpClient client = (OkHttpClient) new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String url = this.getString(R.string.smart_latch_url) + "/nfcUpdate";

        RequestBody formBody = new FormBody.Builder()
                .add("nfcId", nfcId)
                .add("doorId", doorID)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString2fa = response.body().string();
                System.out.println("updateNfc response" + responseString);
                try {
                    jObj2fa = new JSONObject(responseString);
                    responseMessage = jObj2fa.getString("message");
                    System.out.println("response message: " + responseMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
}
