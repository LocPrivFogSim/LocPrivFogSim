
using System.Collections;
using Data;



//Singleton
public class TrackingAttackController
{

    public Dictionary<int, Coord> _locations { get; set; }

    Dictionary<int, List<Coord>> _paths { get; set; }

    Dictionary<int, Device> _fogNodes { get; set; }

    List<SquareField> _squareFields { get; set; }




    //Controller Method for tracking attack
    public async Task RunTrackingAttack()
    {
        DB_Connector connector = new DB_Connector();
        _paths = connector.GetAllPaths();

        JsonParser jsonParser = new JsonParser();
        _locations = jsonParser.GetLocations();
        _fogNodes = jsonParser.InitFogNodes();
        _squareFields = jsonParser.GetSquareFields(_locations, _fogNodes);

        var files = Directory.GetFiles(Constants.EventsFilesDir, "*.json", SearchOption.AllDirectories);
        var results = ArrayList.Synchronized(new ArrayList(files.Length));

        Parallel.ForEach(files, (file, _, _) =>
        {
            var res = getAttackResultForFile(file);
            results.Add(res);
        });


       printResultsToCSV(results);
    }


    public void printResultsToCSV(ArrayList results)
    {
        //TODO
        var file = Path.Combine(Directory.GetCurrentDirectory(), Constants.ResultsFilePath, "test.csv");
        using var writer = new StreamWriter(file, false);
        writer.WriteLine($"Strategy,Rate,Iteration,CorrAvgDtwDist,CorrFullDtwDist");

        foreach(var obj in results )
        {
            var result = obj as AttackResult;
            if (result is null)
                continue;

            writer.WriteLine($"{result.strat},{result.rate},{result.iteration},{result.corrAvgDtwDist},{result.corrFullDtwDist}");
        }

        writer.Flush();
    }


    private AttackResult getAttackResultForFile(String filepath)
    {
        Console.WriteLine(filepath);

        int[] experimentParams = getExperimentParamsFromFileName(filepath);
        int strategy = experimentParams[0];
        int rate = experimentParams[1];
        int iteration = experimentParams[2];

        JsonParser jp = new JsonParser();
        EventFileData eventFileData = jp.ParseEventFile(filepath);

        Dictionary<int, DeviceStats> fogDeviceStats = getDeviceStats(eventFileData);

        var result = strategy switch
        {
            1 => calcNotSlow(_locations, eventFileData, _paths, _fogNodes, fogDeviceStats),
            2 => calcFastest(_locations, eventFileData, _paths, _fogNodes, fogDeviceStats),
            3 => calcClosest(_locations, eventFileData, _paths, _fogNodes),
            _ => throw new ArgumentOutOfRangeException("Unknown value for strategy")
        };

        result.strat = strategy;
        result.rate = rate;
        result.iteration = iteration;

        return result;
    }



    private AttackResult calcClosest(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes)
    {
        //TODO

        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        for (int i = 0; i < paths.Count(); i++)
        {
            double alpha = 0;
            List<Coord> path = paths[i];

            Dictionary<int, Segment> segments = Calculations.SampleSegments(path);

        }


        return result;
    }

