package com.example.diploma.service;

import com.example.diploma.algorithm.DuckhamHull;
import com.example.diploma.algorithm.KnearestAlgorithm;
import com.example.diploma.config.Config;
import com.example.diploma.model.IsoLabelWithCoordinates;
import com.example.diploma.model.exception.IllegalInputParameterException;
import com.example.diploma.util.GraphHopperUtil;
import com.graphhopper.GraphHopper;
import com.graphhopper.isochrone.algorithm.ShortestPathTree;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.DefaultSnapFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.MultiLineString;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static com.example.diploma.util.GraphHopperUtil.convert;
import static com.example.diploma.util.GraphHopperUtil.getPoints;

@Service
public class IsoServiceImpl implements IsoService {
    private final Config config;
    private final static GeometryFactory geometryFactory = new GeometryFactory();
    private final GraphHopper graphHopper;
    private final LocationIndex locationIndex;
    private final Graph graph;

    public IsoServiceImpl(Config config) {
        this.config = config;
        final GraphHopperUtil graphHopperUtil = new GraphHopperUtil(config.getOsmPath());
        graphHopper = graphHopperUtil.getGraphHopper();
        locationIndex = graphHopper.getLocationIndex();
        graph = graphHopper.getGraphHopperStorage();
    }

    @Override
    public GeoJSON getIsochrone(String profileName, Double lat, Double lon, Long timeLimitInSeconds, String algorithm, Integer smoothingFactor) {

        if (timeLimitInSeconds > config.getMaxTimeTravel() || timeLimitInSeconds < 0) {
            throw new IllegalInputParameterException("Travel Time is too long");
        }
        if (smoothingFactor > config.getSmoothingFactor() || smoothingFactor < 0) {
            throw new IllegalInputParameterException("Smoothing Factor is too long");
        }

        FastestWeighting weighting = getFastestWeighting(profileName);
        BooleanEncodedValue inSubnetworkEnc = graphHopper.getEncodingManager().getBooleanEncodedValue(Subnetwork.key(profileName));
        Snap snap = locationIndex.findClosest(lat, lon, new DefaultSnapFilter(weighting, inSubnetworkEnc));
        QueryGraph queryGraph = QueryGraph.create(graph, snap);
        ShortestPathTree shortestPathTree = new ShortestPathTree(queryGraph, queryGraph.wrapWeighting(weighting), false, TraversalMode.NODE_BASED);
        double limit = timeLimitInSeconds * 1000;

        shortestPathTree.setDistanceLimit(limit);
        ToDoubleFunction<ShortestPathTree.IsoLabel> fz = l -> l.time;
        List<Coordinate> coordinates = getPoints(snap, queryGraph, shortestPathTree, fz);

        switch (algorithm) {
            case "GRAHAM": {
                ConvexHull convexHull = new ConvexHull(coordinates.toArray(Coordinate[]::new), geometryFactory);
                return convert(geometryFactory.createPolygon(convexHull.getConvexHull().getCoordinates()));
            }
            case "K-NEAREST": {
                List<Coordinate> list = KnearestAlgorithm.calculateConcaveHull(coordinates, 10 + smoothingFactor * 10);
                return convert(geometryFactory.createPolygon(list.toArray(Coordinate[]::new)));
            }
            case "DUCKHAM": {
                DuckhamHull duckhamHull = new DuckhamHull(geometryFactory.createMultiPointFromCoords(coordinates.toArray(Coordinate[]::new)), 0.0015 + smoothingFactor * 0.0002);
                return convert(geometryFactory.createPolygon(Arrays.stream(duckhamHull.getConcaveHull().getCoordinates()).map(x -> new Coordinate(x.x, x.y)).collect(Collectors.toList()).toArray(Coordinate[]::new)));
//                List<com.vividsolutions.jts.geom.Coordinate> coordinates2 = coordinates.stream().map(x -> new com.vividsolutions.jts.geom.Coordinate(x.x, x.y, 0)).collect(Collectors.toList());
//                com.vividsolutions.jts.geom.GeometryFactory geometryFactory1 = new com.vividsolutions.jts.geom.GeometryFactory();
//                DuckhamHull duckhamHull = new DuckhamHull(geometryFactory1.createMultiPoint(coordinates2.toArray(com.vividsolutions.jts.geom.Coordinate[]::new)), 0.002);
//                return convert(geometryFactory.createPolygon(Arrays.stream(duckhamHull.getConcaveHull().getCoordinates()).map(x -> new Coordinate(x.x, x.y)).collect(Collectors.toList()).toArray(Coordinate[]::new)));
            }
            default:
                throw new IllegalInputParameterException(String.format("Unknown algorithm: %s", algorithm));
        }
    }

    @Override
    public GeoJSON getTree(String profileName, Double lat, Double lon, Long timeLimitInSeconds, String algorithm) {
        FastestWeighting weighting = getFastestWeighting(profileName);
        BooleanEncodedValue inSubnetworkEnc = graphHopper.getEncodingManager().getBooleanEncodedValue(Subnetwork.key(profileName));
        Snap snap = locationIndex.findClosest(lat, lon, new DefaultSnapFilter(weighting, inSubnetworkEnc));
        QueryGraph queryGraph = QueryGraph.create(graph, snap);
        ShortestPathTree shortestPathTree = new ShortestPathTree(queryGraph, queryGraph.wrapWeighting(weighting), false, TraversalMode.NODE_BASED);
        double limit = timeLimitInSeconds * 1000;
        shortestPathTree.setDistanceLimit(limit);

        List<double[][]> lists = new LinkedList<>();
        shortestPathTree.search(snap.getClosestNode(), l -> {
            IsoLabelWithCoordinates label = IsoLabelWithCoordinates.isoLabelWithCoordinates(graph.getNodeAccess(), l);
            if (label.prevCoordinate != null) {
                lists.add(
                        new double[][]{
                                new double[]{label.prevCoordinate.lon, label.prevCoordinate.lat},
                                new double[]{label.coordinate.lon, label.coordinate.lat}
                        }
                );
            }
        });
        System.out.println(new MultiLineString(lists.toArray(double[][][]::new)));
        return new MultiLineString(lists.toArray(double[][][]::new));
    }

    private FastestWeighting getFastestWeighting(String profileName) {
        try {
            return new FastestWeighting(graphHopper.getEncodingManager().getEncoder(profileName));
        } catch (IllegalArgumentException exception) {
            throw new IllegalInputParameterException(String.format("Unknown profile: %s", profileName));
        }
    }
}
