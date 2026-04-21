package labs.franklee.celero.engine;

import java.util.ArrayList;
import java.util.List;

public class Report {

    private List<Route> routes = new ArrayList<>();

    void append(Route route) {
        this.routes.add(route);
    }

    public List<Route> getRoutes() {
        return routes;
    }
}
