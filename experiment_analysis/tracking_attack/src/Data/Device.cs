namespace Data;

public class Device
{
    
    public int Id { get; set;}
    
    public Coord Position { get; set;}
    
    public List<Coord> VoronoiVertices{ get; set;}

    public List<Coord> RelevantLocations {get; set;}

}

public class DeviceStats
{

    public bool IsCompromised { get; set;}
    
    public double DownlinkBandwidth { get; set;}
    public double UplinkBandwidth { get; set;}

    public double UplinkLatency { get; set;}

}