    private AttackResult calcFastest(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats)
    {

        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        List<Event> events = eventFileData.Events;

        List<Coord> originalPath = _paths[eventFileData.SimulatedPathId];


        for(int pathID = 0 ; pathID < paths.Count(); pathID++)
        {
            //heuristic filter to improve perfomance
            List<Coord> path = _paths[pathID];
            Coord a_first = path[0];
            Coord a_last = path.Last();

            Coord b_first = originalPath[0];
            Coord b_last = originalPath.Last();
            if(Calculations.CalcDistanceInMetres(a_first,b_first) > 10000 ) continue;

            double bearingA = Calculations.Bearing(a_first, a_last);
            double bearingB = Calculations.Bearing(b_first, b_last);
            double bearingDelta = Math.Abs(bearingA - bearingB);

            if(bearingDelta > 30) continue;

            double alpha = 0;
            Dictionary<int, Segment> segments = Calculations.SampleSegments(path);

            // NOTE(markus): For testing TODO do remove
            // JsonParser.segmentsAndPathToGPX(segments, path);

            int nrOfValidSegmentations = 0;

            for (int j = 0; j < Constants.NumberOfIterations; j++)
            {

                segments = Calculations.SampleSegmentVelocities(segments);

                double sumTraversingTime = calcSumTraversingTime(segments);
                double lastTrackedTS = events[events.Count - 1].Timestamp;
                double trackedDuration = lastTrackedTS - events[0].Timestamp;

                if (sumTraversingTime < trackedDuration) continue;

                nrOfValidSegmentations++;

                double beta = 0;

                double maximumStartTime = sumTraversingTime - trackedDuration;

                double randStartTime = Constants.rnd.NextDouble() * maximumStartTime;

                Dictionary<double, Coord> segmentCoordsAtTimestamps = Calculations.mapSegmentCoordsToTimestamps(segments, events, randStartTime);


                for (int eventIndex = 0; eventIndex < events.Count / 2; eventIndex++)
                {
                    Event addEvent = events[eventIndex * 2];
                    Event removeEvent = events[eventIndex * 2 + 1];

                    Coord guessedLocation = segmentCoordsAtTimestamps[addEvent.Timestamp];
                    Coord actualPos = getActualCoordOnPath(_paths[eventFileData.SimulatedPathId], addEvent.Timestamp);

                    Device device = _fogNodes[addEvent.FogDeviceId];


                    if (!Calculations.CoordIsInPolygon(guessedLocation, addEvent.ConsideredField))
                    {
                        continue;
                    }

                    int chosenDeviceId = fastestRespondingDeviceId(guessedLocation, addEvent, removeEvent, eventFileData, fogDeviceStats);

                    if (chosenDeviceId != device.Id)
                    {
                        continue;
                    }

                    int countOfOtherPossibleLocations = 0;


                    List<Coord> relevantLocations = new List<Coord>();

                    foreach (SquareField sf in _squareFields)
                    {
                        if (sf.fogNodeIDs.Contains(device.Id))
                        {
                            relevantLocations = sf.locations;
                            break;
                        }
                    }

                    foreach( Coord l in relevantLocations)
                    {

                        chosenDeviceId = fastestRespondingDeviceId(l, addEvent, removeEvent, eventFileData, fogDeviceStats);

                        if (chosenDeviceId == device.Id)
                        {
                            countOfOtherPossibleLocations++;
                        }
                    }


                    if (countOfOtherPossibleLocations > 0)
                    {
                       beta += (1 / countOfOtherPossibleLocations);
                    }
                }

                alpha += beta;

            }

            if (alpha > 0)
            {
                alpha = alpha / nrOfValidSegmentations;
                pathProbablity[pathID] = alpha;
            }
        }

        return finalizeResult(result, pathProbablity, originalPath);
    }

    private AttackResult calcNotSlow(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats)
    {
        //TODO
        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        for (int i = 0; i < paths.Count(); i++)
        {
            double alpha = 0;
            List<Coord> path = paths[i];
            Dictionary<int, Segment> segments = Calculations.SampleSegments(path);

        }


        return result;
    }


    private AttackResult finalizeResult(AttackResult result, Dictionary<int, double> pathProbablity, List<Coord> originalPath)
    {
        double totalAlpha = pathProbablity.Values.Sum();

        double correctnessFull = 0;
        double correctnessAvg = 0;

        foreach(int i in pathProbablity.Keys)
        {
            List<Coord> samplePath = _paths[i];

            double[,] fullDTWMatrix = Calculations.DtwMatrix(samplePath, originalPath);

            double fullDTWDistance = Calculations.DtwDistance(fullDTWMatrix);

            var warpingPathLength = Calculations.DtwWarpingPath(fullDTWMatrix).Count;

            double avgDTWDist = fullDTWDistance / warpingPathLength;

            correctnessFull += fullDTWDistance;
            correctnessAvg += avgDTWDist;

        }

        result.corrFullDtwDist = correctnessFull;
        result.corrAvgDtwDist = correctnessAvg;

        return result;
    }

