package in.codehex.traffic;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;
import in.codehex.traffic.model.StepItem;
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
    List<VehicleItem> mVehicleItemList;
    List<StepItem> mStepItemList;
    List<Integer> mWeight, mRoute, mIndexes;
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
        mVehicleItemList = new ArrayList<>();
        mStepItemList = new ArrayList<>();
        mWeight = new ArrayList<>();
        mRoute = new ArrayList<>();
        mIndexes = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                mRouteList);
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
                int mPos = spinRoute.getSelectedItemPosition();
                mPosition = mIndexes.get(mPos);
                mIntent = new Intent(RouteActivity.this, MapActivity.class);
                mIntent.putExtra("position", mPosition);
                mIntent.putExtra("direction", mDirection);
                mIntent.putExtra("source", mSource);
                mIntent.putExtra("destination", mDestination);
                mIntent.putExtra("phone", mPhone);
                mIntent.putExtra("step", mStepItemList.get(mPosition).getStep());
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
            System.out.println(url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        mDirection = response;
                        getUsersLocation();
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
     * Get the location of all the users from the database.
     */
    void getUsersLocation() {
        showProgressDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.URL_API, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        double lat, lng;
                        int weight;
                        lat = object.getDouble("lat");
                        lng = object.getDouble("lng");
                        weight = object.getInt("weight");
                        mVehicleItemList.add(new VehicleItem(lat, lng, weight));
                    }
                    processSteps();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                getUsersLocation();
                System.out.println(error.getMessage());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tag", "traffic");
                params.put("phone", mPhone);

                return params;
            }

        };

        AppController.getInstance().addToRequestQueue(strReq, "routes");
    }

    /**
     * Process the steps obtained in the google maps api.
     */
    void processSteps() {
        try {
            JSONObject jsonObject = new JSONObject(mDirection);
            JSONArray routes = jsonObject.getJSONArray("routes");
            for (int i = 0; i < routes.length(); i++) {
                JSONObject object = routes.getJSONObject(i);
                JSONArray legs = object.getJSONArray("legs");
                JSONObject legObject = legs.getJSONObject(0);
                JSONArray steps = legObject.getJSONArray("steps");
                int temp = 0;
                boolean isDistance = true;
                for (int j = 0; j < steps.length(); j++) {
                    if (isDistance && (j + 1) == steps.length()) {
                        mStepItemList.add(new StepItem(i, j));
                        isDistance = false;
                    } else if (isDistance) {
                        JSONObject step = steps.getJSONObject(j);
                        JSONObject distance = step.getJSONObject("distance");
                        int value = distance.getInt("value");
                        temp += value;
                        if (temp > mDistance) {
                            mStepItemList.add(new StepItem(i, j));
                            isDistance = false;
                        }
                    }
                }
            }
            processTraffic();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the traffic intensity for the given distance in all the routes.
     */
    void processTraffic() {
        for (int i = 0; i < mStepItemList.size(); i++) {
            int temp = 0;
            for (int j = 0; j < mStepItemList.get(i).getStep(); j++) {
                for (int k = 0; k < mVehicleItemList.size(); k++) {
                    double lat = mVehicleItemList.get(k).getLat();
                    double lng = mVehicleItemList.get(k).getLng();
                    int weight = mVehicleItemList.get(k).getWeight();
                    LatLng latLng = new LatLng(lat, lng);
                    try {
                        JSONObject jsonObject = new JSONObject(mDirection);
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        JSONObject object = routes.getJSONObject(i);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObject = legs.getJSONObject(0);
                        JSONArray steps = legObject.getJSONArray("steps");
                        JSONObject polylineObject = steps.getJSONObject(j);
                        JSONObject polyline = polylineObject.getJSONObject("polyline");
                        String points = polyline.getString("points");
                        List<LatLng> decodedPath = PolyUtil.decode(points);
                        if (PolyUtil.isLocationOnPath(latLng, decodedPath, true, 20))
                            temp += weight;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            mWeight.add(i, temp);
        }
        processRoutes();
    }

    /**
     * Determine the best routes and arrange them in the ascending order and then
     * add them to the spinner.
     */
    void processRoutes() {
        mRoute.addAll(mWeight);
        Collections.sort(mRoute);
        for (int i = 0; i < mStepItemList.size(); i++) {
            if (i == 0)
                spinnerAdapter.add("Route " + (i + 1) + " (Best Route)");
            else spinnerAdapter.add("Route " + (i + 1));
        }
        for (int number : mWeight) {
            for (int i = 0; i < mRoute.size(); i++) {
                int sortedNumber = mRoute.get(i);
                if (number == sortedNumber) {
                    if (!mIndexes.contains(i)) {
                        mIndexes.add(i);
                        break;
                    }
                }
            }
        }
        spinnerAdapter.notifyDataSetChanged();
        hideProgressDialog();
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
