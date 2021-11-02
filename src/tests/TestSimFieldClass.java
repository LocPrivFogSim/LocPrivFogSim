package tests;

import org.fog.localization.Coordinate;
import org.fog.localization.SimField;
import org.junit.*;


import java.util.*;

public class TestSimFieldClass {

    SimField simField;


    @Before
    public void initSimField(){
    }

    @Test
    public void testBearing(){
        Coordinate c1 = Coordinate.createGPSCoordinate(51.158296458839814, 8.17532241879791);
        Coordinate c2 = Coordinate.createGPSCoordinate(51.05352442210859, 8.31560665824564);

        double bearingAngle = Coordinate.calcBearingAngle(c1,c2,false);

        Coordinate c3 = Coordinate.createGPSCoordinate(51.23338371077388, 8.040537039672849);

        Coordinate dest = Coordinate.findCoordinateForBearingAndDistance(c3, bearingAngle, 10000);
        System.out.println(dest);

    }

    @Test
    public void testEquals(){
        LinkedList<Integer> a = new LinkedList<>();
        a.add(1);
        a.add(2);
        a.add(3);

        LinkedList<Integer> b = new LinkedList<>();
        b.add(1);
        b.add(2);

        System.out.println(b.equals(a));

    }

    @Test
    public void testGridCreation(){


        Coordinate bottomLeft = Coordinate.createGPSCoordinate(39.75667, 115.13559);
        Coordinate bottomRight= Coordinate.createGPSCoordinate(39.099855, 116.519898);
        Coordinate topLeft = Coordinate.createGPSCoordinate(41.229556359747455, 116.32212585037877);

        }

    @Test
    public void testGridLoading(){


    }


 /*
    @Test
    public void testSortingClockwise(){
        ArrayList<Coordinate> test = new ArrayList<>();
        test.add(new Coordinate(4,7));
        test.add(new Coordinate(1,1));
        test.add(new Coordinate(3,7));
        test.add(new Coordinate(6,6));
        test.add(new Coordinate(-1,5));
        test.add(new Coordinate(5, (float) 2.5));



        simField = new SimField(test);


        ArrayList<Coordinate> manuallySorted = new ArrayList<>();
        manuallySorted.add(new Coordinate(1,1));
        manuallySorted.add(new Coordinate(-1,5));
        manuallySorted.add(new Coordinate(3,7));
        manuallySorted.add(new Coordinate(4,7));
        manuallySorted.add(new Coordinate(6,6));
        manuallySorted.add(new Coordinate(5,(float)2.5));



        System.out.println(manuallySorted+" manually sorted");
        System.out.println(simField.sortCornersClockwise(simField.getCorners())+" sorted by function");

        assertEquals(manuallySorted, simField.sortCornersClockwise(simField.getCorners()));

    }

    @Test
    public void testIsInField(){

        Coordinate isIn = new Coordinate(1,3);
        Coordinate notIn = new Coordinate(-1,0);


        assertEquals(simField.coordIsInField(isIn), true);
        assertEquals(simField.coordIsInField(notIn), false);

    }

    @Test
    public void testCaldDistBetweenCoordinates(){
        Coordinate c1 = new Coordinate(0,0);
        Coordinate c2 = new Coordinate(0,3);
        Coordinate c3 = new Coordinate(1,1);

        double dist = Coordinate.calcEuclidDist(c1, c2);
        double dist2 = Coordinate.calcEuclidDist(c1,c3);

        System.out.println("first: "+dist+ "   second: "+dist2);
    } */

}