    public Dictionary<int, DeviceStats> getDeviceStats(EventFileData ev)
    {
        Dictionary<int, DeviceStats> deviceStats = new Dictionary<int, DeviceStats>();

        foreach (int x in ev.FogDeviceInfos.Keys)
        {
            bool isCompromised = ev.CompromisedFogNodes.Contains(x);
            double downlinkBandwidth = ev.FogDeviceInfos[x][0];
            double uplinkBandwidth = ev.FogDeviceInfos[x][1];
            double uplinkLatency = ev.FogDeviceInfos[x][2];


            DeviceStats ds = new DeviceStats();
            ds.IsCompromised = isCompromised;
            ds.DownlinkBandwidth = downlinkBandwidth;
            ds.UplinkBandwidth = uplinkBandwidth;
            ds.UplinkLatency = uplinkLatency;

            deviceStats[x] = ds;
        }
        return deviceStats;
    }


    public int[] getExperimentParamsFromFileName(string filepath)
    {
        string[] subStrings = filepath.Split("\\");
        string fileNameWithoutExtension = subStrings[subStrings.Length - 1].Split(".")[0];

        string[] fileNameSubStings = fileNameWithoutExtension.Split("_");  // eg. output_2_10_1

        int strategy = int.Parse(fileNameSubStings[1]);
        int rate = int.Parse(fileNameSubStings[2]);
        int iteration = int.Parse(fileNameSubStings[3]);

        return new int[] { strategy, rate, iteration };
    }


    public double calcSumTraversingTime(Dictionary<int, Segment> segments)
    {
        double counter = 0;

        foreach (Segment s in segments.Values)
        {
            counter += s.TraversingTime;
        }

        return counter;
    }

    public Coord getActualCoordOnPath(List<Coord> path, double timestamp)
    {
        return getActualCoordOnPath(path, timestamp, 0, path.Count);
    }
    public Coord getActualCoordOnPath(List<Coord> path, double timestamp, int start, int end)
    {

        if (start > end)
        {
            throw new Exception("getActualCoordOnPath(): no Coordinate with correct timestamp found");
        }

        double x = (double)((start + end) / 2);
        int middle = (int)Math.Floor(x);

        if (path[middle].Timestamp == timestamp) return path[middle];

        if (path[middle].Timestamp > timestamp)
        {
            return getActualCoordOnPath(path, timestamp, start, middle - 1);
        }


        return getActualCoordOnPath(path, timestamp, middle + 1, end);

    }

    public int fastestRespondingDeviceId(Coord guessedLoc, Event addE, Event removeE, EventFileData eFileData, Dictionary<int, DeviceStats> stats)
    {
        int[] consideredDeviceIDs = addE.ConsideredFogNodes;
        int fastestNodeId = 0;
        double minRt = Double.MaxValue;

        Dictionary<int, double> tmp = new Dictionary<int, double>(); //TODO Remove


        foreach (int deviceID in consideredDeviceIDs)
        {
            Device d = _fogNodes[deviceID];
            double rt = Calculations.ResponseTime(addE, removeE, d, stats[deviceID], guessedLoc);

            tmp[d.Id] = rt; //TODO remove

            if (rt < minRt)
            {
                fastestNodeId = deviceID;
                minRt = rt;
            }
        }
        return fastestNodeId;
    }


    //---------- -- Singleton --------------------

    private TrackingAttackController()
    {
    }

    private static TrackingAttackController _instance = new TrackingAttackController();

    public static TrackingAttackController Instance
    {
        get
        {
            if (_instance == null)
                _instance = new TrackingAttackController();
            return _instance;
        }
    }

}



public class AttackResult
{
    public int strat {get;set;}
    public int rate {get;set;}
    public int iteration {get;set;}
    public double corrFullDtwDist {get;set;}
    public double corrAvgDtwDist {get; set;}

}