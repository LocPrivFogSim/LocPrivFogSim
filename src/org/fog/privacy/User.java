package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

import java.util.List;

/**
 * The user (different from the owner) of a mobile device
 */
public class User extends Owner {

    public User(String name) {
        super(name);
        Log.printLine("User is " + name);
    }

    public User(String name, MobileDevice mobileDevice) {
        super(name, mobileDevice);
        Log.printLine("User of " + mobileDevice.getName() + " is " + name);
        Log.printLine();
    }

    @Override
    public List<FogDevice> getDeviceList() {
        List<FogDevice> devices = super.getDeviceList();
        Log.print("\nUser owns ");
        deviceListToString();
        Log.printLine();
        return devices;

    }

    @Override
    public void setDeviceList(List<FogDevice> deviceList) {
        super.setDeviceList(deviceList);
    }
}
