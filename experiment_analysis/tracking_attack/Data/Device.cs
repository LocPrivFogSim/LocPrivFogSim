namespace Data;

public class Device
{
    public Device(int id, Coord position, bool isCompromised, double downlinkBandwidth, double uplinkBandwidth, double uplinkLatency)
    {
        Id = id;
        Position = position;
        IsCompromised = isCompromised;
        DownlinkBandwidth = downlinkBandwidth;
        UplinkBandwidth = uplinkBandwidth;
        UplinkLatency = uplinkLatency;
    }

    public int Id { get; }
    
    public Coord Position { get; }

    public bool IsCompromised { get; }
    
    public double DownlinkBandwidth { get; }

    public double UplinkBandwidth { get; }

    public double UplinkLatency { get; }

}