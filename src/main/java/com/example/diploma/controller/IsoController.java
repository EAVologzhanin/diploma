package com.example.diploma.controller;

import com.example.diploma.model.exception.IllegalInputParameterException;
import com.example.diploma.service.IsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wololo.geojson.GeoJSON;

@RestController
public class IsoController {
    @Autowired
    private IsoService isoService;

    @GetMapping("/iso")
    public GeoJSON getIsochrone(
            @RequestParam String profile,
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Long time,
            @RequestParam Boolean tree,
            @RequestParam String algorithmType,
            @RequestParam Integer smoothingFactor) {
        try {
            if (Boolean.TRUE.equals(tree)) {
                return isoService.getTree(profile, lat, lon, time, algorithmType);
            } else {
                System.out.println(isoService.getIsochrone(profile, lat, lon, time, algorithmType, smoothingFactor));
                return isoService.getIsochrone(profile, lat, lon, time, algorithmType, smoothingFactor);
            }
        } catch (IllegalStateException ex) {
            throw new IllegalInputParameterException("Out of area");
        }
    }
}

