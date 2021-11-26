package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

import java.util.*;

/**
 * The Attacker class
 * - observes the (path of the) mobile device(s)
 */
public class Attacker {

    private String name;
    private List<FogDevice> controlledDeviceList;
    private List<FogDevice> allFogDevicesList;

    public Attacker(String name, List<FogDevice> allFogDevicesList, List<FogDevice> controlledDevices) {
        this.name = name;
        this.controlledDeviceList = controlledDevices;
        this.allFogDevicesList = allFogDevicesList;

        Log.printLine("Attacker " + name + " created");
        observe();
    }

    /* Observer */
    public void observe() {
        for (FogDevice device : this.allFogDevicesList) {
            device.addObserver(this);
        }
    }

    public void update(FogDevice source, MobileDevice mobileDevice, int timestamp, int eventType, String event) {
        Log.formatLine("Attacker was notified: MobileDevice '%s' was %sed to '%s' due to a %s event.", mobileDevice.getName(),
                event, source.getName(), EventType.eventTypeToString(eventType));

//        if (eventType != EventType.OFFLOADING)
//            return;

        // TODO(markus): Überprüfe ob source device is controlled device

        // TODO(markus): Log event to json output file
    }
}
