package org.fog.vmmobile;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.*;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.localization.Path;
import org.fog.localization.SimField;
import org.fog.offloading.FixedOffloadingScheduler;
import org.fog.offloading.IOffloadingResponseTimeCalculator;
import org.fog.offloading.IOffloadingScheduler;
import org.fog.offloading.IOffloadingStrategy;
import org.fog.offloading.OffloadingTask;
import org.fog.offloading.BandwidthCpuResponseTimeCalculator;
import org.fog.offloading.BelowThresholdLowestResponseTimeOffloadingStrategy;
import org.fog.offloading.BelowThresholdRandomDeviceOffloadingStrategy;
import org.fog.placement.MobileController;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.privacy.*;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.DBConnector;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.vmmigration.*;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;
import org.fog.vmmobile.constants.Services;

import java.io.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This is the privacy experiment
 */
@SuppressWarnings("ALL")
public class TestExample4 {

    /* Settings for the simulation setup */
    private static final int SEED = 5; // Cloud, FogDevice, AP
    private static  int NUM_OF_MOBILE_DEVICES = 1;
    private static final int NUM_OF_SENSORS_PER_DEVICE = 1;
    private static final int NUM_OF_ACTUATORS_PER_DEVICE = 1;
    private static final int MIN_DOWN_BANDWITH = 500; // Min Down Bandwidth 500 MB/s
    private static final int MAX_DOWN_BANDWITH = 1000; // Max Down Bandwidth 1000 MB/s (1 Gbit)
    private static final int MIN_UP_BANDWITH = 300; // Min Up Bandwidth 300 MB/s
    private static final int MAX_UP_BANDWITH = 1000; // Max Up Bandwidth 1000 MB/s (1 Gbit)
    private static final int TRANSMISSION_TIME = 1;
    private static final int LATENCY_BETWEEN_FOG_DEVICES = 1;
    private static int NUM_OF_ACCESS_POINTS;      // NUM_OF_ACCESS_POINTS <= NUM_OF_FOG_DEVICES!! (for this simulation only...)
    private static int NUM_OF_FOG_DEVICES;
    /* Settings for the experiment (or parse them from cmd args):
     * - Scenario - what does the adversary know about the fog nodes locations
     * - Rate of fog nodes controlled by the adversary
     * - Possibility if the mobile device is allowed to disconnect
     * - Seeds
     * - Resolution of the (voronoi) map scanning
     * - output file for results
     */
    private static int SCENARIO = 1;  // Scenario 1 or 2 -> differ in the adversary's knowledge about the locations of all fog nodes
    // 1 => knows location of compromised fog nodes only
    // 2 => knows location of all fog nodes
    private static double RATE_OF_COMPROMISED_DEVICES = 0.05; // controls the persentage of compromised devices => numCompromised = NUM_OF_FOG_DEVICES * RATE_OF_COMPROMISED_DEVICES
    private static int MOBILE_CAN_BE_TURNED_OFF = 1;    // boolean
    private static int SEED2 = 28; // MobileDevice, connections, and more
    private static int SEED3 = 5; // Attacker
    private static String filename = "results/results.csv";

    private  static DBConnector dbConnector = new DBConnector();


    //TODO cleanup members if necessary
    private static ArrayList<FogDevice> allFogDevices = new ArrayList<>(); // all fog nodes
    private static List<FogDevice> allCompromisedFogDevices = new ArrayList<>(); //all compromised fog nodes
    private static ArrayList<FogDevice> relevantFogDevicesList = new ArrayList<>(); //all fog node instances in proximity to a selected path
    private static List<FogDevice> relevantCompromisedDevices = new ArrayList<>(); //all compromised device instances in proximity to a selected path
    private static ArrayList<Path> allPaths;
    private static List<Vm> vmList = new ArrayList<>();
    private static List<MobileDevice> mobileDeviceList = new ArrayList<>();
    private static List<ApDevice> accessPointList = new ArrayList<>();
    private static List<FogBroker> brokerList = new ArrayList<>();
    private static List<String> appIdList = new ArrayList<>();
    private static List<Application> applicationList = new ArrayList<>();
    private static List<Attacker> attackerList = new ArrayList<>();
    /* migration settings (see MobFogSim) */
    private static Random rand;
    private static int migrationStrategyPolicy = Policies.LOWEST_DIST_BW_SMARTTING_AP;
    private static int migrationPointPolicy = Policies.SPEED_MIGRATION_POINT;
    private static int policyReplicaVm = Policies.LIVE_MIGRATION;
    private static int stepPolicy = 1;
    private static boolean migrationable = true;
    private static Coordinate map;
    private static DeviceMap deviceMap = new DeviceMap();
    private static SimField field;

    private static long startTime = 0;
    private static long executionTime = 0;

    private static double averagePathLength = 0;

    private static final IOffloadingResponseTimeCalculator offloadingResponseTimeCalculator = new BandwidthCpuResponseTimeCalculator();

    private static final IOffloadingScheduler offloadingScheduler = new FixedOffloadingScheduler(1000, 20, 2000, 2);

    private static double OFFLOADING_THRESHOLD = 0.0462d;
    private static IOffloadingStrategy offloadingStrategy;


