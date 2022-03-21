using Data;
using System.Text.Json;
using Newtonsoft.Json.Linq;

public class JsonParser
{
 
   public EventFileData ParseEventFile(string filepath)
    {
        EventFileData ef = new EventFileData();
        
        var jsonString = File.ReadAllText(filepath);
        var jObj = JObject.Parse(jsonString);

        int simulatedPathId = ((int)jObj["simulatedPath"]);

        int [] compFogNodes = jObj["compromisedFogNodes"].ToObject<int[]>();
        
        
        Dictionary<int, double[]> deviceInfos = new Dictionary<int, double[]> ();
        var fogDeviceInfoArr = jObj["fogDeviceInfos"];
        foreach(var deviceInfoObj in fogDeviceInfoArr)
        {
            JObject jDeviceInfoObj = (JObject) deviceInfoObj;   

            int deviceId = ((int)jDeviceInfoObj["fog_device_id"]);
            double downlinkBandwidth = ((int)jDeviceInfoObj["downlink_bandwidth"]);
            double uplink_bandwidth = ((int)jDeviceInfoObj["uplink_bandwidth"]);
            double uplink_latency = ((int)jDeviceInfoObj["uplink_latency"]);
            
            deviceInfos[deviceId] = new double[] {downlinkBandwidth, uplink_bandwidth, uplink_latency};
        }

        List<Event> events = new List<Event>();         //TODO check if c# Lists sorted by default

        var eventsArr = jObj["events"];
        foreach(var eventObj in eventsArr)
        {
            JObject jEventObj = (JObject) eventObj;  
            int event_id = ((int)jEventObj["event_id"]);
            int fog_device_id = ((int)jEventObj["fog_device_id"]);
            string eventName = ((string)jEventObj["event_name"]);
            int eventType =  ((int)jEventObj["event_type"]);
            double ts =  ((double)jEventObj["timestamp"]);
            double availableMips =  ((double)jEventObj["availableMips"]);
            double dataSize = ((double)jEventObj["dataSize"]);
            double mi = ((double)jEventObj["mi"]);
            double maxMips = ((double)jEventObj["maxMips"]);
            int [] consideredFogNodes = jEventObj["consideredFogNodes"].ToObject<int[]>();

            List<Coord> consideredField = new List<Coord>();
            foreach( var consFieldObj in jEventObj["consideredField"])
            {
                JObject jConsFieldObj = (JObject) consFieldObj;  
                double lat = ((double)jConsFieldObj["lat"]);
                double lon = ((double)jConsFieldObj["lon"]);
                Coord c = new Coord(lat, lon , -1);
                consideredField.Add(c);
            }

            Event e = new Event();
            e.Id = event_id;
            e.FogDeviceId = fog_device_id;
            e.EventName = eventName;
            e.EventType = eventType;
            e.Timestamp = ts;
            e.AvailableMips = availableMips;
            e.DataSize = dataSize;
            e.Mi = mi;
            e.MaxMips = maxMips;
            e.ConsideredFogNodes =consideredFogNodes ;
            e.ConsideredField = consideredField;

            events.Add(e);
        }


        ef.SimulatedPathId = simulatedPathId;
        ef.CompromisedFogNodes = compFogNodes;
        ef.FogDeviceInfos = deviceInfos;
        ef.Events = events;
    

        //Environment.Exit(0);
        return ef;
    }

    public Dictionary<int, Coord> GetLocations()
    {
        
        Dictionary<int, Coord> locations = new Dictionary<int, Coord>();

        var jsonString = File.ReadAllText(Constants.LocationsFilePath);
        var jArr = JArray.Parse(jsonString);

        for(int i = 0; i < jArr.Count(); i ++ )
        {
            var loc = jArr[i];
            double lat = ((double)loc[0]);
            double lon = ((double)loc[1]);

            Coord c = new Coord(lat, lon, -1);

            locations[i] = c;
        }

        Console.WriteLine(jArr[0]);
        return locations;
    }
    
}


public class EventFileData
{
    public int SimulatedPathId {get; set;}
    public int[] CompromisedFogNodes {get; set;}
    
    public Dictionary<int, double[]> FogDeviceInfos {get; set;} // FogDeviceInfos[id] =  {downlinkBandwidth, uplink_bandwidth, uplink_latency};

    public List<Event> Events {get; set;}
    

    
}