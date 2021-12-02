package org.fog.localization;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.fog.vmmobile.constants.Directions;
import org.fog.vmmobile.constants.MaxAndMin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

//
//TODO remove euclidien coords 
//
public class Coordinate {

	private double coordX;
	private double coordY;
	private double lat;
	private double lon;
	private boolean isGPSCoordinate = false;


	public static Coordinate createGPSCoordinate(double lat, double lon){
		Coordinate c = new Coordinate();
		c.setLat(lat);
		c.setLon(lon);
		c.setGPSCoordinate(true);
		return c;
	}

	public static double calcDistance(Coordinate c1, Coordinate c2){
		if(c1.isGPSCoordinate){
			return calcHaversineDist(c1, c2);
		}
		else{
			return calcEuclidDist(c1,c2);
		}
	}

	public static Coordinate createCartesianCoordinate(double coordX, double coordY){
		Coordinate c = new Coordinate();
		c.setCoordX(coordX);
		c.setCoordY(coordY);
		c.setGPSCoordinate(false);
		return c;
	}


	private static double calcEuclidDist(Coordinate c1, Coordinate c2){
		 double first =  Math.pow((c1.coordX-c2.coordX),2.0);
		 double second =  Math.pow((c1.coordY-c2.coordY),2.0);

		return  Math.sqrt((first+second));
	}

	/**
	 *
	 * @return haversine Distance in metres
	 */
	private static double calcHaversineDist(Coordinate c1, Coordinate c2){

		if(c1 == null || c2 == null){
			return 0;
		}

		double lat1 = Math.toRadians( c1.getLat());
		double lat2 = Math.toRadians(c2.getLat());
		double lon1 = Math.toRadians(c1.getLon());
		double lon2 = Math.toRadians(c2.getLon());

		double deltaLat = lat2 - lat1;
		double deltaLon = lon2 - lon1;

		double firstPart =  Math.pow(Math.sin(deltaLat/2),2) +
				Math.cos(lat1) * Math.cos(lat2)
						* Math.pow(  Math.sin(deltaLon/2),2);
		double secondPart =2 * Math.asin(Math.sqrt(firstPart));
		double earthRadius = 6371.0;
		double distInMetres = secondPart * earthRadius * 1000;


		return distInMetres;
	}

