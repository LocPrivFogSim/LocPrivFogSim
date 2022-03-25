public static class Constants
{

    public const double MaxDistance = 14143; 

    public const String LocationsFilePath =@"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\json\locations_points.json";

    public const String NodeLocationsFilePath =@"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\json\node_locations.json";
    
    public const String ResultsFilePath = "./.."; 

    public const String  EventsFilesDir= @"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\input"; 

    public const String DbPath = @"C:\Users\lspie\Desktop\LocPrivFogSim\geoLifePaths.db";

    public const int LenOfSegments = 25; //25 metres

    public static double RandVelocity {
        get{
            return (velocitiesInKPH[rnd.Next(velocitiesInKPH.Count())] * 60) / (60 * 60); //returns as metres/sec

        }
    }

    static Random rnd = new Random();

    static double[] velocitiesInKPH = new double[]{4, 4, 4, 4 ,5 , 5, 5, 5, 6, 6, 7 ,7 ,7, 8, 10, 10, 10, 11, 15, 20, 20, 30, 50}; 

    public static int NumberOfIterations = 15;
}