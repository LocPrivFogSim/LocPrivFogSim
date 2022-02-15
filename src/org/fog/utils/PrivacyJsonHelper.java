package org.fog.utils;

import org.fog.entities.FogDevice;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fog.localization.Coordinate;
import org.fog.offloading.OffloadingTask;
import org.fog.vmmobile.TestExample4;

/*
holds data while simulation is running and prints it to Json once sim finishes is gathered
 */
public class PrivacyJsonHelper {

    int simulatedPath;
    List<Integer> compromisedFogNodes;
    List<FogDeviceInfo> fogDeviceInfos;
    LinkedList<Event> events = new LinkedList<>();
    Map<String, Map<Integer, Double>> deviceStats;

    public PrivacyJsonHelper(int simulatedPath, List<FogDevice> allFogDevices, List<FogDevice> compromisedFogDevices) {
        this.simulatedPath = simulatedPath;

        this.fogDeviceInfos = allFogDevices.stream().map(x -> {
            return new FogDeviceInfo(x.getMyId(), x.getDownlinkBandwidth(), x.getUplinkBandwidth(), x.getUplinkLatency());
        }).collect(Collectors.toList());

        this.compromisedFogNodes = compromisedFogDevices.stream().map(x -> {
            return x.getMyId();
        }).collect(Collectors.toList());

        this.deviceStats = new HashMap<String, Map<Integer, Double>>();
    }

    public void addEvent(int fogNodeId, String eventName, int eventId, int eventType, int timestamp, double availableMips, OffloadingTask task) {
        int dataSize = (eventId == 6001) ? task.getInputDataSize() : task.getOutputDataSize();
        Event e =  new Event(fogNodeId, eventName, eventType, eventId, timestamp, availableMips, task.getUid(), dataSize, task.getMi());
        events.add(e);

        if (eventId != 6001)
            return;

        // Write down available mips from all fog nodes
        Map<Integer, Double> stats = new HashMap<Integer, Double>();
        for (FogDevice device : TestExample4.getAllFogDevices()) {
            stats.put(device.getId(), device.getHost().getPeList().get(0).getPeProvisioner().getAvailableMips());
        }
        this.deviceStats.put(task.getUid(), stats);
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

    public Map<String, Map<Integer, Double>> getDeviceStats() { return deviceStats; }

    public void SetDeviceStats(Map<String, Map<Integer, Double>> deviceStats) { this.deviceStats = deviceStats; }
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
    String taskId;
    int dataSize;
    int mi;

    public Event(int fog_device_id, String event_name, int event_type, int event_id, int timestamp, double availableMips, String taskId, int dataSize, int mi) {
        this.fog_device_id = fog_device_id;
        this.event_name = event_name;
        this.event_type = event_type;
        this.event_id = event_id;
        this.timestamp = timestamp;
        this.availableMips = availableMips;
        this.taskId = taskId;
        this.dataSize = dataSize;
        this.mi = mi;
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public int getMi() {
        return mi;
    }

    public void setMi(int mi) {
        this.mi = mi;
    }
}