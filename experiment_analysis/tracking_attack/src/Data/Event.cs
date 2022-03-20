namespace Data;


public class Event
{
    public Event(int id, int fogDeviceId, int taskId, string eventName, int eventType, double timestamp, double availableMips, double dataSize, double mi, List<Device> consideredFogNodes, List<Coord> consideredField)
    {
        Id = id;
        FogDeviceId = fogDeviceId;
        TaskId = taskId;
        EventName = eventName;
        EventType = eventType;
        Timestamp = timestamp;
        AvailableMips = availableMips;
        DataSize = dataSize;
        Mi = mi;
        ConsideredFogNodes = consideredFogNodes;
        ConsideredField = consideredField;
    }

    public int Id { get; }
    public int FogDeviceId { get; }

    public int TaskId { get; }
    
    public String EventName { get; }

    public int EventType { get; }

    public double Timestamp { get; }

    public double AvailableMips { get; }

    public double DataSize { get; }

    public double Mi { get; }

    public List<Device> ConsideredFogNodes { get; }

    public List<Coord> ConsideredField { get; }

}