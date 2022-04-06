
using Data;
public static class Calculations
{



    public static double CalcDistanceInMetres(in Coord coordA, in Coord coordB)
    {
        double p = 0.017453292519943295;
        var a = 0.5 - Math.Cos((coordB.Lat - coordA.Lat) * p)/2 + Math.Cos(coordA.Lat * p) * Math.Cos(coordB.Lat * p) * (1 - Math.Cos((coordB.Lon - coordA.Lon) * p)) / 2;
        return 1000 * 12742 * Math.Asin(Math.Sqrt(a));
    }

    public static double Bearing(in Coord pt1, in Coord pt2)
    {
        double x = Math.Cos(DegreesToRadians(pt1.Lat)) * Math.Sin(DegreesToRadians(pt2.Lat)) - Math.Sin(DegreesToRadians(pt1.Lat)) * Math.Cos(DegreesToRadians(pt2.Lat)) * Math.Cos(DegreesToRadians(pt2.Lon - pt1.Lon));
        double y = Math.Sin(DegreesToRadians(pt2.Lon - pt1.Lon)) * Math.Cos(DegreesToRadians(pt2.Lat));

        return (((Math.Atan2(y, x) + Math.PI * 2) % (Math.PI * 2)) * 180.0) / Math.PI; ;
    }

    public static double DegreesToRadians(double angle)
    {
        return angle * Math.PI / 180.0d;
    }

    public static bool CoordIsInPolygon(Coord testPoint, List<Coord> polygon)
    {
        bool result = false;
        int j = polygon.Count() - 1;
        for (int i = 0; i < polygon.Count(); i++)
        {
            if (polygon[i].Lon < testPoint.Lon && polygon[j].Lon >= testPoint.Lon || polygon[j].Lon < testPoint.Lon && polygon[i].Lon >= testPoint.Lon)
            {
                if (polygon[i].Lat + (testPoint.Lon - polygon[i].Lon) / (polygon[j].Lon - polygon[i].Lon) * (polygon[j].Lat - polygon[i].Lat) < testPoint.Lat)
                {
                    result = !result;
                }
            }
            j = i;
        }
        return result;
    }

    public static double[,] DtwMatrix(List<Coord> pathA, List<Coord> pathB)
    {
        int x =  pathA.Count();
        int y =  pathB.Count();

        double[,] distMatrix = new double[x, y];

        for(int i = 0; i < x ; i++)
        {
            for(int j = 0; j < y ; j++)
            {
              distMatrix[i, j] = Calculations.CalcDistanceInMetres(pathA[i], pathB[j]);
            }
        }

        double[,] resultMatrix = new double[x+1, y+1];

        for(int i = 1; i < x+1 ; i++)
        {
            resultMatrix[i,0] = Double.MaxValue;
        }

        for(int i = 1; i < y+1 ; i++)
        {
            resultMatrix[0,i] = Double.MaxValue;
        }

        for(int i = 1; i < x+1 ; i++)
        {
            for(int j = 1; j < y+1 ; j++)
            {
                double localDist = distMatrix[i - 1 , j - 1];
                double botLeft = resultMatrix[i - 1, j - 1];
                double bot = resultMatrix[i, j - 1];
                double left = resultMatrix[i - 1, j];
                resultMatrix[i, j] = localDist + Math.Min(botLeft, Math.Min(bot , left));
            }
        }
        return resultMatrix;
    }

    public static double DtwDistance(double[,] dtwMatr)
    {
        int x = dtwMatr.GetLength(0);
        int y = dtwMatr.GetLength(1);

        return dtwMatr[x,y];
    }

    public static List<int[]> DtwWarpingPath(double[,] dtwMatr)
    {
        int n = dtwMatr.GetLength(0) - 1;
        int m = dtwMatr.GetLength(1) - 1;

        List<int[]> warpingPath = new List<int[]>();

        while( n > 0 || m > 0)
        {
            int a = 0;
            int b = 0;

            if (n == 0)
            {
                b = m - 1;
            }

            else if(m == 0)
            {
                a = n - 1;
            }
            else
            {
                var min_val = Math.Min(dtwMatr[n-1, m-1] , Math.Min(dtwMatr[n-1, m], dtwMatr[n, m-1] ));
                if (min_val == dtwMatr[n-1, m-1])
                {
                    a = n-1;
                    b = m -1 ;
                }
                else if(min_val == dtwMatr[n-1, m])
                {
                    a = n-1;
                    b = m;
                }
                else{
                    a = n;
                    b = m -1 ;
                }
            }

            warpingPath.Add(new int[] {a,b});
            n = a;
            m = b;
        }
        warpingPath.Reverse();
        return warpingPath;
    }

    public static double ResponseTime(Event addEvent, Event removeEvent, Device device, DeviceStats stats, Coord samplePoint)
    {
        //TODO repeat testing

        double distance = CalcDistanceInMetres(samplePoint, device.Position);
        double distance_factor = 1 - ( distance / Constants.MaxDistance);

        //double distance_factor = CalcDistanceFactor ( samplePoint, device.Position);

        //Console.WriteLine("factor og: "+distance_factor);
        //Console.WriteLine("factor new: "+distance_factor1);

        double upTransfereTime = addEvent.DataSize / (stats.UplinkBandwidth * distance_factor ) ;
        double downTransfereTime = removeEvent.DataSize / (stats.DownlinkBandwidth * distance_factor ) ;
        double calcTime = addEvent.Mi / addEvent.AvailableMips ;

        return upTransfereTime + downTransfereTime + calcTime;
    }

