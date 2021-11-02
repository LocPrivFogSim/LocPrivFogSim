package org.fog.vmmigration;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.placement.MobileController;
import org.fog.privacy.EventType;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.Directions;
import org.fog.vmmobile.constants.Policies;

import java.io.*;
import java.util.Calendar;
import java.util.List;

public class NextStep {
//	public  int getContNextStep() {
//		return contNextStep;
//	}
//
//	public  void setContNextStep(int contNextStep) {
//		this.contNextStep = contNextStep;
//	}

//	public  int contNextStep = 0;

	private static void saveMobility(MobileDevice st){
//		Log.printLine(st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
//		Log.printLine("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
//		Log.printLine("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications() + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
//		if(st.getDestinationServerCloudlet() == null){
//			Log.printLine("Dest server: null Apps: null Map: null");
//		}
//		else{
//			Log.printLine("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() +  " Map " + st.getDestinationServerCloudlet().getApplicationMap());
//		}
		try(FileWriter fw1 = new FileWriter(st.getMyId()+"out.txt", true);
			    BufferedWriter bw1 = new BufferedWriter(fw1);
			    PrintWriter out1 = new PrintWriter(bw1))
		{
			out1.println(CloudSim.clock()+" "+ st.getMyId() + " Position: " + st.getPosition().getCoordinate().getCoordX() + ", " + st.getPosition().getCoordinate().getCoordY() + " Direction: " + st.getPosition().getDirection() + " Speed: " + st.getPosition().getSpeed());
			out1.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
			out1.println("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications() + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
			if(st.getSourceServerCloudlet() == null){
				out1.println("Source server: null Apps: null Map: null");
			}
			else{
				out1.println("Source server: " + st.getSourceServerCloudlet().getName() + " Apps: " + st.getSourceServerCloudlet().getActiveApplications() +  " Map " + st.getSourceServerCloudlet().getApplicationMap());
			}
			if(st.getDestinationServerCloudlet() == null){
				out1.println("Dest server: null Apps: null Map: null");
			}
			else{
				out1.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() +  " Map " + st.getDestinationServerCloudlet().getApplicationMap());
			}
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(FileWriter fw = new FileWriter(st.getMyId()+"route.txt", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{
			out.println(st.getMyId() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY() + "\t" + st.getPosition().getDirection() + "\t" + st.getPosition().getSpeed() + "\t" + CloudSim.clock());
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(FileWriter fw = new FileWriter(st.getMyId()+"migrationPos.txt", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{
			if(st.getSourceServerCloudlet()==null)
				out.println(st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY()+
						"\t" + CloudSim.clock() +"\t"+ st.getMigTime() + "\t" + (CloudSim.clock() + st.getMigTime()));
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(FileWriter fw = new FileWriter(st.getMyId()+"handoffPos.txt", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{
			if(st.isLockedToHandoff())
				out.println(st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY()+
						"\t" + CloudSim.clock());
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(FileWriter fw = new FileWriter(st.getMyId()+"sourceAp.txt", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{
			if(st.getSourceAp() == null)
				out.println(st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY()+
						"\t" + CloudSim.clock());
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try(FileWriter fw = new FileWriter(st.getMyId()+"sourceServerCloudlet.txt", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
		{
			if(st.getSourceServerCloudlet() == null)
				out.println(st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY()+
						"\t" + CloudSim.clock());
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(MyStatistics.getInstance().getInitialWithoutVmTime().get(st.getMyId())!=null){
			try(FileWriter fw = new FileWriter(st.getMyId()+"withoutVmTime.txt", true);
				    BufferedWriter bw = new BufferedWriter(fw);
				    PrintWriter out = new PrintWriter(bw))
			{
				if(st.getSourceServerCloudlet() == null)
					out.println(st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY()+
							"\t" + CloudSim.clock());
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static  void nextStep(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings,
								 Coordinate coordDevices, int stepPolicy, int seed, MobileController controller) {


		MobileDevice st=null;
		for(int i = 0;i<smartThings.size();i++){//It makes the new position according direction and speed
			st=smartThings.get(i);
				//String[] s = q.peek();
				//Log.printLine(q.size()+" "+s[0]+" "+ s[1]+" "+ s[2]+" "+s[3]);
			//	if((st.getSourceAp()!=null)&&
//			if(st.isStatus()&&

			// NOTE(markus): I belief the mobile device was never moving, due to TestExample2.setMobilityData()
			// Last line: coordinate.setInitialCoordinate(mobileDevice);
			// this results in setting the mobile device travle time id to -1
			// Thus we exit this loop and do not set the next step
			// Hence, I changed the method in setCoordinate.InitialCoordinate() to set travel time id to 0 instead of -1.
			if(st.getTravelTimeId()==-1){
				continue;
			}

			if((st.getPosition().getDirection() != Directions.NONE)){
				// NOTE(markus): Sets the mobile device to its next location based on its path...
				// Logic is in Coordinate.newCoordinate
				controller.updatePosition(st);
//				coordinate.newCoordinate(st, stepPolicy, coordDevices);//1 -> It means that only one step
			}
			if(st.getPosition().getCoordinate().getCoordX()==-1){

				if(st.getSourceServerCloudlet()!=null){
					int j=0,indexCloud=0;
					for(FogDevice sc:serverCloudlets){
						if(st.getSourceServerCloudlet().equals(sc)){
							indexCloud=j;
							break;
						}
						j++;
					}
					//serverCloudlets.get(st.getSourceServerCloudlet().getId()).getSmartThings().remove(st);
					serverCloudlets.get(indexCloud).getSmartThings().remove(st);

					j=0;
					int indexAp=0;
					for(ApDevice ap:apDevices){
						if(st.getSourceAp() == null){
							break;
						}
						if(st.getSourceAp().equals(ap)){
							indexAp=j;
							break;
						}
						j++;
					}
//					apDevices.get(st.getSourceAp().getId()).getSmartThing().remove(st);
					apDevices.get(indexAp).getSmartThings().remove(st);

					st.setSourceAp(null);
					st.setSourceServerCloudlet(null);

					st.setMigStatus(false);
//					setContNextStep(getContNextStep()+1);

				}
//				st.setStatus(false);
				if(st.getSourceAp()==null){
					smartThings.remove(st);
					LogMobile.debug("NextStep.java", st.getName()+" was removed!");
				}
				else{
					//caso termine logo apos um handoff. A st tera se conectado novamente ao ap mas ainda nao ocorreu a conexao st cloudlet
					if(st.getSourceServerCloudlet() != null){
						st.getSourceServerCloudlet().setSmartThings(st, st.getPosition().getTimestamp(),EventType.MIGRATION, Policies.REMOVE); //it'll remove the smartThing from serverCloudlets-smartThing's set
					}
					st.getSourceAp().setSmartThings(st, st.getPosition().getTimestamp(), EventType.MIGRATION, Policies.REMOVE);//it'll remove the smartThing from ap-smartThing's set
					LogMobile.debug("NextStep.java", st.getName()+" was removed!");
					smartThings.remove(st);
				}
			}
			else{
				Log.printLine("MobileDevice - Move: " + st.getName() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY() + "\t" + CloudSim.clock() + "\t" + Calendar.getInstance().getTime());
				saveMobility(st);
			}
		}

//		Random rand = new Random((long)(CloudSim.clock())*seed);
//		int direction=0, speed=0,exchange=0;
//
//		for(MobileDevice stt: smartThings){//It exchange the x% of the cases
//			exchange = rand.nextInt(100);// 4 -> 25%, 5 -> 20%, 10 -> 10%, 20 -> 5%, 50 -> 2% and 100 -> 1%
//
//			if(exchange == 0){
//				while(true){
//					direction = rand.nextInt(MaxAndMin.MAX_DIRECTION-1)+1;
//					if(direction!=stt.getDirection()){
//						stt.setDirection(direction);
//						LogMobile.debug("NextStep.java", stt.getName()+" has a new direction");
//						break;
//					}
//				}
//			}
//			exchange = rand.nextInt(20);// 4 -> 25%, 5 -> 20%, 10 -> 10%, 20 -> 5%, 50 -> 2% and 100 -> 1%
//			if(exchange == 0){
//				while(true){
//					speed = rand.nextInt(MaxAndMin.MAX_SPEED);
//					if(speed !=stt.getSpeed()){
//						stt.setSpeed(speed);
//						LogMobile.debug("NextStep.java", stt.getName()+" has a new speed");
//						break;
//					}
//				}
//			}
//		}



	}


	//	public  void newCoord(MobileDevice smartThing, int add){//add is the quantity of steps
	//		int direction = smartThing.getDirection();
	//		double speed = smartThing.getSpeed();
	//		int coordX = smartThing.getCoord().getCoordX();
	//		int coordY = smartThing.getCoord().getCoordY();
	//		if(direction == Directions.EAST){
	//	    	/*same Y, increase X*/
	//			smartThing.getCoord().setCoordX(coordX+((int)speed*add));
	//		}
	//		else if(direction==Directions.WEST)
	//	    	/*same Y, decrease X*/
	//			smartThing.getCoord().setCoordX(coordX-((int)speed*add));
	//	    else if(direction==Directions.NORTH)
	//	    	/*same X, increase Y*/
	//			smartThing.getCoord().setCoordY(coordY+((int)speed*add));
	//
	//	    	else if(direction==Directions.SOUTH)
	//	    	/*same X, decrease Y*/
	//			smartThing.getCoord().setCoordY(coordY-((int)speed*add));
	//	    else if(direction==Directions.NORTHEAST){
	//	    	/*increase X and Y*/
	//			smartThing.getCoord().setCoordX(coordX+((int)speed*add));
	//			smartThing.getCoord().setCoordY(coordY+((int)speed*add));
	//	    }
	//	    else if(direction==Directions.SOUTHWEST){
	//	    	/*decrease X and Y*/
	//			smartThing.getCoord().setCoordX(coordX-((int)speed*add));
	//			smartThing.getCoord().setCoordY(coordY-((int)speed*add));
	//	    }
	//	    else if(direction==Directions.NORTHWEST){
	//	    	/*decrease X increase Y*/
	//			smartThing.getCoord().setCoordX(coordX-((int)speed*add));
	//			smartThing.getCoord().setCoordY(coordY+((int)speed*add));
	//	    }
	//	    else if(direction==Directions.SOUTHEAST){
	//	    	/*increase X decrease Y*/
	//			smartThing.getCoord().setCoordX(coordX+((int)speed*add));
	//			smartThing.getCoord().setCoordY(coordY-((int)speed*add));
	//	    }
	//
	//	}
	//




}
