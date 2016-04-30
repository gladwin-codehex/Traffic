package in.codehex.traffic.app;

/**
 * Created by codehex-gladwin on 30-04-2016
 */
public class Config {
    public static final String URL_API = "http://traffic.zordec.com/";
    public static final String URL_API_MAP = "https://maps.googleapis.com/" +
            "maps/api/directions/json?";

    public static final String API_BROWSER_KEY = "AIzaSyAdz0K5C3qbDBS53EyfxIXRtqeleVMPOXg";

    public static final String PREF_USER = "user";
    public static final String PREF_USER_PHONE = "phone";
    public static final String PREF_USER_VEHICLE = "vehicle";
    public static final String PREF_USER_WEIGHT = "weight";

    public static final int REQUEST_PLAY_SERVICES = 4;

    public static final int PERMISSION_REQUEST_CODE = 1;
    public static final String TAG_RESULT = "predictions";
    public static final long INTERVAL = 1000 * 10;
    public static final long FASTEST_INTERVAL = 1000 * 5;
}
