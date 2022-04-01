
using System.Collections;
using Data;



//Singleton 
public class TrackingAttackController
{

    public Dictionary<int, Coord> _locations { get; set;}

    Dictionary<int, List<Coord>> _paths { get; set;}
    
    Dictionary<int, Device> _fogNodes {get; set;}
  
    

    //Controller Method for tracking attack
    public void RunTrackingAttack()
    {   
        ArrayList results = ArrayList.Synchronized(new ArrayList());
        
        DB_Connector connector =  new DB_Connector();
        _paths =  connector.GetAllPaths();
        
        
        JsonParser jsonParser = new JsonParser();
        _locations = jsonParser.GetLocations();
        _fogNodes = jsonParser.InitFogNodes();

        foreach (var file in  Directory.GetFiles(Constants.EventsFilesDir, "*.json" , SearchOption.AllDirectories))
        { 
            //TODO parallel execution of following 
            AttackResult r = getAttackResultForFile(file);
            results.Add(r);
        }

    }



    private AttackResult getAttackResultForFile(String filepath)
    { 
        Console.WriteLine(filepath);

        int [] experimentParams = getExperimentParamsFromFileName(filepath);
        int strategy = experimentParams[0];
        int rate = experimentParams[1];
        int iteration = experimentParams[2];

        JsonParser jp = new JsonParser();
        EventFileData eventFileData = jp.ParseEventFile(filepath);

        Dictionary<int, DeviceStats> fogDeviceStats = getDeviceStats(eventFileData);
        AttackResult result = null;

        switch (strategy)
        {
            case 1:
                result =  calcNotSlow(_locations, eventFileData, _paths, _fogNodes, fogDeviceStats);
                break;
            case 2:
                result =  calcFastest(_locations, eventFileData, _paths, _fogNodes, fogDeviceStats);
                break;
            case 3:
                result =  calcClosest(_locations, eventFileData, _paths, _fogNodes);
                break;
        }

        result.strat = strategy;
        result.rate = rate;
        result.iteration = iteration;
        
        
        return result;        
    } 



    private AttackResult calcClosest(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes )
    {
        //TODO

        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        for(int i = 0 ; i < paths.Count(); i++)
        {
            double alpha = 0;
            List<Coord> path = paths[i];
            
            Dictionary<int, Segment> segments =  Calculations.SampleSegments(path);

        }



        return result;
    }

