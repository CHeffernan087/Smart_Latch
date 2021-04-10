package com.example.smart_latch_app;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
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
import android.os.CountDownTimer;

public class ThisDoorActivity extends AppCompatActivity {
    private TextView mTextViewResult;
    private TextView doorIdTitle;
    private ImageButton backBtn;
    private Button refreshBtn;
    private ImageButton grantAccessBtn;
    private String dialogTextBox = "";
    private View view;
    private TextView mTextViewDoorState;

    public static ThisDoorActivity instance = null;

    // NFC setup
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    int NFC_RESET_TIME = 20000;
    int NFC_COUNTDOWN_INTERVAL = 1000;
    private String macAddress = null;

    String responseString = "";

    String doorID;
    String[] doors = new String[] {};
    JSONObject doorDetails;

    JSONObject jObj = null;
    Boolean locked = true;

    @Override
    protected void onResume () {
        super.onResume();
        assert nfcAdapter != null;
        instance = this;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    protected void onPause() {
        super.onPause();
        instance = null;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages.length > 0) {
                NdefMessage msg = (NdefMessage) rawMessages[0];

                if (msg.getRecords().length > 0) {
                    NdefRecord rec = msg.getRecords()[0];
                    String s = new String(rec.getPayload());
                    macAddress = getMacAddr(s);
                }
            }

        }
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


        mTextViewResult = (TextView) findViewById(R.id.textview_result);
        mTextViewDoorState = (TextView) findViewById(R.id.textview_door_state);
        doorIdTitle = (TextView) findViewById(R.id.textview_doorid);
        backBtn = (ImageButton) findViewById(R.id.back_nav);
        refreshBtn = (Button) findViewById(R.id.refreshBtn);

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

        refreshState(); // get the doors current status 'locked'

        backBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                gotoMyDoorActivity();
            }
        });

        // === Refresh ===
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               refreshState();
            }
        });


        // == Grant access ==
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
        OkHttpClient client = (OkHttpClient) new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String url = this.getString(R.string.smart_latch_url) + "/nfcUpdate?doorId=" + macAddress;

        RequestBody formBody = new FormBody.Builder()
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
                String responseString2 = response.body().string();
                try {
                    JSONObject jObj2 = new JSONObject(responseString2);
                    String responseMessage2 = jObj2.getString("message");
                    ThisDoorActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            successfulNfcAuth();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    ThisDoorActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ThisDoorActivity.this, "NFC failed to authenticate.", Toast.LENGTH_SHORT).show();

                        }
                    });
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

    private String getMacAddr(String macAddrNfcString) {
        for (int i = 0; i < macAddrNfcString.length(); i++) {
            char c = macAddrNfcString.charAt(i);
            if (Character.isDigit(c)) {
                return macAddrNfcString.substring(i, macAddrNfcString.length());
            }
        }
        return macAddrNfcString;
    }

    private void successfulNfcAuth() {
        Toast.makeText(this, "NFC authenticated!", Toast.LENGTH_SHORT).show();
        mTextViewResult.setText("NFC authenticated: YES");
        new CountDownTimer(NFC_RESET_TIME, NFC_COUNTDOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                mTextViewResult.setText("NFC authenticated: NO");
            }

        }.start();
    }

    private void changeLockState(Boolean lockState) {
        if(lockState) {
            mTextViewDoorState.setText(getString(R.string.door_state) + " LOCKED");
        } else {
            mTextViewDoorState.setText(getString(R.string.door_state) + " OPEN");
        }
    }

    private void refreshState () {
        String url = getString(R.string.smart_latch_url) + "/getLockState?doorId=" + doorID;
        OkHttpClient client = (OkHttpClient) new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();
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
                    locked = jObj.getBoolean("locked");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ThisDoorActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeLockState(locked);
                    }
                });
            }
        });

    }
}
