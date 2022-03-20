namespace Data;

public class Device
{
    public Device(int id, Coord position, bool isCompromised, double downlinkBandwidth, double uplinkBandwidth, double uplinkLatency, List<Coord> voronoiVertices)
    {
        Id = id;
        Position = position;
        IsCompromised = isCompromised;
        DownlinkBandwidth = downlinkBandwidth;
        UplinkBandwidth = uplinkBandwidth;
        UplinkLatency = uplinkLatency;
        VoronoiVertices = voronoiVertices;
    }

    public Device()
    {
        
    }

    public int Id { get; set;}
    
    public Coord Position { get; set;}

    public bool IsCompromised { get; set;}
    
    public double DownlinkBandwidth { get; set;}
    public double UplinkBandwidth { get; set;}

    public double UplinkLatency { get; set;}

    public List<Coord> VoronoiVertices{ get; set;}



}