    private AttackResult calcFastest(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats )
    {

        Console.WriteLine("In calc Fastest");
        Console.WriteLine("actual path is: "+ eventFileData.SimulatedPathId);

        
        //TODO
        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        List<Event> events = eventFileData.Events;
    
        for(int pathID = 0 ; pathID < paths.Count(); pathID++)
        //for(int pathID = 25381 ; pathID < 25382; pathID++)

        {
            //heuristic filter to improve perfomance
            Coord a_first = _paths[pathID][0];
            Coord a_last = _paths[pathID].Last();
            Coord b_first = _paths[eventFileData.SimulatedPathId][0];
            Coord b_last = _paths[eventFileData.SimulatedPathId].Last();
            if(Calculations.CalcDistanceInMetres(a_first,b_first) > 10000 ) continue;

            double bearingA = Calculations.Bearing(a_first, a_last);
            double bearingB = Calculations.Bearing(b_first, b_last); 
            double bearingDelta = Math.Abs(bearingA - bearingB);

            //Console.WriteLine(bearingDelta+ "        delta");

            if(bearingDelta > 30) continue;

            //Environment.Exit(0);

            // ######################################################


            Console.WriteLine("path: "+pathID);
            double alpha = 0;
            List<Coord> path = paths[pathID];
            Dictionary<int, Segment> segments =  Calculations.SampleSegments(path);

            int nrOfValidSegmentations = 0;

            for(int j  = 0 ; j < Constants.NumberOfIterations; j++)
            {
              
                segments = Calculations.SampleSegmentVelocities(segments);

                double sumTraversingTime = calcSumTraversingTime(segments) ;
                double lastTrackedTS = events[events.Count -1 ].Timestamp;
                double trackedDuration = lastTrackedTS - events[0].Timestamp;

                if(sumTraversingTime < trackedDuration) continue;

                nrOfValidSegmentations ++;

                double beta = 0;
                
                double maximumStartTime = sumTraversingTime - trackedDuration;

                double randStartTime = Constants.rnd.NextDouble() * maximumStartTime; 

                Dictionary<double, Coord> segmentCoordsAtTimestamps = Calculations.mapSegmentCoordsToTimestamps(segments, events, randStartTime);

//               Console.WriteLine("t0 is: "+randStartTime+ "    segment Coord at first ts: " + segmentCoordsAtTimestamps[segmentCoordsAtTimestamps.Keys.First()]);

                for(int eventIndex = 0 ; eventIndex < events.Count/2 ; eventIndex++){
                    Event addEvent = events[eventIndex * 2];
                    Event removeEvent = events[ eventIndex * 2 + 1];

                    Coord guessedLocation = segmentCoordsAtTimestamps[addEvent.Timestamp];
                    Coord actualPos = getActualCoordOnPath(_paths[eventFileData.SimulatedPathId], addEvent.Timestamp);

                    Device device = _fogNodes[addEvent.FogDeviceId];

                     
                    if (!Calculations.CoordIsInPolygon(guessedLocation, addEvent.ConsideredField)){
                        continue;
                    }

                    int chosenDeviceId = fastestRespondingDeviceId(guessedLocation, addEvent, removeEvent, eventFileData, fogDeviceStats);

                    if(chosenDeviceId != device.Id)
                    {
                        continue;
                    }   

                   //Console.WriteLine("chosenid = actualId"+ "    pathId: "+pathID+ "    actual path: "+eventFileData.SimulatedPathId);


                    int countOfOtherPossibleLocations = 0;

                    
                  

                    foreach( int locID in _locations.Keys)
                    {
                        Coord l = _locations[locID];

                        if(!Calculations.CoordIsInPolygon(l, addEvent.ConsideredField)) continue;

                        chosenDeviceId = fastestRespondingDeviceId(l, addEvent, removeEvent, eventFileData, fogDeviceStats);

                        if(chosenDeviceId == device.Id)
                        {
                            countOfOtherPossibleLocations ++;
                        }

                    }
                    
                    //Console.WriteLine("nach all locations loop,    count ist: "+countOfOtherPossibleLocations);

                    beta += (1 / countOfOtherPossibleLocations);          
                
                }
                
                alpha += beta;

                if(alpha > 0)
                {
                    alpha = alpha / nrOfValidSegmentations;
                    pathProbablity[pathID] = alpha;
                }
            }
        }  

        Console.WriteLine("echter Pfad: "+pathProbablity[25381]);
        Console.WriteLine(" +1 : "+pathProbablity[25381 + 1]);
        Console.WriteLine("+10: "+pathProbablity[25381 +10]);
        Console.WriteLine("+100: "+pathProbablity[25381 + 100]);
        Console.WriteLine("+1000: "+pathProbablity[25381 + 1000]);


         
        Environment.Exit(0);
        return result;
    }

      private AttackResult calcNotSlow(Dictionary<int, Coord> locations, EventFileData eventFileData, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats )
    {
        //TODO
        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        for(int i = 0 ; i < paths.Count(); i++)
        {
            double alpha = 0;
            List<Coord> path = paths[i];
            Dictionary<int, Segment> segments =  Calculations.SampleSegments(path);

        }


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
       
        return new int[] {strategy, rate, iteration};
    }


    public double calcSumTraversingTime(Dictionary<int, Segment> segments)
    {
        double counter = 0;

        foreach(Segment s in segments.Values){
            counter += s.TraversingTime;
        }

        return counter;
    }
    
    public Coord getActualCoordOnPath(List<Coord> path, double timestamp)
    {
        
        foreach(Coord c in path)
        {
            if (c.Timestamp == timestamp){
                return c;
            }
        }


        throw new Exception("getActualCoordOnPath(): no Coordinate with correct timestamp found");

        return new Coord(-1, -1, -1);
    }
    
    public int fastestRespondingDeviceId(Coord guessedLoc, Event addE, Event removeE, EventFileData eFileData,  Dictionary<int, DeviceStats> stats)
    {
        int[] consideredDeviceIDs = addE.ConsideredFogNodes;
        int fastestNodeId = 0;
        double minRt = Double.MaxValue;
        foreach (int deviceID in consideredDeviceIDs)
        {
            Device d = _fogNodes[deviceID];
            double rt = Calculations.ResponseTime(addE, removeE, d, stats[deviceID], guessedLoc);

            if(rt < minRt)
            {
                fastestNodeId = deviceID;
                minRt = rt;
            }

        }        
        return fastestNodeId;
    }


    //---------- -- Singleton --------------------

    private TrackingAttackController(){
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



class AttackResult
{
    public int strat {get;set;}
    public int rate {get;set;}
    public int iteration {get;set;}
    public double corrFullDtwDist {get;set;}
    public double corrAvgDtwDist {get; set;}
    public int nrOfObservations {get;set;}

}