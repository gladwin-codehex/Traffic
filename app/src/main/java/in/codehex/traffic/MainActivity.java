package in.codehex.traffic;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;

public class MainActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    Toolbar mToolbar;
    FloatingActionButton buttonSubmit;
    AutoCompleteTextView autoSource, autoDestination;
    EditText editTraffic, editDistance;
    Intent mIntent;
    GoogleApiClient mGoogleApiClient;
    Location location;
    LocationRequest mLocationRequest;
    ArrayList<String> places;
    ArrayAdapter<String> mAdapter;
    SharedPreferences userPreferences;
    double mLat, mLng;
    String mPhone, mSource, mDestination, mTraffic, mDistance, mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initObjects();
        prepareObjects();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null)
            if (mGoogleApiClient.isConnected())
                mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null)
            if (mGoogleApiClient.isConnected())
                stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null)
            if (mGoogleApiClient.isConnected())
                startLocationUpdates();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location loc) {
        location = loc;
        mLat = location.getLatitude();
        mLng = location.getLongitude();
        updateLocation();
    }

    /**
     * Initialize the objects.
     */
    void initObjects() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        autoSource = (AutoCompleteTextView) findViewById(R.id.auto_source);
        autoDestination = (AutoCompleteTextView) findViewById(R.id.auto_destination);
        editTraffic = (EditText) findViewById(R.id.edit_traffic);
        editDistance = (EditText) findViewById(R.id.edit_distance);
        buttonSubmit = (FloatingActionButton) findViewById(R.id.button_submit);

        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
    }

    /**
     * Implement and manipulate the objects.
     */
    void prepareObjects() {
        if (checkLogin()) {
            setSupportActionBar(mToolbar);
            autoSource.setThreshold(0);
            autoDestination.setThreshold(0);

            autoSource.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    places = new ArrayList<>();
                    updateList(s.toString(), "source");
                }
            });

            autoDestination.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    places = new ArrayList<>();
                    updateList(s.toString(), "destination");
                }
            });

            buttonSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSource = autoSource.getText().toString();
                    mDestination = autoDestination.getText().toString();
                    mTraffic = editTraffic.getText().toString();
                    mDistance = editDistance.getText().toString();
                    if (mSource.length() > 2 && mDestination.length() > 2
                            && !mTraffic.isEmpty() && !mDistance.isEmpty()) {
                        mIntent = new Intent(getApplicationContext(), RouteActivity.class);
                        mIntent.putExtra("source", mSource);
                        mIntent.putExtra("destination", mDestination);
                        mIntent.putExtra("traffic", mTraffic);
                        mIntent.putExtra("distance", mDistance);
                        startActivity(mIntent);
                    }
                }
            });

            if (checkPlayServices())
                buildGoogleApiClient();

            createLocationRequest();

            if (!isGPSEnabled(getApplicationContext()))
                showAlertGPS();
        }
    }

    /**
     * Update the current location of the user.
     */
    void updateLocation() {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.URL_API, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                // parsing json response data
                try {
                    JSONObject jObj = new JSONObject(response);
                    // int error = jObj.getInt("error");
                    String message = jObj.getString("message");

                   /* Toast.makeText(getApplicationContext(),
                            message, Toast.LENGTH_SHORT).show();*/
                    System.out.println(message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),
                        "Network error - " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<>();
                params.put("tag", "gps");
                params.put("phone", mPhone);
                params.put("lat", String.valueOf(mLat));
                params.put("lng", String.valueOf(mLng));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, "update_location");
    }

    /**
     * Get the suggestion list for the typed location data.
     *
     * @param place the entered place data
     * @param loc   source or destination tag
     */
    void updateList(String place, final String loc) {
        String input = "";
        try {
            input = "input=" + URLEncoder.encode(place, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        final String output = "json";
        String parameter = input + "&types=geocode&sensor=true&key=" + Config.API_BROWSER_KEY;
        mUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/"
                + output + "?" + parameter;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, mUrl,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray array = response.getJSONArray(Config.TAG_RESULT);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String description = object.getString("description");
                        places.add(description);
                    }

                    mAdapter = new ArrayAdapter<String>(getApplicationContext(),
                            android.R.layout.simple_list_item_1, places) {
                        @Override
                        public View getView(int position,
                                            View convertView, ViewGroup parent) {
                            View view = super.getView(position,
                                    convertView, parent);
                            TextView text = (TextView) view
                                    .findViewById(android.R.id.text1);
                            text.setTextColor(Color.BLACK);
                            return view;
                        }
                    };
                    if (loc.equals("source")) {
                        autoSource.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                    } else if (loc.equals("destination")) {
                        autoDestination.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjectRequest, "places");
    }

    /**
     * Check whether the user has logged in.
     *
     * @return true if logged in else false
     */
    boolean checkLogin() {
        if (userPreferences.contains(Config.PREF_USER_PHONE)) {
            mPhone = userPreferences.getString(Config.PREF_USER_PHONE, null);
            return true;
        } else {
            mIntent = new Intent(MainActivity.this, LoginActivity.class);
            mIntent.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mIntent);
            return false;
        }
    }

    /**
     * Initializes and implements location request object.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Config.INTERVAL);
        mLocationRequest.setFastestInterval(Config.FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Initializes the google api client.
     */
    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Checks for the availability of google play services functionality.
     *
     * @return true if play services is enabled else false
     */
    boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        Config.REQUEST_PLAY_SERVICES).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * display an alert to notify the user that GPS has to be enabled
     */
    void showAlertGPS() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Enable GPS");
        alertDialog.setMessage("GPS service is not enabled." +
                " Do you want to go to location settings?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(mIntent);
            }
        });
        alertDialog.show();
    }

    /**
     * @param context context of the MainActivity class
     * @return true if GPS is enabled else false
     */
    boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Start receiving location updates.
     */
    void startLocationUpdates() {
        // marshmallow runtime location permission
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Stop receiving location updates.
     */
    void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
}
