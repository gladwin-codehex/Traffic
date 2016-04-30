package in.codehex.traffic.model;

/**
 * Created by codehex-gladwin on 30-04-2016
 */
public class RouteItem {

    private String route;
    private int point;

    public RouteItem(String route, int point) {
        this.route = route;
        this.point = point;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }
}
