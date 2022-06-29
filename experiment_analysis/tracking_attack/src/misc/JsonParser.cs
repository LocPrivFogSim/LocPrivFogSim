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

        return locations;
    }

    public Dictionary<int, Device> InitFogNodes()
    {

        Dictionary<int, Device> devices = new Dictionary<int, Device>();

        var jsonString = File.ReadAllText(Constants.NodeLocationsFilePath);
        var jArr = JArray.Parse(jsonString);

        for(int i = 0; i < jArr.Count(); i ++ )
        {
            var nodeInfo = (JArray) jArr[i];
            int id = ((int)nodeInfo[0]);


            Coord position = new Coord(
                (double) ((JArray) nodeInfo[1])[0],
                (double) ((JArray) nodeInfo[1])[1],
                -1);


            List<Coord> relevantLocations = new List<Coord>();
            var relevantLocationsJArr = nodeInfo[2];
            foreach (var relevantLoc in relevantLocationsJArr)
            {
                //Console.WriteLine("x  "+ relevantLocationsJArr);
                double lat = ((double)relevantLoc[0]);
                double lon = ((double)relevantLoc[1]);
                Coord c = new Coord(lat, lon, -1);
                relevantLocations.Add(c);
            }

            List<Coord> voronoiVertices = new List<Coord>();
            var verticesJArr = nodeInfo[3];
            foreach (var vertix in verticesJArr)
            {
                double lat = ((double)vertix[0]);
                double lon = ((double)vertix[1]);
                Coord c = new Coord(lat, lon, -1);
                voronoiVertices.Add(c);
            }



            // Console.WriteLine("id  "+ id);
            // Console.WriteLine("position  "+ position);
            // Console.WriteLine("voronoiVertices[0]  "+ voronoiVertices[0]);
            //Console.WriteLine("relevantLocations[0]  "+ relevantLocations[0]);


            Device d = new Device();
            d.Id = id;
            d.Position = position;
            d.VoronoiVertices = voronoiVertices;
            d.RelevantLocations = relevantLocations;

            devices[i] = d;
        }

        return devices;
    }


    //to complete the json file
    public List<SquareField> GetSquareFields(Dictionary<int, Coord> locations, Dictionary<int, Device> fogNodes)
    {
        List<SquareField> fields = new List<SquareField>();

        var jsonString = File.ReadAllText(Constants.SquaresFilePath);
        var jArr = JArray.Parse(jsonString);
        for(int i = 0; i < jArr.Count(); i ++ )
        {
            var fieldObj = (JObject) jArr[i];

            int id = (int) fieldObj["id"];

            List<Coord> edges = new List<Coord>();
            var edgesArr = (JArray) fieldObj["edges"];
            for(int j = 0 ; j < edgesArr.Count ; j ++)
            {
                var edgeObj = (JObject) edgesArr[j];
                double lat = (double) edgeObj["lat"];
                double lon = (double) edgeObj["lon"];
                Coord c = new Coord(lat, lon, -1);
                edges.Add(c);
            }

            SquareField f = new SquareField();
            f.Id = id;
            f.edges = edges;
            f.locations = new List<Coord>();
            f.fogNodeIDs = new List<int>();
            fields.Add(f);

        }


        foreach(int i in locations.Keys)
        {

            Coord l = locations[i];

            foreach(SquareField f in fields)
            {
                if (Calculations.CoordIsInPolygon(l, f.edges)){
                    f.locations.Add(l);
                    break;
                }
            }

        }

        foreach(int i in fogNodes.Keys)
        {

            Device d = fogNodes[i];

            foreach(SquareField f in fields)
            {
                if (Calculations.CoordIsInPolygon(d.Position, f.edges)){
                    f.fogNodeIDs.Add(d.Id);
                    break;
                }
            }

        }

        //var targetJson = JsonSerializer.Serialize(fields);
        //File.WriteAllText(@"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\json\10x10_squares_after.json", targetJson);

        return fields;
    }


    public static void printGPXToFile(List<Coord> coords, string filename)
    {
        string beginning = "<gpx>\n<trk>\n<trkseg>\n";
        string end = "</trkseg>\n</trk>\n</gpx>";

        string coordsString = "";

        foreach(Coord c in coords)
        {
            string lat =  c.Lat.ToString();
            string lon =  c.Lon.ToString();

            coordsString += "<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\"> </trkpt>\n";
        }

        string fullGPX = beginning + coordsString + end;
        fullGPX = fullGPX.Replace(",",".");

        File.WriteAllText(filename, fullGPX);
    }


    public static void segmentsAndPathToGPX( Dictionary<int, Segment> segments, List<Coord> path){

          List<Coord> segmentCoords = new List<Coord>();

            foreach(int s in segments.Keys){

                segmentCoords.Add(segments[s].StartCoord);
            }

            JsonParser.printGPXToFile(segmentCoords, "./_segments.gpx");
            JsonParser.printGPXToFile(path, "./_path.gpx");

    }


      public static void segmentsToGPX( Dictionary<int, Segment> segments,string filename ){

          List<Coord> segmentCoords = new List<Coord>();

            foreach(int s in segments.Keys){

                segmentCoords.Add(segments[s].StartCoord);
            }
            JsonParser.printGPXToFile(segmentCoords, filename);
    }



}


public class EventFileData
{
    public int SimulatedPathId {get; set;}
    public int[] CompromisedFogNodes {get; set;}

    public Dictionary<int, double[]> FogDeviceInfos {get; set;} // FogDeviceInfos[id] =  {downlinkBandwidth, uplink_bandwidth, uplink_latency};

    public List<Event> Events {get; set;}



}