    public static void main(String args[]) {

        try {

            long time1 = System.currentTimeMillis();

            /* parse settings from command line args
             * or comment these out to use settings from above
             */
            int mySc = Integer.parseInt(args[0]);
            if(mySc == 1){
                SCENARIO =1;
                MOBILE_CAN_BE_TURNED_OFF = 0;
            }
            if(mySc == 2){
                SCENARIO = 2;
                MOBILE_CAN_BE_TURNED_OFF = 0;
            }
            if(mySc == 3){
                SCENARIO = 1;
                MOBILE_CAN_BE_TURNED_OFF =1;
            }
            if(mySc == 4){
                SCENARIO = 2;
                MOBILE_CAN_BE_TURNED_OFF = 1;
            }

            RATE_OF_COMPROMISED_DEVICES = Double.parseDouble(args[1])/100;
            SEED2 = Integer.parseInt(args[2]);
            SEED3 = Integer.parseInt(args[3]);
            NUM_OF_MOBILE_DEVICES = Integer.parseInt(args[4]);

            OFFLOADING_THRESHOLD = Double.parseDouble(args[5]);
            String OFFLOADING_STRATEGY = args[6];

            if (OFFLOADING_STRATEGY.equalsIgnoreCase("BelowThresholdRandomDevice"))
                offloadingStrategy = new BelowThresholdRandomDeviceOffloadingStrategy(SEED3, OFFLOADING_THRESHOLD);
            else if (OFFLOADING_STRATEGY.equalsIgnoreCase("BelowThresholdLowestResponseTime"))
                offloadingStrategy = new BelowThresholdLowestResponseTimeOffloadingStrategy(OFFLOADING_THRESHOLD);
            else {
                Log.printLine("Unknown offloading strategy...");
                return;
            }

            System.out.println("Scenario: "+SCENARIO);
            System.out.println("can be turned of: "+MOBILE_CAN_BE_TURNED_OFF);
            System.out.println("rate: "+RATE_OF_COMPROMISED_DEVICES);
            System.out.println("mobiles per run: "+NUM_OF_MOBILE_DEVICES);
            System.out.println("Offloading Threshold:"  + OFFLOADING_THRESHOLD);
            System.out.println("Offloading Strategy: " + OFFLOADING_STRATEGY);

            FileOutputStream stream = new FileOutputStream("privacy/output");
            LogMobile.ENABLED = true;
            LogMobile.setOutput(stream);
            Log.enable();
            // Log.disable();
            Log.setOutput(stream);
            Log.printLine("Starting Test4...");

            /* init cloudSim */
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUser, calendar, traceFlag);

            /* more inits */
            String appId = "TestApp";
            setRand(new Random(SEED * Integer.MAX_VALUE));

            Log.printLine("\n########  CREATING DEVICES  ########\n");

            /* create Datacenters / Devices */
            Datacenter cloud = createDatacenter("Cloud");

            field = SimField.getFieldForBeijing();

            allPaths = dbConnector.getAllPaths();

            List<Path> selectedPaths = getRandomPaths(NUM_OF_MOBILE_DEVICES);

            /* create FogDevice(s) */
            createFogDevices(cloud.getId(), selectedPaths);

            System.out.println("relevante nodes: "+relevantFogDevicesList.size() + " davon komp: "+relevantCompromisedDevices.size());

            for (FogDevice fogDevice : relevantFogDevicesList) {
                deviceMap.addDevice(fogDevice);
            }

            /* create AccessPoints */
            createAccessPoints();
            Log.printLine(accessPointList);
            for (ApDevice accessPoint : accessPointList) {
                deviceMap.addDevice(accessPoint);
            }


            long before = System.currentTimeMillis();

            /* create Attacker */
            Attacker attacker = new Attacker("attacker", allCompromisedFogDevices, allFogDevices, allPaths);
            attackerList.add(attacker);

            System.out.println("all paths loaded in : "+((System.currentTimeMillis() - before)/1000) + " sekunden");

        /*
            var allPreProcessedPaths = attacker.getKnownPaths();

            for (Integer i : allPreProcessedPaths.keySet()){
                String result = "";

                String s = "D:/BA/theresasPaths/pfad_"+i+".csv";

                File f = new File(s);
                FileWriter writer = new FileWriter(f);

                for(Integer j : allPreProcessedPaths.get(i)){
                    FogDevice device = deviceMap.getFogDeviceList().get(j);
                    result += "\n" + (int)device.getCoord().getCoordX() + ","+(int)device.getCoord().getCoordY() ;
                }
                result = result.substring(0, result.length()-1);

                writer.write(result);
                writer.close();
            }



            int minDist = -1;
            int maxDist = 0;
            int totalDist = 0;

            var allPreProcessedPaths = attacker.getKnownPaths();


            for (Integer i : allPreProcessedPaths.keySet()){
                LinkedList<Integer> integers = allPreProcessedPaths.get(i);
                double localDist = 0;

                System.out.println("size: "+ allPreProcessedPaths.get(i).size() + "   " +allPreProcessedPaths.get(i));

                for (int j=1 ; j<integers.size(); j++){
                    int first = integers.get(j-1);
                    int second = integers.get(j);

                    Coordinate prev = deviceMap.getFogDeviceList().get(first).getCoord();
                    Coordinate curr = deviceMap.getFogDeviceList().get(second).getCoord();

                    localDist += Coordinate.calcEuclidDist(prev, curr);
                }
                if (localDist > maxDist){
                    maxDist = (int)localDist;
                }else if(minDist == -1 || localDist < minDist){
                    minDist = (int)localDist;
                }
                System.out.println("localDist: "+localDist);
                totalDist += (int) localDist;
            }

            System.out.println("total: "+totalDist);
            System.out.println("min: "+ minDist);
            System.out.println("max:" +maxDist);
            System.out.println("avg:"+(totalDist/50));

            */

            Log.print("\nCompromisedDevices: ");
            for (FogDevice fogDevice1 : relevantCompromisedDevices) {
                fogDevice1.addAttacker(attacker);
            }

            switch (SCENARIO) {
                case 1:
                    attacker.setHasDeviceMap(false);
                    break;

                case 2:
                    attacker.setHasDeviceMap(true);
                    break;
            }

            setRand(new Random(SEED2 * Integer.MAX_VALUE));


            /* create MobileDevice(s) */
            createMobileDevices(appId);


            /* create Broker*/
            for (MobileDevice mobileDevice : mobileDeviceList) {
                FogBroker broker = new FogBroker("BrokerOf" + mobileDevice.getName());
                brokerList.add(broker);
                Log.printLine(broker.getName() + " created");
            }


            Log.printLine("\n\n\n########  CONNECTIONS  ########\n");

            /* configure network of FogDevices */
            // TODO: Offloading needs latencies, bandwidth and other device related data to calculate offloading target
           // createFogDevicesNetwork();
            Log.printLine();




            /* connect MobileDevices and the closest AccessPoint */
            for (MobileDevice mobileDevice : mobileDeviceList) {
                if (ApDevice.connectApSmartThing(accessPointList, mobileDevice, getRand().nextDouble())) {
                    Log.printLine(mobileDevice.getName() + " connected to " + mobileDevice.getSourceAp().getName());
                } else {
                    Log.printLine(mobileDevice.getName() + " not connected");
                }
            }


            /* connect MobileDevices and the closest FogDevice */
            for (MobileDevice mobileDevice : mobileDeviceList) {
                FogDevice closest = Distances.theClosestServerCloudlet(relevantFogDevicesList, mobileDevice);
                mobileDevice.setSourceServerCloudlet(closest);


                /* create symbolic link between Mobile and Fog Device */
                closest.connectServerCloudletSmartThing(mobileDevice);
                closest.setSmartThingsWithVm(mobileDevice, Policies.ADD);

                Log.printLine(mobileDevice.getName() + " connected to closest Fog Device: " + closest.getName());
            }

            /* connect AccessPoint to closest FogDevice */

            for (ApDevice accessPoint : accessPointList) {
                FogDevice closest = Distances.theClosestServerCloudletToAp(relevantFogDevicesList, accessPoint);


                Log.printLine("closest Fog Device of " + accessPoint.getName() + " is " + closest.getName());
                accessPoint.setServerCloudlet(closest);
                accessPoint.setParentId(closest.getMyId());
                closest.setApDevices(accessPoint, Policies.ADD);

                NetworkTopology.addLink(closest.getMyId(), accessPoint.getMyId(), accessPoint.getDownlinkBandwidth(), getRand().nextDouble());
            }


            Log.printLine();

            Log.printLine("\n########  CREATING APPLICATIONS  ########\n");

            /* create VM (AppModule) for each MobileDevice */
            for (MobileDevice mobileDevice : mobileDeviceList) {
                String applicationId = "ApplicationOf" + mobileDevice.getName();
                createVm(mobileDevice, applicationId);
            }
            Log.printLine();

            /* connect broker and VMs */
            int i = 0;
            for (FogBroker broker : brokerList) {
                List<Vm> tempVmList = new ArrayList<>();
                tempVmList.add(mobileDeviceList.get(i++).getVmMobileDevice());
                broker.submitVmList(tempVmList);
                Log.printLine("Broker " + broker.getName() + " has VMs: " + broker.getVmList());
            }


            /* create Application */
            i = 0;
            for (FogBroker broker : brokerList) {
                MobileDevice mobileDevice = mobileDeviceList.get(i);
                appIdList.add("ApplicationOf" + mobileDevice.getName());
                AppModule MobileDeviceVm = (AppModule) mobileDeviceList.get(i).getVmMobileDevice();
                Application application = createApplication(appIdList.get(i), broker.getId(), i, MobileDeviceVm);
                applicationList.add(application);
                i++;
            }


            /* link sensors and actuators for each MobileDevice / broker */
            for (MobileDevice mobileDevice : mobileDeviceList) {

                int mobileId = 0;
                int brokerId = brokerList.get(mobileId).getId();
                //int brokerId = brokerList.get(mobileDevice.getMyId()).getId();
                appId = appIdList.get(mobileId);
                //appId = appIdList.get(mobileDevice.getMyId());

                for (MobileSensor sensor : mobileDevice.getSensors()) {
                    sensor.setAppId(appId);
                    sensor.setUserId(brokerId);
                    sensor.setGatewayDeviceId(mobileDevice.getId());
                    sensor.setLatency(5.0);
                }

                for (MobileActuator actuator : mobileDevice.getActuators()) {
                    actuator.setAppId(appId);
                    actuator.setUserId(brokerId);
                    actuator.setGatewayDeviceId(mobileDevice.getId());
                    actuator.setLatency(2.0);
                    //actuator.setActuatorType();
                }
            }


            Log.printLine("\n########  MODULE MAPPING AND CONTROLLING  ########\n");

            /* create ModuleMapping */
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

            for (Application application : applicationList) {
                application.setPlacementStrategy("Mapping");
            }

            for (FogDevice fogDevice2 : relevantFogDevicesList) {
                i = 0;
                for (MobileDevice mobileDevice : mobileDeviceList) {
                    if (mobileDevice.getApDevices() != null) {
                        if (fogDevice2.equals(mobileDevice.getSourceServerCloudlet())) {
                            moduleMapping.addModuleToDevice(((AppModule) mobileDevice.getVmMobileDevice()).getName(), fogDevice2.getName(), 1);
                            moduleMapping.addModuleToDevice("module" + mobileDevice.getMyId(), mobileDevice.getName(), 1);
                        }
                    }
                    i++;
                }
            }

            /* create controller */
            MobileController mobileController = new MobileController("MobileController", relevantFogDevicesList, accessPointList, mobileDeviceList, brokerList, moduleMapping, migrationPointPolicy, migrationStrategyPolicy, stepPolicy, map, SEED, migrationable);

            for(int j = 0; j < mobileDeviceList.size() ; j++){
                deviceMap.addDevice(mobileDeviceList.get(j));
                setMobilityData(mobileDeviceList.get(j), mobileController,selectedPaths.get(j));
                Log.printLine(mobileDeviceList.get(j).getName() + " path: " + mobileDeviceList.get(j));
            }

            attacker.initMaps(mobileDeviceList);

            i = 0;

            for (Application application : applicationList) {
                mobileController.submitApplication(application, 1);
            }

            Log.printLine("\n########  STATISTIC DATA  ########\n");

            // print response time matrix
            BufferedWriter csvWriter = new BufferedWriter(new FileWriter("results/response_time_matrix_offloading.csv", false));


            //
            //TODO determine which fogDevices have to be iterated
            //
            csvWriter.write("APs; ");
            // TODO(markus): Check whether we need relevantFogDevicesList or allFogDevices
            for (FogDevice current : relevantFogDevicesList) {
                csvWriter.write(current.getName() + "; ");
            }
            csvWriter.newLine();

            BandwidthCpuResponseTimeCalculator ctemp = new BandwidthCpuResponseTimeCalculator();
            MobileDevice m = mobileDeviceList.get(0);
            ApDevice beforeAp = m.getSourceAp();
            for (ApDevice current : accessPointList) {
                m.setSourceAp(current);
                csvWriter.write(current.getName() + "; ");

                // TODO(markus): Check whether we need relevantFogDevicesList or allFogDevices
                for (FogDevice target : relevantFogDevicesList) {
                    double r = ctemp.calculateResponseTime(allFogDevices, accessPointList, m, target, new OffloadingTask(-1, -1, 20, 2000, 2));
                    csvWriter.write(String.format("%,.4f; ", r));
                }

                csvWriter.newLine();
            }
            csvWriter.flush();
            csvWriter.close();

            m.setSourceAp(beforeAp);

            long time2 = System.currentTimeMillis();

            System.out.println("start sim.  Init took: "+ (time2 - time1)/1000);


            /* Simulation */
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            startTime = TimeKeeper.getInstance().getSimulationStartTime();

            Log.printLine("\nStarting Simulation at " + Calendar.getInstance().getTime());
            CloudSim.startSimulation();

            executionTime = Calendar.getInstance().getTimeInMillis();

            Log.printLine();

            PrintWriter resultsWriter = new PrintWriter(new FileWriter(filename, true));
            resultsWriter.write("traceCompVal;sizeOfUncertainty;scenario;canBeTurnedOff;rate\n");

            //attacker.cleanupPositionMap();

            long time3 = System.currentTimeMillis();

            System.out.println("sim finished, took: "+ (time3 - time2)/1000);


            /* results */
            ArrayList<Integer> traceCompVal  = attacker.calcTraceCompVal();
            ArrayList<Double> sizeOfUncertainty = attacker.calcSizeOfUncertaintyRegion();

            for(int it = 0 ; it < traceCompVal.size() ; it++ ){
                resultsWriter.write(traceCompVal.get(it)+ ";" );
                resultsWriter.printf(Locale.US, "%.9f", sizeOfUncertainty.get(it));

                resultsWriter.write(";"+SCENARIO+";"+MOBILE_CAN_BE_TURNED_OFF+";"+RATE_OF_COMPROMISED_DEVICES+"\n");    // path information

            }

            double total1 = 0;
            for(Double jk : sizeOfUncertainty){
                total1 = total1 + jk;
            }

            total1 = total1/sizeOfUncertainty.size();

            int total2 = 0;
            for(Integer integer : traceCompVal){
                total2 = total2 + integer;
            }

            double avgTraceVal = (double) total2/ (double) sizeOfUncertainty.size();

            System.out.println("size of uncertainty avg: "+total1 );
            System.out.println("traceCompVal avg: "+avgTraceVal);
            resultsWriter.close();
            Log.printLine("\nTest4 finished");

            long time4 = System.currentTimeMillis();

            System.out.println("results finished, took: "+ (time4 - time3)/1000+  "     total time: "+(time3-time1)/1000);
            System.out.println("\n####################################################\n");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }
    }

    private static List<Path> getRandomPaths(int numOfMobileDevices) {

        List<Path> randomPaths = new ArrayList<>();

        List<Integer> pathIdsWithDuplicates = new LinkedList<>() ;
        allPaths.stream().forEach(path -> {

            int k = path.getNrOfDuplicates() +1;

            for(int i = 0; i<k ; i++){
                pathIdsWithDuplicates.add(path.getPathId());
            }
        });

        Collections.shuffle(pathIdsWithDuplicates);
        int x = pathIdsWithDuplicates.size();


        for(int i = 0 ; i < numOfMobileDevices ; i++){

            int rand = new Random().nextInt(x);
            Path toAdd = allPaths.get(pathIdsWithDuplicates.get(rand));
            randomPaths.add(toAdd);
        }

        return randomPaths;
    }

    private static void setMobilityData(MobileDevice mobileDevice, MobileController controller, Path loadedPath) {
        mobileDevice.setPath( loadedPath);
        controller.setInitialCoordinate(mobileDevice);
    }


    private static void createFogDevicesStarTopologyNetwork() {

        //get square-fields of size fieldEdgeLen x fieldEdgeLen

        List<Coordinate> corners = field.sortCornersClockwise(field.getCorners());
        double x = Coordinate.calcDistance(corners.get(0), corners.get(1));
        double y = Coordinate.calcDistance(corners.get(1), corners.get(2));

        double bearingX = Coordinate.calcBearingAngle(corners.get(0), corners.get(1), false);
        double bearingY = Coordinate.calcBearingAngle(corners.get(1), corners.get(2), false);

        Coordinate initialCorner = corners.get(0);

        double fieldEdgeLen = 10000; // 10km x 10km fields

        for (double i = 0.0; i <= x; i += fieldEdgeLen) {
            for (double j = 0.0; j <= y; j += fieldEdgeLen) {
                Coordinate moveDirectionX = Coordinate.findCoordinateForBearingAndDistance(initialCorner, bearingX, i);
                Coordinate bottomLeft = Coordinate.findCoordinateForBearingAndDistance(moveDirectionX, bearingY, j);

                moveDirectionX = Coordinate.findCoordinateForBearingAndDistance(bottomLeft, bearingX, fieldEdgeLen);
                Coordinate topRight = Coordinate.findCoordinateForBearingAndDistance(moveDirectionX, bearingY, fieldEdgeLen);

                //bounding-box
                double minLat = bottomLeft.getLat();
                double maxLat = topRight.getLat();
                double minLon = bottomLeft.getLon();
                double maxLon = topRight.getLon();

                //TODO test if this runs in reasonable time.
                ArrayList<FogDevice> devicesInField = (ArrayList<FogDevice>) allFogDevices.stream()
                        .filter(device -> device.getPosition().getCoordinate().isInBoundingBox(minLat,maxLat,minLon, maxLon))
                        .collect(Collectors.toList());

                FogDevice centralFogNode = devicesInField.get(0); //might want to create a seperate "router-node" instead of using an existing one
                HashMap<Integer, Double> network = new HashMap<>();

                //connect all fogNodes to centralFogNode
                devicesInField.forEach(device -> {
                    if (!device.equals(centralFogNode)) {
                        double latency =  LATENCY_BETWEEN_FOG_DEVICES; //TODO Latency richtig berechnen (vorher wurde da irgendwas mit Column/Line berechnet siehe alte Funktion)

                        if (centralFogNode.getUplinkBandwidth() < device.getDownlinkBandwidth()) {
                            network.put(device.getMyId(), centralFogNode.getUplinkBandwidth());
                            NetworkTopology.addLink(centralFogNode.getMyId(), device.getMyId(), centralFogNode.getUplinkBandwidth(), latency + getRand().nextDouble());

                            Log.printLine("Bandwidth between " + centralFogNode.getName() + " and " + device.getName() + ": " + centralFogNode.getUplinkBandwidth());
                        } else {
                            network.put(device.getMyId(), device.getDownlinkBandwidth());
                            NetworkTopology.addLink(centralFogNode.getMyId(), device.getMyId(), device.getDownlinkBandwidth(), latency + getRand().nextDouble());

                            Log.printLine("Bandwidth between " + centralFogNode.getName() + " and " + device.getName() + ": " + centralFogNode.getDownlinkBandwidth());
                        }
                    }});

                centralFogNode.setNetServerCloudlets(network);
            }
        }
    }

    //
    //TODO remove once createFogDevicesStarTopologyNetwork() is working
    //
    private static void createFogDevicesNetwork() {

        HashMap<Integer, Double> network = new HashMap<>();

        long start = System.currentTimeMillis();

        int i = 1, j = 1, line, column;
        for (FogDevice fogDeviceX : allFogDevices) {
            j = 1;
            for (FogDevice fogDeviceY : allFogDevices) {

                if (fogDeviceX.equals(fogDeviceY)) {
                    continue; // NOTE(markus): previously break; now continue as I think this was the intended behavior.
                }

                line = (int) (j / 12) - (int) (i / 12);
                if (line < 0) {
                    line *= -1;
                }
                column = (int) (j / 12) - (int) (i / 12);
                if (column < 0) {
                    column *= -1;
                }

                if (fogDeviceX.getUplinkBandwidth() < fogDeviceY.getDownlinkBandwidth()) {
                    network.put(fogDeviceY.getMyId(), fogDeviceX.getUplinkBandwidth());
                    NetworkTopology.addLink(fogDeviceX.getMyId(), fogDeviceY.getMyId(), fogDeviceX.getUplinkBandwidth(), (line + column) * LATENCY_BETWEEN_FOG_DEVICES + getRand().nextDouble());

                    Log.printLine("Bandwidth between " + fogDeviceX.getName() + " and " + fogDeviceY.getName() + ": " + fogDeviceX.getUplinkBandwidth());
                } else {
                    network.put(fogDeviceY.getMyId(), fogDeviceY.getDownlinkBandwidth());
                    NetworkTopology.addLink(fogDeviceX.getMyId(), fogDeviceY.getMyId(), fogDeviceY.getDownlinkBandwidth(), (line + column) * LATENCY_BETWEEN_FOG_DEVICES + getRand().nextDouble());

                    Log.printLine("Bandwidth between " + fogDeviceX.getName() + " and " + fogDeviceY.getName() + ": " + fogDeviceX.getDownlinkBandwidth());
                }
                j++;

                Log.printLine("Delay between " + fogDeviceX.getName() + " and " + fogDeviceY.getName() + ": " + NetworkTopology.getDelay(fogDeviceX.getMyId(), fogDeviceY.getMyId()));
            }
            i++;
            Log.printLine("Downlink Bandwith of " + fogDeviceX.getName() + ": " + fogDeviceX.getDownlinkBandwidth());

            fogDeviceX.setNetServerCloudlets(network);
        }
    }

    private static void createAccessPoints() {

        //TODO adjust to start-topology

        for (int i = 0; i < relevantFogDevicesList.size(); i++) {

            Position position = relevantFogDevicesList.get(i).getPosition();

            int id = i;

            int downLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkLatency = 1;
            int energy = 200; // energy consumption
            int maxMobDevInAP = MaxAndMin.MAX_ST_IN_AP; //max number of MobileDevice in AccessPoint range

            ApDevice accessPoint = new ApDevice("AcessPoint" + id, position, id, downLinkBw, energy, maxMobDevInAP, upLinkBw, upLinkLatency);
            accessPointList.add(accessPoint);

            Log.printLine(accessPoint.getName() + " created at " + accessPoint.getPosition().getCoordinate().toString());
        }
    }


    private static void createMobileDevices(String appId) {
        try {

            for (int i = 0; i < NUM_OF_MOBILE_DEVICES; i++) {

                /* Movement Settings */
                int direction = 2; //(NORTHEAST) OR: getRand().nextInt(9); //Choose from 9 directions (NONE, NORTH, SOUTH, ...)
                int speed = getRand().nextInt(MaxAndMin.MAX_SPEED - 1) + 1;

                /* MobileDevice Configurations */
                String name = "MobileDevice" + i;
                Coordinate coord = Coordinate.createGPSCoordinate(0, 0);
                Position position = new Position(coord, 0, direction, speed);
                int id = 0; //FogUtils.generateEntityId();
                long storage = 1000 * 1024 * 1024;
                int bandwith = 1000 * 1024 * 1024;
                int ram = 25000;
                FogLinearPowerModel powerModel = new FogLinearPowerModel(107.339d, 83.433d);

                int downLinkBw = 1 * 1024 * 1024; // 100Mbits
                int upLinkBw = 2 * 1024 * 1024; // 100Mbits
                int upLinkLatency = 2;


                /* set migration policy */
                VmMigrationTechnique migrationTechnique = null;
                if (policyReplicaVm == Policies.MIGRATION_COMPLETE_VM) {
                    migrationTechnique = new CompleteVM(migrationPointPolicy);
                } else if (policyReplicaVm == Policies.MIGRATION_CONTAINER_VM) {
                    migrationTechnique = new ContainerVM(migrationPointPolicy);
                } else if (policyReplicaVm == Policies.LIVE_MIGRATION) {
                    migrationTechnique = new LiveMigration(migrationPointPolicy);
                }


                /* create Sensors and Actuators */
                DeterministicDistribution distribution = new DeterministicDistribution(TRANSMISSION_TIME);

                Set<MobileSensor> sensorSet = new HashSet<>();

                for (int j = 0; j < NUM_OF_SENSORS_PER_DEVICE; j++) {
                    MobileSensor sensor = new MobileSensor("SENSOR", "SENSOR", i, appId, distribution);
                    sensorSet.add(sensor);
                }

                Set<MobileActuator> actuatorSet = new HashSet<>();

                for (int j = 0; j < NUM_OF_ACTUATORS_PER_DEVICE; j++) {
                    MobileActuator actuator = new MobileActuator("AACTUATOR", i, appId, "ACTUATOR");
                    actuatorSet.add(actuator);
                }


                /* create mobile Device */

                /* Processing Element (PE) represents CPU unit */
                List<Pe> peList = new ArrayList<>();
                int mips = 28000000;
                peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

                PowerHost host = new PowerHost(id, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bandwith), storage, peList, new StreamOperatorScheduler(peList), powerModel);
                List<Host> hostList = new ArrayList<>();
                hostList.add(host);

                /* Device Characteristics */
                String arch = "x86"; // system architecture
                String os = "Android"; // operating system
                String vmm = "Empty";// Empty
                double vmSize = MaxAndMin.MAX_VM_SIZE;
                double time_zone = 10.0; // time zone this resource located
                double cost = 1.0; // the cost of using processing in this resource
                double costPerMem = 0.005; // the cost of using memory in this resource
                double costPerStorage = 0.0001; // the cost of using storage in this resource
                double costPerBw = 0.001; // the cost of using bw in this resource

                int schedulingInterval = 2;

                LinkedList<Storage> storageList = new LinkedList<>();

                FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

                /* Allocation Policy */
                AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(hostList);

                float maxServiceValue = getRand().nextFloat() * 100;
                double ratePerMips = 0.01;

                MobileDevice mobileDevice = new MobileDevice(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, upLinkBw, downLinkBw, upLinkLatency, ratePerMips, position, id, maxServiceValue, vmSize, migrationTechnique, offloadingScheduler, offloadingStrategy, offloadingResponseTimeCalculator);
                //MobileDevice mobileDevice = new MobileDevice(name, coordX, coordY, id, direction, speed);

                /* add Sensors and Actuators to the Device */
                mobileDevice.setSensors(sensorSet);
                mobileDevice.setActuators(actuatorSet);

                /* more setup */
                mobileDevice.setTempSimulation(0);
                mobileDevice.setTimeFinishDeliveryVm(-1);
                mobileDevice.setTimeFinishHandoff(0);

                mobileDeviceList.add(mobileDevice);

                Log.printLine(mobileDevice.getName() + " created at " + mobileDevice.getPosition().getCoordinate().toString() + " with Direction " + mobileDevice.getPosition().getDirection() + " and Speed " + mobileDevice.getPosition().getSpeed());

                /* create User */
                User user = new User("userOf" + name, mobileDevice);
                mobileDevice.setDeviceUser(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }
    }

    /**
     * @param parentId
     * @param path     only initializes fogNodes in proximity to selected path to improve perfomance
     */
    private static void createFogDevices(int parentId, List<Path> selectedPaths) {

        //
        //TODO refactor this method
        //

        long t1 = System.currentTimeMillis();

        List<Position> selectedPositions = new ArrayList<>();

        for(Path path : selectedPaths){
            selectedPositions.addAll(path.getPositions());
        }
        selectedPositions = selectedPositions.stream().distinct().collect(Collectors.toList()); //remove duplicates

        HashMap<Integer,Coordinate> fogNodePositions = dbConnector.getAllFogNodePositions();

        NUM_OF_FOG_DEVICES = fogNodePositions.size();
        NUM_OF_ACCESS_POINTS = NUM_OF_FOG_DEVICES;

        /* make random devices compromised */
        relevantCompromisedDevices = new ArrayList<>();

        int n = (int) (fogNodePositions.size() * RATE_OF_COMPROMISED_DEVICES);

        ArrayList<Coordinate> allCoords = new ArrayList<>(fogNodePositions.values());
        Collections.shuffle(allCoords);
         List<Coordinate> compromisedCoords = allCoords.subList(0, n);

        HashMap<Coordinate, Boolean> allPositions = new HashMap<>();
        HashMap<Coordinate, Boolean> relevantPositions = new HashMap<>();


        for ( Integer i : fogNodePositions.keySet()) {
            Coordinate coord = fogNodePositions.get(i);
            if(compromisedCoords.contains(coord)){
                allPositions.put(coord, true);
            }else{
                allPositions.put(coord,false);
            }

            for (Position position : selectedPositions) {
                double distance = Coordinate.calcDistance(coord, position.getCoordinate());
                if (distance < 1500) { //only add nodes in proximity of 3 km
                    if (compromisedCoords.contains(coord)) {
                        relevantPositions.put(coord, true);
                    } else {
                        relevantPositions.put(coord, false);
                    }
                   break;
                }
            }
        }

        try {

            for (Integer i : fogNodePositions.keySet()) {
                Coordinate coord = fogNodePositions.get(i);
                int id = i;
                String name = "FogDevice" + id;

                long storage = 1000 * 1024 * 1024;
                int bandwith = 1000 * 1024 * 1024;
                int ram = 25000;
                int schedulingInterval = 10;
                FogLinearPowerModel powerModel = new FogLinearPowerModel(107.339d, 83.433d);


                /* set migration strategy */
                DecisionMigration migrationStrategy = null;
                if (migrationStrategyPolicy == Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET) {
                    migrationStrategy = new LowestDistBwSmartThingServerCloudlet(relevantFogDevicesList, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_LATENCY) {
                    migrationStrategy = new LowestLatency(relevantFogDevicesList, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_DIST_BW_SMARTTING_AP) {
                    migrationStrategy = new LowestDistBwSmartThingAP(relevantFogDevicesList, accessPointList, migrationPointPolicy, policyReplicaVm);
                }

                /* set before migration */
                BeforeMigration beforeMigration = null;
                if (policyReplicaVm == Policies.MIGRATION_COMPLETE_VM) {
                    beforeMigration = new PrepareCompleteVM();
                } else if (policyReplicaVm == Policies.MIGRATION_CONTAINER_VM) {
                    beforeMigration = new PrepareContainerVM();
                } else if (policyReplicaVm == Policies.LIVE_MIGRATION) {
                    beforeMigration = new PrepareLiveMigration();
                }

                /* Processing Element (PE) represents CPU unit */
                List<Pe> peList = new ArrayList<>();
                int mips = 28000000;
                peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

                PowerHost host = new PowerHost(id, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bandwith), storage, peList, new StreamOperatorScheduler(peList), powerModel);
                List<Host> hostList = new ArrayList<>();
                hostList.add(host);

                /* Device Characteristics */
                String arch = "x86"; // system architecture
                String os = "Linux"; // operating system
                String vmm = "Empty";// Empty
                double time_zone = 10.0; // time zone this resource located
                double cost = 3.0; // the cost of using processing in this resource
                double costPerMem = 0.05; // the cost of using memory in this resource
                double costPerStorage = 0.001; // the cost of using storage in this resource
                double costPerBw = 0.0; // the cost of using bw in this resource
                LinkedList<Storage> storageList = new LinkedList<>();

                FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);


                /* Allocation Policy */
                AppModuleAllocationPolicy vmAllocationPolicy = new AppModuleAllocationPolicy(hostList);
                double upLinkRandom = (int) ((getRand().nextDouble() * (MAX_UP_BANDWITH - MIN_UP_BANDWITH)) + MIN_UP_BANDWITH);
                double downLinkRandom = (int) ((getRand().nextDouble() * (MAX_DOWN_BANDWITH - MIN_DOWN_BANDWITH)) + MIN_DOWN_BANDWITH);
                int upLinkLatency = 1;
                double ratePerMips = 0.01;

                /* Service Offer */
                Service serviceOffer = new Service();
                serviceOffer.setType(getRand().nextInt(10000) % MaxAndMin.MAX_SERVICES);
                if (serviceOffer.getType() == Services.HIBRID
                        || serviceOffer.getType() == Services.PUBLIC) {
                    serviceOffer.setValue(getRand().nextFloat() * 10);
                } else {
                    serviceOffer.setValue(0);
                }



                if(relevantPositions.keySet().contains(coord)){

                    //
                    //TODO update FogDevice Constructors
                    //

                    FogDevice fogDevice = new FogDevice(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, upLinkRandom, downLinkRandom, upLinkLatency, ratePerMips, coord, id, serviceOffer, migrationStrategy, policyReplicaVm, beforeMigration, offloadingResponseTimeCalculator);
//              FogDevice fogDevice = new FogDevice(name, coordX, coordY, id);
                    // TODO(): setzen von cloudserver/datacenter
                    fogDevice.setParentId(parentId);
                    relevantFogDevicesList.add(fogDevice);
                    allFogDevices.add(fogDevice);
                    Log.printLine(fogDevice.getName() + " created at " + fogDevice.getPosition().getCoordinate().toString());
                    /* create Owner */
                    Owner owner = new Owner("ownerOf" + name, fogDevice);
                    fogDevice.setDeviceOwner(owner);
                    if (allPositions.get(coord) == true){
                        allCompromisedFogDevices.add(fogDevice);
                    }
                    if (relevantPositions.get(coord) == true) {
                        relevantCompromisedDevices.add(fogDevice);
                    }
                } else {
                    //dummy instance
                    FogDevice fogDevice = new FogDevice( coord, id);
                    allFogDevices.add(fogDevice);
                    if(allPositions.get(coord)==true){
                        allCompromisedFogDevices.add(fogDevice);
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            System.out.println("dauer Pfade setzten: "+(t2-t1));
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }
    }

    private static void createVm(MobileDevice mobileDevice, String appId) {

        /* properties of virtual machine */
        int vmId = 0;
        int mips = 2000;
        long size = getRand().nextInt(MaxAndMin.MAX_VM_SIZE);
        int ram = 64;
        long bw = 1000;
        int pesNumber = 1; //num of CPUs
        String vmm = "Xen";
        String appModuleName = "AppModuleVmOf" + mobileDevice.getName();

        CloudletScheduler scheduler = new TupleScheduler(500, 1);

        //
        //TODO update AppModule Constructor
        //
        AppModule testVm = new AppModule(mobileDevice.getMyId(), appModuleName, appId, mobileDevice.getMyId(), mips, ram, bw, size, "VmOf" + mobileDevice.getName(), scheduler, new HashMap<Pair<String, String>, SelectivityModel>());


        /* more setup for MobileDevice */
        mobileDevice.setVmMobileDevice(testVm);
        boolean worked = mobileDevice.getSourceServerCloudlet().getHost().vmCreate(testVm);
        mobileDevice.setVmLocalServerCloudlet(mobileDevice.getSourceServerCloudlet());

        Log.printLine(mobileDevice.getName() + " -- Position: " + mobileDevice.getPosition().getCoordinate() + ", Direction: " + mobileDevice.getPosition().getDirection() + ", Speed: " + mobileDevice.getPosition().getSpeed());
        Log.printLine("    Source AP: " + mobileDevice.getSourceAp() + ", Destination AP: " + mobileDevice.getDestinationAp() + ", Host: " + mobileDevice.getHost().getId());
        Log.printLine("    Local Server: " + mobileDevice.getVmLocalServerCloudlet().getName() + ", Apps: " + mobileDevice.getVmLocalServerCloudlet().getActiveApplications() + ", Map: " + mobileDevice.getVmLocalServerCloudlet().getApplicationMap());
        if (mobileDevice.getDestinationServerCloudlet() == null) {
            Log.printLine("    Destination server: null, Apps: null, Map: null");
        } else {
            Log.printLine("    Destination server: " + mobileDevice.getDestinationServerCloudlet().getName() + ", Apps: " + mobileDevice.getDestinationServerCloudlet().getActiveApplications() + ", Map: " + mobileDevice.getDestinationServerCloudlet().getApplicationMap());
        }

        //Vm vm = new Vm(vmId, broker.getId(), mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

        //vmList.add(vm);
        //broker.submitVmList(vmList);
    }

    private static Application createApplication(String appId, int userId, int myId, AppModule userVm) {

        Application application = Application.createApplication(appId, userId);

        /* add module client to application model */
        application.addAppModule(userVm);
        application.addAppModule("module" + myId, 10);
        application.addAppModule("sensorRessource" + myId, 10);


        /* Connect AppModules with AppEdges*/
        application.addAppEdge("SENSOR", "module" + myId, 1000, 2000, "SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("module" + myId, userVm.getName(), 1000, 2000, "MODULE_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(userVm.getName(), "module" + myId, 1000, 2000, "MODULE_DATA", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("module" + myId, "ACTUATOR", 1000, 2000, "ACTUATOR", Tuple.DOWN, AppEdge.ACTUATOR);


        /* Add input - output relationships of the Modules */
        application.addTupleMapping("module" + myId, "SENSOR", "MODULE_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping(userVm.getName(), "MODULE_DATA", "MODULE_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping("module" + myId, "MODULE_DATA", "ACTUATOR", new FractionalSelectivity(1.0));


        /* (Define AppLoops for monitoring latencies) */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("SENSOR");
            add("module" + myId);
            add(userVm.getName());
            add("ACTUATOR");
        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};

        application.setLoops(loops);

        return application;
    }

    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        //    our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        //4. Create Host with its id and list of PEs and add them to the list of machines
        int hostId = 0;
        int ram = 2048; //host memory (MB)
        long storage = 1000000; //host storage
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        ); // This is our machine

        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;        // the cost of using memory in this resource
        double costPerStorage = 0.001;    // the cost of using storage in this resource
        double costPerBw = 0.0;            // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();    //we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.printLine(datacenter.getName() + " created");

        return datacenter;
    }

    private static FogBroker createBroker() {

        FogBroker broker = null;
        try {
            broker = new FogBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        TestExample4.rand = rand;
    }

    public static int getSCENARIO() {
        return SCENARIO;
    }

    public static void setSCENARIO(int SCENARIO) {
        TestExample4.SCENARIO = SCENARIO;
    }

    public static DeviceMap getDeviceMap() {
        return deviceMap;
    }

    public static ArrayList<FogDevice> getRelevantFogDevicesList() {
        return relevantFogDevicesList;
    }

    public static void setRelevantFogDevicesList(ArrayList<FogDevice> relevantFogDevicesList) {
        TestExample4.relevantFogDevicesList = relevantFogDevicesList;
    }

    public static List<FogDevice> getRelevantCompromisedDevices() {
        return relevantCompromisedDevices;
    }

    public static void setRelevantCompromisedDevices(List<FogDevice> relevantCompromisedDevices) {
        TestExample4.relevantCompromisedDevices = relevantCompromisedDevices;
    }

    public static int getNumOfFogDevices() {
        return NUM_OF_FOG_DEVICES;
    }

    public static double getRateOfCompromisedDevices() {
        return RATE_OF_COMPROMISED_DEVICES;
    }

    public static void setRateOfCompromisedDevices(double rateOfCompromisedDevices) {
        RATE_OF_COMPROMISED_DEVICES = rateOfCompromisedDevices;
    }

    public static void setAveragePathLength(double averagePathLength) {
        TestExample4.averagePathLength = averagePathLength;
    }

    public static int getMobileCanBeTurnedOff() {
        return MOBILE_CAN_BE_TURNED_OFF;
    }

    public static long getStartTime() {
        return startTime;
    }

    public static SimField getSimfield() {
        return field;
    }

    public static ArrayList<FogDevice> getAllFogDevices() {
        return allFogDevices;
    }

    public static void setAllFogDevices(ArrayList<FogDevice> allFogDevices) {
        TestExample4.allFogDevices = allFogDevices;
    }

    public static List<FogDevice> getAllCompromisedFogDevices() {
        return allCompromisedFogDevices;
    }

    public static void setAllCompromisedFogDevices(List<FogDevice> allCompromisedFogDevices) {
        TestExample4.allCompromisedFogDevices = allCompromisedFogDevices;
    }
}
