package org.fog.localization;

import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.vmmobile.constants.Directions;
import org.fog.vmmobile.constants.MaxAndMin;

import java.util.Collections;
import java.util.List;
public class Distances {

	public Distances() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @author Marcio Moraes Lopes
	 */
	private static double coordX;
	private static double coordY;
	private static double first;
	private static double second;
	private static double distance;
	private static double theta;

	public static ApDevice theClosestAp(List<ApDevice> apDevices, MobileDevice smartThing){//verify what return type is better (int or ApDevice)

		Collections.shuffle(apDevices);

		double min= Double.MAX_VALUE;

		ApDevice closest = apDevices.get(0);

		for(ApDevice ap:apDevices){
			double dist = Coordinate.calcDistance(ap.getPosition().getCoordinate(), smartThing.getPosition().getCoordinate());

			if (dist<min){
				closest=ap;//index or id
				min=dist;
			}
		}

		if(min<=MaxAndMin.AP_COVERAGE){
			return closest;
		}
		else{
			return null;//flag error
		}
	}

	public static FogDevice theClosestServerCloudlet(List<FogDevice> serverCloudlets, MobileDevice smartThing){
		FogDevice choose=serverCloudlets.get(0);

		double min =checkDistance(serverCloudlets.get(0).getPosition().getCoordinate(), smartThing.getPosition().getCoordinate());


		for(FogDevice sc : serverCloudlets){
			//setDistance(checkDistance(sc.getCoord(), smartThing.getFutureCoord()));
			setDistance(checkDistance(sc.getPosition().getCoordinate(), smartThing.getPosition().getCoordinate()));
			//if(getDistance() != 0)
			if (getDistance()<min){
				choose=sc;//id
				min=getDistance();
			}
		}

		return choose;
	}
	public static FogDevice theClosestServerCloudletToAp(List<FogDevice> serverCloudlets, ApDevice apDevice){

		FogDevice choose=serverCloudlets.get(0);
		double min=checkDistance(serverCloudlets.get(0).getPosition().getCoordinate(), apDevice.getPosition().getCoordinate());

		for(FogDevice sc : serverCloudlets){
			setDistance(checkDistance(sc.getPosition().getCoordinate(), apDevice.getPosition().getCoordinate()));
			//if(getDistance() != 0)
			if (getDistance()<min){
				choose=sc;
				min=getDistance();
			}

		}
		return choose;

	}

	public static double findTheta(double coordX, double coordY){
		setCoordX(coordX);
		setCoordY(coordY);
		if(coordX!=0)//verify if the point is on Y axis and must not do the y/0
			setTheta(Math.atan(((double) coordY) / ((double)coordX)) * (180/Math.PI));/*Arc tangent's formula*/
		else
			setTheta(0);//the point is 
		if(coordX < 0 && coordY >= 0)
			return 180 + getTheta(); //Theta is negative, but it is first quadrant and it needs to be in second quadrant

		else if(coordX < 0 && coordY < 0)
			return 180 + getTheta();//Theta is positive and it needs to be in third quadrant

		else if(coordX > 0 && coordY < 0)
			return 360 + getTheta(); //Theta is negative and it needs to be in fourth quadrant

		else if(coordX == 0 && coordY > 0) 
			return 90 + getTheta();//Theta is zero and it needs to be on Y (positive)

		else if(coordX == 0 && coordY < 0)
			return 270 + getTheta();//Theta is zero and it needs to be on y (negative)

		else
			return getTheta(); /*theta is ok*/
	}
	public static int findPosition(double th){
		setTheta(th);
		/*find the position in 45 degree for each - make a review here*/
		if((getTheta()>=0&&getTheta()<=22.5)||(getTheta()>337.5&&getTheta()<=360)) /*for it, two time of 22.5 degree*/
			return Directions.EAST;

		else if(getTheta()>22.5&&getTheta()<=67.5)
			return Directions.SOUTHEAST;//Directions.NORTHEAST;

		else if(getTheta()>67.5&&getTheta()<=112.5)
			return Directions.SOUTH;// Directions.NORTH;

		else if(getTheta()>112.5&&getTheta()<=157.5)
			return Directions.SOUTHWEST;//Directions.NORTHWEST;

		else if(getTheta()>157.5&&getTheta()<=202.5)
			return Directions.WEST;

		else if(getTheta()>202.5&&getTheta()<=247.5)
			return Directions.NORTHWEST;//Directions.SOUTHWEST;

		else if(getTheta()>247.5&&getTheta()<=292.5)
			return Directions.NORTH;//Directions.SOUTH;

		else if(getTheta()>292.5&&getTheta()<=337.5)
			return Directions.NORTHEAST;//Directions.SOUTHEAST;

		return Directions.NONE;

	}


	public static double checkDistance(Coordinate firstCoord, Coordinate secondCoord){
		setDistance(Coordinate.calcDistance(firstCoord,secondCoord));
		return getDistance();
	}

	public static double getCoordX() {
		return coordX;
	}
	public static void setCoordX(double coordX) {
		Distances.coordX = coordX;
	}
	public static double getCoordY() {
		return coordY;
	}
	public static void setCoordY(double coordY) {
		Distances.coordY = coordY;
	}
	public static double getFirst() {
		return first;
	}
	public static void setFirst(double first) {
		Distances.first = first;
	}
	public static double getSecond() {
		return second;
	}
	public static void setSecond(double second) {
		Distances.second = second;
	}
	public static double getDistance() {
		return distance;
	}
	public static void setDistance(double distance) {
		Distances.distance = distance;
	}
	public static double getTheta() {
		return theta;
	}
	public static void setTheta(double theta) {
		Distances.theta = theta;
	}




}
