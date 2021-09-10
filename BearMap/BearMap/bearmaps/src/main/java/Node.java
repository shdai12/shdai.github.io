public class Node {
    Long id;
    double lon;
    double lat;
    double projectx;
    double projecty;


    Node(Long id, double lon, double lat) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
        this.projectx = GraphDB.projectToX(lon, lat);
        this.projecty = GraphDB.projectToY(lon, lat);
    }

}