    private static double CalcDistanceFactor(Coord samplePoint, Coord devicePosition)
    {
        //TODO
        double deltaLat = Math.Abs(samplePoint.Lat - devicePosition.Lat);
        double deltaLon = Math.Abs(samplePoint.Lon - samplePoint.Lon);

        double factor = Math.Sqrt((deltaLat + deltaLon)) / Math.Sqrt((Constants.DeltaLatFullArea + Constants.DeltaLonFullArea));
        return 1 - factor;

    }

    //e.g. factor = 0.5 -> target point is exact midpoint between c1 and c2
    public static Coord TargetCoordOnLine(Coord c1, Coord c2, double factor){
        double latV = c2.Lat - c1.Lat;
        double lonV = c2.Lon - c1.Lon;
        double targetLat = c1.Lat + latV * factor;
        double targetLon = c1.Lon + lonV * factor;

        return new Coord(targetLat, targetLon, -1);
    }

    public static Dictionary<int, Segment> SampleSegments(List<Coord> path)
    {
        Dictionary<int, Segment> segments = new Dictionary<int, Segment>();

        int segmentIndex = 0;
        Coord currSegmentStart = path[0];
        double leftForCurrSegment = Constants.LenOfSegments;

        for (int i = 1; i < path.Count(); i++){
            Coord x = path[i -1 ];
            Coord y = path[i];

            double distance = Calculations.CalcDistanceInMetres(x, y);

            while(leftForCurrSegment < distance)
            {
                Segment segment = new Segment();
                segment.StartCoord = currSegmentStart;

                double factor = leftForCurrSegment / distance;
                Coord target = Calculations.TargetCoordOnLine(x,y,factor);
                segment.EndCoord = target;

                segment.SegmentLength = Calculations.CalcDistanceInMetres(segment.EndCoord, segment.StartCoord);


                //segment.Velocity = Constants.RandVelocity;
                //segment.TraversingTime = distance/segment.Velocity;

                segments[segmentIndex] = segment;

                segmentIndex ++;
                x = target;
                distance -= leftForCurrSegment;
                leftForCurrSegment = Constants.LenOfSegments;
                currSegmentStart = x;
            }
            leftForCurrSegment -= distance;
        }

        //add final segment (has distance < LenOfSegments)
        Coord finalCoord = path[path.Count() - 1];
        Coord previousCoord = currSegmentStart;
        double dist = Calculations.CalcDistanceInMetres(previousCoord, finalCoord);
        Segment s = new Segment();

        s.StartCoord = previousCoord;
        s.EndCoord = finalCoord;
        s.SegmentLength = dist;
        //s.Velocity = Constants.RandVelocity;
        //s.TraversingTime = s.SegmentLength/s.Velocity;

        segments[segmentIndex] = s;
        return segments;
    }


    public static Dictionary<int, Segment> SampleSegmentVelocities(Dictionary<int, Segment> segments)
    {
        foreach(int key in segments.Keys)
        {
            Segment s = segments[key];
            s.Velocity = Constants.RandVelocity;
            s.TraversingTime = s.SegmentLength/s.Velocity;
        }

        return segments;
    }



    public static Dictionary<double, Coord> mapSegmentCoordsToTimestamps(Dictionary<int, Segment> segments, List<Event> events, double t0)
    {
        Dictionary<double, Coord> coordsAtTimestamp = new Dictionary<double, Coord>();

        List<double> timestamps = events.Where(e =>  e.EventName == "add").Select(e => e.Timestamp).ToList<double>();  //TODO verify if this works to get Timestamps of each add-Event

        int segIndex = 0;
        double currTraversingTimeSum = 0;

        Segment currSegment = segments[segIndex];


        foreach( double ts in timestamps)
        {

            double correspondingSegmentTimestamp = ts + t0;

            //walk along the segements until correspondingSegmentTimestamp is on the checked segment
            while(true)
            {
                if (currTraversingTimeSum + currSegment.TraversingTime > correspondingSegmentTimestamp)
                {
                    break;
                }
                currTraversingTimeSum += currSegment.TraversingTime;
                segIndex++;

                if(segIndex == segments.Count) break;

                currSegment = segments[segIndex];

            }

            double factor = currSegment.TraversingTime / (correspondingSegmentTimestamp - currTraversingTimeSum);
            coordsAtTimestamp[ts] = TargetCoordOnLine(currSegment.StartCoord, currSegment.EndCoord, factor);

        }
        return coordsAtTimestamp;
    }


}






public readonly struct Coord
{
    public Coord(double lat, double lon, double timestamp)
    {
        Lat = lat;
        Lon = lon;
        Timestamp = timestamp;
    }

    public double Lat { get; }
    public double Lon { get; }

    public double Timestamp { get; }

    public override string ToString() => $"({Lat}. {Lon})   timestamp: {Timestamp}";
}

public class Segment
{
    public Coord StartCoord {get;set;}
    public Coord EndCoord {get;set;}
    public double SegmentLength {get;set;}
    public double Velocity {get;set;}
    public double TraversingTime {get;set;}


}