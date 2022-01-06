package org.fog.utils;

import org.fog.entities.FogDevice;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
holds data while simulation is running and prints it to Json once sim finishes is gathered
 */
public class PrivacyJsonHelper {

    int simulatedPath;
    int simulatedScenario;
    List<Integer> compromisedFogNodes;
    List<FogDeviceInfo> fogDeviceInfos;
    LinkedList<Event> events = new LinkedList<>();

    public PrivacyJsonHelper(int simulatedPath, int simulatedScenario, List<FogDevice> allFogDevices, List<FogDevice> compromisedFogDevices) {
        this.simulatedPath = simulatedPath;
        this.simulatedScenario = simulatedScenario;

        this.fogDeviceInfos = allFogDevices.stream().map(x -> {
            return new FogDeviceInfo(x.getMyId(), x.getDownlinkBandwidth(), x.getUplinkBandwidth(), x.getUplinkLatency());
        }).collect(Collectors.toList());

        this.compromisedFogNodes = compromisedFogDevices.stream().map(x -> {
            return x.getMyId();
        }).collect(Collectors.toList());
    }

    public void addEvent(int fogNodeId, String eventName, int eventId, int eventType, int timestamp, double availableMips, int dataSize) {
        Event e =  new Event(fogNodeId,eventName,eventType,eventId, timestamp, availableMips, dataSize);
        events.add(e);
    }

    public void writeJsonToFile(String filePath){
        File file = new File(filePath);

        ObjectMapper mapper = new ObjectMapper();
        String json = "empty";
        try {
            json = mapper.writeValueAsString(this);

            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSimulatedPath() {
        return simulatedPath;
    }

    public void setSimulatedPath(int simulatedPath) {
        this.simulatedPath = simulatedPath;
    }

    public int getSimulatedScenario() {
        return simulatedScenario;
    }

    public void setSimulatedScenario(int simulatedScenario) {
        this.simulatedScenario = simulatedScenario;
    }

    public List<FogDeviceInfo> getFogDeviceInfos() {
        return fogDeviceInfos;
    }

    public void setFogDeviceInfos(ArrayList<FogDeviceInfo> fogDeviceInfos) {
        this.fogDeviceInfos = fogDeviceInfos;
    }

    public List<Integer> getCompromisedFogNodes() {
        return compromisedFogNodes;
    }

    public void setCompromisedFogNodes(ArrayList<Integer> compromisedFogNodes) {
        this.compromisedFogNodes = compromisedFogNodes;
    }

    public LinkedList<Event> getEvents() {
        return events;
    }

    public void setEvents(LinkedList<Event> events) {
        this.events = events;
    }
}

/*
/for easier Json-parsing
*/
class FogDeviceInfo  {
    int fog_device_id;
    double downlink_bandwidth;
    double uplink_bandwidth;
    double uplink_latency;

    public FogDeviceInfo(int fog_device_id, double downlink_bandwidth, double uplink_bandwidth, double uplink_latency)
    {
        this.fog_device_id = fog_device_id;
        this.downlink_bandwidth = downlink_bandwidth;
        this.uplink_bandwidth = uplink_bandwidth;
        this.uplink_latency = uplink_latency;
    }

    public int getFog_device_id() {
        return fog_device_id;
    }

    public void setFog_device_id(int fog_device_id) {
        this.fog_device_id = fog_device_id;
    }

    public double getDownlink_bandwidth() {
        return downlink_bandwidth;
    }

    public void setDownlink_bandwidth(double downlink_bandwidth) {
        this.downlink_bandwidth = downlink_bandwidth;
    }

    public double getUplink_bandwidth() {
        return uplink_bandwidth;
    }

    public void setUplink_bandwidth(double uplink_bandwidth) {
        this.uplink_bandwidth = uplink_bandwidth;
    }

    public double getUplink_latency() {
        return uplink_latency;
    }

    public void setUplink_latency(double uplink_latency) {
        this.uplink_latency = uplink_latency;
    }
}

/*
/for easier Json-parsing
*/
class Event {
    int fog_device_id;
    String event_name;
    int event_type;
    int event_id;
    int timestamp;
    double availableMips;
    int dataSize;

    public Event(int fog_device_id, String event_name, int event_type, int event_id, int timestamp, double availableMips, int dataSize) {
        this.fog_device_id = fog_device_id;
        this.event_name = event_name;
        this.event_type = event_type;
        this.event_id = event_id;
        this.timestamp = timestamp;
        this.availableMips = availableMips;
        this.dataSize = dataSize;
    }

    public int getFog_device_id() {
        return fog_device_id;
    }

    public void setFog_device_id(int fog_device_id) {
        this.fog_device_id = fog_device_id;
    }

    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public int getEvent_type() {
        return event_type;
    }

    public void setEvent_type(int event_type) {
        this.event_type = event_type;
    }

    public int getEvent_id() {
        return event_id;
    }

    public void setEvent_id(int event_id) {
        this.event_id = event_id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public double getAvailableMips() {
        return availableMips;
    }

    public void setAvailableMips(double availableMips) {
        this.availableMips = availableMips;
    }

    public double getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }
}