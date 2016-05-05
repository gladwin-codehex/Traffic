package in.codehex.traffic;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;
import in.codehex.traffic.app.ItemClickListener;
import in.codehex.traffic.model.TrafficItem;
import in.codehex.traffic.model.VehicleItem;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    Toolbar mToolbar;
    RecyclerView mRecyclerView;
    MapRecyclerViewAdapter mAdapter;
    List<VehicleItem> mVehicleItemList;
    GoogleApiClient mGoogleApiClient;
    Location location;
    GoogleMap mMap;
    LocationRequest mLocationRequest;
    double mLat, mLng;
    List<TrafficItem> trafficItemList;
    ProgressDialog mProgressDialog;
    LinearLayoutManager mLinearLayoutManager;
    Intent mIntent;
    String mDirection, mSource, mDestination, mPhone;
    int mPosition, mStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initObjects();
        prepareObjects();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_CODE);
        }
        drawOnMap();
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
        mRecyclerView = (RecyclerView) findViewById(R.id.traffic_list);

        mProgressDialog = new ProgressDialog(this);
        mDirection = getIntent().getStringExtra("direction");
        mPosition = getIntent().getIntExtra("position", 0);
        mSource = getIntent().getStringExtra("source");
        mDestination = getIntent().getStringExtra("destination");
        mPhone = getIntent().getStringExtra("phone");
        mStep = getIntent().getIntExtra("step", 0);
        trafficItemList = new ArrayList<>();
        mVehicleItemList = new ArrayList<>();
        mLinearLayoutManager = new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        mAdapter = new MapRecyclerViewAdapter(this, trafficItemList);
    }

    /**
     * Implement and manipulate the objects.
     */
    void prepareObjects() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mProgressDialog.setMessage("Loading..");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        getUsersLocation();

        if (checkPlayServices())
            buildGoogleApiClient();

        createLocationRequest();

        if (!isGPSEnabled(getApplicationContext()))
            showAlertGPS();
    }

    /**
     * Draw poly lines and markers on the map
     */
    private void drawOnMap() {
        try {
            JSONObject jsonObject = new JSONObject(mDirection);
            JSONArray jsonArray = jsonObject.getJSONArray("routes");
            JSONObject route = jsonArray.getJSONObject(mPosition);
            JSONObject polyline = route.getJSONObject("overview_polyline");
            String points = polyline.getString("points");
            List<LatLng> decodedPath = PolyUtil.decode(points);
            mMap.addPolyline(new PolylineOptions().color(ContextCompat
                    .getColor(getApplicationContext(), R.color.accent)).addAll(decodedPath));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Geocoder geocoder = new Geocoder(this);
        try {
            double sourceLat, sourceLng, destLat, destLng;
            List<Address> sourceAddress = geocoder.getFromLocationName(mSource, 1);
            List<Address> destinationAddress = geocoder.getFromLocationName(mDestination, 1);
            Address sAddress = sourceAddress.get(0);
            sourceLat = sAddress.getLatitude();
            sourceLng = sAddress.getLongitude();
            Address dAddress = destinationAddress.get(0);
            destLat = dAddress.getLatitude();
            destLng = dAddress.getLongitude();
            LatLng sourcePos = new LatLng(sourceLat, sourceLng);
            LatLng destinationPos = new LatLng(destLat, destLng);
            mMap.addMarker(new MarkerOptions().position(sourcePos).title(mSource));
            mMap.addMarker(new MarkerOptions().position(destinationPos).title(mDestination));
        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println(error.getMessage());
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
     * Get the location of all the users from the database.
     */
    void getUsersLocation() {
        showProgressDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.URL_API, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                processList(response, mPosition);
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
     * Process the routes with users location and add the traffic data item to the list.
     */
    void processList(String data, int pos) {
        try {
            JSONObject direction = new JSONObject(mDirection);
            JSONArray array = direction.getJSONArray("routes");
            JSONObject routes = array.getJSONObject(pos);
            JSONArray legs = routes.getJSONArray("legs");
            JSONObject object = legs.getJSONObject(0);
            JSONArray steps = object.getJSONArray("steps");
            trafficItemList.add(new TrafficItem("Path", "Bike", "Car", "Truck", "Speed"));
            mAdapter.notifyDataSetChanged();
            int segment = 0;

            for (int i = 0; i < mStep; i++) {
                JSONObject polyObject = steps.getJSONObject(i);
                JSONObject polyline = polyObject.getJSONObject("polyline");
                String point = polyline.getString("points");
                List<LatLng> decodedPath = PolyUtil.decode(point);
                int bikeCount = 0, carCount = 0, truckCount = 0;
                List<Double> speedList = new ArrayList<>();

                JSONArray jsonArray = new JSONArray(data);
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(j);
                    double lat = jsonObject.getDouble("lat");
                    double lng = jsonObject.getDouble("lng");
                    LatLng latLng = new LatLng(lat, lng);

                    if (PolyUtil.isLocationOnPath(latLng, decodedPath, true, 100)) {
                        speedList.add(jsonObject.getDouble("speed"));
                        switch (jsonObject.getString("vehicle")) {
                            case "Bike":
                                bikeCount++;
                                break;
                            case "Car":
                                carCount++;
                                break;
                            case "Truck":
                                truckCount++;
                                break;
                        }
                    }
                }

                Double speed = 0.00, avgSpeed = 0.00;
                String mSpeed;
                for (Double d : speedList)
                    speed += d;
                if (!speedList.isEmpty())
                    avgSpeed = speed / speedList.size();

                NumberFormat formatter = new DecimalFormat("#0.00");

                if (avgSpeed == 0.0)
                    mSpeed = "-";
                else mSpeed = formatter.format(avgSpeed);

                trafficItemList.add(new TrafficItem(String.valueOf(++segment),
                        String.valueOf(bikeCount), String.valueOf(carCount),
                        String.valueOf(truckCount), mSpeed));
                mAdapter.notifyDataSetChanged();
            }
            if (trafficItemList.size() < 2)
                Toast.makeText(getApplicationContext(),
                        "No traffic data available for the given distance",
                        Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * An view holder for the traffic data item.
     */
    private class MapRecyclerViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView textSegment, textBike, textCar, textTruck, textSpeed;
        private ItemClickListener itemClickListener;

        public MapRecyclerViewHolder(View view) {
            super(view);
            textSegment = (TextView) view.findViewById(R.id.segment);
            textBike = (TextView) view.findViewById(R.id.bike);
            textCar = (TextView) view.findViewById(R.id.car);
            textTruck = (TextView) view.findViewById(R.id.truck);
            textSpeed = (TextView) view.findViewById(R.id.speed);
            view.setOnClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onClick(view, getAdapterPosition(), false);
        }
    }

    /**
     * An adapter for the traffic data recycler view.
     */
    public class MapRecyclerViewAdapter extends RecyclerView.Adapter<MapRecyclerViewHolder> {
        private List<TrafficItem> trafficItemList;
        private Context context;

        public MapRecyclerViewAdapter(Context context, List<TrafficItem> trafficItemList) {
            this.trafficItemList = trafficItemList;
            this.context = context;
        }

        @Override
        public MapRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_traffic, parent, false);
            return new MapRecyclerViewHolder(layoutView);
        }

        @Override
        public void onBindViewHolder(MapRecyclerViewHolder holder, int position) {
            TrafficItem trafficItem = trafficItemList.get(position);
            holder.textSegment.setText(trafficItem.getSegment());
            holder.textBike.setText(trafficItem.getBike());
            holder.textCar.setText(trafficItem.getCar());
            holder.textTruck.setText(trafficItem.getTruck());
            holder.textSpeed.setText(trafficItem.getSpeed());
            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    if (position != 0)
                        try {
                            JSONObject direction = new JSONObject(mDirection);
                            JSONArray array = direction.getJSONArray("routes");
                            JSONObject routes = array.getJSONObject(mPosition);
                            JSONArray legs = routes.getJSONArray("legs");
                            JSONObject object = legs.getJSONObject(0);
                            JSONArray steps = object.getJSONArray("steps");
                            JSONObject polyObject = steps.getJSONObject(position - 1);
                            JSONObject polyline = polyObject.getJSONObject("polyline");
                            String point = polyline.getString("points");
                            List<LatLng> decodedPath = PolyUtil.decode(point);
                            mMap.clear();
                            drawOnMap();
                            mMap.addPolyline(new PolylineOptions().color(ContextCompat
                                    .getColor(getApplicationContext(), R.color.blue))
                                    .addAll(decodedPath));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.trafficItemList.size();
        }
    }
}
