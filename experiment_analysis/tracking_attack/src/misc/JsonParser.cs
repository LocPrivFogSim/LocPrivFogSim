using Data;
using System.Text.Json;
using Newtonsoft.Json.Linq;

public class JsonParser
{

    //TODO
   public List<Event> GetEvents(string filepath)
    {
        return null;
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