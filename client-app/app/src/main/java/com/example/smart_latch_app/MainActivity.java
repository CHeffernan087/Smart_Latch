package com.example.smart_latch_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
    private String welcomeText;
    private MainFragment mainFragment = new MainFragment();
    private FirstFragment firstFragment = new FirstFragment();
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private FragmentTransaction fragmentTransaction;
    String responseString = "";
    String responseMessage = "";
    JSONObject jObj = null;
    JSONArray responseDoors;

    public static Context contextOfApplication;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        welcomeText = "Welcome, " + getIntent().getStringExtra("USER_NAME");
        Toast.makeText(this, welcomeText, Toast.LENGTH_SHORT).show();

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
            gotoFirstFragment();
            Toast.makeText(MainActivity.this, "We can add buttons here to operate without NFC", Toast.LENGTH_SHORT).show();
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

    private void gotoMyDoorsActivity(String[] doors) {
        Intent i = new Intent(MainActivity.this, MyDoorsActivity.class);
        i.putExtra("DOORS", doors);
        startActivity(i);
        finish();
    }

    private void signOut () {
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

    private void gotoFirstFragment() {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer,firstFragment);
        fragmentTransaction.commit();// replace the fragment
    }

    private void getUserDoorsAndNavToMyDoors () {
        String email ="";
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            email = acct.getEmail();
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
            .addInterceptor(new AuthenticationInterceptor()).build();

        String hostUrl = getString(R.string.smart_latch_url) + "/testAuthMiddleware";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token = prefs.getString("token", "defaultToken");

        RequestBody formBody = new FormBody.Builder()
                .build();
        Request request = new Request.Builder()
                .url(hostUrl)
                .post(formBody)
                .build();
        System.out.println("1. MAKING THIS INITIAL REQUEST: " + request.toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.body().string();
                System.out.println("1. RESPONSE ORIGINAL: " + responseString);
                System.out.println("1. STATUS: " + response.code());
                try {
                    jObj = new JSONObject(responseString);
                    responseMessage = jObj.getString("message");
                    responseDoors = jObj.getJSONArray("doors");
                    String[] userDoorsAsStringArray = toStringArray(responseDoors);
                    gotoMyDoorsActivity(userDoorsAsStringArray);
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

}