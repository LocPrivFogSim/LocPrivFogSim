package org.fog.privacy;


import org.cloudbus.cloudsim.Log;
import org.fog.entities.MobileDevice;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * This class represents the information about a mobile device, that an attacker can get in a real fog environment
 */
public class MobileDeviceInformation {

    private int deviceId;       // refers to MobileDevice.myId
    private String deviceName;
    
    private MobileDevice mobileDevice;

    private TreeMap<Long, Position> positionMap = new TreeMap<>();
    
    private long time;

    public MobileDeviceInformation() {
    }

    public MobileDeviceInformation(MobileDevice mobileDevice) {
        saveDeviceInformation(mobileDevice);
    }
    
    public void saveDeviceInformation(MobileDevice mobileDevice) {
        deviceId = mobileDevice.getMyId();
        deviceName = mobileDevice.getName();

        this.mobileDevice = mobileDevice;
        
        // Should we add the initial position?
        // Disabled by markus
        // addPosition(mobileDevice);
    }

    public void addPosition(MobileDevice mobileDevice) {
        Position position = new Position();
        position.setClosestFogDevice(mobileDevice.getSourceServerCloudlet());
        time = Calendar.getInstance().getTimeInMillis();
        addPosition(time, position);
    }


    public void addPosition(long time, Position position) {
        positionMap.put(time, position);
    }

    public Position getPositionAtTime(long time) {
        Set<Long> keys = positionMap.keySet();

        Position position = null;
        Position previousPosition = null;
        boolean in = true;

        for (Long key : keys) {
            previousPosition = position;

            if (time < key) {
                if (previousPosition.getState().equals("leaving")) {
                    return null;
                } else {
                    return position;
                }
            }

            position = positionMap.get(key);
        }


        return position;
    }

    public void printDeviceInformation() {
        Log.printLine("Knowledge about the Mobile Device with ");
        Log.printLine("Id: " + deviceId + ", Name: " + deviceName );

        if (!positionMap.isEmpty()) {
            BiConsumer<Long, Position> action =
                    (time, position) -> Log.printLine("Position at time " + time + ": " + position.toString());
            positionMap.forEach(action);
        }
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public TreeMap<Long, Position> getPositionMap() {
        return positionMap;
    }

    public void setPositionMap(TreeMap<Long, Position> positionMap) {
        this.positionMap = positionMap;
    }

	public MobileDevice getMobileDevice() {
		return mobileDevice;
	}
}
