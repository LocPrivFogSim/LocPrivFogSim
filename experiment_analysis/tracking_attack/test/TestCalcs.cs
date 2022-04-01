 using Xunit;
 
 public class TestCalculations
{
    [Fact]
    public void TestCalcDistanceInMetres()
    {
        Coord c1 = new Coord(51.04737651139436, 7.885147458579185, -1);
        
        Coord c2 = new Coord(51.0465009213328, 7.884063157752519, -1);  //Distance c1 to c2 is ca 123 metres
        double dist_c1_c2 = Calculations.CalcDistanceInMetres(c1, c2);

        Coord c3 = new Coord(51.04740196406515, 7.857979607060225, -1); //Distance c1 to c3 ca 1900 metres
        double dist_c1_c3 = Calculations.CalcDistanceInMetres(c1, c3);

        Coord c4 = new Coord(51.055846024180944, 7.725617941463685, -1); //Distance c1 to c4 ca 11190 metres
        double dist_c1_c4 = Calculations.CalcDistanceInMetres(c1, c4);

        
        Assert.True( Math.Abs(dist_c1_c2 - 123) < 1);
        Assert.True( Math.Abs(dist_c1_c3 - 1900) < 1);
        Assert.True( Math.Abs(dist_c1_c4 - 11190) < 1);
        
    }

    [Fact]
     public void TestCoordIsInPolygon()
    {
        List<Coord> poly = new List<Coord>( new Coord[] {
            new Coord(50.9763375260645, 7.6424811325967505, -1),
            new Coord(50.98948921708571, 7.6235130717907955, -1),
            new Coord(50.994079541032, 7.638779227264003, -1),
            new Coord(50.98607843920678, 7.639409472114132, -1),
            new Coord(50.98690446540096, 7.650592757906544, -1),
        });

        

        Coord isIn1 = new Coord(50.9890015488522, 7.632687680014726, -1);
        Coord isIn2 = new Coord(50.98684073942077, 7.65041363473669, -1);
        Coord isIn3 = new Coord (50.98606711520583, 7.6394136942123785, -1);
        Coord isIn4 = new Coord(50.98948710878243, 7.6235379085578225, -1);

        Coord isOut1 = new Coord (50.98967861747906, 7.621207856666457, -1);
        Coord isOut2 = new Coord(50.98829272273967, 7.625185795373152 , -1);
        Coord isOut3 = new Coord(50.98612618760002, 7.63945886454669, -1);
        Coord isOut4 = new Coord(50.976243916127125, 7.642377295852217, -1);

        Assert.True(Calculations.CoordIsInPolygon(isIn1, poly));
        Assert.True(Calculations.CoordIsInPolygon(isIn2, poly));
        Assert.True(Calculations.CoordIsInPolygon(isIn3, poly));
        Assert.True(Calculations.CoordIsInPolygon(isIn4, poly));

        Assert.False(Calculations.CoordIsInPolygon(isOut1, poly));
        Assert.False(Calculations.CoordIsInPolygon(isOut2, poly));
        Assert.False(Calculations.CoordIsInPolygon(isOut3, poly));
        Assert.False(Calculations.CoordIsInPolygon(isOut4, poly));

    }


    [Fact]
     public void TestDtw()
    {
        List<Coord> pathA = new List<Coord>(new Coord[] {
            new Coord(1.0, 1.0 , -1),
            new Coord(5.0, 3.0 , -1),
            new Coord(2.0, 2.0 , -1),
            new Coord(10.0, 8.0 , -1),
        });

        List<Coord> pathB = new List<Coord>(new Coord[] {
            new Coord(2.0, 1.0 , -1),
            new Coord(6.0, 1.0 , -1),
            new Coord(6.0, 6.0 , -1),
            new Coord(10.0, 6.0 , -1),
        });

        var x = Calculations.DtwMatrix(pathA, pathB);  
        int rowLength = x.GetLength(0);
        int colLength = x.GetLength(1);
        for (int i = 0; i < rowLength; i++)
        {
            for (int j = 0; j < colLength; j++)
            {
                Console.Write(string.Format("{0} | ", x[i, j]));
            }
            Console.Write(Environment.NewLine + Environment.NewLine);
        }

        var y = Calculations.DtwWarpingPath(x);
        foreach (var s in y)
        {
            Console.WriteLine(s[0]+"   "+s[1]);
        }
    }
    

    [Fact]
    public void TestTargetCoordOnLine()
    {
        Coord c1 = new Coord (1.0, 1.0, -1);
        Coord c2 = new Coord (2.0, 2.0, -1);

        var t1 =  Calculations.TargetCoordOnLine(c1,c2, 0.5);
        var t2 =  Calculations.TargetCoordOnLine(c1,c2, 0.75);
        var t3 =  Calculations.TargetCoordOnLine(c1,c2, 0);

        Assert.True(t1.Lat == 1.5 && t1.Lon == 1.5);
        Assert.True(t2.Lat == 1.75 && t2.Lon == 1.75);
        Assert.True(t3.Lat == 1.0 && t3.Lon == 1.0);
    }


    [Fact]
    public void TestMapSegmentsToTimestampCoords()
    {
        //TODO
        // needed: segements for path, events from a file

        double t0 = 20;





    }



    //helper Method

    public void printGPXToFile(List<Coord> coords, string filename)
    {
        string beginning = "<gpx>\n<trk>\n<trkseg>\n";
        string end = "</trkseg>\n</trk>\n</gpx>";

        string coordsString = "";

        foreach(Coord c in coords)
        {
            string lat =  c.Lat.ToString();
            string lon =  c.Lon.ToString();

            coordsString += "<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\"> </trkpt>\n";
        }

        string fullGPX = beginning + coordsString + end;

        File.WriteAllText(filename, fullGPX);
    }


}