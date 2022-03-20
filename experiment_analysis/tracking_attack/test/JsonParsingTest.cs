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
}