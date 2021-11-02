package org.fog.localization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;


public class SimField {

    ArrayList<Coordinate> corners;  //typically square or rectangle = 4 corners
    Coordinate center;


    public SimField(ArrayList<Coordinate> corners) {
        this.corners = corners;
        center = getCenterpoint();
    }

    public static SimField getFieldForBeijing() {
        ArrayList<Coordinate> fieldSurroundingPeking = new ArrayList<>();
        fieldSurroundingPeking.add(Coordinate.createGPSCoordinate(41.229556359747455, 116.32212585037877));
        fieldSurroundingPeking.add(Coordinate.createGPSCoordinate(40.56688298442188, 117.73143927643063));
        fieldSurroundingPeking.add(Coordinate.createGPSCoordinate(39.099855, 116.519898));
        fieldSurroundingPeking.add(Coordinate.createGPSCoordinate(39.75667, 115.13559));

        SimField field = new SimField(fieldSurroundingPeking);

        return field;
    }

    public boolean coordIsInField(Coordinate coordToCompare) {

        HashMap<Coordinate, Vector> vectorMap = new HashMap<>();

        //add vectors (n -> n+1)
        for (int i = 0; i < this.corners.size(); i++) {
            Coordinate current = corners.get(i);
            Coordinate next;
            if (i + 1 == this.corners.size()) {
                next = corners.get(0);
            } else {
                next = corners.get(i + 1);
            }
            Vector v = new Vector(next.getCoordX() - current.getCoordX(), next.getCoordY() - current.getCoordY());
            vectorMap.put(current, v);
        }

        //if point is in, it has to be to the right of each vector in map -> cross has to be > 0 for each
        boolean isAlwaysToTheRight = true;

        for (Coordinate coordinate : vectorMap.keySet()) {
            Vector tmp = new Vector(coordToCompare.getCoordX() - coordinate.getCoordX(), coordToCompare.getCoordY() - coordinate.getCoordY());
            double cross = cross2D(tmp, vectorMap.get(coordinate));

            if (cross < 1) {
                isAlwaysToTheRight = false;
                break;
            }
        }
        return isAlwaysToTheRight;
    }

    public ArrayList<Coordinate> sortCornersClockwise(ArrayList<Coordinate> cornerpoints) {
        if (!cornerpoints.get(0).isGPSCoordinate()) {
            ArrayList<Coordinate> sortedClockwise = (ArrayList<Coordinate>) cornerpoints.stream()
                    .sorted(((o1, o2) -> {
                        double crossproduct = (o1.getCoordX() - center.getCoordX()) * (o2.getCoordY() - center.getCoordY()) - (o1.getCoordY() - center.getCoordY()) * (o2.getCoordX() - center.getCoordX());
                        if (crossproduct > 0) return 1;
                        if (crossproduct < 0) return -1;
                        return 0;
                    }))
                    .collect(Collectors.toList());

            return sortedClockwise;
        }
        else{
            ArrayList<Coordinate> sortedClockwise = (ArrayList<Coordinate>) cornerpoints.stream()
                    .sorted(((o1, o2) -> {
                        double crossproduct = (o1.getLon() - center.getLon()) * (o2.getLat() - center.getLat()) - (o1.getLat() - center.getLat()) * (o2.getLon() - center.getLon());
                        if (crossproduct > 0) return 1;
                        if (crossproduct < 0) return -1;
                        return 0;
                    }))
                    .collect(Collectors.toList());

            return sortedClockwise;
        }
    }

    private Coordinate getCenterpoint() {

        float sumX = 0, sumY = 0, sumLat = 0, sumLon = 0;

        for (Coordinate coord : corners) {
            sumX += coord.getCoordX();
            sumY += coord.getCoordY();
            sumLat += coord.getLat();
            sumLon += coord.getLon();
        }

        if (sumX > sumLon) {  //cartesian coord given
            float centerX = sumX / corners.size();
            float centerY = sumY / corners.size();
            return Coordinate.createCartesianCoordinate(centerX, centerY);
        } else {
            float centerLat = sumLat / corners.size();
            float centerLon = sumLon / corners.size();
            return Coordinate.createGPSCoordinate(centerLat, centerLon);
        }
    }

    /**
     * @return negative if v1 is counterclockwise to v2, positive if v1 is clockwise to v2
     */
    private double cross2D(Vector v1, Vector v2) {
        double c2D = v1.getX() * v2.getY() - v1.getY() * v2.getX();
        return c2D;
    }

    public ArrayList<Coordinate> getCorners() {
        return corners;
    }

    public Coordinate getCenter() {
        return center;
    }

}

class Vector {

    double x;
    double y;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
