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
import org.fog.offloading.*;
import org.fog.placement.MobileController;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.privacy.*;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.DBConnector;
import org.fog.utils.PrivacyJsonHelper;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.vmmigration.*;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;
import org.fog.vmmobile.constants.Services;
import org.junit.experimental.theories.Theories;

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
    private static final int NUM_OF_SENSORS_PER_DEVICE = 1;
    private static final int NUM_OF_ACTUATORS_PER_DEVICE = 1;
    private static final int MIN_DOWN_BANDWITH = 500; // Min Down Bandwidth 500 MB/s
    private static final int MAX_DOWN_BANDWITH = 1000; // Max Down Bandwidth 1000 MB/s (1 Gbit)
    private static final int MIN_UP_BANDWITH = 300; // Min Up Bandwidth 300 MB/s
    private static final int MAX_UP_BANDWITH = 1000; // Max Up Bandwidth 1000 MB/s (1 Gbit)
    private static final int TRANSMISSION_TIME = 1;
    private static final int LATENCY_BETWEEN_FOG_DEVICES = 1;
    private static int NUM_OF_ACCESS_POINTS;
    private static int NUM_OF_FOG_DEVICES;

    // TODO(markus): Which scenarios do we need?
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

    private static double RATE_OF_COMPROMISED_DEVICES = 0.05; // Controls the persentage of compromised devices => numCompromised = NUM_OF_FOG_DEVICES * RATE_OF_COMPROMISED_DEVICES

    private static int SEED2 = 28; // MobileDevice, connections, and more
    private static int SEED3 = 5; // Attacker
    private static String filename;
    private static int NUM_OF_MOBILE_DEVICES = 1;

    private  static DBConnector dbConnector = new DBConnector();
    public static PrivacyJsonHelper jsonHelper;

    private static ArrayList<FogDevice> allFogDevices = new ArrayList<>(); // all fog nodes
    private static List<FogDevice> compromisedFogDevices = new ArrayList<>(); // all compromised fog nodes

    private static List<MobileDevice> mobileDeviceList = new ArrayList<>();
    private static List<ApDevice> accessPointList = new ArrayList<>();
    private static List<FogBroker> brokerList = new ArrayList<>();
    private static List<String> appIdList = new ArrayList<>();
    private static List<Application> applicationList = new ArrayList<>();

    /* migration settings (see MobFogSim) */
    private static Random rand;
    private static int migrationStrategyPolicy = Policies.LOWEST_DIST_BW_SMARTTING_AP;
    private static int migrationPointPolicy = Policies.SPEED_MIGRATION_POINT;
    private static int policyReplicaVm = Policies.LIVE_MIGRATION;
    private static int stepPolicy = 1;
    private static boolean migrationable = true;
    private static Coordinate map;
    private static SimField field;

    private static long startTime = 0;
    private static long executionTime = 0;

    private static double averagePathLength = 0;

    private static final IOffloadingResponseTimeCalculator offloadingResponseTimeCalculator = new BandwidthCpuResponseTimeCalculator();

    private static IOffloadingScheduler offloadingScheduler;

    private static double OFFLOADING_THRESHOLD = 0.0462d;
    private static IOffloadingStrategy offloadingStrategy;

    public static HashMap<List<Coordinate>, List<FogDevice>> fogDevicesInField = new HashMap<>();

    public static void main(String args[]) {

        try {

            long time1 = System.currentTimeMillis();

            /* parse settings from command line args
             * or comment these out to use settings from above
             */
            SCENARIO = Integer.parseInt(args[0]);
            RATE_OF_COMPROMISED_DEVICES = Integer.parseInt(args[1])/100d;
            SEED2 = Integer.parseInt(args[2]);
            SEED3 = Integer.parseInt(args[3]);
            OFFLOADING_THRESHOLD = Double.parseDouble(args[4]);
            int OFFLOADING_STRATEGY = Integer.parseInt(args[5]);
            boolean debug = Boolean.parseBoolean(args[6]);
            int iteration = Integer.parseInt(args[7]);

            if (OFFLOADING_STRATEGY == 1) // "BelowThresholdRandomDevice"
                offloadingStrategy = new BelowThresholdRandomDeviceOffloadingStrategy(SEED3, OFFLOADING_THRESHOLD);
            else if (OFFLOADING_STRATEGY == 2) // "BelowThresholdLowestResponseTime"
                offloadingStrategy = new BelowThresholdLowestResponseTimeOffloadingStrategy(OFFLOADING_THRESHOLD);
            else if (OFFLOADING_STRATEGY == 3) // "ClosestFogDevice"
                offloadingStrategy = new ClosestFogDeviceOffloadingStrategy();
            else {
                Log.printLine("Unknown offloading strategy...");
                return;
            }

            System.out.println("Scenario: "+SCENARIO);
            System.out.println("rate: "+RATE_OF_COMPROMISED_DEVICES);
            System.out.println("Offloading Threshold:"  + OFFLOADING_THRESHOLD);
            System.out.println("Offloading Strategy: " + OFFLOADING_STRATEGY);
            System.out.println("Debug: " + debug);
            System.out.println("Iteration: " + iteration);

            filename = "privacy/output_" +SCENARIO + "_" + args[1] + "_" + OFFLOADING_STRATEGY + "_" + iteration + ".json";

            FileOutputStream stream = new FileOutputStream("privacy/output");
            LogMobile.ENABLED = debug;

            if (debug == true) LogMobile.setOutput(stream);

            if (debug== true) Log.enable();
            else Log.disable();

            if (debug == true) Log.setOutput(stream);

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

            Path selectedPath = getRandomPath();

            int travelTime = selectedPath.getPositions().get(selectedPath.getPositions().size() - 1).getTimestamp();
            offloadingScheduler = new FixedOffloadingScheduler(travelTime, 100, 20, 2000, 2);

            /* create FogDevice(s) */
            createFogDevices(cloud.getId());

            System.out.println("Kompromitierte fog nodes: "+ compromisedFogDevices.size());

            /* create AccessPoints */
            createAccessPoints();
            Log.printLine(accessPointList);

            long before = System.currentTimeMillis();

            /* create Attacker */
            Attacker attacker = new Attacker("attacker", allFogDevices, compromisedFogDevices);

            System.out.println("all paths loaded in : "+((System.currentTimeMillis() - before)/1000) + " sekunden");


            Log.print("\nCompromisedDevices: ");
            for (FogDevice fogDevice1 : compromisedFogDevices) {
                fogDevice1.addAttacker(attacker);
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

            //NetworkTopology.generateMatrices();

            /* configure network of FogDevices */
            createFogDevicesStarTopologyNetwork();
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
                FogDevice closest = Distances.theClosestServerCloudlet(allFogDevices, mobileDevice);
                mobileDevice.setSourceServerCloudlet(closest);


                /* create symbolic link between Mobile and Fog Device */
                closest.connectServerCloudletSmartThing(mobileDevice);
                closest.setSmartThingsWithVm(mobileDevice, Policies.ADD);

                Log.printLine(mobileDevice.getName() + " connected to closest Fog Device: " + closest.getName());
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

            for (FogDevice fogDevice2 : allFogDevices) {
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
            MobileController mobileController = new MobileController("MobileController", allFogDevices, accessPointList, mobileDeviceList, brokerList, moduleMapping, migrationPointPolicy, migrationStrategyPolicy, stepPolicy, map, SEED, migrationable);

            for(int j = 0; j < mobileDeviceList.size() ; j++){
                setMobilityData(mobileDeviceList.get(j), mobileController, selectedPath);
                Log.printLine(mobileDeviceList.get(j).getName() + " path: " + mobileDeviceList.get(j));
            }

            i = 0;

            for (Application application : applicationList) {
                mobileController.submitApplication(application, 1);
            }

            long time2 = System.currentTimeMillis();

            System.out.println("start sim.  Init took: "+ (time2 - time1)/1000);

            /*init Jsonhelper */
            jsonHelper = new PrivacyJsonHelper(selectedPath.getPathId(), SCENARIO, allFogDevices, compromisedFogDevices);

            /* Simulation */
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            startTime = TimeKeeper.getInstance().getSimulationStartTime();

            Log.printLine("\nStarting Simulation at " + Calendar.getInstance().getTime());
            CloudSim.startSimulation();

            executionTime = Calendar.getInstance().getTimeInMillis();

            Log.printLine();

            long time3 = System.currentTimeMillis();

            System.out.println("sim finished, took: "+ (time3 - time2)/1000);

            /* results */
            Log.printLine("\nTest4 finished");

            jsonHelper.writeJsonToFile(filename);

            long time4 = System.currentTimeMillis();

            System.out.println("results finished, took: "+ (time4 - time3)/1000+  "     total time: "+(time3-time1)/1000);
            System.out.println("\n####################################################\n");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }
    }

    private static Path getRandomPath() {
        Random random = new Random(SEED2 * Integer.MAX_VALUE);
        int maxCount = dbConnector.getMaxPathCount();
        int index = random.nextInt(maxCount);
        Log.printLine("Loaded path with id: " + index + " from " + maxCount + " available paths");
        System.out.println("Loaded path with id: " + index + " from " + maxCount + " available paths");
        return dbConnector.getPathById(index);
    }

    private static void setMobilityData(MobileDevice mobileDevice, MobileController controller, Path loadedPath) {
        mobileDevice.setPath( loadedPath);
        controller.setInitialCoordinate(mobileDevice);
    }

    private static void createFogDevicesStarTopologyNetwork() {

        int consideredNodes = 0;

        //get square-fields of size fieldEdgeLen x fieldEdgeLen

        List<Coordinate> corners = field.sortCornersClockwise(field.getCorners());
        double x = Coordinate.calcDistance(corners.get(0), corners.get(1));
        double y = Coordinate.calcDistance(corners.get(1), corners.get(2));

        double bearingX = Coordinate.calcBearingAngle(corners.get(0), corners.get(1), false);
        double bearingY = Coordinate.calcBearingAngle(corners.get(1), corners.get(2), false);

        double fieldEdgeLen = 10000; // 10km x 10km fields

        Coordinate initialTopLeft = corners.get(0);

        //add some padding
        initialTopLeft = Coordinate.findCoordinateForBearingAndDistance(initialTopLeft, (bearingX + Math.PI)%(2*Math.PI) , fieldEdgeLen/2);
        initialTopLeft = Coordinate.findCoordinateForBearingAndDistance(initialTopLeft, (bearingY + Math.PI)%(2*Math.PI) , fieldEdgeLen/2);


        Coordinate topLeft = null;
        Coordinate topRight = null;
        Coordinate botLeft = null;
        Coordinate botRight = null;


        for (double i = 0.0; i <= y+fieldEdgeLen; i += fieldEdgeLen) {
            topLeft = Coordinate.findCoordinateForBearingAndDistance(initialTopLeft, bearingY, i);
            botLeft = Coordinate.findCoordinateForBearingAndDistance(initialTopLeft, bearingY, i + fieldEdgeLen);

            for (double j = 0.0; j <= x+fieldEdgeLen; j += fieldEdgeLen) {

                /*
                        topLeft ---- bearingX ------> topRight
                            |                           |
                            |                           |
                            |                           |
                            |                        bearingY
                            |                           |
                            |                           |
                            v                           v
                         botLeft -----------------> botRight
                 */
                 topRight = Coordinate.findCoordinateForBearingAndDistance(topLeft, bearingX, fieldEdgeLen);
                 botRight = Coordinate.findCoordinateForBearingAndDistance(botLeft, bearingX, fieldEdgeLen);

                List<Coordinate> cornersSortedClockwise =  List.of(topLeft,topRight,botRight, botLeft);

                List<FogDevice> devicesInField = allFogDevices.stream()
                        .filter(device ->
                               Coordinate.coordIsInField(cornersSortedClockwise, device.getPosition().getCoordinate()))
                        .collect(Collectors.toList());

                consideredNodes = consideredNodes + devicesInField.size();

                if (devicesInField.size() == 0) continue;

                FogDevice centralFogNode = devicesInField.get(0);

                 fogDevicesInField.put(cornersSortedClockwise, devicesInField);


                topLeft = topRight;
                botLeft = botRight;
            }
        }
    }

    private static void createAccessPoints() {
        for (int i = 0; i < allFogDevices.size(); i++) {
            FogDevice fogDevice = allFogDevices.get(i);

            Position position = fogDevice.getPosition();

            int id = i;

            int downLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkLatency = 1;
            int energy = 200; // energy consumption
            int maxMobDevInAP = MaxAndMin.MAX_ST_IN_AP; //max number of MobileDevice in AccessPoint range

            ApDevice accessPoint = new ApDevice("AcessPoint" + id, position, id, downLinkBw, energy, maxMobDevInAP, upLinkBw, upLinkLatency);
            accessPointList.add(accessPoint);

            Log.printLine(accessPoint.getName() + " created at " + accessPoint.getPosition().getCoordinate().toString());

            /* connect AccessPoint to closest FogDevice */
            accessPoint.setServerCloudlet(fogDevice);
            accessPoint.setParentId(fogDevice.getMyId());
            fogDevice.setApDevices(accessPoint, Policies.ADD);

            NetworkTopology.addLinkWithoutGeneratingMatrices(fogDevice.getMyId(), accessPoint.getMyId(), accessPoint.getDownlinkBandwidth(), getRand().nextDouble());

            Log.printLine("Fog Device of " + accessPoint.getName() + " is " + fogDevice.getName());
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
     */
    private static void createFogDevices(int parentId) {
        long t1 = System.currentTimeMillis();

        HashMap<Integer,Coordinate> fogNodePositions = dbConnector.getAllFogNodePositions();

        NUM_OF_FOG_DEVICES = fogNodePositions.size();
        NUM_OF_ACCESS_POINTS = NUM_OF_FOG_DEVICES;

        /* make random devices compromised */
        int n = (int) (fogNodePositions.size() * RATE_OF_COMPROMISED_DEVICES);

        ArrayList<Coordinate> allCoords = new ArrayList<>(fogNodePositions.values());
        Collections.shuffle(allCoords);
        List<Coordinate> compromisedCoords = allCoords.subList(0, n);

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
                    migrationStrategy = new LowestDistBwSmartThingServerCloudlet(allFogDevices, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_LATENCY) {
                    migrationStrategy = new LowestLatency(allFogDevices, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_DIST_BW_SMARTTING_AP) {
                    migrationStrategy = new LowestDistBwSmartThingAP(allFogDevices, accessPointList, migrationPointPolicy, policyReplicaVm);
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

                FogDevice fogDevice = new FogDevice(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, upLinkRandom, downLinkRandom, upLinkLatency, ratePerMips, coord, id, serviceOffer, migrationStrategy, policyReplicaVm, beforeMigration, offloadingResponseTimeCalculator);

                fogDevice.setParentId(parentId);
                allFogDevices.add(fogDevice);
                Log.printLine(fogDevice.getName() + " created at " + fogDevice.getPosition().getCoordinate().toString());
                /* create Owner */
                Owner owner = new Owner("ownerOf" + name, fogDevice);
                fogDevice.setDeviceOwner(owner);

                if (compromisedCoords.contains(coord))
                    compromisedFogDevices.add(fogDevice);
            }

            long t2 = System.currentTimeMillis();
            System.out.println("dauer createFogDevices: "+(t2-t1));
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

    public static List<FogDevice> getCompromisedFogDevices() {
        return compromisedFogDevices;
    }

    public static void setCompromisedFogDevices(List<FogDevice> compromisedFogDevices) {
        TestExample4.compromisedFogDevices = compromisedFogDevices;
    }

    public static HashMap<List<Coordinate>, List<FogDevice>> getFogDevicesInField() {
        return fogDevicesInField;
    }
}
