package com.example.diploma.model;

import com.graphhopper.isochrone.algorithm.ShortestPathTree;
import com.graphhopper.resources.SPTResource;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.shapes.GHPoint;

public class IsoLabelWithCoordinates {
    public int nodeId = -1;
    public int edgeId;
    public int prevEdgeId;
    public int prevNodeId = -1;
    public int timeMillis;
    public int prevTimeMillis;
    public int distance;
    public int prevDistance;
    public GHPoint coordinate;
    public GHPoint prevCoordinate;

    public IsoLabelWithCoordinates() {
    }

    public static IsoLabelWithCoordinates isoLabelWithCoordinates(NodeAccess na, ShortestPathTree.IsoLabel label) {
        double lat = na.getLat(label.node);
        double lon = na.getLon(label.node);
        IsoLabelWithCoordinates isoLabelWC = new IsoLabelWithCoordinates();
        isoLabelWC.nodeId = label.node;
        isoLabelWC.coordinate = new GHPoint(lat, lon);
        isoLabelWC.timeMillis = Math.round((float)label.time);
        isoLabelWC.distance = (int)Math.round(label.distance);
        isoLabelWC.edgeId = label.edge;
        if (label.parent != null) {
            ShortestPathTree.IsoLabel prevLabel = label.parent;
            int prevNodeId = prevLabel.node;
            double prevLat = na.getLat(prevNodeId);
            double prevLon = na.getLon(prevNodeId);
            isoLabelWC.prevNodeId = prevNodeId;
            isoLabelWC.prevEdgeId = prevLabel.edge;
            isoLabelWC.prevCoordinate = new GHPoint(prevLat, prevLon);
            isoLabelWC.prevDistance = (int)Math.round(prevLabel.distance);
            isoLabelWC.prevTimeMillis = Math.round((float)prevLabel.time);
        }

        return isoLabelWC;
    }
}
