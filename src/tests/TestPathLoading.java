package tests;



import org.fog.localization.Coordinate;
import org.fog.utils.DBConnector;
import org.junit.*;

import java.util.LinkedList;
import java.util.List;


public class TestPathLoading {

    DBConnector loader;

    @Before
    public void init(){
        loader = new DBConnector();
    }

    @Test
    public void testLoad(){
        loader.getPathById(1);
        loader.getPathsWithingBorders(38, 50,100, 120);
    }

    @Test
    public void testLoadingFogNodes (){
    }

    @Test
    public void setFogNodePositions(){

        Coordinate bl = Coordinate.createGPSCoordinate(39.75667, 115.13559);
        Coordinate br = Coordinate.createGPSCoordinate(39.099855, 116.519898);
        Coordinate tl = Coordinate.createGPSCoordinate(41.229556359747455, 116.32212585037877);
        double sizeX = Coordinate.calcDistance(bl,br)/1000;
        double sizeY = Coordinate.calcDistance(bl, tl)/1000;
        double intervall = 1;
        double distToEdges = 0.5;
        double maxShift = 0.5;


        loader.generateRandomFogNodePositionInDB(bl, br , tl, sizeX, sizeY, intervall, distToEdges,maxShift);
    }

    @Test
    public void testAproxOffset(){
        Coordinate bl = Coordinate.createGPSCoordinate(39.75667, 115.13559);
        double earthRad = 6378137;
        double minLatafterOffset = bl.getLat() + (-2000/earthRad) * (180/Math.PI);


        double minLonAfterOffset = bl.getLon()+ (-2000 /  (earthRad * Math.cos(Math.PI * bl.getLat() /  180    )) ) * (180/Math.PI);

        System.out.println(minLonAfterOffset);
        System.out.println(minLatafterOffset);


    }


    @Test
    public void testingStuff(){

        LinkedList<Integer> trace = new LinkedList<>();
        trace.addAll(List.of(1,4,1,8,7));


        LinkedList<Integer> compar = new LinkedList<>();
        compar.addAll(List.of(1,1, 4, 4, 8, 4,1 ,1,8,8,8,7));



            int indexA = 0;
            int indexB = 0;

            while(indexA < compar.size() && indexB < trace.size()){

                int checkedId = compar.get(indexA);

                if (checkedId ==  trace.get(indexB)){
                    indexB++;
                }
                indexA++;
                System.out.println("a: "+indexA+ "    b:"+indexB);

            }

        System.out.println("aa: "+indexB);


    }
}
