package org.fog.utils;

import org.fog.entities.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import com.fasterxml.jackson.databind.*;

/*
holds data while simulation is running and prints it to Json once sim finishes is gathered
 */
public class PrivacyJsonHelper {

    int simulatedPath;
    int simulatedScenario;
    ArrayList<Integer> compromisedFogNodes;
    LinkedList<Event> events = new LinkedList<>();


    public PrivacyJsonHelper(int simulatedPath, int simulatedScenario, ArrayList<Integer> compromisedFogNodes) {
        this.simulatedPath = simulatedPath;
        this.simulatedScenario = simulatedScenario;
        this.compromisedFogNodes = compromisedFogNodes;
    }

    public void addEvent(int eventId, String eventType){
        Event e =  new Event(eventType,eventId);
        events.add(e);
    }


    public void writeJsonToFile(String filePath){
        File file = new File(filePath);


    }


}


/*
/for easier Json-parsing
*/
class Event {
    String event_type;  //for some?!
    int event_id;

    public Event(String event_type, int event_id) {
        this.event_type = event_type;
        this.event_id = event_id;
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
}
