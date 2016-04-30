package in.codehex.traffic.model;

/**
 * Created by codehex-gladwin on 30-04-2016
 */
public class VehicleItem {

    private double lat, lng;
    private int weight;

    public VehicleItem(double lat, double lng, int weight) {
        this.lat = lat;
        this.lng = lng;
        this.weight = weight;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
