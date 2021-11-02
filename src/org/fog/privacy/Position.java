package org.fog.privacy;

import org.fog.entities.FogDevice;
import org.fog.localization.Coordinate;

/**
 * Representation of the mobile devices position
 * consisting of
 * - coordinate
 * - closest fog device
 * - distance and speed
 */
public class Position {

    private FogDevice closestFogDevice;
    private String state;

    private int direction;
    private int speed;

    private Coordinate coordinate;

    private double maxDistance;  // max. distance between mobile device and closest FogDevice in the given angle


    @Override
    public String toString() {
        return  "closestFogDevice=" + closestFogDevice.getName() + " (" + state +
                "), direction=" + direction +
                ", speed=" + speed +
                ", coordinate=" + coordinate +
                ", maxDistance to closest FogDevice=" + maxDistance;
    }

    public Position() {
    }

    public Position(Position pos) {
        this.closestFogDevice = pos.closestFogDevice;
        this.state = pos.state;
        this.direction = pos.direction;
        this.speed = pos.speed;
        this.coordinate = pos.coordinate;
        this.maxDistance = pos.maxDistance;
    }

    public Position(FogDevice closestFogDevice) {
        this.closestFogDevice = closestFogDevice;
    }

    public Position(FogDevice closestFogDevice, String state) {
        this.closestFogDevice = closestFogDevice;
        this.state = state;
    }


    public Position(int direction, int speed) {
        this.direction = direction;
        this.speed = speed;
    }

    public Position(FogDevice closestFogDevice, double maxDistance) {
        this.closestFogDevice = closestFogDevice;
        this.maxDistance = maxDistance;
    }

    public Position(Coordinate coordinate) {
        this.coordinate = coordinate;
    }




    public FogDevice getClosestFogDevice() {
        return closestFogDevice;
    }

    public void setClosestFogDevice(FogDevice closestFogDevice) {
        this.closestFogDevice = closestFogDevice;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }
}
