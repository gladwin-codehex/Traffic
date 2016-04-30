package in.codehex.traffic;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;
import in.codehex.traffic.model.RouteItem;
import in.codehex.traffic.model.VehicleItem;

public class RouteActivity extends AppCompatActivity {

    Toolbar mToolbar;
    Spinner spinRoute;
    FloatingActionButton buttonSubmit;
    SharedPreferences userPreferences;
    ArrayAdapter<String> spinnerAdapter;
    ProgressDialog mProgressDialog;
    Intent mIntent;
    List<String> mRouteList;
    List<RouteItem> mRouteItemList;
    List<VehicleItem> mVehicleItemList;
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
        mRouteList = new ArrayList<>();
        mRouteItemList = new ArrayList<>();
        mVehicleItemList = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mRouteList);
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

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIntent = new Intent(RouteActivity.this, MapActivity.class);
                startActivity(mIntent);
            }
        });
        getDirections();
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
        showProgressDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.URL_API, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        double lat, lng;
                        lat = object.getDouble("lat");
                        lng = object.getDouble("lng");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                processRoute();
                Toast.makeText(getApplicationContext(),
                        "Network error - " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "traffic");
                params.put("phone", mPhone);

                return params;
            }

        };

        AppController.getInstance().addToRequestQueue(strReq, "routes");
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
     * Hide progress bar if it is being displayed.
     */
    void hideProgressDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
