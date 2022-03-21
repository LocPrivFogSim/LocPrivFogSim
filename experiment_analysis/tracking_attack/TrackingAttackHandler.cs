
using System.Collections;
using Data;



//Singleton 
public class TrackingAttackHandler
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
        _fogNodes = connector.InitFogNodesWithPositions();
        
        JsonParser jsonParser = new JsonParser();
        _locations = jsonParser.GetLocations();

        foreach (var file in  Directory.GetFiles(Constants.EventsFilesDir, "*.json" , SearchOption.AllDirectories))
        { 
            //TODO parallel execution of following 
            AttackResult r = getAttackResultForFile(file);
            results.Add(r);
        }

    }



    private AttackResult getAttackResultForFile(String filepath)
    { 
        int [] experimentParams = getExperimentParamsFromFileName(filepath);
        int strategy = experimentParams[0];
        int rate = experimentParams[1];
        int iteration = experimentParams[2];

        JsonParser jp = new JsonParser();
        EventFileData eventFileData = jp.ParseEventFile(filepath);
        List<Event> events = eventFileData.Events;

        List<Device> devices = InitAllDevices();


        return calcAttackResults(strategy, rate, iteration, _locations, events, _paths);

    } 



    private AttackResult calcAttackResults(int strategy, int rate, int iteration, Dictionary<int, Coord> locations, List<Event> events, Dictionary<int, List<Coord>> paths )
    {
        //TODO

        return null;
    }

    //TODO  init all fognodes with data from DB, eventlog.json and locations files (-> comments)
    public List<Device> InitAllDevices()
    {
        List<Device> devices = new List<Device>();
    
        List<int> relevant_device_ids = new List<int> (); //TODO just the relevant fognodes or all fognodes?! if just relevants get it from JsonParser 

        foreach (int x in relevant_device_ids)  
        {
            int id = 0 ;//DB
            Coord position = new Coord(); //DB
            bool isCompromised = false; //event Json
            double downlinkBandwidth = 0; //event Json
            double uplinkBandwidth = 0; //event Json
            double uplinkLatency = 0; //event Json
            List<Coord> voronoiVertices = null; // node_locations.json  -> stays the same

            Device d = new Device(id, position, isCompromised, downlinkBandwidth, uplinkBandwidth, uplinkLatency, voronoiVertices);
        }
        return devices;
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

    private TrackingAttackHandler(){
    }

    private static TrackingAttackHandler _instance = new TrackingAttackHandler();

      public static TrackingAttackHandler Instance
        {
            get
            {
                if (_instance == null)
                    _instance = new TrackingAttackHandler();
                return _instance;
            }
        }


}



class AttackResult
{
    //TODO
    
}