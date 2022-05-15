package com.example.diploma.algorithm;

import io.sentry.util.Pair;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KnearestAlgorithm {
    public static List<Coordinate> calculateConcaveHull(List<Coordinate> coordinateList, Integer k) {
        ArrayList<Coordinate> CoordinatesWithOutDuplicates;
        ArrayList<Coordinate> concaveHull;
        do {

            concaveHull = new ArrayList<>();

            HashSet<Coordinate> setCoordinates = new HashSet<>(coordinateList);
            CoordinatesWithOutDuplicates = new ArrayList<>(setCoordinates);

            int smoothingCoefficient = Math.max(k, 3);

            if (CoordinatesWithOutDuplicates.size() < 3) {
                return CoordinatesWithOutDuplicates;
            }

            smoothingCoefficient = Math.min(smoothingCoefficient, CoordinatesWithOutDuplicates.size() - 1);

            Coordinate firstCoordinate = findMinYCoordinate(CoordinatesWithOutDuplicates);
            concaveHull.add(firstCoordinate);
            CoordinatesWithOutDuplicates.remove(firstCoordinate);

            double previousAngle = 0.0;
            int step = 2;

            CoordinatesWithOutDuplicates = (ArrayList<Coordinate>) filterCoordinates(
                    coordinateList,
                    firstCoordinate,
                    firstCoordinate,
                    step,
                    CoordinatesWithOutDuplicates,
                    smoothingCoefficient,
                    previousAngle,
                    concaveHull,
                    k

            );
            k++;
        } while (!allCoordinatesInside(CoordinatesWithOutDuplicates, concaveHull));

        return concaveHull;
    }

    private static List<Coordinate> filterCoordinates(List<Coordinate> coordinateList, Coordinate currentCoordinate, Coordinate firstCoordinate, int step, List<Coordinate> coordinateArraySet, int smoothingCoefficient, double previousAngle, List<Coordinate> concaveHull, int k) {
        while ((currentCoordinate != firstCoordinate || step == 2) && !coordinateArraySet.isEmpty()) {

            if (step == 5) {
                coordinateArraySet.add(firstCoordinate);
            }

            ArrayList<Coordinate> kNearestCoordinates = kNearestNeighbors((ArrayList<Coordinate>) coordinateArraySet, currentCoordinate, smoothingCoefficient);
            ArrayList<Coordinate> clockwiseCoorddinates = sortByAngle(kNearestCoordinates, currentCoordinate, previousAngle);

            boolean its = true;
            int i = -1;
            while (its && i < clockwiseCoorddinates.size() - 1) {
                i++;

                int lastCoordinate = 0;
                if (clockwiseCoorddinates.get(i) == firstCoordinate) {
                    lastCoordinate = 1;
                }

                int j = 2;
                its = false;
                while (!its && j < concaveHull.size() - lastCoordinate) {
                    its = intersect(concaveHull.get(step - 2), clockwiseCoorddinates.get(i), concaveHull.get(step - 2 - j), concaveHull.get(step - 1 - j));
                    j++;
                }
            }

            if (its) {
                return calculateConcaveHull(coordinateList, k + 1);
            }

            currentCoordinate = clockwiseCoorddinates.get(i);
            concaveHull.add(currentCoordinate);
            coordinateArraySet.remove(currentCoordinate);

            previousAngle = calculateAngle(concaveHull.get(step - 1), concaveHull.get(step - 2));

            step++;
        }
        return coordinateArraySet;
    }

    private static boolean allCoordinatesInside(List<Coordinate> CoordinateArraySet, ArrayList<Coordinate> concaveHull) {
        boolean insideCheck = true;
        int i = CoordinateArraySet.size() - 1;

        while (insideCheck && i > 0) {
            insideCheck = coordinateInPolygon(CoordinateArraySet.get(i), concaveHull);
            i--;
        }

        return insideCheck;
    }

    private static Double euclideanDistance(Coordinate a, Coordinate b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private static ArrayList<Coordinate> kNearestNeighbors(ArrayList<Coordinate> l, Coordinate q, Integer k) {
        ArrayList<Pair<Double, Coordinate>> nearestList = new ArrayList<>();
        for (Coordinate o : l) {
            nearestList.add(new Pair<>(euclideanDistance(q, o), o));
        }

        nearestList.sort(Comparator.comparing(Pair::getFirst));

        ArrayList<Coordinate> result = new ArrayList<>();

        for (int i = 0; i < Math.min(k, nearestList.size()); i++) {
            result.add(nearestList.get(i).getSecond());
        }

        return result;
    }

    private static Coordinate findMinYCoordinate(ArrayList<Coordinate> coordinates) {
        AtomicReference<Coordinate> min = new AtomicReference<>(coordinates.get(0));
        coordinates.forEach(coordinate -> {
            if (coordinate.y < min.get().y) {
                min.set(coordinate);
            }
        });
        return min.get();
    }

    private static Double calculateAngle(Coordinate p1, Coordinate p2) {
        return Math.atan2(p2.y - p1.x, p2.x - p1.x);
    }

    private static Double angleDifference(Double a1, Double a2) {
        if ((a1 > 0 && a2 >= 0) && a1 > a2) {
            return Math.abs(a1 - a2);
        } else if ((a1 >= 0 && a2 > 0) && a1 < a2) {
            return 2 * Math.PI + a1 - a2;
        } else if ((a1 < 0 && a2 <= 0) && a1 < a2) {
            return 2 * Math.PI + a1 + Math.abs(a2);
        } else if ((a1 <= 0 && a2 < 0) && a1 > a2) {
            return Math.abs(a1 - a2);
        } else if (a1 <= 0 && 0 < a2) {
            return 2 * Math.PI + a1 - a2;
        } else if (a1 >= 0 && 0 >= a2) {
            return a1 + Math.abs(a2);
        } else {
            return 0.0;

        }

    }

    private static ArrayList<Coordinate> sortByAngle(ArrayList<Coordinate> coordinates, Coordinate coordinate, Double angle) {
        coordinates.sort((o1, o2) -> {
            Double a1 = angleDifference(angle, calculateAngle(coordinate, o1));
            Double a2 = angleDifference(angle, calculateAngle(coordinate, o2));
            return a2.compareTo(a1);
        });
        return coordinates;
    }

    private static Boolean intersect(Coordinate l1p1, Coordinate l1p2, Coordinate l2p1, Coordinate l2p2) {
        Double a1 = l1p2.y - l1p1.y;
        Double b1 = l1p1.x - l1p2.x;
        Double c1 = a1 * l1p1.x + b1 * l1p1.y;
        Double a2 = l2p2.y - l2p1.y;
        Double b2 = l2p1.x - l2p2.x;
        Double c2 = a2 * l2p1.x + b2 * l2p1.y;

        double tmp = (a1 * b2 - a2 * b1);

        double pX = (c1 * b2 - c2 * b1) / tmp;

        if ((pX > l1p1.x && pX > l1p2.x) || (pX > l2p1.x && pX > l2p2.x)
                || (pX < l1p1.x && pX < l1p2.x) || (pX < l2p1.x && pX < l2p2.x)) {
            return false;
        }
        double pY = (a1 * c2 - a2 * c1) / tmp;

        return !(pY > l1p1.y && pY > l1p2.y) && !(pY > l2p1.y && pY > l2p2.y)
                && !(pY < l1p1.y && pY < l1p2.y) && !(pY < l2p1.y && pY < l2p2.y);
    }

    private static boolean coordinateInPolygon(Coordinate coordinate, ArrayList<Coordinate> coordinates) {
        boolean result = false;
        for (int i = 0, j = coordinates.size() - 1; i < coordinates.size(); j = i++) {
            if ((coordinates.get(i).y > coordinate.y) != (coordinates.get(j).y > coordinate.y) &&
                    (coordinate.x < (coordinates.get(j).x - coordinates.get(i).x) *
                            (coordinate.y - coordinates.get(i).y) / (coordinates.get(j).y - coordinates.get(i).y) + coordinates.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }

}
