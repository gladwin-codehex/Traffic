package in.codehex.traffic.model;

/**
 * Created by codehex-gladwin on 30-04-2016
 */
public class TrafficItem {

    private String segment, bike, car, truck, speed;

    public TrafficItem(String segment, String bike, String car, String truck, String speed) {
        this.segment = segment;
        this.bike = bike;
        this.car = car;
        this.truck = truck;
        this.speed = speed;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getBike() {
        return bike;
    }

    public void setBike(String bike) {
        this.bike = bike;
    }

    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public String getTruck() {
        return truck;
    }

    public void setTruck(String truck) {
        this.truck = truck;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }
}
