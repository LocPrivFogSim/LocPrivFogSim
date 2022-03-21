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
        foreach(var obj in fogDeviceInfoArr)
        {
            JObject jObj1 = (JObject) obj;   

            int deviceId = ((int)jObj1["fog_device_id"]);
            double downlinkBandwidth = ((int)jObj1["downlink_bandwidth"]);
            double uplink_bandwidth = ((int)jObj1["uplink_bandwidth"]);
            double uplink_latency = ((int)jObj1["uplink_latency"]);
            
            deviceInfos[deviceId] = new double[] {downlinkBandwidth, uplink_bandwidth, uplink_latency};
        }

        List<Event> events = new List<Event>();         //TODO check if c# Lists sorted by default

        var eventsArr = jObj["events"];
        foreach(var obj in eventsArr)
        {
            JObject jObj1 = (JObject) obj;  
            int event_id = ((int)jObj1["event_id"]);
            int fog_device_id = ((int)jObj1["fog_device_id"]);
            string eventName = ((string)jObj1["event_name"]);
            int eventType =  ((int)jObj1["event_type"]);
            double ts =  ((double)jObj1["timestamp"]);
            double availableMips =  ((double)jObj1["availableMips"]);
            double dataSize = ((double)jObj1["dataSize"]);
            double mi = ((double)jObj1["mi"]);
            double maxMips = ((double)jObj1["maxMips"]);
            int [] consideredFogNodes = jObj["consideredFogNodes"].ToObject<int[]>();

            List<Coord> consideredField = new List<Coord>();
            foreach( var obj1 in jObj["consideredField"])
            {
                JObject jObj2 = (JObject) obj1;  
                double lat = ((double)jObj2["lat"]);
                double lon = ((double)jObj2["lon"]);
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
            e.ConsideredFogNodes = null ;//consideredFogNodes; //TODO 
            e.ConsideredField = consideredField;

        }


        ef.SimulatedPathId = simulatedPathId;
        ef.CompromisedFogNodes = compFogNodes;
        ef.FogDeviceInfos = deviceInfos;
        ef.Events = events;
    

        Environment.Exit(0);
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