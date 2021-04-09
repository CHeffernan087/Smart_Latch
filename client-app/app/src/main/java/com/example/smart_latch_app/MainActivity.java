package com.example.smart_latch_app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.CountDownTimer;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
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


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private GoogleSignInClient mGoogleSignInClient;
    private MainFragment mainFragment = new MainFragment();
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FragmentTransaction fragmentTransaction;
    private IntentFilter[] writeTagFilters;
    String responseString = "";
    String responseMessage = "";
    JSONObject jObj = null;
    JSONArray responseDoors;
    JSONObject responseDetails;

    //NFC setup
    NfcAdapter nfcAdapter = null;
    PendingIntent pendingIntent = null;
    private String macAddress = null;

    public static Context contextOfApplication;
    public static MainActivity instance = null;

    @Override
    protected void onResume () {
        super.onResume();
        instance = this;
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);

    }

    protected void onPause() {
        super.onPause();
        instance = null;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        contextOfApplication = getApplicationContext();


        // Setup Google stuff for signing out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestProfile()
                .requestEmail().build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer,mainFragment);
        fragmentTransaction.commit(); // add the home fragment

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.sign_out:
                signOut();

            case R.id.action_settings:

            default:

        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            gotoMainFragment();
            Toast.makeText(MainActivity.this, "Home", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_doors) {
            // here we put in the doors retrieved from the endpoint /getUserDoors
            getUserDoorsAndNavToMyDoors();
            Toast.makeText(MainActivity.this, "View available doors", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_manual) {
            gotoCameraActivity();
            Toast.makeText(MainActivity.this, "Upload a selfie for facial recognition.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_share) {
            gotoMainFragment();
            Toast.makeText(MainActivity.this, "Just for show", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void gotoLoginActivity() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void gotoMyDoorsActivity(String[] doors, JSONObject doorDetails) {
        Intent i = new Intent(MainActivity.this, MyDoorsActivity.class);
        i.putExtra("DOORS", doors);
        i.putExtra("DETAILS", doorDetails.toString());

        startActivity(i);
        finish();
    }

    private void gotoCameraActivity() {
        Intent i = new Intent(MainActivity.this, Selfie.class);

        startActivity(i);
        finish();
    }

    private void signOut () {
        OkHttpClient logoutClient = new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String refreshToken = prefs.getString("refreshToken", "defaultToken");
        String email = prefs.getString("email", "defaultToken");

        String hostName = this.getString(R.string.smart_latch_url);
        String url = hostName + "/logout?refreshToken=" + refreshToken + "&email=" + email;

        RequestBody formBody = new FormBody.Builder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        logoutClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                    responseString = response.body().string();

                    try {
                        jObj = new JSONObject(responseString);
                        String message = jObj.getString("message");
                        System.out.println("Logout message " + message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "Signed out.", Toast.LENGTH_SHORT).show();
                        gotoLoginActivity();
                    }
                });
    }

    private void gotoMainFragment() {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer,mainFragment);
        fragmentTransaction.commit();// replace the fragment
    }

    private void getUserDoorsAndNavToMyDoors () {
        String email ="";
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            email = acct.getEmail();
        }

        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .addInterceptor(new AuthenticationInterceptor())
                .build();

        String hostUrl = getString(R.string.smart_latch_url) + "/getUserDoors?email=" + email;

        RequestBody formBody = new FormBody.Builder()
                .build();
        Request request = new Request.Builder()
                .url(hostUrl)
                .post(formBody)
                .build();

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
                    responseDoors = jObj.getJSONArray("doors");
                    responseDetails = jObj.getJSONObject("doorDetails");

                    String[] userDoorsAsStringArray = toStringArray(responseDoors);
                    gotoMyDoorsActivity(userDoorsAsStringArray, responseDetails);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public static String[] toStringArray(JSONArray array) {
        if(array==null)
            return null;

        String[] arr=new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }

    public static Context getContextOfApplication(){
        return contextOfApplication;
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

    private String getMacAddr(String macAddrNfcString) {
        for (int i = 0; i < macAddrNfcString.length(); i++) {
            char c = macAddrNfcString.charAt(i);
            if (Character.isDigit(c)) {
                return macAddrNfcString.substring(i, macAddrNfcString.length());
            }
        }
        return macAddrNfcString;
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
                    MainActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "NFC authenticated!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    MainActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "NFC failed to authenticate.", Toast.LENGTH_SHORT).show();

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
}