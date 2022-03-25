
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
        List<Event> events = eventFileData.Events;

        Dictionary<int, DeviceStats> fogDeviceStats = getDeviceStats(eventFileData);
        AttackResult result = null;

        switch (strategy)
        {
            case 1:
                result =  calcNotSlow(_locations, events, _paths, _fogNodes, fogDeviceStats);
                break;
            case 2:
                result =  calcFastest(_locations, events, _paths, _fogNodes, fogDeviceStats);
                break;
            case 3:
                result =  calcClosest(_locations, events, _paths, _fogNodes);
                break;
        }

        result.strat = strategy;
        result.rate = rate;
        result.iteration = iteration;
        
        
        return result;        
    } 



    private AttackResult calcClosest(Dictionary<int, Coord> locations, List<Event> events, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes )
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

      private AttackResult calcFastest(Dictionary<int, Coord> locations, List<Event> events, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats )
    {
        //TODO
        AttackResult result = new AttackResult();

        Dictionary<int, double> pathProbablity = new Dictionary<int, double>();

        for(int i = 0 ; i < paths.Count(); i++)
        {
            double alpha = 0;
            List<Coord> path = paths[i];
            Dictionary<int, Segment> segments =  Calculations.SampleSegments(path);


            for(int i  = 0 ; i < Constants.NumberOfIterations; i++)
            {
                segments = Calculations.SampleSegmentVelocities(segments);

                double sumTraversingTime = 0 ;
                

            }


        }


        return result;
    }

      private AttackResult calcNotSlow(Dictionary<int, Coord> locations, List<Event> events, Dictionary<int, List<Coord>> paths, Dictionary<int, Device> fogNodes, Dictionary<int, DeviceStats> fogDeviceStats )
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