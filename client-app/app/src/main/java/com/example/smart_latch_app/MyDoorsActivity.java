package com.example.smart_latch_app;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MyDoorsActivity extends AppCompatActivity implements Listener{

    public static final String TAG = MainActivity.class.getSimpleName();

    private EditText mEtMessage;
    private Button mBtScan;
    private AddDoorFragment mAddDoorFragment;
    private boolean isDialogDisplayed = false;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_doors);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

        initViews();
        initNFC();

        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMainActivity();
            }
        });
    }

    private void gotoMainActivity() {
        startActivity(new Intent(MyDoorsActivity.this, MainActivity.class));
        finish();
    }


    private void initViews() {
        mBtScan = (Button) findViewById(R.id.btn_read);
        mBtScan.setOnClickListener(view -> showAddDoorFragment());
    }

    private void initNFC(){
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }


    private void showAddDoorFragment() {

        mAddDoorFragment = (AddDoorFragment) getSupportFragmentManager().findFragmentByTag(AddDoorFragment.TAG);
        if (mAddDoorFragment == null) {
            mAddDoorFragment = AddDoorFragment.newInstance();
        }
        mAddDoorFragment.show(getSupportFragmentManager(),AddDoorFragment.TAG);
    }

    @Override
    public void onDialogDisplayed() {
        isDialogDisplayed = true;
    }

    @Override
    public void onDialogDismissed() {
        isDialogDisplayed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if(mNfcAdapter!= null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

        @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String action = intent.getAction();
        Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        assert tag != null;
        detectTagData(tag);

    }

    //For NFC detection handling
    private void detectTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append(toHex(id));

        if (isDialogDisplayed) {
                Toast.makeText(this, "NFC Tag Detected !", Toast.LENGTH_SHORT).show();
                mAddDoorFragment = (AddDoorFragment) getSupportFragmentManager().findFragmentByTag(AddDoorFragment.TAG);
                mAddDoorFragment.onNfcDetected(sb.toString());
        }
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