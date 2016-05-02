package in.codehex.traffic;

import com.google.android.gms.maps.CameraUpdateFactory;
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
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;
import in.codehex.traffic.model.TrafficItem;
import in.codehex.traffic.model.VehicleItem;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    Toolbar mToolbar;
    RecyclerView mRecyclerView;
    MapRecyclerViewAdapter mAdapter;
    List<VehicleItem> mVehicleItemList;
    List<TrafficItem> trafficItemList;
    ProgressDialog mProgressDialog;
    LinearLayoutManager mLinearLayoutManager;
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
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_CODE);
        }
        try {
            JSONObject jsonObject = new JSONObject(mDirection);
            JSONArray jsonArray = jsonObject.getJSONArray("routes");
            JSONObject route = jsonArray.getJSONObject(mPosition);
            JSONObject polyline = route.getJSONObject("overview_polyline");
            String points = polyline.getString("points");
            List<LatLng> decodedPath = PolyUtil.decode(points);
            map.addPolyline(new PolylineOptions().color(ContextCompat
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
            map.addMarker(new MarkerOptions().position(sourcePos).title(mSource));
            map.addMarker(new MarkerOptions().position(destinationPos).title(mDestination));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(sourcePos, 11));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                Toast.makeText(getApplicationContext(),
                        "Network error - " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            trafficItemList.add(new TrafficItem("Segment", "Bike", "Car", "Truck", "Speed"));
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
                    LatLng latLng = new LatLng(jsonObject.getDouble("lat"),
                            jsonObject.getDouble("lng"));

                    if (PolyUtil.isLocationOnPath(latLng, decodedPath, true, 10)) {
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

                if (avgSpeed == 0.0)
                    mSpeed = "-";
                else mSpeed = String.valueOf(avgSpeed);

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
    private class MapRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView textSegment, textBike, textCar, textTruck, textSpeed;

        public MapRecyclerViewHolder(View view) {
            super(view);
            textSegment = (TextView) view.findViewById(R.id.segment);
            textBike = (TextView) view.findViewById(R.id.bike);
            textCar = (TextView) view.findViewById(R.id.car);
            textTruck = (TextView) view.findViewById(R.id.truck);
            textSpeed = (TextView) view.findViewById(R.id.speed);
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
        }

        @Override
        public int getItemCount() {
            return this.trafficItemList.size();
        }
    }
}
