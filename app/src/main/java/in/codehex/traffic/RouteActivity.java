package in.codehex.traffic;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;

public class RouteActivity extends AppCompatActivity {

    Toolbar mToolbar;
    Spinner spinRoute;
    FloatingActionButton buttonSubmit;
    SharedPreferences userPreferences;
    ArrayAdapter<String> spinnerAdapter;
    ProgressDialog mProgressDialog;
    Intent mIntent;
    List<String> routeList;
    String[] mPoint = new String[100];
    int[] mWeight = new int[100];
    int mTraffic, mPosition, mDistance;
    String mSource, mDestination, mPhone, mDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        initObjects();
        prepareObjects();
    }

    /**
     * Initialize the objects.
     */
    void initObjects() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        spinRoute = (Spinner) findViewById(R.id.spin_route);
        buttonSubmit = (FloatingActionButton) findViewById(R.id.button_submit);

        mProgressDialog = new ProgressDialog(this);
        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
        routeList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList);
    }

    /**
     * Implement and manipulate the objects.
     */
    void prepareObjects() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mProgressDialog.setMessage("Loading..");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinRoute.setAdapter(spinnerAdapter);

        mPhone = userPreferences.getString(Config.PREF_USER_PHONE, null);
        mSource = getIntent().getStringExtra("source");
        mDestination = getIntent().getStringExtra("destination");
        mTraffic = Integer.parseInt(getIntent().getStringExtra("traffic"));
        mDistance = Integer.parseInt(getIntent().getStringExtra("distance"));
    }

    /**
     * Get the directions list from the google maps api.
     */
    void getDirections() {
        showProgressDialog();
        String url = null;
        try {
            url = Config.URL_API_MAP + "origin=" + URLEncoder.encode(mSource, "utf-8")
                    + "&destination=" + URLEncoder.encode(mDestination, "utf-8")
                    + "&alternatives=true&key=" + Config.API_BROWSER_KEY;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        mDirection = response;
                        processRoute();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                error.printStackTrace();
                getDirections();
            }
        });
        AppController.getInstance().addToRequestQueue(stringRequest, "direction");
    }

    /**
     * Process the directions and select the best route and organize the other routes.
     */
    void processRoute() {

    }

    /**
     * Display progress bar if it is not being shown.
     */
    void showProgressDialog() {
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    /**
     * Hide progress bar if it being displayed.
     */
    void hideProgressDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
