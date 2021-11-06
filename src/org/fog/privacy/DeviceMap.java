package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;


import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the Map including all fog devices
 */
public class DeviceMap {

    private int length;
    private ArrayList<FogDevice>[][] map;
    private ArrayList<FogDevice> fogDeviceList = new ArrayList<>();


    public DeviceMap(int size) {
        this.length = size;
       /* map = new ArrayList[length][length];

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                map[i][j] = new ArrayList<FogDevice>();
            }
        }
        */
    }

    public DeviceMap(){
    }


    public FogDevice findClosestFogDevice(List<FogDevice> fogDeviceList, MobileDevice mobileDevice) {
        FogDevice closestFogDevice = null;
        double minDistance = 200000000;
        double distance = 0.0;
        double a = 0.0, b = 0.0;
        Coordinate fogCoord = null;
        Coordinate mobCoord = mobileDevice.getPosition().getCoordinate();

        for (FogDevice fogDevice : fogDeviceList) {
            fogCoord = fogDevice.getPosition().getCoordinate();
            distance = Coordinate.calcDistance(fogCoord,mobileDevice.getPosition().getCoordinate());

            if (distance < minDistance) {
                minDistance = distance;
                closestFogDevice = fogDevice;
            }
        }

        return closestFogDevice;
    }


    public void addDevice(FogDevice device) {
        double x = device.getPosition().getCoordinate().getCoordX();
        double y = device.getPosition().getCoordinate().getCoordY();
       // map[x][y].add(device);
        fogDeviceList.add(device);
    }

    public void removeDevice(FogDevice device) {
        Coordinate coord = device.getPosition().getCoordinate();
        //map[coord.getCoordX()][coord.getCoordY()].remove(device);
    }

    //public ArrayList<FogDevice> getDevicesAtPoint(Coordinate coord) {
    //    return map[coord.getCoordX()][coord.getCoordY()];
    //}

   // public boolean pointIsEmpty(Coordinate coord) {
   //     return map[coord.getCoordX()][coord.getCoordY()].isEmpty();
    //}


    public void printMap() {
        Log.printLine();
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                Log.print("[");
                map[i][j].forEach(entry -> Log.print(entry.getName()+","));
                Log.print("]  ");
            }
            Log.print("\n");
        }
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public ArrayList<FogDevice>[][] getMap() {
        return map;
    }

    public void setMap(ArrayList<FogDevice>[][] map) {
        this.map = map;
    }

    public ArrayList<FogDevice> getFogDeviceList() {
        return fogDeviceList;
    }
}
