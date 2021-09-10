import org.xml.sax.SAXException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;


/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {
    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing. Modify this constructor to initialize your data structures.
     *
     * @param dbPath Path to the XML file to be parsed.
     */
    private HashMap<Long, Node> idtonode;
    //    private ArrayList<LocationParams> locationParams;
    private HashMap<Long, LinkedList<Long>> neighbor;
    private LinkedList<Node> toRemove;
    private KDTree t;

    public GraphDB(String dbPath) {
//        this.locationParams = new ArrayList<>();
        this.neighbor = new HashMap<>();
        this.toRemove = new LinkedList<>();
        this.idtonode = new HashMap<>();

        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();

        this.t = new KDTree(idtonode.values());
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume
     * this since typically roads are connected.
     */
    private void clean() {
        for (long id : idtonode.keySet()) {
            Iterable<Long> a = adjacent(id);
            if (a == null) {
                toRemove.add(idtonode.get(id));
            }
        }
        for (Node n2 : toRemove) {
            removeNode(n2);
        }
    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */

    double lon(long v) {
        if (idtonode.containsKey(v)) {
            return idtonode.get(v).lon;
        }
        return 0.0;
    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        if (idtonode.containsKey(v)) {
            return idtonode.get(v).lat;
        }
        return 0.0;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     *
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {
        return idtonode.keySet();
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     *
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        if (!neighbor.containsKey(v)) {
            return null;
        }
        return neighbor.get(v);
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     *
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */

    public long closest(double lon, double lat) {
        double x1 = projectToX(lon, lat);
        double y1 = projectToY(lon, lat);
        return t.nearestneighbor(t.root, x1, y1).id;
    }

    private class KDTree {
        /* instance variable */
        private KDNode root;

        /* build a kdTree */
        public KDTree(Collection<Node> nodes) {
            ArrayList<Node> node = new ArrayList<>(nodes);
            root = build(node, true);
        }

        private class KDNode {
            /* left and right children of kdNode */
            private Node kdRoot;
            private KDNode left;
            private KDNode right;
            boolean vertical;

            private KDNode(Node n, boolean vertical) {
                this.kdRoot = n;
                left = null;
                right = null;
                this.vertical = vertical;
            }
        }

        private KDNode build(List<Node> nodes, boolean vertical) {
            if (nodes.isEmpty()) {
                return null;
            }
            if (vertical) {
                Collections.sort(nodes, (o1, o2) -> Double.compare(o1.projectx, o2.projectx));
            } else {
                Collections.sort(nodes, (o1, o2) -> Double.compare(o1.projecty, o2.projecty));
            }
            int a = nodes.size();
            int middle = a / 2;
            Node median = nodes.get(middle);
            KDNode toReturn = new KDNode(median, vertical);
            if (a == 1) {
                return toReturn;
            } else {
                vertical = !vertical;
                toReturn.left = build(nodes.subList(0, middle), vertical);
                toReturn.right = build(nodes.subList(middle + 1, a), vertical);
            }
            return toReturn;
        }

        private Node nearestneighbor(KDNode n, double x1, double y1) {
            if (n == null) {
                return null;
            }
            Node nearest0;
            Node nearest;
            Node nearest1;
            Node nearest3;
            double dist;
            double x2;
            double y2;
            double dist2;
            nearest = n.kdRoot;
            x2 = nearest.projectx;
            y2 = nearest.projecty;
            dist = euclidean(x1, x2, y1, y2);
            if ((n.vertical && x1 < x2) || (!n.vertical && y1 < y2)) {
                nearest1 = nearestneighbor(n.left, x1, y1);
                if (nearest1 != null) {
                    dist2 = euclidean(nearest1.projectx, x1, nearest1.projecty, y1);
                    if (dist2 < dist) {
                        nearest0 = nearest1;
                        dist = dist2;
                    } else {
                        nearest0 = nearest;
                    }
                } else {
                    nearest0 = nearest;
                }
                if (n.vertical) {
                    if (x2 - x1 > dist) {
                        return nearest0;
                    }
                } else {
                    if (y2 - y1 > dist) {
                        return nearest0;
                    }
                }
                nearest = nearestneighbor(n.right, x1, y1);
                nearest3 = nearest;
                if (nearest == null
                        || dist < euclidean(nearest.projectx, x1, nearest.projecty, y1)) {
                    nearest3 = nearest0;
                }
                return nearest3;
            } else {
                nearest1 = nearestneighbor(n.right, x1, y1);
                if (nearest1 != null) {
                    dist2 = euclidean(nearest1.projectx, x1, nearest1.projecty, y1);
                    if (dist2 < dist) {
                        nearest0 = nearest1;
                        dist = dist2;
                    } else {
                        nearest0 = nearest;
                    }
                } else {
                    nearest0 = nearest;
                }
                if (n.vertical) {
                    if (x1 - x2 > dist) {
                        return nearest0;
                    }
                } else {
                    if (y1 - y2 > dist) {
                        return nearest0;
                    }
                }
                nearest = nearestneighbor(n.left, x1, y1);
                nearest3 = nearest;
                if (nearest == null
                        || dist < euclidean(nearest.projectx, x1, nearest.projecty, y1)) {
                    nearest3 = nearest0;
                }
                return nearest3;
            }
        }
    }


    /* compute straight line distance between two points in the kdTree */
    static double euclidean(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    void addNode(Node x) {
        idtonode.put(x.id, x);
    }

    void addEdge(Edge x) {
        if (!neighbor.containsKey(x.from.id)) {
            LinkedList<Long> id = new LinkedList<>();
            neighbor.put(x.from.id, id);
            id.add(x.to.id);
        } else {
            neighbor.get(x.from.id).add(x.to.id);
        }
        if (!neighbor.containsKey(x.to.id)) {
            LinkedList<Long> id2 = new LinkedList<>();
            neighbor.put(x.to.id, id2);
            id2.add(x.from.id);
        } else {
            neighbor.get(x.to.id).add(x.from.id);
        }
    }

    void removeNode(Node x) {
        idtonode.remove(x.id, x);
    }

    HashMap<Long, Node> getMap() {
        return this.idtonode;
    }

//    ArrayList<LocationParams> getLocationParams() {
//        return this.locationParams;
//    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));
        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Radius of the Earth in miles.
     */
    private static final int R = 3963;
    /**
     * Latitude centered on Berkeley.
     */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /**
     * Longitude centered on Berkeley.
     */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     *
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;
}
