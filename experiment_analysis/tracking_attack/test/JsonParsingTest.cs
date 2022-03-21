using Xunit;


    
public class JsonParsingTest
{
    [Fact]
    public void TestLocationParsing()
    {
        JsonParser jp = new JsonParser();
        var c1 = jp.GetLocations()[0];
        
         //first location is [39.75667, 115.13559, 0.0]   
        Coord c2 = new Coord(39.75667, 115.13559, -1);
         Assert.True(c1.Lat == c2.Lat );
    }


    [Fact]
    public void TestEventParsing()
    {

        string path = @"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\input\Strategie_2\output_2_10_1.json";

        /* should be

            path id == 36841
            compromisedFogNodes[0] == 28
            downlink_bandwidth of fog_device 0   == 585.0   
            first event with ts==4 and consideredFogNodes[0] == 1830 
        */

        

        
    }




}