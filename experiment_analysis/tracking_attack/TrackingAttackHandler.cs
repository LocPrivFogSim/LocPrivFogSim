
using System.Collections;
using Data;


//Singleton 
public class TrackingAttackHandler
{

    public List<Coord> Locations { get;}

    Dictionary<int, List<Coord>> paths { get; set;}
    

    public void RunTrackingAttack()
    {   
        ArrayList results = ArrayList.Synchronized(new ArrayList());
        

        paths =  new DB_Connector().GetAllPaths();

        //TODO set Locations

        foreach (var file in 
        Directory.GetFiles(Constants.EventsFilesDir, "*.json" , SearchOption.AllDirectories))
        { 
            //TODO parallel execution of following 
            AttackResult r = getAttackResultForFile(file);
            results.Add(r);


        }

    }



    private AttackResult getAttackResultForFile(String filepath)
    {
     //example: C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\input\Test\output_3_20_13.json
    
    //TODO parse from File name
    int strategy = 0;
    int rate = 0;
    int iteration = 0;
    List<Event> events = JsonPaser.GetEvents(filepath);
    List<Device> devices = InitAllDevices();


    return calcAttackResults(strategy, rate, iteration, Locations, events, paths);

    } 



    private AttackResult calcAttackResults(int strategy, int rate, int iteration, List<Coord> locations, List<Event> events, Dictionary<int, List<Coord>> paths )
    {



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




}