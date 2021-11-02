package org.fog.localization;

import org.fog.privacy.Position;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Path {
    private int pathId;
    private ArrayList<Position> positions;
    private int nrOfDuplicates; //how often a path was represented in geoLife Dataset
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private double distance;
    private LinkedList<Integer> trace; //sorted List with IDs of Fog-nodes that the device connects with on path


    public Path() {
    }

    public Path(int pathId, ArrayList<Position> positions, int nrOfDuplicates, double minLat, double maxLat, double minLon, double maxLon, double distance, LinkedList<Integer> trace) {
        this.pathId = pathId;
        this.positions = positions;
        this.nrOfDuplicates = nrOfDuplicates;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.distance = distance;
        this.trace = trace;
    }


    public int getPathId() {
        return pathId;
    }

    public void setPathId(int pathId) {
        this.pathId = pathId;
    }

    public ArrayList<Position> getPositions() {
        return positions;
    }

    public void setPositions(ArrayList<Position> positions) {
        this.positions = positions;
    }

    public int getNrOfDuplicates() {
        return nrOfDuplicates;
    }

    public void setNrOfDuplicates(int nrOfDuplicates) {
        this.nrOfDuplicates = nrOfDuplicates;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public void setMinLon(double minLon) {
        this.minLon = minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public LinkedList<Integer> getTrace() {
        return trace;
    }

    public void setTrace(LinkedList<Integer> trace) {
        this.trace = trace;
    }

    @Override
    public String toString() {
        return "Path{" +
                "pathId=" + pathId +
                ", positions=" + positions +
                ", nrOfDuplicates=" + nrOfDuplicates +
                ", minLat=" + minLat +
                ", maxLat=" + maxLat +
                ", minLon=" + minLon +
                ", maxLon=" + maxLon +
                ", distance=" + distance +
                '}';
    }
}
