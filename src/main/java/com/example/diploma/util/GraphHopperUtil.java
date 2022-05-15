package com.example.diploma.util;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.isochrone.algorithm.ShortestPathTree;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import lombok.Data;
import lombok.Getter;
import org.locationtech.jts.geom.*;
import org.wololo.geojson.Feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.ToDoubleFunction;

@Data
public class GraphHopperUtil {
    @Getter
    private GraphHopper graphHopper;
    public GraphHopperUtil(String path) {
        graphHopper = new GraphHopper();
        graphHopper.setOSMFile(path);
        graphHopper.setGraphHopperLocation("target/isochrone-graph-cache");
        graphHopper.setProfiles(
                Arrays.asList(
                        new Profile("foot").setVehicle("foot").setWeighting("fastest"),
                        new Profile("car").setVehicle("car").setWeighting("fastest"),
                        new Profile("bike").setVehicle("bike").setWeighting("fastest")
                )
        );
        graphHopper.importOrLoad();
    }
    public static List<Coordinate> getPoints(Snap snap, QueryGraph queryGraph, ShortestPathTree shortestPathTree, ToDoubleFunction<ShortestPathTree.IsoLabel> fz) {
        final NodeAccess na = queryGraph.getNodeAccess();
        List<Coordinate> sites = new ArrayList<>();
        shortestPathTree.search(snap.getClosestNode(), label -> {
            double exploreValue = fz.applyAsDouble(label);
            double lat = na.getLat(label.node);
            double lon = na.getLon(label.node);
            Coordinate site = new Coordinate(lon, lat);
            site.z = exploreValue;
            sites.add(site);

            // add a pillar node to increase precision a bit for longer roads
            if (label.parent != null) {
                EdgeIteratorState edge = queryGraph.getEdgeIteratorState(label.edge, label.node);
                PointList innerPoints = edge.fetchWayGeometry(FetchMode.PILLAR_ONLY);
                if (innerPoints.size() > 0) {
                    int midIndex = innerPoints.size() / 2;
                    double lat2 = innerPoints.getLat(midIndex);
                    double lon2 = innerPoints.getLon(midIndex);
                    Coordinate site2 = new Coordinate(lon2, lat2);
                    site2.z = exploreValue;
                    sites.add(site2);
                }
            }
        });
        return sites;
    }
    public static org.wololo.geojson.Point convert(Point point) {
        org.wololo.geojson.Point json = new org.wololo.geojson.Point(
                convert(point.getCoordinate()));
        return json;
    }

    public static org.wololo.geojson.MultiPoint convert(MultiPoint multiPoint) {
        return new org.wololo.geojson.MultiPoint(
                convert(multiPoint.getCoordinates()));
    }

    public static org.wololo.geojson.LineString convert(LineString lineString) {
        return new org.wololo.geojson.LineString(
                convert(lineString.getCoordinates()));
    }

    public static org.wololo.geojson.LineString convert(LinearRing ringString) {
        return new org.wololo.geojson.LineString(
                convert(ringString.getCoordinates()));
    }

    public static org.wololo.geojson.MultiLineString convert(MultiLineString multiLineString) {
        int size = multiLineString.getNumGeometries();
        double[][][] lineStrings = new double[size][][];
        for (int i = 0; i < size; i++) {
            lineStrings[i] = convert(multiLineString.getGeometryN(i).getCoordinates());
        }
        return new org.wololo.geojson.MultiLineString(lineStrings);
    }

    public static org.wololo.geojson.Polygon convert(Polygon polygon) {
        int size = polygon.getNumInteriorRing() + 1;
        double[][][] rings = new double[size][][];
        rings[0] = convert(polygon.getExteriorRing().getCoordinates());
        for (int i = 0; i < size - 1; i++) {
            rings[i + 1] = convert(polygon.getInteriorRingN(i).getCoordinates());
        }
        return new org.wololo.geojson.Polygon(rings);
    }

    public static org.wololo.geojson.MultiPolygon convert(MultiPolygon multiPolygon) {
        int size = multiPolygon.getNumGeometries();
        double[][][][] polygons = new double[size][][][];
        for (int i = 0; i < size; i++) {
            polygons[i] = convert((Polygon) multiPolygon.getGeometryN(i)).getCoordinates();
        }
        return new org.wololo.geojson.MultiPolygon(polygons);
    }

    public static double[] convert(Coordinate coordinate) {
        return new double[]{coordinate.x, coordinate.y};
    }

    public static  double[][] convert(Coordinate[] coordinates) {
        double[][] array = new double[coordinates.length][];
        for (int i = 0; i < coordinates.length; i++) {
            array[i] = convert(coordinates[i]);
        }
        return array;
    }

}
