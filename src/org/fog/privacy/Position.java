package org.fog.privacy;

import org.fog.entities.FogDevice;
import org.fog.localization.Coordinate;

/**
 * Representation of the mobile devices position
 * consisting of
 * - coordinate
 * - closest fog device
 * - direction and speed
 * - timestamp
 */
public class  Position {

    private FogDevice closestFogDevice;
    private String state;

    private int direction;
    private int speed;

    private Coordinate coordinate;

    private int timestamp; // in seconds (for geolife each timestamp equals the time that has passed in seconds



    public Position() {
    }

    public Position(Position pos) {
        this.closestFogDevice = pos.closestFogDevice;
        this.state = pos.state;
        this.direction = pos.direction;
        this.speed = pos.speed;
        this.coordinate = pos.coordinate;
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
    }

    public Position(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public Position(Coordinate coordinate, int timestamp, int direction, int speed) {
        this.coordinate = coordinate;
        this.timestamp =  timestamp;
        this.direction =  direction;
        this.speed =  speed;
    }


    @Override
    public String toString() {
        return "Position{" +
              //  "closestFogDevice=" + closestFogDevice +
                ", state='" + state + '\'' +
                ", direction=" + direction +
                ", speed=" + speed +
                ", coordinate=" + coordinate +
                ", timestamp=" + timestamp +
                '}';
    }

    //----------Getters & Setters-----------------


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

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

}