	public static double calcBearingAngle(Coordinate c1, Coordinate c2, boolean asDegrees){
		if (!c1.isGPSCoordinate || !c2.isGPSCoordinate){
			//todo
			return  0;
		}

		double lat1 = Math.toRadians(c1.getLat());
		double lat2 = Math.toRadians(c2.getLat());

		double deltaLong = Math.toRadians(c2.getLon() - c1.getLon());

		double angle =  Math.atan2(Math.sin(deltaLong) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) -
						Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLong));

		if (asDegrees){
			return Math.toDegrees((angle+Math.PI) % Math.PI);
		}
		return angle;
	}

	//https://stackoverflow.com/questions/7222382/get-lat-long-given-current-point-distance-and-bearing
	//distance in m
	public static Coordinate findCoordinateForBearingAndDistance(Coordinate c, double bearingAngleAsRadian, double distance){
		double earthRadius = 6371.0;
		double latRad = Math.toRadians(c.getLat());
		double lonRad = Math.toRadians(c.getLon());
		distance = distance/1000;

		double destinyLat =Math.asin(Math.sin(latRad) * Math.cos(distance / earthRadius) + Math.cos(latRad) * Math.sin(distance / earthRadius) * Math.cos(bearingAngleAsRadian));
		lonRad += Math.atan2(Math.sin(bearingAngleAsRadian) * Math.sin(distance / earthRadius) * Math.cos(latRad), Math.cos(distance / earthRadius) - Math.sin(latRad) * Math.sin(latRad));
		return  Coordinate.createGPSCoordinate( (destinyLat * 180)/Math.PI , (lonRad * 180 / Math.PI));
	}

	public static Coordinate newCoordinateWithError(Coordinate coord, int mobilityPredictionError, int direction) {

		double x = coord.getCoordX(), y=coord.getCoordY();


		if (direction == Directions.EAST){
			x += mobilityPredictionError;
		}
		else if (direction == Directions.NORTHEAST){
			x += mobilityPredictionError;
			y -= mobilityPredictionError;
		}
		else if (direction == Directions.NORTH){
			y -= mobilityPredictionError;
		}
		else if (direction == Directions.NORTHWEST){
			x -= mobilityPredictionError;
			y -= mobilityPredictionError;
		}
		else if (direction == Directions.WEST){
			x -= mobilityPredictionError;
		}
		else if (direction == Directions.SOUTHWEST){
			x -= mobilityPredictionError;
			y += mobilityPredictionError;
		}
		else if (direction == Directions.SOUTH){
			y += mobilityPredictionError;
		}
		else{
			x += mobilityPredictionError;
			y += mobilityPredictionError;
		}

		if (x<0)
			x = 0;
		if (y<0)
			y = 0;
		if (x>=MaxAndMin.MAX_X)
			x=MaxAndMin.MAX_X;
		if (y>=MaxAndMin.MAX_Y)
			y=MaxAndMin.MAX_Y;

		Coordinate coord_result = new Coordinate();
		coord_result.setCoordX(x);
		coord_result.setCoordY(y);
		return coord_result;
	}

	/**
	 *
	 * @param coord start coord
	 * @param distance offset distance in metres
	 * @return lat after offset
	 */
	public static double getOffsetLat(Coordinate coord, double distance){
		double earthRad = 6378137;
		return coord.getLat()+ (distance/earthRad) * (180/Math.PI);
	}



	/**
	 *
	 * @param coord start coord
	 * @param distance offset distance in metres
	 * @return lon after offset
	 */
	public static double getOffsetLon(Coordinate coord, double distance){
		double earthRad = 6378137;
		return  coord.getLon()+ (distance /  (earthRad * Math.cos(Math.PI * coord.getLat() /  180 )) ) * (180/Math.PI);
	}

	public boolean isInBoundingBox (double minLat, double maxLat, double minLon, double maxLon){
		return this.lat >= minLat && this.lat <= maxLat && this.lon >= minLon && this.lon <= maxLon;
	}


	public double getCoordX() {
		return coordX;
	}

	public void setCoordX(double coordX) {
		this.coordX = coordX;
	}

	public double getCoordY() {
		return coordY;
	}

	public void setCoordY(double coordY) {
		this.coordY = coordY;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	@Override
	public String toString() {
		return "Coordinate{" +
				"coordX=" + coordX +
				", coordY=" + coordY +
				", lat=" + lat +
				", lon=" + lon +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Coordinate that = (Coordinate) o;
		return coordX == that.coordX &&
				coordY == that.coordY && this.lat == that.lat && this.lon == that.lat;
	}

	@Override
	public int hashCode() {
		return Objects.hash(coordX, coordY, lat, lon);
	}

	public boolean isGPSCoordinate() {
		return isGPSCoordinate;
	}

	public void setGPSCoordinate(boolean GPSCoordinate) {
		isGPSCoordinate = GPSCoordinate;
	}

	/**
	 * @return negative if v1 is counterclockwise to v2, positive if v1 is clockwise to v2
	 */
	public static double cross2D(Vector2D v1, Vector2D v2) {
		double c2D = v1.getX() * v2.getY() - v1.getY() * v2.getX();
		return c2D;
	}

	/**
	 *
	 * @param corners  <- clockwise sorted corners
	 */
	public static boolean coordIsInField(List<Coordinate> corners, Coordinate coordToCompare) {

		HashMap<Coordinate, Vector2D> vectorMap = new HashMap<>();

		//System.out.println("corners: "+corners);

		//add vectors (n -> n+1)
		for (int i = 0; i < corners.size(); i++) {
			Coordinate current = corners.get(i);
			Coordinate next;
			if (i + 1 == corners.size()) {
				next = corners.get(0);
			} else {
				next = corners.get(i + 1);
			}
			Vector2D v = new Vector2D(next.getLon() - current.getLon(), next.getLat() - current.getLat());
			vectorMap.put(current, v);
		}

		//if point is in, it has to be to the right of each vector in map -> cross has to be > 0 for each
		boolean isAlwaysToTheRight = true;

		for (Coordinate coordinate : vectorMap.keySet()) {
			Vector2D tmp = new Vector2D(coordToCompare.getLon() - coordinate.getLon(), coordToCompare.getLat() - coordinate.getLat());
			double cross = cross2D(tmp, vectorMap.get(coordinate));

			//System.out.println("hi");
			if (cross < 0) {
				isAlwaysToTheRight = false;
				break;
			}
		}
		return isAlwaysToTheRight;
	}
}

