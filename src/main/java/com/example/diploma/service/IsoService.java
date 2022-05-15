package com.example.diploma.service;

import org.wololo.geojson.GeoJSON;

public interface IsoService {
    GeoJSON getIsochrone(String profileName, Double lat, Double lon, Long timeLimitInSeconds, String algorithm, Integer smoothingFactor);
    GeoJSON getTree(String profileName, Double lat, Double lon, Long timeLimitInSeconds, String algorithm);
}
