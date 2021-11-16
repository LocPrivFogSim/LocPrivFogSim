package org.fog.entities;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.localization.Coordinate;
import org.fog.localization.Path;
import org.fog.offloading.IOffloadingResponseTimeCalculator;
import org.fog.offloading.IOffloadingScheduler;
import org.fog.offloading.IOffloadingStrategy;
import org.fog.offloading.OffloadingEvents;
import org.fog.offloading.OffloadingTask;
import org.fog.placement.MobileController;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.privacy.Position;
import org.fog.privacy.User;
import org.fog.utils.*;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.VmMigrationTechnique;
import org.fog.vmmobile.constants.Directions;
import org.fog.vmmobile.constants.MaxAndMin;

import java.io.*;
import java.util.*;

public class MobileDevice extends FogDevice {


	private User deviceUser;
	protected Coordinate futureCoord;// = new Coordinate();//myiFogSim
	private Path path;
	private FogDevice sourceServerCloudlet;
	private FogDevice destinationServerCloudlet;
	private FogDevice vmLocalServerCloudlet;
	private ApDevice sourceAp;
	private ApDevice destinationAp;
	private Vm vmMobileDevice;
	private double migTime;
	private boolean migPoint;
	private boolean migZone;
	private Set<MobileSensor> sensors;//Set of Sensors
	private Set<MobileActuator> actuators;//Set of Actuators
	private float maxServiceValue;
	private boolean migStatus;
	private boolean postCopyStatus;
	private boolean handoffStatus;
	private boolean lockedToHandoff;
	private boolean lockedToMigration;
	private boolean abortMigration;
	private double vmSize;
	private double tempSimulation;
	private double timeFinishHandoff = 0;
	private double timeFinishDeliveryVm = 0;
	private double timeStartLiveMigration = 0;
	private boolean status;
	private boolean migStatusLive;

	private IOffloadingScheduler offloadingScheduler;
	private IOffloadingStrategy offloadingStrategy;
	private IOffloadingResponseTimeCalculator offloadingResponseTimeCalculator;

