package org.fog.vmmigration;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.vmmobile.constants.Services;

public class ServiceAgreement {
	private static int serviceType;
	private static float serviceValue;

	public static boolean serviceAgreement(FogDevice serverCloudlet, MobileDevice smartThing){
		setServiceType(serverCloudlet.getService().getType());

		if(!checkLinkStatus(smartThing.getVmLocalServerCloudlet(), serverCloudlet)){
//				Log.printLine("Link between ServerCloudlets is down");
			return false;
		}
		else if(!serverCloudlet.isAvailable()){ //to define some policy for this available!!!
//					Log.printLine("ServerCloudlet is not available: "+serverCloudlet);
			return false;//no migration
		}
		else if(getServiceType() == Services.PRIVATE){
			smartThing.setDestinationServerCloudlet(serverCloudlet);//serverCloudlets.get(getNextServerClouletId()));//it saves the destination serverCloudlet
//			Log.printLine("Service is Private");
			return true;
		}
		else if(getServiceType() == Services.HIBRID){//it needs to define the policy
			smartThing.setDestinationServerCloudlet(serverCloudlet);
//							Log.printLine("Service is Hibrid");
			return true;
		}
		else if(getServiceType() == Services.PUBLIC){
			setServiceValue(serverCloudlet.getService().getValue());
			if(getServiceValue() <= smartThing.getMaxServiceValue()){
				smartThing.setDestinationServerCloudlet(serverCloudlet);
//				Log.printLine("Service is Public");
				return true; //the smartThing agrees
			}
			else{
						Log.printLine("The value is expensive for the "+serverCloudlet.getName());
						Log.printLine(smartThing.getName()+": Source "+smartThing.getSourceServerCloudlet().getName()+
								" - LocalVm "+smartThing.getVmLocalServerCloudlet().getName());
				return false;
			}
		}
		else{
			Log.printLine("ServiceAgreement.java - Nao pode passar aqui!");
			System.exit(0);
			return false;
		}
	}
	public static boolean checkLinkStatus (FogDevice sourceServerCloudlet, FogDevice destinationServerCloudlet){

		if(sourceServerCloudlet.getNetServerCloudlets().get(destinationServerCloudlet)!=null){
			return true;
		}
		else{
			return false;
		}
	}
	public static int getServiceType() {
		return serviceType;
	}

	public static void setServiceType(int serviceType) {
		ServiceAgreement.serviceType = serviceType;
	}

	public static float getServiceValue() {
		return serviceValue;
	}

	public static void setServiceValue(float serviceValue) {
		ServiceAgreement.serviceValue = serviceValue;
	}

}