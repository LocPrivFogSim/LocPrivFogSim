package org.fog.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.fog.entities.Tuple;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
holds data while simulation is running and prints it to Json once sim finishes is gathered
 */
public class PrivacyJsonHelper {

    int simulatedPath;
    int simulatedScenario;
    List<Integer> compromisedFogNodes;
    LinkedList<Event> events = new LinkedList<>();


    public PrivacyJsonHelper(int simulatedPath, int simulatedScenario, List<Integer> compromisedFogNodes) {
        this.simulatedPath = simulatedPath;
        this.simulatedScenario = simulatedScenario;
        this.compromisedFogNodes = compromisedFogNodes;
    }

    public void addEvent(int eventId, String eventType, int timestamp){
        Event e =  new Event(eventType,eventId, timestamp);
        events.add(e);
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

    public ArrayList<Integer> getCompromisedFogNodes() {
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
class Event {
    String event_type;  //for some?!
    int event_id;
    int timestamp;

    public Event(String event_type, int event_id, int timestamp) {
        this.event_type = event_type;
        this.event_id = event_id;
        this.timestamp = timestamp;
    }

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
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
}