	protected VmMigrationTechnique migrationTechnique;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.getName() == null) ? 0 : this.getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MobileDevice other = (MobileDevice) obj;
		if (this.getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!this.getName().equals(other.getName()))
			return false;
		return true;
	}

	public MobileDevice() {
		// TODO Auto-generated constructor stub
	}



	public MobileDevice(String name, Coordinate coord, Position position, int id) {
		// TODO Auto-generated constructor stub
		super(name, position, id);

		deviceUser = new User("userOf"+name);
		position.setDirection(0);
		position.setSpeed(0);
		//	setDistanceAp(0);
		setSourceServerCloudlet(null);
		setDestinationServerCloudlet(null);
		setVmLocalServerCloudlet(null);
		setSourceAp(null);
		setDestinationAp(null);
		setVmMobileDevice(null);
		setMigTime(0);
		setMigStatus(false);
		setMigStatusLive(false);
		setPostCopyStatus(false);
		setHandoffStatus(false);
		setLockedToHandoff(false);
		setLockedToMigration(false);
		setAbortMigration(false);
		setMigPoint(false);
		setMigZone(false);
		actuators = new HashSet<>();
		sensors = new HashSet<>();
		setStatus(true);
		this.futureCoord = new Coordinate();
		setFutureCoord(-1,-1);

	}
	public MobileDevice(String name, Position position, int id, int dir, int sp) {
		//			public MobileDevice(String name, Coordinate coord, int coordX, int coordY, int id, int dir, int sp) {

		// TODO Auto-generated constructor stub
		//		super(name, coord, coordX, coordY, id);
		super(name, position, id);

		deviceUser = new User("userOf"+name);
		position = new Position();
		position.setDirection(dir);
		position.setSpeed(sp);
		//	setDistanceAp(0);
		setSourceServerCloudlet(null);
		setDestinationServerCloudlet(null);
		setVmLocalServerCloudlet(null);
		setSourceAp(null);
		setDestinationAp(null);
		setVmMobileDevice(null);
		setMigTime(0);
		setMigStatus(false);
		setMigStatusLive(false);
		setPostCopyStatus(false);

		setHandoffStatus(false);
		setLockedToHandoff(false);
		setLockedToMigration(false);
		setStatus(true);
		setAbortMigration(false);
		this.futureCoord = new Coordinate();
		setFutureCoord(-1,-1);

		actuators = new HashSet<>();
		sensors = new HashSet<>();


	}


	public MobileDevice(String name,
						FogDeviceCharacteristics characteristics,
						AppModuleAllocationPolicy vmAllocationPolicy,
						LinkedList<Storage> storageList, double schedulingInterval, double uplinkBandwidth
			, double downlinkBandwidth, double uplinkLatency,
						double d, Position position, int id, float maxServiceValue, double vmSize
			, VmMigrationTechnique migrationTechnique) throws Exception {
		// TODO Auto-generated constructor stub
		super(name, characteristics, vmAllocationPolicy
				, storageList, schedulingInterval
				, uplinkBandwidth
				, downlinkBandwidth
				, uplinkLatency, position.getSpeed(), position.getCoordinate(), id);

		deviceUser = new User("userOf"+name);
		this.position = position;
		//		setDistanceAp(0);
		setSourceServerCloudlet(null);
		setDestinationServerCloudlet(null);
		setVmLocalServerCloudlet(null);
		setSourceAp(null);
		setDestinationAp(null);
		setVmMobileDevice(null);
		setMigTime(0);
		setMigStatus(false);
		setMigStatusLive(false);
		setPostCopyStatus(false);

		setVmSize(vmSize);
		setHandoffStatus(false);
		setLockedToHandoff(false);
		setStatus(true);
		setAbortMigration(false);
		setMigrationTechnique(migrationTechnique);
		this.futureCoord = new Coordinate();
		setFutureCoord(-1,-1);
		actuators = new HashSet<>();
		sensors = new HashSet<>();
		setMaxServiceValue(maxServiceValue);
	}


	public MobileDevice(String name,
			FogDeviceCharacteristics characteristics,
			AppModuleAllocationPolicy vmAllocationPolicy,
			LinkedList<Storage> storageList, double schedulingInterval, double uplinkBandwidth
			, double downlinkBandwidth, double uplinkLatency,
			double d, Position position, int id, float maxServiceValue, double vmSize
			, VmMigrationTechnique migrationTechnique,
			IOffloadingScheduler offloadingScheduler,
			IOffloadingStrategy offloadingStrategy,
			IOffloadingResponseTimeCalculator responseTimeCalculator) throws Exception {
		// TODO Auto-generated constructor stub
		super(name, characteristics, vmAllocationPolicy
				, storageList, schedulingInterval
				, uplinkBandwidth
				, downlinkBandwidth
				, uplinkLatency, position.getSpeed(), position.getCoordinate(), id);

		deviceUser = new User("userOf"+name);
		this.position = position;

		//		setDistanceAp(0);
		setSourceServerCloudlet(null);
		setDestinationServerCloudlet(null);
		setVmLocalServerCloudlet(null);
		setSourceAp(null);
		setDestinationAp(null);
		setVmMobileDevice(null);
		setMigTime(0);
		setMigStatus(false);
		setMigStatusLive(false);
		setPostCopyStatus(false);

		setVmSize(vmSize);
		setHandoffStatus(false);
		setLockedToHandoff(false);
		setStatus(true);
		setAbortMigration(false);
		setMigrationTechnique(migrationTechnique);
		this.futureCoord = new Coordinate();
		setFutureCoord(-1,-1);
		actuators = new HashSet<>();
		sensors = new HashSet<>();
		setMaxServiceValue(maxServiceValue);
		setOffloadingScheduler(offloadingScheduler);
		setOffloadingStrategy(offloadingStrategy);
		setOffloadingResponseTimeCalculator(responseTimeCalculator);
	}


	public static double radiansToDegree (Double direction){

		double degree = direction*(180/Math.PI);

		if (degree < 0)
			degree += 360;

		return degree;
	}

	public static int convertDirection(Double direction){

		double degree = radiansToDegree(direction);

		if (degree > 337.5 || degree <= 22.5)
			return Directions.EAST;
		else if (degree > 22.5 && degree <= 67.5)
			return Directions.NORTHEAST;
		else if (degree > 67.5 && degree <= 112.5)
			return Directions.NORTH;
		else if (degree > 112.5 && degree <= 157.5)
			return Directions.NORTHWEST;
		else if (degree > 157.5 && degree <= 202.5)
			return Directions.WEST;
		else if (degree > 202.5 && degree <= 247.5)
			return Directions.SOUTHWEST;
		else if (degree > 247.5 && degree <= 292.5)
			return Directions.SOUTH;
		else
			return Directions.SOUTHEAST;
	}




	public  void newCoordinate(MobileDevice smartThing, int add, Coordinate coordDevices){//(pointUSER user,float add)

		if(smartThing.getPosition().getSpeed()!=0){
			double increaseX= (smartThing.getPosition().getCoordinate().getCoordX()+(smartThing.getPosition().getSpeed()*add));
			double increaseY= (smartThing.getPosition().getCoordinate().getCoordY()+(smartThing.getPosition().getSpeed()*add));
			double decreaseX= (smartThing.getPosition().getCoordinate().getCoordX()-(smartThing.getPosition().getSpeed()*add));
			double decreaseY= (smartThing.getPosition().getCoordinate().getCoordY()-(smartThing.getPosition().getSpeed()*add));
			int direction= smartThing.getPosition().getDirection();


			if(decreaseX<0||decreaseY<0||increaseX>=MaxAndMin.MAX_X||increaseY>=MaxAndMin.MAX_Y){//It checks the CoordDevices limits.
				disableSelf();
				return;
			}

			if(direction==Directions.EAST){
				smartThing.getPosition().getCoordinate().setCoordX(increaseX);
			}
			else if(direction==Directions.WEST){
				smartThing.getPosition().getCoordinate().setCoordX(decreaseX);//next position in the same direction
			}
			else if(direction==Directions.SOUTH){//Directions.NORTH){
				smartThing.getPosition().getCoordinate().setCoordY(increaseY);//next position in the same direction
			}
			else if(direction==Directions.NORTH){//Directions.SOUTH){
				smartThing.getPosition().getCoordinate().setCoordY(decreaseY);
			}
			else if(direction==Directions.SOUTHEAST){//Directions.NORTHEAST){
				smartThing.getPosition().getCoordinate().setCoordX(increaseX);
				smartThing.getPosition().getCoordinate().setCoordY(increaseY);
			}
			else if(direction==Directions.NORTHWEST){//Directions.SOUTHWEST){
				smartThing.getPosition().getCoordinate().setCoordX(decreaseX);
				smartThing.getPosition().getCoordinate().setCoordY(decreaseY);
			}
			else if(direction==Directions.SOUTHWEST){//Directions.NORTHWEST){
				smartThing.getPosition().getCoordinate().setCoordX(decreaseX);
				smartThing.getPosition().getCoordinate().setCoordY(increaseY);
			}
			else if(direction==Directions.NORTHEAST){//Directions.SOUTHEAST){
				smartThing.getPosition().getCoordinate().setCoordX(increaseX);
				smartThing.getPosition().getCoordinate().setCoordY(decreaseY);

			}
		}
	}



	public  void disableSelf(){
		Log.printLine("Removing SmartThing: "+this.getName());
		this.getPosition().getCoordinate().setCoordX(-1);
		this.getPosition().getCoordinate().setCoordY(-1);
	}

	@Override
	public String toString() {
		return this.getName() + "[coordX="+this.getPosition().getCoordinate().getCoordX()
				+ ", coordY="+this.getPosition().getCoordinate().getCoordY()
				+ ", direction=" + position.getDirection() + ", speed=" + position.getSpeed()
				+ /*", distanceAp=" + distanceAp + */", sourceCloudletServer="
				+ sourceServerCloudlet + ", destinationCloudletServer="
				+ destinationServerCloudlet + ", sourceAp=" + sourceAp
				+ ", destinationAp=" + destinationAp + ", vmMobileDevice="
				+ vmMobileDevice + ", migTime=" + migTime + ", sensors="
				+ sensors + ", actuators=" + actuators + "]";
	}
	@Override
	protected void processOtherEvent(SimEvent ev) {
		if (Log.TRACE_EVNETS)
			Log.print("MobileDevice.java: " + this.getName() + " => Event: " + ev.toString() + "; Tag: ");

		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.TUPLE_ARRIVAL (" + ev.getTag() + ")");
			processTupleArrival(ev);
			break;
		case FogEvents.LAUNCH_MODULE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.LAUNCH_MODULE (" + ev.getTag() + ")");
			processModuleArrival(ev);
			break;
		case FogEvents.RELEASE_OPERATOR:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.RELEASE_OPERATOR (" + ev.getTag() + ")");
			processOperatorRelease(ev);
			break;
		case FogEvents.SENSOR_JOINED:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.SENSOR_JOINED (" + ev.getTag() + ")");
			processSensorJoining(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.SEND_PERIODIC_TUPLE (" + ev.getTag() + ")");
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.APP_SUBMIT (" + ev.getTag() + ")");
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.UPDATE_NORTH_TUPLE_QUEUE (" + ev.getTag() + ")");
			updateNorthTupleQueue();
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.UPDATE_SOUTH_TUPLE_QUEUE (" + ev.getTag() + ")");
			updateSouthTupleQueue();
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.ACTIVE_APP_UPDATE (" + ev.getTag() + ")");
			updateActiveApplications(ev);
			break;
		case FogEvents.ACTUATOR_JOINED:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.ACTUATOR_JOINED (" + ev.getTag() + ")");
			processActuatorJoined(ev);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.LAUNCH_MODULE_INSTANCE (" + ev.getTag() + ")");
			updateModuleInstanceCount(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			if (Log.TRACE_EVNETS)
				Log.printLine("FogEvents.RESOURCE_MGMT (" + ev.getTag() + ")");
			manageResources(ev);
			break;
		case OffloadingEvents.MAKE_OFFLOADING_SCHEDULING_DECISION:
			if (Log.TRACE_EVNETS)
				Log.printLine("OffloadingEvents.MAKE_OFFLOADING_SCHEDULING_DECISION (" + ev.getTag() + ")");
			invokeMakeOffloadingSchedulingDecision();
			break;
		case OffloadingEvents.START_OFFLOADING:
			if (Log.TRACE_EVNETS)
				Log.printLine("OffloadingEvents.START_OFFLOADING (" + ev.getTag() + ")");
			startOffloading(ev);
			break;
		case OffloadingEvents.FINISHED_OFFLOADING:
			if (Log.TRACE_EVNETS)
				Log.printLine("OffloadingEvents.FINISHED_OFFLOADING (" + ev.getTag() + ")");
			endOffloading(ev);
			break;
		default:
			break;
		}
	}

	public void saveLostTupple(String a, String filename){
		try(FileWriter fw1 = new FileWriter(filename, true);
			    BufferedWriter bw1 = new BufferedWriter(fw1);
			    PrintWriter out1 = new PrintWriter(bw1))
		{
			out1.println(a);
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

	protected void processTupleArrival(SimEvent ev){

		Tuple tuple = (Tuple)ev.getData();
		MyStatistics.getInstance().setMyCountTotalTuple(1);

		boolean flagContinue=false;
		if(!MobileController.getSmartThings().contains(this)){
			return;
		}

		for(MobileDevice st: MobileController.getSmartThings()){// verifica se o smartthing ainda esta na lista
			for(Sensor s: st.getSensors()){
				if(tuple.getAppId().equals(s.getAppId())){
					flagContinue=true;
					break;
				}
			}
		}
		if(!flagContinue){
			return;
		}

		if(tuple.getInitialTime()==-1){
			MyStatistics.getInstance().getTupleLatency().put(tuple.getMyTupleId(), CloudSim.clock()-getUplinkLatency());
			tuple.setInitialTime(CloudSim.clock()-getUplinkLatency());
		}

		if(getName().equals("cloud")){
			updateCloudTraffic();
		}

		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
Log.printLine(++numClients);
}*/
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+" with tupleType = "+tuple.getTupleType()+"\t| Source : "+
				CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

		if(FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())){
		}

		if(tuple.getDirection() == Tuple.ACTUATOR){
			//			Log.printLine("MobileDevice.java - Destination: "+CloudSim.getEntityName(ev.getDestination()));
			//			Log.printLine("MobileDevice.java - Source: "+CloudSim.getEntityName(ev.getSource()));
			//			Log.printLine("******to Actuador ******");
			sendTupleToActuator(tuple);
			return;
		}
		//		int index=0;


		if((isMigStatus()||isHandoffStatus())){// && (ev.getSource()!=ev.getDestination())){
//			if(!tuple.getTupleType().contains("EEG")){

			MyStatistics.getInstance().setMyCountLostTuple(1);
			saveLostTupple(String.valueOf(CloudSim.clock()), "results/"+tuple.getUserId()+"mdlostTupple.txt");

				if(isMigStatus()){//send again
//					if(MyStatistics.getInstance().getInitialWithoutVmTime().get(getMyId())==null){
//						MyStatistics.getInstance().startWithoutVmTime(getMyId(), CloudSim.clock());
//					}

					//fazer mediçao aqui.
					//				MyStatistics.getInstance().putLatencyFile(CloudSim.clock());
//										LogMobile.debug("MobileDevice.java", getName()+" is in Migration");
					//				Log.printLine("Clock: "+CloudSim.clock()+" - MobileDevice.java - Destination: "+CloudSim.getEntityName(ev.getDestination())+": Tuple "+tuple.getCloudletId());
					//				Log.printLine("Clock: "+CloudSim.clock()+" - MobileDevice.java - Source: "+CloudSim.getEntityName(ev.getSource()));
					//				Logger.ENABLED=true;
					//				Log.printLine("Clock: "+CloudSim.clock()+" - TupleId: "+tuple.getMyTupleId()+" MobileDevice.java - Migration");
//					double mt= getMigTime();
//					double clock = CloudSim.clock();
//					double semVm = MyStatistics.getInstance().getInitialWithoutVmTime().get(getMyId());
//					double iddatuple = (tuple.getCloudletId()/1000.0);
//					double delay = getMigTime() - (CloudSim.clock() - MyStatistics.getInstance().getInitialWithoutVmTime().get(getMyId()));
//					if(delay < 100){
//						delay = 1000;
//					}
//					send(ev.getDestination(),1000, FogEvents.TUPLE_ARRIVAL, tuple);
					return;
				}
				else{//send again
					//	MyStatistics.getInstance().putLatencyFile(CloudSim.clock());
					//					LogMobile.debug("MobileDevice.java", getName()+" is in Handoff");
					//				Log.printLine("Clock: "+CloudSim.clock()+" - MobileDevice.java - Destination: "+CloudSim.getEntityName(ev.getDestination())+": Tuple "+tuple.getCloudletId());
					//				Log.printLine("Clock: "+CloudSim.clock()+" - MobileDevice.java - Source: "+CloudSim.getEntityName(ev.getSource()));
					//				Log.printLine("Clock: "+CloudSim.clock()+" - TupleId: "+tuple.getMyTupleId()+" MobileDevice.java - Handoff");
//					send(ev.getDestination(), 100, FogEvents.TUPLE_ARRIVAL, tuple);
					return;
				}
//			}
		}
		else {
//			if(MyStatistics.getInstance().getInitialWithoutVmTime().get(getMyId())!=null){
//				MyStatistics.getInstance().finalWithoutVmTime(getMyId(), CloudSim.clock());
//				MyStatistics.getInstance().getInitialWithoutVmTime().remove(getMyId());
//			}
			if(getHost().getVmList().size() > 0){
				for(Vm vm:getHost().getVmList()){
					//	if(vm.getVmm().equals("Xen")){//if(tuple.getAppId().equals("MyApp_vr_game"+vm.getId())){
					final AppModule operator = (AppModule)vm;//getHost().getVmList().get(index);
					if(CloudSim.clock() > 0){
						getHost().getVmScheduler().deallocatePesForVm(operator);
						getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
							protected static final long serialVersionUID = 1L;
							{add((double) getHost().getTotalMips());}});
					}

					break;
					//}
					//				Log.printLine("tuple.getAppId(): "+tuple.getAppId());
					//				Log.printLine("Vm #: "+getHost().getVmList().get(index).getId()+ " UserId: "+getHost().getVmList().get(index).getUserId()+" Name: "+getHost().getVmList().get(index).getVmm());
					//				index++;
				}

			}
			if(getName().equals("cloud") && tuple.getDestModuleName()==null){
				sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
			}
			//			if(tuple.getTupleType().contains("EEG")){
			//				Logger.ENABLED=true;
			//				Logger.debug(getName(), "Received tuple "+tuple.getMyTupleId()+" with tupleType = "+tuple.getTupleType()+"\t| Source : "+
			//					CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
			//				Logger.ENABLED=false;
			//			}


			if(appToModulesMap.containsKey(tuple.getAppId())){
				if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
					int vmId = -1;
					for(Vm vm : getHost().getVmList()){
						//if(vm.getVmm().equals("Xen")){
						if(((AppModule)vm).getName().equals(tuple.getDestModuleName()))
							vmId = vm.getId();
						//}
					}
					if(vmId < 0
							|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
									tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
						return;
					}
					tuple.setVmId(vmId);
					//Logger.error(getName(), "Executing tuple for operator " + moduleName);

					updateTimingsOnReceipt(tuple);

					executeTuple(ev, tuple.getDestModuleName());
				}else if(tuple.getDestModuleName()!=null){
					if(tuple.getDirection() == Tuple.UP)
						sendUp(tuple);
					else if(tuple.getDirection() == Tuple.DOWN){
						for(int childId : getChildrenIds())
							sendDown(tuple, childId);
					}
				}else{
					sendUp(tuple);
				}
			}else{
				if(tuple.getDirection() == Tuple.UP)
					sendUp(tuple);
				else if(tuple.getDirection() == Tuple.DOWN){
					for(int childId : getChildrenIds())
						sendDown(tuple, childId);
				}
			}
		}
	}
	private void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModule = edge.getSource();
		AppModule module = null;
		for(Vm vm : getHost().getVmList()){
			//	if(vm.getVmm().equals("Xen")){
			if(((AppModule)vm).getName().equals(srcModule)){
				module=(AppModule)vm;
				break;
			}
			//}
		}
		if(module == null)
			return;

		int instanceCount = getModuleInstanceCount().get(module.getAppId()).get(srcModule);

		/*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
		 */
		for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
			Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);
		}
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}

	private void updateModuleInstanceCount(SimEvent ev) {
		ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		String appId = config.getModule().getAppId();
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		Log.printLine(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
	}

	private void manageResources(SimEvent ev) {
		//		updateEnergyConsumption();
		//send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}

	private void invokeMakeOffloadingSchedulingDecision() {
		getOffloadingScheduler().scheduleOffloadingTask(this);
	}

	private void startOffloading(SimEvent event) {
		OffloadingTask task = (OffloadingTask)event.getData();
		task.setSource(this);

		FogDevice device = getOffloadingStrategy().selectOffloadingTarget(
				MobileController.getServerCloudlets(),
				MobileController.getApDevices(),
				this,
				task);
		
		if (device == null) {
			Log.printLine("No suitable offloading target found. Abort task offloading.");
			return;
		}

		scheduleNow(device.getId(), OffloadingEvents.BEGIN_OFFLOAD_TASK_EXECUTION, task);
	}

	private void endOffloading(SimEvent event) {
		OffloadingTask task = (OffloadingTask)event.getData();
		task.setSource(null);

		// call the offloading task scheduler to may schedule a new offloading task
		scheduleNow(getId(), OffloadingEvents.MAKE_OFFLOADING_SCHEDULING_DECISION);
	}

	public void scheduleOffloadingTask(int delay, OffloadingTask task) {
		schedule(getId(), delay, OffloadingEvents.START_OFFLOADING, task);
	}

	public float getMaxServiceValue() {
		return maxServiceValue;
	}

	public void setMaxServiceValue(float maxServiceValue) {
		this.maxServiceValue = maxServiceValue;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public Coordinate getFutureCoord() {//myiFogSim
		return futureCoord;
	}

	public void setFutureCoord(double coordX, double coordY) { //myiFogSim
		this.futureCoord.setCoordX(coordX);
		this.futureCoord.setCoordY(coordY);
	}

	//	public double getDistanceAp() {
	//		return distanceAp;
	//	}
	//
	//	public void setDistanceAp(double distanceAp) {
	//		this.distanceAp = distanceAp;
	//	}


	public FogDevice getSourceServerCloudlet() {
		return sourceServerCloudlet;
	}

	public void setSourceServerCloudlet(FogDevice sourceServerCloudlet) {
		this.sourceServerCloudlet = sourceServerCloudlet;
	}

	public FogDevice getDestinationServerCloudlet() {
		return destinationServerCloudlet;
	}

	public void setDestinationServerCloudlet(FogDevice destinationServerCloudlet) {
		this.destinationServerCloudlet = destinationServerCloudlet;
	}

	public ApDevice getSourceAp() {
		return sourceAp;
	}

	public void setSourceAp(ApDevice sourceAp) {
		this.sourceAp = sourceAp;
	}

	public ApDevice getDestinationAp() {
		return destinationAp;
	}

	public void setDestinationAp(ApDevice destinationAp) {
		this.destinationAp = destinationAp;
	}

	public Vm getVmMobileDevice() {
		return vmMobileDevice;
	}

	public void setVmMobileDevice(Vm vmMobileDevice) {
		this.vmMobileDevice = vmMobileDevice;
	}

	public double getVmSize() {
		return vmSize;
	}

	public void setVmSize(double vmSize) {
		this.vmSize = vmSize;
	}

	public double getMigTime() {
		return migTime;
	}

	public void setMigTime(double d) {
		this.migTime = d;
	}

	public Set<MobileSensor> getSensors() {
		return sensors;
	}

	public void setSensors(Set<MobileSensor> sensors) {
		this.sensors = sensors;
	}

	public Set<MobileActuator> getActuators() {
		return actuators;
	}

	public void setActuators(Set<MobileActuator> actuators) {
		this.actuators = actuators;
	}

	public boolean isMigStatus() {
		return migStatus;
	}

	public void setMigStatus(boolean migStatus) {
		this.migStatus = migStatus;
	}

	public boolean isMigStatusLive() {
		return migStatusLive;
	}

	public void setMigStatusLive(boolean migStatusLive) {
		this.migStatusLive = migStatusLive;
	}

	public double getTempSimulation() {
		return tempSimulation;
	}

	public void setTempSimulation(double tempSimulation) {
		this.tempSimulation = tempSimulation;
	}

	public boolean isHandoffStatus() {
		return handoffStatus;
	}

	public void setHandoffStatus(boolean handoffStatus) {
		this.handoffStatus = handoffStatus;
	}

	public double getTimeFinishHandoff() {
		return timeFinishHandoff;
	}

	public void setTimeFinishHandoff(double timeFinishHandoff) {
		this.timeFinishHandoff = timeFinishHandoff;
	}

	public double getTimeFinishDeliveryVm() {
		return timeFinishDeliveryVm;
	}

	public void setTimeFinishDeliveryVm(double timeFinishDeliveryVm) {
		this.timeFinishDeliveryVm = timeFinishDeliveryVm;
	}

	public FogDevice getVmLocalServerCloudlet() {
		return vmLocalServerCloudlet;
	}

	public void setVmLocalServerCloudlet(FogDevice vmLocalServerCloudlet) {
		this.vmLocalServerCloudlet = vmLocalServerCloudlet;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public boolean isLockedToHandoff() {
		return lockedToHandoff;
	}

	public void setLockedToHandoff(boolean LockedToHandoff) {
		this.lockedToHandoff = LockedToHandoff;
	}

	public boolean isLockedToMigration() {
		return lockedToMigration;
	}

	public void setLockedToMigration(boolean lockedToMigration) {
		this.lockedToMigration = lockedToMigration;
	}

	public boolean isAbortMigration() {
		return abortMigration;
	}

	public void setAbortMigration(boolean abortMigration) {
		this.abortMigration = abortMigration;
	}

	public VmMigrationTechnique getMigrationTechnique() {
		return migrationTechnique;
	}

	public void setMigrationTechnique(VmMigrationTechnique migrationTechnique) {
		this.migrationTechnique = migrationTechnique;
	}

	public boolean isMigPoint() {
		return migPoint;
	}

	public void setMigPoint(boolean migPoint) {
		this.migPoint = migPoint;
	}

	public boolean isMigZone() {
		return migZone;
	}

	public void setMigZone(boolean migZone) {
		this.migZone = migZone;
	}

	public boolean isPostCopyStatus() {
		return postCopyStatus;
	}

	public void setPostCopyStatus(boolean postCopyStatus) {
		this.postCopyStatus = postCopyStatus;
	}

	public double getTimeStartLiveMigration() {
		return timeStartLiveMigration;
	}

	public void setTimeStartLiveMigration(double timeStartLiveMigration) {
		this.timeStartLiveMigration = timeStartLiveMigration;
	}

	public void setNextServerClouletId(int i) {
		// TODO Auto-generated method stub
	}

	public int getNextServerClouletId() {
		// TODO Auto-generated method stub
		return 1;
	}

	public User getDeviceUser() {
		return deviceUser;
	}

	public void setDeviceUser(User deviceUser) {
		this.deviceUser = deviceUser;
	}

	public IOffloadingScheduler getOffloadingScheduler() {
		return this.offloadingScheduler;
	}
	
	public void setOffloadingScheduler(IOffloadingScheduler offloadingScheduler) {
		this.offloadingScheduler = offloadingScheduler;
	}

	public IOffloadingStrategy getOffloadingStrategy() {
		return this.offloadingStrategy;
	}
	
	public void setOffloadingStrategy(IOffloadingStrategy offloadingStrategy) {
		this.offloadingStrategy = offloadingStrategy;
	}
	
	public IOffloadingResponseTimeCalculator getOffloadingResponseTimeCalculator() {
		return this.offloadingResponseTimeCalculator;
	}
	
	public void setOffloadingResponseTimeCalculator(IOffloadingResponseTimeCalculator responseTimeCalculator) {
		this.offloadingResponseTimeCalculator = responseTimeCalculator;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}
}
