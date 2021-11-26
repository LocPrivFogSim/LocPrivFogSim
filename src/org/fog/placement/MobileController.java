package org.fog.placement;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.*;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.localization.Path;
import org.fog.offloading.OffloadingEvents;
import org.fog.privacy.Position;
import org.fog.utils.*;
import org.fog.vmmigration.Migration;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.NextStep;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.MobileEvents;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class MobileController extends SimEntity {

    static final int numOfDepts = 1;
    static final int numOfMobilesPerDept = 4;
    private static boolean migrationAble;
    private static int migPointPolicy;
    private static int stepPolicy; //Quantity of steps in the nextStep Function
    private static Coordinate coordDevices;//=new Coordinate(MaxAndMin.MAX_X, MaxAndMin.MAX_Y);//Grid/Map
    private static int migStrategyPolicy;
    private static int seed;
    private static List<FogDevice> serverCloudlets;
    private static List<MobileDevice> smartThings;
    private static List<ApDevice> apDevices;
    private static List<FogBroker> brokerList;
    private static Random rand;
    int repeats = 0;
    private Map<String, Application> applications;
    private Map<String, Integer> appLaunchDelays;
    private ModuleMapping moduleMapping;
    private Map<Integer, Double> globalCurrentCpuLoad;

    public MobileController() {

    }

    public MobileController(String name, List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings, List<FogBroker> brokers, ModuleMapping moduleMapping
            , int migPointPolicy, int migStrategyPolicy, int stepPolicy, Coordinate coordDevices, int seed, boolean migrationAble) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : serverCloudlets) {
            sc.setControllerId(getId());
        }
        setSeed(seed);
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setSmartThings(smartThings);
        setBrokerList(brokers);
        setMigPointPolicy(migPointPolicy);
        setMigStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);
        setCoordDevices(coordDevices);
        connectWithLatencies();
        initializeCPULoads();
        setRand(new Random(getSeed() * Long.MAX_VALUE));
        setMigrationAble(migrationAble);
    }

    public MobileController(String name, List<FogDevice> serverCloudlets,
                            List<ApDevice> apDevices, List<MobileDevice> smartThings,
                            int migPointPolicy, int migStrategyPolicy, int stepPolicy,
                            Coordinate coordDevices, int seed) {
        // TODO Auto-generated constructor stub
        super(name);
        this.applications = new HashMap<String, Application>();
        this.globalCurrentCpuLoad = new HashMap<Integer, Double>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setModuleMapping(moduleMapping);
        for (FogDevice sc : serverCloudlets) {
            sc.setControllerId(getId());
        }
        setSeed(seed);
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setSmartThings(smartThings);
        setMigPointPolicy(migPointPolicy);
        setMigStrategyPolicy(migStrategyPolicy);
        setStepPolicy(stepPolicy);
        setCoordDevices(coordDevices);
        connectWithLatencies();
        initializeCPULoads();
        setRand(new Random(getSeed() * Long.MAX_VALUE));
    }

    private static void saveHandOff(MobileDevice st) {
        Log.printLine("HANDOFF " + st.getMyId() + " Position: " + st.getPosition().getCoordinate().getCoordX() + ", " + st.getPosition().getCoordinate().getCoordY() + " Direction: " + st.getPosition().getDirection() + " Speed: " + st.getPosition().getSpeed());
        try (FileWriter fw = new FileWriter("results/"+st.getMyId() + "handoff.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(st.getMyId() + "\t" + CloudSim.clock() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY() + "\t" + st.getPosition().getDirection() + "\t" + st.getPosition().getSpeed() + "\t" + st.getSourceAp().getName() + "\t" + st.getDestinationAp().getName());
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int getMigPointPolicy() {
        return migPointPolicy;
    }

    public static void setMigPointPolicy(int migPointPolicy) {
        MobileController.migPointPolicy = migPointPolicy;
    }

    public static int getMigStrategyPolicy() {
        return migStrategyPolicy;
    }

    public static void setMigStrategyPolicy(int migStrategyPolicy) {
        MobileController.migStrategyPolicy = migStrategyPolicy;
    }

    public static int getStepPolicy() {
        return stepPolicy;
    }

    public static void setStepPolicy(int stepPolicy) {
        MobileController.stepPolicy = stepPolicy;
    }

    public static Coordinate getCoordDevices() {
        return coordDevices;
    }

    public static void setCoordDevices(Coordinate coordDevices) {
        MobileController.coordDevices = coordDevices;
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        MobileController.seed = seed;
    }

    public static List<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public static void setServerCloudlets(List<FogDevice> serverCloudlets) {
        MobileController.serverCloudlets = serverCloudlets;
    }

    public static List<MobileDevice> getSmartThings() {
        return smartThings;
    }

    public static void setSmartThings(List<MobileDevice> smartThings) {
        MobileController.smartThings = smartThings;
    }

    public static List<ApDevice> getApDevices() {
        return apDevices;
    }

    public static void setApDevices(List<ApDevice> apDevices) {
        MobileController.apDevices = apDevices;
    }

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        MobileController.rand = rand;
    }

    public static boolean isMigrationAble() {
        return migrationAble;
    }

    public static void setMigrationAble(boolean migrationAble) {
        MobileController.migrationAble = migrationAble;
    }

    private void connectWithLatencies() {
        for (FogDevice st : getSmartThings()) {
            FogDevice parent = getFogDeviceById(st.getParentId());
            if (parent == null) {
                continue;
            }
            double latency = st.getUplinkLatency();
            parent.getChildToLatencyMap().put(st.getId(), latency);
            parent.getChildrenIds().add(st.getId());
        }
    }

    private FogDevice getFogDeviceById(int id) {
        for (FogDevice sc : getServerCloudlets()) {
            if (id == sc.getId())
                return sc;
        }
        return null;
    }

    private void initializeCPULoads() {
        //		Map<String, Map<String, Integer>> mapping = moduleMapping.getModuleMapping();
        //		for(String deviceName : mapping.keySet()){
        //			FogDevice device = getDeviceByName(deviceName);
        //			for(String moduleName : mapping.get(deviceName).keySet()){
        //
        //				AppModule module = getApplication().getModuleByName(moduleName);
        //				if(module == null)
        //					continue;
        //				getCurrentCpuLoad().put(device.getId(), getCurrentCpuLoad().get(device.getId()).doubleValue() + module.getMips());
        //			}
        //		}
        for (FogDevice sc : getServerCloudlets()) {
            this.globalCurrentCpuLoad.put(sc.getId(), 0.0);
        }
        for (MobileDevice st : getSmartThings()) {
            this.globalCurrentCpuLoad.put(st.getId(), 0.0);
        }
    }

    @Override
    public void startEntity() {



        // TODO Auto-generated method stub
        for (String appId : applications.keySet()) {
            LogMobile.debug("MobileController.java", appId + " - " + getAppLaunchDelays().get(appId));
//			if(getAppLaunchDelays().get(appId)==0)
            processAppSubmit(applications.get(appId));
//			else{
//				Log.printLine("MobileController 174 startEntity "+getAppLaunchDelays().get(appId));
//				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
//			}
        }

        
        // Initial setting and checking of path and mobile device position for handoff or migration
        send(getId()//Application
                , 0
                , MobileEvents.NEXT_STEP
        );//, getSmartThings());
        send(getId()
                , 0
                , MobileEvents.CHECK_NEW_STEP);


        if (isMigrationAble()) {
            /*
            for (FogDevice sc : getServerCloudlets()) {
                for (int i = 0; i < MaxAndMin.MAX_SIMULATION_TIME; i += 1000) {
                    send(sc.getId()//serverCloudlet
                            , i //delay -> When the event will occur
                            , MobileEvents.MAKE_DECISION_MIGRATION
                            , sc.getSmartThings());
                }
            }
            */
        }

        for (MobileDevice mobileDevice : getSmartThings()) {
            if (mobileDevice.getOffloadingScheduler() != null)
                scheduleNow(mobileDevice.getId(), OffloadingEvents.MAKE_OFFLOADING_SCHEDULING_DECISION);
        }

        // NOTE(markus)
//		for (MobileDevice st : getSmartThings()){
//			Log.printLine(st.getStartTravelTime()*1000);
//			send(getId(), st.getStartTravelTime()*1000, MobileEvents.CREATE_NEW_SMARTTHING, st);
//			st.getSourceAp().desconnectApSmartThing(st);
//			st.getSourceServerCloudlet().desconnectServerCloudletSmartThing(st);
//			if(st.isLockedToMigration()||st.isMigStatus()){
//				sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION,st);
//			}
//		}

        //send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);

        //for(FogDevice dev : getServerCloudlets())
        //sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

        send(getId(), MaxAndMin.MAX_SIMULATION_TIME, MobileEvents.STOP_SIMULATION);
    }

    public void updatePosition(MobileDevice mobileDevice){


        Path path = mobileDevice.getPath();
        if(mobileDevice.getTravelTimeId() < path.getPositions().size()){
            Position old = mobileDevice.getPosition();
            Position position = path.getPositions().get(mobileDevice.getTravelTimeId());

            mobileDevice.setTravelTimeId(mobileDevice.getTravelTimeId()+1);
            mobileDevice.setPosition(position);

            int delay = (position.getTimestamp() - old.getTimestamp()) * 100;
            send(getId()//Application
                , delay
                , MobileEvents.NEXT_STEP
            );
            send(getId()
                    , delay
                    , MobileEvents.CHECK_NEW_STEP);
        }
        else{
            mobileDevice.disableSelf();
        }
    }

    public void setInitialCoordinate(MobileDevice mobileDevice){

        Path path = mobileDevice.getPath();
        if(!path.getPositions().isEmpty()){
            Position position = path.getPositions().get(0);

            // NOTE(markus): See comment note in NextStep.nextStep()
            // Changed form -1 to new value of 0...
            mobileDevice.setTravelTimeId(0);

            int time = position.getTimestamp();
            double x = position.getCoordinate().getCoordX();
            double y = position.getCoordinate().getCoordY();


            //Log.printLine("x: " + x+" y: "+y+"\tx: " + coodinates[1]+" y: "+coodinates[2]);

            if(x<0||y<0||x>=MaxAndMin.MAX_X||y>=MaxAndMin.MAX_Y){//It checks the CoordDevices limits.
                mobileDevice.disableSelf();
                //					coordDevices.setPositions(-1, oldCoordX, oldCoordY);
                //			break;
            }
            else{
                mobileDevice.setStartTravelTime(time);
                mobileDevice.setPosition(position);
            }
        }
        else{
            mobileDevice.disableSelf();
        }
    }


    private void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        processAppSubmit(app);
    }

    private void processAppSubmit(Application application) {
        Log.printLine("MobileController 213 processAppSubmit " + CloudSim.clock() + " Submitted application " + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        List<FogDevice> tempAllDevices = new ArrayList<>();
        for (FogDevice sc : getServerCloudlets()) {
            tempAllDevices.add(sc);
        }

        for (MobileDevice st : getSmartThings()) {
            tempAllDevices.add(st);
        }

        ModulePlacement modulePlacement = new ModulePlacementMapping(tempAllDevices//getServerCloudlets()
                , application, getModuleMapping(), globalCurrentCpuLoad);

        for (FogDevice fogDevice : getServerCloudlets()) {
            sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }
        for (MobileDevice st : getSmartThings()) {
            sendNow(st.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        Map<Integer, Map<String, Integer>> instanceCountMap = modulePlacement.getModuleInstanceCountMap();
        for (Integer deviceId : deviceToModuleMap.keySet()) {
            for (AppModule module : deviceToModuleMap.get(deviceId)) {
                Log.printLine("MobileController 240 ProcessAppSubmit");
                sendNow(deviceId, FogEvents.APP_SUBMIT, application);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE_INSTANCE,
                        new ModuleLaunchConfig(module, instanceCountMap.get(deviceId).get(module.getName())));
            }
        }
    }

    private void processAppSubmitMigration(SimEvent ev) {
        Application application = (Application) ev.getData();
        Log.printLine(CloudSim.clock() + " Submitted application after migration " + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        FogDevice sc = (FogDevice) CloudSim.getEntity(ev.getSource());
        List<FogDevice> tempList = new ArrayList<>();
        tempList.add(sc);
        ModulePlacement modulePlacement = new ModulePlacementMapping(tempList//getServerCloudlets()
                , application, getModuleMapping(), globalCurrentCpuLoad, true);

        //		for(FogDevice fogDevice : getServerCloudlets()){
        sendNow(sc.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        //		}

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        Map<Integer, Map<String, Integer>> instanceCountMap = modulePlacement.getModuleInstanceCountMap();
        //		for(Integer deviceId : deviceToModuleMap.keySet()){
        for (AppModule module : deviceToModuleMap.get(sc.getId())) {
            Log.printLine("MobileController 268 processAppSubmitMigration");
            sendNow(sc.getId(), FogEvents.APP_SUBMIT, application);
            sendNow(sc.getId(), FogEvents.LAUNCH_MODULE, module);
            sendNow(sc.getId(), FogEvents.LAUNCH_MODULE_INSTANCE,
                    new ModuleLaunchConfig(module, instanceCountMap.get(sc.getId()).get(module.getName())));
        }

        //		}
    }

    private void processTupleFinished(SimEvent ev) {
    }

    protected void manageResources() {
        //send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
    }

    @Override
    public void processEvent(SimEvent ev) {
        // TODO Auto-generated method stub
        switch (ev.getTag()) {
            case FogEvents.APP_SUBMIT:
                Log.printLine("APP_SUBMIT");
                processAppSubmit(ev);
                break;
            case MobileEvents.APP_SUBMIT_MIGRATE:
                processAppSubmitMigration(ev);
                break;

            case FogEvents.TUPLE_FINISHED:
                Log.printLine("TUPLE_FINISHED");
                processTupleFinished(ev);
                break;
            case FogEvents.CONTROLLER_RESOURCE_MANAGE:
                repeats++;
                manageResources();
                break;
            case MobileEvents.NEXT_STEP:

                repeats = 0;
                NextStep.nextStep(getServerCloudlets()
                        , getApDevices()
                        , getSmartThings()
                        , getCoordDevices()
                        , getStepPolicy()
                        , getSeed()
                        ,this);

                break;
            case MobileEvents.CREATE_NEW_SMARTTHING:
                createNewSmartThing(ev);
                break;
            case MobileEvents.CHECK_NEW_STEP:
                checkNewPosition();
                Log.printLine("SmartThingListSize: " + getSmartThings().size());
                if (getSmartThings().isEmpty())
                    sendNow(getId(), MobileEvents.STOP_SIMULATION);
                break;
            case MobileEvents.STOP_SIMULATION:
                Log.printLine("*********************myStopSimulation MobilieController 149 ***********");
                Log.printLine("CloudSim.clock(): " + CloudSim.clock());
                Log.printLine("Size SmartThings: " + getSmartThings().size());
                CloudSim.finishSimulation();
                CloudSim.stopSimulation();
                printTimeDetails();
                printPowerDetails();
                printCostDetails();
                printNetworkUsageDetails();
                printMigrationsDetalis();
                //System.exit(0);
                break;
        }
    }

    private void createNewSmartThing(SimEvent ev) {
        MobileDevice st = (MobileDevice) ev.getData();
        // TODO Auto-generated method stub

        Log.printLine("criado...");
        st.setTravelTimeId(0);
//		if(ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())){
//			st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
//			Log.printLine("conectado... "+st.getSourceServerCloudlet().getName());
//		}

    }

    //	{antigo createNewsmartthing
    ////
    ////		int create=0;
    ////		create = rand.nextInt(2);//2 -> 50%, 4 -> 25%, 5 -> 20%, 10 -> 10%, 20 -> 5%, 50 -> 2% and 100 -> 1%
    //
    ////		if(create == 0){//(!smartThing.isStatus()){
    //			int i=AppExemplo2.getServerCloudlets().get(1).getSmartThings().size();
    //			//rand = new Random(i);
    //			short coordX,coordY;
    //			int direction, speed;
    //			direction = rand.nextInt(MaxAndMin.MAX_DIRECTION-1)+1;
    //			speed = rand.nextInt(MaxAndMin.MAX_SPEED);
    //			while(true){
    //				coordX = (short) rand.nextInt(MaxAndMin.MAX_X);
    //				coordY = (short) rand.nextInt(MaxAndMin.MAX_Y);
    //				if(AppExemplo2.getCoordDevices().getPositions(coordX, coordY)==null){//verify if it is empty
    //					smartThing.setDirection(direction);
    //					smartThing.setSpeed(speed);
    //					smartThing.setSourceServerCloudlet(null);
    //					smartThing.setDestinationServerCloudlet(null);
    //					smartThing.setVmLocalServerCloudlet(null);
    //					smartThing.setSourceAp(null);
    //					smartThing.setDestinationAp(null);
    //					smartThing.setVmMobileDevice(null);
    //					smartThing.setMigTime(0);
    //					smartThing.setMigStatus(false);
    //					smartThing.setHandoffStatus(false);
    //					smartThing.setStatus(true);
    //					smartThing.setCoord(coordX, coordY);
    //					AppExemplo2.getCoordDevices().setPositions(smartThing.getName()
    //							, smartThing.getCoord().getCoordX(), smartThing.getCoord().getCoordY());
    //					smartThing.setTempSimulation(0);
    //					smartThing.setTimeFinishDeliveryVm(0);
    //					smartThing.setTimeFinishHandoff(0);
    //
    //					if(ApDevice.connectApSmartThing(AppExemplo2.getApDevices(), smartThing)){
    //						FogDevice.connectServerCloudletSmartThing(smartThing.getSourceAp().getServerCloulet()
    //								, smartThing);
    //					}
    //
    //					break;
    //				}
    //			}
    ////		}
    //	}
    private double migrationTimeToLiveMigration(MobileDevice smartThing) {
        // TODO Auto-generated method stub
        double runTime = CloudSim.clock() - smartThing.getTimeStartLiveMigration();
        if (smartThing.getMigTime() > runTime) {
            runTime = smartThing.getMigTime() - runTime;
            return runTime;
        } else {
            return 0;
        }
    }
/*
    private void checkNewStep() {
        // TODO Auto-generated method stub
        ApDevice apDevice = null;
        //	Random rand = new Random((long) (getSeed()+CloudSim.clock()));
        //		Migration migration = new Migration();
        for (MobileDevice st : getSmartThings()) {
            if (st.getTravelTimeId() == -1) {
                continue;
            }
            MyStatistics.getInstance().getEnergyHistory().put(st.getMyId(), st.getEnergyConsumption());
            MyStatistics.getInstance().getPowerHistory().put(st.getMyId(), st.getHost().getPower());

            if (st.getSourceAp() != null) {
                // MobileDevice had an AP; Now we want to transition to a new AP

                Log.printLine(st.getName() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY());
                Log.printLine(st.getSourceAp().getName() + "\t" + st.getSourceAp().getPosition().getCoordinate().getCoordX() + "\t" + st.getSourceAp().getPosition().getCoordinate().getCoordY());
                Log.printLine(Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate()));
                if (!st.isLockedToHandoff()) {//(!st.isHandoffStatus()){
                    double distance = Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate());
                    //					List<ApDevice> tempApList=new ArrayList<>();

                    Log.printLine("Distance " + distance + " Diff " + (MaxAndMin.AP_COVERAGE - MaxAndMin.MAX_DISTANCE_TO_HANDOFF) + " max " + MaxAndMin.AP_COVERAGE);
                    if (distance >= MaxAndMin.AP_COVERAGE - MaxAndMin.MAX_DISTANCE_TO_HANDOFF && distance < MaxAndMin.AP_COVERAGE) { //Handoff Zone


                        List<ApDevice> devices = getApDevices();
                        System.out.println(devices);

                        ApDevice current = apDevice;

                        apDevice = Migration.nextAp(getApDevices(), st);

                        if (apDevice != null) {//index isn't negative
                            System.out.println(apDevice);
                            boolean distLower = true;
                            if(current != null) {

                                double distCurr = Coordinate.calcDistance(current.getServerCloudlet().getPosition().getCoordinate(), st.getPosition().getCoordinate());
                                double distNext = Coordinate.calcDistance(apDevice.getServerCloudlet().getPosition().getCoordinate(), st.getPosition().getCoordinate());
                                distLower = distNext<distCurr;
                            }
                            //apDevice != current &&
                            if (apDevice != current && distLower) {

                                current = apDevice;

                                st.setDestinationAp(apDevice);
                                st.setHandoffStatus(true);
                                st.setLockedToHandoff(true);

                                double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
//							float handoffLocked = (MaxAndMin.MAX_DISTANCE_TO_HANDOFF/(st.getSpeed()+1))*2000;
                                float handoffLocked = (float) (handoffTime * 4);
                                int delayConnection = 100; //connection between SmartT and ServerCloudlet

                                if (!st.getDestinationAp().getServerCloudlet().equals(st.getSourceServerCloudlet())) {

                                    //send(st.getDestinationAp().getServerCloulet().getId(), handoffTime+delayConnection+10,MobileEvents.MAKE_DECISION_MIGRATION,st);
                                    if (isMigrationAble()) {

                                        LogMobile.debug("MobileController.java", st.getName() + " will be desconnected from " +
                                                st.getSourceServerCloudlet().getName() + " by handoff");
                                        sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, st);
                                        sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, st);
                                        send(st.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, st);
//									sendNow(st.getDestinationAp().getServerCloudlet().getId(),MobileEvents.MAKE_DECISION_MIGRATION,st);
                                    }
                                    if (st.isPostCopyStatus() && !st.isMigStatus()) {

                                        if (!st.isMigStatusLive()) {

                                            st.setMigStatusLive(true);
                                            double newMigTime = migrationTimeToLiveMigration(st);
                                            if (newMigTime == 0) {
                                                newMigTime = ((st.getVmMobileDevice().getHost().getRamProvisioner().getUsedRam() * 8 * 1024 * 1024) / st.getVmLocalServerCloudlet().getUplinkBandwidth()) * 1000.0;
                                            }
                                            double delayProcess = st.getVmLocalServerCloudlet().getCharacteristics().
                                                    getCpuTime((st.getVmMobileDevice().getSize() * 1024 * 1024 * 8) * 0.7, 0.0);//the connection already is opened
                                            st.setTimeFinishDeliveryVm(-1.0);
                                            Log.printLine(CloudSim.clock() + " startWithoutVmTime");
                                            MyStatistics.getInstance().startWithoutVmTime(st.getMyId(), CloudSim.clock());
                                            send(st.getVmLocalServerCloudlet().getId(), newMigTime + delayProcess, MobileEvents.SET_MIG_STATUS_TRUE, st);
                                        }
                                    }
                                }

                                send(st.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, st);
                                send(st.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, st);
                                MyStatistics.getInstance().setTotalHandoff(1);

                                saveHandOff(st);

                                LogMobile.debug("MobileController.java", st.getName() + " handoff was scheduled! " + "SourceAp: " + st.getSourceAp().getName()
                                        + " NextAp: " + st.getDestinationAp().getName() + "\n");
                                LogMobile.debug("MobileController.java", "Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": " +
                                        Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate()));
                            }
                        } else {
                            LogMobile.debug("MobileController.java", st.getName() + " can't make handoff because don't exist closest nextAp");
                        }
                    } else if (false) {

                    } else if (distance >= MaxAndMin.AP_COVERAGE) {
                        st.getSourceAp().desconnectApSmartThing(st);
                        st.getSourceServerCloudlet().desconnectServerCloudletSmartThing(st);
                        if (st.isLockedToMigration() || st.isMigStatus()) {
                            sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, st);
                        }
                        LogMobile.debug("MobileController.java", st.getName() + " desconnected by AP_COVERAGE - Distance: " + distance);
                        LogMobile.debug("MobileController.java", st.getName() + " X: " + st.getPosition().getCoordinate().getCoordX() + " Y: " + st.getPosition().getCoordinate().getCoordY());
                    }
                }
            } else {
                // Create connection when MobileDevice had no AP connected => select a new closest AP
                if (ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())) {

                    System.out.println("from mobile controller");

                    st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
                    LogMobile.debug("MobileController.java", st.getName() + " has a new connection - SourceAp: " + st.getSourceAp().getName() +
                            " SourceServerCouldlet: " + st.getSourceServerCloudlet().getName());

                    CloudletScheduler cloudletScheduler = new CloudletSchedulerTimeShared();

                    long sizeVm = (MaxAndMin.MIN_VM_SIZE + (long) ((MaxAndMin.MAX_VM_SIZE - MaxAndMin.MIN_VM_SIZE) * (getRand().nextDouble())));
                    AppModule vmSmartThing = new AppModule(
                            st.getMyId(),                                // id
                            "AppModuleVm_" + st.getName()                    // name
                            , "MyApp_vr_game" + st.getMyId()                // app Id
                            , getBrokerList().get(st.getMyId()).getId()    // userId
                            , 2000                                        // mips
                            , 64                                        // ram
                            , 1000                                        // bw
                            , sizeVm                                    // size
                            , "Vm_" + st.getName()                        // vmm
                            , cloudletScheduler                            // cloudlet scheduler
                            , new HashMap<Pair<String, String>, SelectivityModel>()); // selectivity map
                    Log.printLine("before: " + st.getVmLocalServerCloudlet().getName());
                    st.setVmMobileDevice(vmSmartThing);
                    st.getSourceServerCloudlet().getHost().vmCreate(vmSmartThing);

                    st.setVmLocalServerCloudlet(st.getSourceServerCloudlet());
                    st.setLockedToMigration(false);
                    Log.printLine("after: " + st.getVmLocalServerCloudlet().getName());

                    Log.printLine(st.getName() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" + st.getPosition().getCoordinate().getCoordY());
                    Log.printLine(st.getSourceAp().getName() + "\t" + st.getSourceAp().getPosition().getCoordinate().getCoordX() + "\t" + st.getSourceAp().getPosition().getCoordinate().getCoordY());
                    Log.printLine("Distance: " + Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate()));

                    //					Log.printLine("Vm allocated to "+st.getName());
                    int brokerId = getBrokerList().get(st.getMyId()).getId();
                    for (MobileSensor s : st.getSensors()) {
                        s.setAppId("MyApp_vr_game" + st.getMyId());
                        s.setUserId(brokerId);
                        s.setGatewayDeviceId(st.getId());
                        s.setLatency(6.0);
                    }
                    for (MobileActuator a : st.getActuators()) {
                        a.setUserId(brokerId);
                        a.setAppId("MyApp_vr_game" + st.getMyId());
                        a.setGatewayDeviceId(st.getId());
                        a.setLatency(1.0);
                        a.setActuatorType("DISPLAY" + st.getMyId());
                    }
                    ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
                    moduleMapping.addModuleToDevice(((AppModule) st.getVmMobileDevice()).getName(), st.getSourceServerCloudlet().getName(), 1);//numOfDepts*numOfMobilesPerDept);
                    moduleMapping.addModuleToDevice("client" + st.getMyId(), st.getName(), 1);
                    //					moduleMapping.addModuleToDevice("connector"+st.getMyId(), st.getSourceServerCloudlet().getName() ,1);// MaxAndMin.MAX_SMART_THING); // fixing all instances of the Connector module to cloudlets
                    //					moduleMapping.addModuleToDevice("concentration_calculator"+st.getMyId(), st.getSourceServerCloudlet().getName(), 1);//MaxAndMin.MAX_SMART_THING);
                    processAppSubmit(getApplications().get("ApplicationOf" + st.getName()));
                } else {
                    //To do something
                }
            }
        }
    }
*/

    private void checkNewPosition() {


        for (MobileDevice st : getSmartThings()) {
            if (st.getTravelTimeId() == -1) {
                continue;
            }

            MyStatistics.getInstance().getEnergyHistory().put(st.getMyId(), st.getEnergyConsumption());
            MyStatistics.getInstance().getPowerHistory().put(st.getMyId(), st.getHost().getPower());

            if (st.getSourceAp() != null) {
                // MobileDevice had an AP; Now we want to transition to a new AP

                if (!st.isLockedToHandoff()) {
                    double distance = Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate());

                    // if(distance>=MaxAndMin.AP_COVERAGE - MaxAndMin.MAX_DISTANCE_TO_HANDOFF && distance < MaxAndMin.AP_COVERAGE){ //Handoff Zone
                    if (distance >= 300) {
                        List<ApDevice> devices =getApDevices();
                        ApDevice current = st.getSourceAp();
                        ApDevice apDevice = Migration.nextAp(devices, st);

                        if (apDevice != null && current != apDevice) {
                            st.setDestinationAp(apDevice);
                            st.setHandoffStatus(true);
                            st.setLockedToHandoff(true);

                           // double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * getRand().nextDouble(); //"Maximo" tempo para handoff
                            double handoffTime = 1;
                            float handoffLocked = (float) (handoffTime * 4);
                            int delayConnection = 0; //connection between SmartT and ServerCloudlet

                            if (!st.getDestinationAp().getServerCloudlet().equals(st.getSourceServerCloudlet())) {

                                if (isMigrationAble()) {
                                    LogMobile.debug("MobileController.java", st.getName() + " will be desconnected from " +
                                            st.getSourceServerCloudlet().getName() + " by handoff");
                                    //     sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.MAKE_DECISION_MIGRATION, st);
                                    sendNow(st.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, st);
                                    send(st.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, st);

                                }
                                if (st.isPostCopyStatus() && !st.isMigStatus()) {
                                    if (!st.isMigStatusLive()) {
                                        st.setMigStatusLive(true);
                                        double newMigTime = migrationTimeToLiveMigration(st);
                                        if (newMigTime == 0) {
                                            newMigTime = ((st.getVmMobileDevice().getHost().getRamProvisioner().getUsedRam() * 8 * 1024 * 1024) / st.getVmLocalServerCloudlet().getUplinkBandwidth()) * 1000.0;
                                        }
                                        double delayProcess = st.getVmLocalServerCloudlet().getCharacteristics().
                                                getCpuTime((st.getVmMobileDevice().getSize() * 1024 * 1024 * 8) * 0.7, 0.0);//the connection already is opened
                                        st.setTimeFinishDeliveryVm(-1.0);
                                        Log.printLine(CloudSim.clock() + " startWithoutVmTime");
                                        MyStatistics.getInstance().startWithoutVmTime(st.getMyId(), CloudSim.clock());
                                        send(st.getVmLocalServerCloudlet().getId(), newMigTime + delayProcess, MobileEvents.SET_MIG_STATUS_TRUE, st);
                                    }
                                }
                            }

                            send(st.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, st);
                            send(st.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, st);
                            MyStatistics.getInstance().setTotalHandoff(1);

                            saveHandOff(st);




                        } else {
                            LogMobile.debug("MobileController.java", st.getName() + " can't make handoff because don't exist closest nextAp");
                        }
                    } else if (distance >= MaxAndMin.AP_COVERAGE) {
                        st.getSourceAp().desconnectApSmartThing(st);
                        st.getSourceServerCloudlet().desconnectServerCloudletSmartThing(st);
                        if (st.isLockedToMigration() || st.isMigStatus()) {
                            sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, st);
                        }
                        LogMobile.debug("MobileController.java", st.getName() + " desconnected by AP_COVERAGE - Distance: " + distance);
                    }
                }
            } else {
                // Create connection when MobileDevice had no AP connected => select a new closest AP
                if (ApDevice.connectApSmartThing(getApDevices(), st, getRand().nextDouble())) {
                    st.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(st);
                    LogMobile.debug("MobileController.java", st.getName() + " has a new connection - SourceAp: " + st.getSourceAp().getName() +
                            " SourceServerCouldlet: " + st.getSourceServerCloudlet().getName());

                    CloudletScheduler cloudletScheduler = new CloudletSchedulerTimeShared();

                    long sizeVm = (MaxAndMin.MIN_VM_SIZE + (long) ((MaxAndMin.MAX_VM_SIZE - MaxAndMin.MIN_VM_SIZE) * (getRand().nextDouble())));
                    AppModule vmSmartThing = new AppModule(
                            st.getMyId(),                                // id
                            "AppModuleVm_" + st.getName()                    // name
                            , "MyApp_vr_game" + st.getMyId()                // app Id
                            , getBrokerList().get(st.getMyId()).getId()    // userId
                            , 2000                                        // mips
                            , 64                                        // ram
                            , 1000                                        // bw
                            , sizeVm                                    // size
                            , "Vm_" + st.getName()                        // vmm
                            , cloudletScheduler                            // cloudlet scheduler
                            , new HashMap<Pair<String, String>, SelectivityModel>()); // selectivity map
                    Log.printLine("before: " + st.getVmLocalServerCloudlet().getName());
                    st.setVmMobileDevice(vmSmartThing);
                    st.getSourceServerCloudlet().getHost().vmCreate(vmSmartThing);

                    st.setVmLocalServerCloudlet(st.getSourceServerCloudlet());
                    st.setLockedToMigration(false);
                    Log.printLine("after: " + st.getVmLocalServerCloudlet().getName());

                    //					Log.printLine("Vm allocated to "+st.getName());
                    int brokerId = getBrokerList().get(st.getMyId()).getId();
                    for (MobileSensor s : st.getSensors()) {
                        s.setAppId("MyApp_vr_game" + st.getMyId());
                        s.setUserId(brokerId);
                        s.setGatewayDeviceId(st.getId());
                        s.setLatency(6.0);
                    }
                    for (MobileActuator a : st.getActuators()) {
                        a.setUserId(brokerId);
                        a.setAppId("MyApp_vr_game" + st.getMyId());
                        a.setGatewayDeviceId(st.getId());
                        a.setLatency(1.0);
                        a.setActuatorType("DISPLAY" + st.getMyId());
                    }
                    ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
                    moduleMapping.addModuleToDevice(((AppModule) st.getVmMobileDevice()).getName(), st.getSourceServerCloudlet().getName(), 1);//numOfDepts*numOfMobilesPerDept);
                    moduleMapping.addModuleToDevice("client" + st.getMyId(), st.getName(), 1);
                    //					moduleMapping.addModuleToDevice("connector"+st.getMyId(), st.getSourceServerCloudlet().getName() ,1);// MaxAndMin.MAX_SMART_THING); // fixing all instances of the Connector module to cloudlets
                    //					moduleMapping.addModuleToDevice("concentration_calculator"+st.getMyId(), st.getSourceServerCloudlet().getName(), 1);//MaxAndMin.MAX_SMART_THING);
                    processAppSubmit(getApplications().get("ApplicationOf" + st.getName()));
                } else {
                    //To do something
                }
            }
        }
    }

    @Override
    public void shutdownEntity() {
        // TODO Auto-generated method stub

    }

    private void printCostDetails() {
        //Log.printLine("Cost of execution in cloud = "+getCloud().getTotalCost());
    }

    private FogDevice getCloud() {
        for (FogDevice dev : getServerCloudlets())
            if (dev.getName().equals("cloud"))
                return dev;
        return null;
    }

    public void printResults(String a, String filename) {
        try (FileWriter fw1 = new FileWriter("results/"+filename, true);
             BufferedWriter bw1 = new BufferedWriter(fw1);
             PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(a);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void printPowerDetails() {
        // TODO Auto-generated method stub
        double energyConsumedMean = 0.0;
        int j = 0;
        Log.printLine("=========================================");
        Log.printLine("CLOUDLETS ENERGY CONSUMPTION");
        Log.printLine("=========================================");
        for (FogDevice fogDevice : getServerCloudlets()) {
            if (fogDevice.getEnergyConsumption() != 5.8736831999993116E7) {
                Log.printLine(fogDevice.getName() + ": Power = " + fogDevice.getHost().getPower());
                Log.printLine(fogDevice.getName() + ": Energy Consumed = " + fogDevice.getEnergyConsumption());
                energyConsumedMean += fogDevice.getEnergyConsumption();
                j++;
            }
        }
        Log.printLine("Total consumido Coudlets: " + energyConsumedMean + " Media: " + energyConsumedMean / j);
        printResults(String.valueOf(energyConsumedMean / j), "averageEnergyHistoryDevice.txt");
        printResults(String.valueOf(energyConsumedMean) + "\t" + String.valueOf(energyConsumedMean / j), "resultados.txt");
        energyConsumedMean = 0.0;
        Log.printLine("=========================================");
        Log.printLine("AP DEVICES ENERGY CONSUMPTION");
        Log.printLine("=========================================");
        for (FogDevice apDevice : getApDevices()) {
//			Log.printLine(apDevice.getName()+ ": Power = "+apDevice.getHost().getPower());
            Log.printLine(apDevice.getName() + ": Energy Consumed = " + apDevice.getEnergyConsumption());
            energyConsumedMean += apDevice.getEnergyConsumption();
            j++;
        }
        Log.printLine("Total consumido AP: " + energyConsumedMean + " Media: " + energyConsumedMean / j);
        energyConsumedMean = 0.0;
        Log.printLine("=========================================");
        Log.printLine("SMARTTHINGS ENERGY CONSUMPTION");
        Log.printLine("=========================================");
        for (FogDevice mobileDevice : getSmartThings()) {
            Log.printLine(mobileDevice.getName() + ": Power = " + mobileDevice.getHost().getPower());
            Log.printLine(mobileDevice.getName() + ": Energy Consumed = " + mobileDevice.getEnergyConsumption());
        }
        for (int i = 0; i < MyStatistics.getInstance().getPowerHistory().size(); i++) {
            Log.printLine("SmartThing" + i + ": Power = " + MyStatistics.getInstance().getPowerHistory().get(i));
        }
        for (int i = 0; i < MyStatistics.getInstance().getEnergyHistory().size(); i++) {
            Log.printLine("SmartThing" + i + ": Energy Consumed = " + MyStatistics.getInstance().getEnergyHistory().get(i));
            printResults(String.valueOf(MyStatistics.getInstance().getEnergyHistory().get(i)), "resultados.txt");
//			energyConsumedMean += MyStatistics.getInstance().getEnergyHistory().get(i);
        }
    }

    private String getStringForLoopId(int loopId) {
        for (String appId : getApplications().keySet()) {
            Application app = getApplications().get(appId);
            for (AppLoop loop : app.getLoops()) {
                if (loop.getLoopId() == loopId)
                    return loop.getModules().toString();
            }
        }
        return null;
    }

    private void printTimeDetails() {

        Log.printLine("=========================================");
        Log.printLine("============== RESULTS ==================");
        Log.printLine("=========================================");
        Log.printLine("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
        Log.printLine("=========================================");
        Log.printLine("APPLICATION LOOP DELAYS");
        Log.printLine("=========================================");
        double mediaLatencia = 0.0;
        double mediaLatenciaMax = 0.0;
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
            //			double average = 0, count = 0;
            //			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
            //				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
            //				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
            //				if(startTime == null || endTime == null)
            //					break;
            //				average += endTime-startTime;
            //				count += 1;
            //			}
            //			Log.printLine(getStringForLoopId(loopId) + " ---> "+(average/count));
            Log.printLine(getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) + " MaxExecutionTime: " + TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId));
            printResults(String.valueOf(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)), "resultados.txt");
            printResults(String.valueOf(TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId)), "resultados.txt");
//TODO			mediaLatencia += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
//TODO			mediaLatenciaMax += TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loopId);
        }
        printResults(String.valueOf(mediaLatencia / TimeKeeper.getInstance().getLoopIdToCurrentAverage().keySet().size()), "averageLoopIdToCurrentAverage.txt");
        printResults(String.valueOf(mediaLatenciaMax / TimeKeeper.getInstance().getMaxLoopExecutionTime().keySet().size()), "averageMaxLoopExecutionTime.txt");
        Log.printLine("=========================================");
        Log.printLine("TUPLE CPU EXECUTION DELAY");
        Log.printLine("=========================================");

        for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
            Log.printLine(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
        }

        Log.printLine("=========================================");
    }

    private void printNetworkUsageDetails() {
        Log.printLine("=========================================");
        Log.printLine("=============NETWORK USAGE===============");
        Log.printLine("=========================================");
        double deviceNetworkUsage = NetworkUsageMonitor.getNetworkUsage() - NetworkUsageMonitor.getNetWorkUsageInMigration();
        Log.printLine("Device's network usage = " + deviceNetworkUsage);
        printResults(String.valueOf(deviceNetworkUsage / CloudSim.clock()) + '\t' + String.valueOf(deviceNetworkUsage) + '\t' + CloudSim.clock(), "resultados.txt");
        printResults(String.valueOf(deviceNetworkUsage / CloudSim.clock()) + '\t' + String.valueOf(deviceNetworkUsage) + '\t' + CloudSim.clock(), "deviceNetworkUsage.txt");
        Log.printLine("Migration' network usage = " + NetworkUsageMonitor.getNetWorkUsageInMigration());
        printResults(String.valueOf(NetworkUsageMonitor.getNetWorkUsageInMigration() / CloudSim.clock()) + '\t' + String.valueOf(NetworkUsageMonitor.getNetWorkUsageInMigration()) + '\t' + CloudSim.clock(), "resultados.txt");
        printResults(String.valueOf(NetworkUsageMonitor.getNetWorkUsageInMigration() / CloudSim.clock()) + '\t' + String.valueOf(NetworkUsageMonitor.getNetWorkUsageInMigration()) + '\t' + CloudSim.clock(), "cloudletNetworkUsage.txt");
        Log.printLine("Total network usage = " + NetworkUsageMonitor.getNetworkUsage());
        printResults(String.valueOf(NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf(NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "resultados.txt");
        printResults(String.valueOf(NetworkUsageMonitor.getNetworkUsage() / CloudSim.clock()) + '\t' + String.valueOf(NetworkUsageMonitor.getNetworkUsage()) + '\t' + CloudSim.clock(), "totalNetworkUsage.txt");
    }

    private void printMigrationsDetalis() {
        Log.printLine("=========================================");
        Log.printLine("==============MIGRATIONS=================");
        Log.printLine("=========================================");
        Log.printLine("Total of migrations: " + MyStatistics.getInstance().getTotalMigrations());
        Log.printLine("Total of handoff: " + MyStatistics.getInstance().getTotalHandoff());
        Log.printLine("Total of migration to differents SC: " + MyStatistics.getInstance().getMyCountLowestLatency());

        printResults(String.valueOf(MyStatistics.getInstance().getTotalMigrations()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getTotalHandoff()), "resultados.txt");

        printResults(String.valueOf(MyStatistics.getInstance().getTotalMigrations()), "totalMigrations.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getMyCountLowestLatency()), "totalMyCountLowestLatency.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getTotalHandoff()), "totalHandoff.txt");

        MyStatistics.getInstance().printResults();
        Log.printLine("***Last time without connection***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutConnectionTime().entrySet()) {
            Log.printLine("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutConnectionTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutConnectionTime().get(test.getKey()));
        }

        Log.printLine("Average of without connection: " + MyStatistics.getInstance().getAverageWithoutConnection());

        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutConnection()), "resultados.txt");

        Log.printLine("***Last time without Vm***");

        for (Entry<Integer, Double> test : MyStatistics.getInstance().getWithoutVmTime().entrySet()) {
            Log.printLine("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getWithoutVmTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxWithoutVmTime().get(test.getKey()));
        }

        Log.printLine("Average of without Vm: " + MyStatistics.getInstance().getAverageWithoutVmTime());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageWithoutVmTime()), "averageWithoutVmTime.txt");

        Log.printLine("===Last delay after connection===");
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getDelayAfterNewConnection().entrySet()) {
            Log.printLine("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getDelayAfterNewConnection().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxDelayAfterNewConnection().get(test.getKey()));
        }
        Log.printLine("Average of delay after new Connection: " + MyStatistics.getInstance().getAverageDelayAfterNewConnection());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageDelayAfterNewConnection()), "averageDelayAfterNewConnection.txt");

        Log.printLine("---Average of Time of Migrations---");
        double totalTempoMigracaoMax = 0.0;
        for (Entry<Integer, Double> test : MyStatistics.getInstance().getMigrationTime().entrySet()) {
            Log.printLine("SmartThing" + test.getKey() + ": " + MyStatistics.getInstance().getMigrationTime().get(test.getKey()) + " - Max: " + MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey()));
            printResults(String.valueOf(MyStatistics.getInstance().getMigrationTime().get(test.getKey())), "resultados.txt");
            printResults(String.valueOf(MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey())), "resultados.txt");
            totalTempoMigracaoMax += MyStatistics.getInstance().getMaxMigrationTime().get(test.getKey());
        }
        Log.printLine("Average of Time of Migrations: " + MyStatistics.getInstance().getAverageMigrationTime());
        printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "resultados.txt");
        printResults(String.valueOf(MyStatistics.getInstance().getAverageMigrationTime()), "averageMigrationTime.txt");
        printResults(String.valueOf(totalTempoMigracaoMax / MyStatistics.getInstance().getMigrationTime().entrySet().size()), "averageMigrationMaxTime.txt");
        Log.printLine("Tuple lost: " + (((double) MyStatistics.getInstance().getMyCountLostTuple() / MyStatistics.getInstance().getMyCountTotalTuple())) * 100 + "%");
        Log.printLine("Tuple lost: " + MyStatistics.getInstance().getMyCountLostTuple());
        Log.printLine("Total tuple: " + MyStatistics.getInstance().getMyCountTotalTuple());
    }

    public void submitApplication(Application application, int delay) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);
        for (MobileDevice st : getSmartThings()) {
            for (Sensor s : st.getSensors()) {
                if (s.getAppId().equals(application.getAppId()))
                    s.setApp(application);
            }
            for (Actuator a : st.getActuators()) {
                if (a.getAppId().equals(application.getAppId()))
                    a.setApp(application);
            }
        }
        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (MobileDevice st : getSmartThings()) {
                    for (Actuator actuator : st.getActuators()) {
                        if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
                            application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                    }
                }
            }
        }
    }

    public void submitApplicationMigration(MobileDevice smartThing, Application application, int delay) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);

        //			for(Sensor s : smartThing.getSensors()){
        ////				if(s.getAppId().equals(application.getAppId()))
        //					s.setApp(application);
        //			}
        //			for(Actuator a : smartThing.getActuators()){
        ////				if(a.getAppId().equals(application.getAppId()))
        //					a.setApp(application);
        //			}
        //
        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (MobileDevice st : getSmartThings()) {
                    for (Actuator actuator : st.getActuators()) {
                        if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
                            application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                    }
                }
            }
        }
    }

    public Map<String, Application> getApplications() {
        return applications;
    }

    public void setApplications(Map<String, Application> applications) {
        this.applications = applications;
    }

    public Map<String, Integer> getAppLaunchDelays() {
        return appLaunchDelays;
    }

    public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
        this.appLaunchDelays = appLaunchDelays;
    }

    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }

    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }

    public Map<Integer, Double> getGlobalCurrentCpuLoad() {
        return globalCurrentCpuLoad;
    }

    public void setGlobalCurrentCpuLoad(Map<Integer, Double> globalCurrentCpuLoad) {
        this.globalCurrentCpuLoad = globalCurrentCpuLoad;
    }

    public void setGlobalCPULoad(Map<Integer, Double> currentCpuLoad) {
        for (FogDevice device : getServerCloudlets()) {
            this.globalCurrentCpuLoad.put(device.getId(), currentCpuLoad.get(device.getId()));
        }
    }

    public List<FogBroker> getBrokerList() {
        return brokerList;
    }

    public void setBrokerList(List<FogBroker> brokerList) {
        this.brokerList = brokerList;
    }
}
