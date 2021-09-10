/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /** The max image depth level. */
    public static final int MAX_DEPTH = 7;

    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     *     <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     *     possible, while still covering less than or equal to the amount of longitudinal distance
     *     per pixel in the query box for the user viewport size.</li>
     *     <li>Contains all tiles that intersect the query bounding box that fulfill the above
     *     condition.</li>
     *     <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     * @param params The RasterRequestParams containing coordinates of the query box and the browser
     *               viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */
    public RasterResultParams getMapRaster(RasterRequestParams params) {
        /* handle the corner cases */
        if (params.lrlon <= params.ullon || params.lrlat >= params.ullat) {
            return RasterResultParams.queryFailed();
        } else if (params.ullon < MapServer.ROOT_ULLON || params.lrlon > MapServer.ROOT_LRLON
                || params.ullat > MapServer.ROOT_ULLAT || params.lrlat < MapServer.ROOT_LRLAT) {
            return RasterResultParams.queryFailed();
        }
        /* a query window in lat and lon that is not proportional to its size in pixels???*/

        /* compute the depth */
        double expected = lonDPP(params.lrlon, params.ullon, params.w);
        int depth = helper(params, expected);

        /* Set the left and right boundaries of x and y for the tiles */
        int len = (int) Math.pow(2, depth);
        int xleft = (int) ((params.ullon - MapServer.ROOT_ULLON)
                / MapServer.ROOT_LON_DELTA * len);
        int xright = (int) (((params.lrlon - MapServer.ROOT_ULLON)
                / MapServer.ROOT_LON_DELTA) * len);
        int yup = (int) ((MapServer.ROOT_ULLAT - params.ullat)
                / MapServer.ROOT_LAT_DELTA * len);
        int ydown = (int) ((MapServer.ROOT_ULLAT - params.lrlat)
                / MapServer.ROOT_LAT_DELTA * len);

        /* initiate and fill in the renderGrid with tiles */
        int arrlen = xright - xleft + 1;
        int arrwid = ydown - yup + 1;
        String[][] renderGrid = new String[arrwid][arrlen];
        for (int i = xleft, m = 0; m < arrlen; i++, m++) {
            for (int j = yup, n = 0; n < arrwid; j++, n++) {
                renderGrid[n][m] = 'd'
                        + String.valueOf(depth) + '_' + 'x'
                        + String.valueOf(i) + '_' + 'y'
                        + String.valueOf(j) + '.' + 'p' + 'n' + 'g';
            }
        }

        double rasterUlLon = MapServer.ROOT_LON_DELTA / len * xleft + MapServer.ROOT_ULLON;
        double rasterUlLat = MapServer.ROOT_ULLAT - MapServer.ROOT_LAT_DELTA / len * yup;
        double rasterLrLon = MapServer.ROOT_LON_DELTA / len * (xright + 1) + MapServer.ROOT_ULLON;
        double rasterLrLat = MapServer.ROOT_ULLAT - MapServer.ROOT_LAT_DELTA / len * (ydown + 1);

        RasterResultParams resultParams = new RasterResultParams.Builder()
                .setRenderGrid(renderGrid)
                .setRasterUlLon(rasterUlLon)
                .setRasterUlLat(rasterUlLat)
                .setRasterLrLon(rasterLrLon)
                .setRasterLrLat(rasterLrLat)
                .setDepth(depth)
                .setQuerySuccess(true)
                .create();

        return resultParams;
    }


    private static int helper(RasterRequestParams params, double expected) {
        double londpp;
        for (int k = 0; k < MAX_DEPTH; k++) {
            londpp = (MapServer.ROOT_LRLON - MapServer.ROOT_ULLON)
                    / Math.pow(2, k) / MapServer.TILE_SIZE;
            if (londpp <= expected) {
                return k;
            }
        }
        return MAX_DEPTH;
    }

    /**
     * Calculates the lonDPP of an image or query box
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }
}
