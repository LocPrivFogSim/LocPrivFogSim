package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * The owner (different from the user) of a mobile device
 */
public class Owner {

    public List<FogDevice> deviceList = new ArrayList<>();
    private String name;

    public Owner(String name) {
        this.name = name;
    }

    public Owner(String name, FogDevice fogDevice) {
        this.name = name;
        deviceList.add(fogDevice);
    }


    public void addDevice(FogDevice device) {
        deviceList.add(device);
    }

    public List<FogDevice> getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(List<FogDevice> deviceList) {
        this.deviceList = deviceList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void deviceListToString() {
        for (FogDevice fogDevice : deviceList) {
            Log.print(fogDevice.getName());
        }
        Log.printLine();
    }
}
