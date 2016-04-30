package in.codehex.traffic;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import in.codehex.traffic.app.AppController;
import in.codehex.traffic.app.Config;

public class LoginActivity extends AppCompatActivity {

    Toolbar mToolbar;
    EditText editPhone;
    Spinner spinVehicle;
    FloatingActionButton fabLogin;
    SharedPreferences userPreferences;
    Intent mIntent;
    CoordinatorLayout mCoordinatorLayout;
    ProgressDialog mProgressDialog;
    String mPhone, mVehicle;
    int mWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initObjects();
        prepareObjects();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Config.PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(mCoordinatorLayout, "Location access permission granted",
                            Snackbar.LENGTH_SHORT).show();
                }
        }
    }

    /**
     * Initialize the objects.
     */
    void initObjects() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_coordinator);
        editPhone = (EditText) findViewById(R.id.edit_phone);
        spinVehicle = (Spinner) findViewById(R.id.spin_vehicle);
        fabLogin = (FloatingActionButton) findViewById(R.id.button_login);

        mProgressDialog = new ProgressDialog(this);
        userPreferences = getSharedPreferences(Config.PREF_USER, MODE_PRIVATE);
    }

    /**
     * Implement and manipulate the objects.
     */
    void prepareObjects() {
        setSupportActionBar(mToolbar);
        mProgressDialog.setMessage("Logging in..");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);

        fabLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhone = editPhone.getText().toString();
                mVehicle = spinVehicle.getSelectedItem().toString();
                switch (mVehicle) {
                    case "Bike":
                        mWeight = 1;
                        break;
                    case "Car":
                        mWeight = 2;
                        break;
                    case "Truck":
                        mWeight = 3;
                        break;
                    default:
                        mWeight = 0;
                        break;
                }
                if (mPhone.length() == 10) {
                    showProgressDialog();
                    processLogin();
                } else editPhone.setError(getResources().getString(R.string.error_phone));
            }
        });

        if (checkPermission())
            requestPermission();
    }

    /**
     * Check for runtime location access permission.
     *
     * @return true if permission available else false
     */
    boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request for permission if it is not granted.
     */
    void requestPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Send the login credentials to the server to authenticate the user.
     */
    void processLogin() {
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.URL_API, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                try {
                    JSONObject jObj = new JSONObject(response);
                    int error = jObj.getInt("error");
                    String message = jObj.getString("message");

                    Toast.makeText(getApplicationContext(),
                            message, Toast.LENGTH_SHORT).show();

                    if (error == 0) {
                        SharedPreferences.Editor editor = userPreferences.edit();
                        editor.putString(Config.PREF_USER_PHONE, mPhone);
                        editor.putString(Config.PREF_USER_VEHICLE, mVehicle);
                        editor.putInt(Config.PREF_USER_WEIGHT, mWeight);
                        editor.apply();

                        mIntent = new Intent(LoginActivity.this, MainActivity.class);
                        mIntent.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mIntent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(),
                        "Network error - " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tag", "login");
                params.put("phone", mPhone);
                params.put("vehicle", mVehicle);
                params.put("weight", String.valueOf(mWeight));

                return params;
            }

        };

        AppController.getInstance().addToRequestQueue(strReq, "login");
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
