namespace Data;

public class Event
{
    public int Id { get; set; }

    public int FogDeviceId { get; set; }

    public string EventName { get; set; }

    public int EventType { get; set;}

    public double Timestamp { get; set;}

    public double AvailableMips { get; set;}

    public double DataSize { get; set;}

    public double Mi { get; set;}

    public double MaxMips { get; set;}

    public int[] ConsideredFogNodes { get; set;}

    public List<Coord> ConsideredField { get; set;}
}