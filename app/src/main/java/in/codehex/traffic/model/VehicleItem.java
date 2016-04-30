package in.codehex.traffic.model;

/**
 * Created by codehex-gladwin on 30-04-2016
 */
public class VehicleItem {

    private double lat, lng;

    public VehicleItem(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
