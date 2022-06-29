public static class Constants
{

    public const double MaxDistance = 14143;

    public const String LocationsFilePath =@"..\json\locations_points.json";

    public const String NodeLocationsFilePath =@"..\json\locs_in_voronoy_for_node.json";

    public const String SquaresFilePath = @"..\json\10x10_squares.json";
    public const String ResultsFilePath = "./../results";

    public const String  EventsFilesDir= @"..\input";

    public const String DbPath = @"..\..\geoLifePaths.db";

    public const int LenOfSegments = 25; //25 metres

    public const double DeltaLatFullArea = 40.34910418139634 - 40.22636213689161;

    //41.229556359747455 - 39.099855;

    public const double DeltaLonFullArea = 117.22784601135815 - 117.19343285136212;

    //116.519898 - 116.32212585037877;


    public static double RandVelocity {
        get{
            return (velocitiesInKPH[rnd.Next(velocitiesInKPH.Count())] * 1000) / (60 * 60); //returns as metres/sec

        }
    }


    public static Random rnd = new Random();

    static double[] velocitiesInKPH = new double[]{4, 4, 4, 4 ,5 , 5, 5, 5, 6, 6, 7 ,7 ,7, 8, 10, 10, 10, 11, 15, 20, 20, 30, 50};

    public static int NumberOfIterations = 10000;
}