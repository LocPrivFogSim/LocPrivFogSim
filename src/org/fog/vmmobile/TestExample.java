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
import org.fog.placement.MobileController;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.privacy.Attacker;
import org.fog.privacy.DeviceMap;
import org.fog.privacy.Owner;
import org.fog.privacy.User;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.vmmigration.*;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.Policies;
import org.fog.vmmobile.constants.Services;

import java.io.FileOutputStream;
import java.util.*;

@SuppressWarnings("ALL")
public class TestExample {


    private static final int SCENARIO = 4;  // Scenario 1, 2, 3 or 4
    private static final double RATE_OF_COMPROMISED_DEVICES = 1; // for scenarios 3, 4

    private static double GRID_INTERVAL = 0.1;
    
    private static ArrayList<FogDevice> fogDeviceList = new ArrayList<>();
    private static List<Vm> vmList = new ArrayList<>();
    private static List<MobileDevice> mobileDeviceList = new ArrayList<>();
    private static List<ApDevice> accessPointList = new ArrayList<>();
    private static List<FogBroker> brokerList = new ArrayList<>();
    private static List<String> appIdList = new ArrayList<>();
    private static List<Application> applicationList = new ArrayList<>();
    private static List<FogDevice> compromisedDevicesList = new ArrayList<>();

    private static List<Attacker> attackerList = new ArrayList<>();

    private static Random rand;
    private static int migrationStrategyPolicy = Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET;
    private static int migrationPointPolicy = Policies.FIXED_MIGRATION_POINT;
    private static int policyReplicaVm = Policies.MIGRATION_COMPLETE_VM;
    private static int stepPolicy = 1;
    private static boolean migrationable = true;

    private static final int SEED = 2;
    private static final int MAP_SIZE = MaxAndMin.MAX_X;    // x == y
    private static final int NUM_OF_ACCESS_POINTS = 10;      // NUM_OF_ACCESS_POINTS <= NUM_OF_FOG_DEVICES!!
    private static final int NUM_OF_FOG_DEVICES = 10;
    private static final int NUM_OF_MOBILE_DEVICES = 1;
    private static final int NUM_OF_SENSORS_PER_DEVICE = 1;
    private static final int NUM_OF_ACTUATORS_PER_DEVICE = 1;
    private static final int MAX_BANDWITH = 100;
    private static final int TRANSMISSION_TIME = 1;
    private static final int LATENCY_BETWEEN_FOG_DEVICES = 1;


    private static Coordinate map; // TODO: beide Maps vereinigen
    private static DeviceMap deviceMap = new DeviceMap(MAP_SIZE);

    private static long startTime = 0;
    private static long executionTime = 0;

    private static double averagePathLength = 0;

    public static void main(String[] args) {

        try {
            Log.printLine("Starting Test2...");
            Log.setOutput(new FileOutputStream("privacy/output"));

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


            /* create FogDevice(s) */
            createFogDevices(cloud.getId());
            for (FogDevice fogDevice : fogDeviceList) {
                deviceMap.addDevice(fogDevice);
            }


            /* create AccessPoints */
            createAccessPoints();
            Log.printLine(accessPointList);
            for (ApDevice accessPoint : accessPointList) {
                deviceMap.addDevice(accessPoint);
            }

            setRand(new Random(1 * Integer.MAX_VALUE));

            /* create MobileDevice(s) */
            createMobileDevices(appId);
            for (MobileDevice mobileDevice : mobileDeviceList) {
                deviceMap.addDevice(mobileDevice);
                setMobilityData(mobileDevice);
                ArrayList<String[]> path = mobileDevice.getPath();
            }


            /* create Broker*/
            for (MobileDevice mobileDevice : mobileDeviceList) {
                FogBroker broker = new FogBroker("BrokerOf" + mobileDevice.getName());
                brokerList.add(broker);
                Log.printLine(broker.getName() + " created");
            }




            /* create Attacker */
            List<FogDevice> compromisedDevices = new ArrayList<>();
            FogDevice fogDevice = null;
            int n=0;
            List<FogDevice> copiedList = null;

            switch (SCENARIO) {
                case 1:
                    fogDevice = fogDeviceList.get(rand.nextInt(NUM_OF_FOG_DEVICES));
                    compromisedDevices.add(fogDevice);
                    break;

                case 2:
                    fogDevice = fogDeviceList.get(rand.nextInt(NUM_OF_FOG_DEVICES));
                    compromisedDevices.add(fogDevice);
                    break;

                case 3:
                    n = (int) (NUM_OF_FOG_DEVICES * RATE_OF_COMPROMISED_DEVICES);
                    copiedList = new ArrayList<>(fogDeviceList);

                    Collections.shuffle(copiedList);
                    compromisedDevicesList = copiedList.subList(0, n);

                    for (int i = 0; i < n; i++) {
                        fogDevice = compromisedDevicesList.get(i);
                        compromisedDevices.add(fogDevice);
                    }
                    break;

                case 4:
                    n = (int) (NUM_OF_FOG_DEVICES * RATE_OF_COMPROMISED_DEVICES);
                    copiedList = new ArrayList<>(fogDeviceList);

                    Collections.shuffle(copiedList);
                    compromisedDevicesList = copiedList.subList(0, n);

                    for (int i = 0; i < n; i++) {
                        fogDevice = compromisedDevicesList.get(i);
                        compromisedDevices.add(fogDevice);
                    }
                    break;
            }

            Attacker attacker = new Attacker("attacker", compromisedDevices, fogDeviceList);
            attackerList.add(attacker);

            for (FogDevice fogDevice1 : compromisedDevices) {
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


            Log.printLine("\n\n\n########  CONNECTIONS  ########\n");

            /* configure network of FogDevices */
            createFogDevicesNetwork();
            Log.printLine();


            /* connect MobileDevices and the closest AccessPoint */
            for (MobileDevice mobileDevice : mobileDeviceList) {
                if (ApDevice.connectApSmartThing(accessPointList, mobileDevice, getRand().nextDouble())) {
                    Log.printLine(mobileDevice.getName() + " connected to " + mobileDevice.getSourceAp().getName());

                } else {
                    Log.printLine(mobileDevice.getName() + " not connected");
                }
            }


            int closestFogDeviceId = 0;

            /* connect MobileDevices and the closest FogDevice */
            for (MobileDevice mobileDevice : mobileDeviceList) {
                closestFogDeviceId = Distances.theClosestServerCloudlet(fogDeviceList, mobileDevice);
                mobileDevice.setSourceServerCloudlet(fogDeviceList.get(closestFogDeviceId));


                /* create symbolic link between Mobile and Fog Device */
                fogDeviceList.get(closestFogDeviceId).connectServerCloudletSmartThing(mobileDevice);
                fogDeviceList.get(closestFogDeviceId).setSmartThingsWithVm(mobileDevice, Policies.ADD);

                Log.printLine(mobileDevice.getName() + " connected to closest Fog Device: " + fogDeviceList.get(closestFogDeviceId).getName());
            }


            /* connect AccessPoint to closest FogDevice */
            // TODO: gehört das nicht eigentlich andersrum (|AP| < |FD|) ???
            closestFogDeviceId = 0;
            int i = 0;

            for (ApDevice accessPoint : accessPointList) {
                closestFogDeviceId = Distances.theClosestServerCloudletToAp(fogDeviceList, accessPoint);
                Log.printLine("closest Fog Device of " + accessPoint.getName() + " is " + fogDeviceList.get(closestFogDeviceId).getName());

                accessPoint.setServerCloudlet(fogDeviceList.get(closestFogDeviceId));
                accessPoint.setParentId(fogDeviceList.get(closestFogDeviceId).getMyId());
                fogDeviceList.get(closestFogDeviceId).setApDevices(accessPoint, Policies.ADD);
                NetworkTopology.addLink(fogDeviceList.get(closestFogDeviceId).getMyId(), accessPoint.getMyId(), accessPoint.getDownlinkBandwidth(), getRand().nextDouble());
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
            i = 0;
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

                //TODO: IndexOutOfBoundsError
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

            for (FogDevice fogDevice2 : fogDeviceList) {
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
            // TODO: deviceMap
            MobileController mobileController = new MobileController("MobileController", fogDeviceList, accessPointList, mobileDeviceList, brokerList, moduleMapping, migrationPointPolicy, migrationStrategyPolicy, stepPolicy, map, SEED, migrationable);

            i = 0;

            for (Application application : applicationList) {
                mobileController.submitApplication(application, 1);
            }


            Log.printLine("\n########  STATISTIC DATA  ########\n");

            /* Statistics */
            MyStatistics.getInstance().setSeed(SEED);

            for (MobileDevice mobileDevice : mobileDeviceList) {
                MyStatistics.getInstance().setFileMap("./outputLatenciesOf" + mobileDevice.getName() + ".txt", mobileDevice.getMyId());
                MyStatistics.getInstance().putLantencyFileName("FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET_seed_"
                        + SEED + "_mobileDevice_" + mobileDevice.getMyId(), mobileDevice.getMyId());
                MyStatistics.getInstance().setToPrint("FIXED_MIGRATION_POINT_with_LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET");

                MyStatistics.getInstance().putLantencyFileName("Time-Latency", mobileDevice.getMyId());
                MyStatistics.getInstance().getMyCount().put(mobileDevice.getMyId(), 0);
            }

            int count = 0;

            Log.printLine("\n_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");
            for (MobileDevice mobileDevice : mobileDeviceList) {
                Log.printLine("Distance between " + mobileDevice.getName() + " and " + mobileDevice.getSourceAp().getName() + ": " + Distances.checkDistance(mobileDevice.getCoord(), mobileDevice.getSourceAp().getCoord()));
                Log.printLine("Distance between" + mobileDevice.getName() + " and " + mobileDevice.getSourceServerCloudlet().getName() + ": " + Distances.checkDistance(mobileDevice.getCoord(), mobileDevice.getSourceServerCloudlet().getCoord()));
                Log.printLine(mobileDevice.getName() + " - coord: " + mobileDevice.getCoord() + ", Direction: " + mobileDevice.getDirection() + ", Speed: " + mobileDevice.getSpeed() + ", VmSize: " + mobileDevice.getVmMobileDevice().getSize());
            }
            Log.printLine("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");


            for (FogDevice fogDevice3 : fogDeviceList) {
                Log.printLine(fogDevice3.getName() + " - coord: " + fogDevice3.getCoord() + ", uplinkLatency: " + fogDevice3.getUplinkLatency());
            }
            Log.printLine("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");

            for (ApDevice accessPoint : accessPointList) {
                Log.printLine(accessPoint.getName() + " - coord: " + accessPoint.getCoord() + ", connected to: " + accessPoint.getServerCloudlet().getName());
            }
            Log.printLine("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_");



            Log.printLine("\n########  SIMULATION  ########\n");





            /** get path of 50 different, possible ways */


//            for (int k = 0; k < 50; k++) {
//                setRand(new Random(SEED * Integer.MAX_VALUE));
//
//
//                Log.printLine("\nStarting Simulation " + k + " at " + Calendar.getInstance().getTime());
//                CloudSim.startSimulation();
//
//
//                Log.setOutput(new FileOutputStream("privacy/paths"));
//
//                LinkedList<Integer> path = attacker.getPath();
//                attacker.addKnownPath(k, path);
//
//                Log.print("Way Nr. " + k + ": ,");
//                for (Integer p : path) {
//                    Log.print(p + ", ");
//                }
//                Log.printLine();
//
//
//                Log.setOutput(new FileOutputStream("privacy/output"));
//            }




            Log.setOutput(new FileOutputStream("privacy/output"));


            /* Simulation */
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            startTime = TimeKeeper.getInstance().getSimulationStartTime();

            Log.printLine("\nStarting Simulation at " + Calendar.getInstance().getTime());
            CloudSim.startSimulation();
            //CloudSim.stopSimulation();


//            executionTime = Calendar.getInstance().getTimeInMillis();
//
//            // TimeUnit.SECONDS.sleep(1);
//            Log.printLine();
//            for (Attacker attacker1 : attackerList) {
//                attacker1.printKnowledge();
//            }
//
//            if (executionTime < attacker.getHighestTime()) {
//                executionTime = attacker.getHighestTime();
//            }
//
//            for (MobileDevice mobileDevice : mobileDeviceList) {
//                MobileDeviceInformation deviceInformation = attacker.getDeviceInformationById(mobileDevice.getMyId());
//                for (long time = startTime; time < executionTime; time += 10) {
//                    Position position = deviceInformation.getPositionAtTime(time);
//
//                }
//            }
//
//
//            double controlledArea =  attacker.getControlledArea();
//            Log.printLine("Controlled Area of Attacker: " + controlledArea);


            Log.setOutput(System.out);

            Log.printLine("\npath :" + attacker.getPath().toString());

            Log.printLine("\nTest2 finished");


        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }
    }



    private static void setupAndSimulation() {



    }






    private static void setMobilityData(MobileDevice mobileDevice) {

        int time = 0;
        double direction = mobileDevice.getDirection();
        int x = mobileDevice.getCoord().getCoordX();
        int y = mobileDevice.getCoord().getCoordY();
        int speed = mobileDevice.getSpeed();


        ArrayList<String[]> path;

        for (int i = 0; i < 50; i++) {
            time = i + 1;
            x = getRand().nextInt(MaxAndMin.MAX_X);
            y = getRand().nextInt(MaxAndMin.MAX_Y);

            String[] position = new String[5];

            position[0] = Integer.toString(time);
            position[1] = Double.toString(direction);
            position[2] = Integer.toString(x);
            position[3] = Integer.toString(y);
            position[4] = Integer.toString(speed);

            path = mobileDevice.getPath();
            path.add(position);
            mobileDevice.setPath(path);
        }

        Coordinate coordinate = new Coordinate();
        coordinate.setInitialCoordinate(mobileDevice);
    }


    private static void createFogDevicesNetwork() {

        HashMap<FogDevice, Double> network = new HashMap<>();

        int i = 1, j = 1, line, column;
        for (FogDevice fogDeviceX : fogDeviceList) {
            j = 1;
            for (FogDevice fogDeviceY : fogDeviceList) {
                if (fogDeviceX.equals(fogDeviceY)) {
                    break;
                }

                //TODO: versteh ich nicht... - warum x/12 ??
                line = (int) (j / 12) - (int) (i / 12);
                if (line < 0) {
                    line *= -1;
                }
                column = (int) (j / 12) - (int) (i / 12);
                if (column < 0) {
                    column *= -1;
                }

                if (fogDeviceX.getUplinkBandwidth() < fogDeviceY.getDownlinkBandwidth()) {
                    network.put(fogDeviceY, fogDeviceX.getUplinkBandwidth());
                    NetworkTopology.addLink(fogDeviceX.getMyId(), fogDeviceY.getMyId(), fogDeviceX.getUplinkBandwidth(), (line + column) * LATENCY_BETWEEN_FOG_DEVICES + getRand().nextDouble());
                } else {
                    network.put(fogDeviceY, fogDeviceY.getDownlinkBandwidth());
                    NetworkTopology.addLink(fogDeviceX.getMyId(), fogDeviceY.getMyId(), fogDeviceY.getDownlinkBandwidth(), (line + column) * LATENCY_BETWEEN_FOG_DEVICES + getRand().nextDouble());
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

        int coordX = 0;
        int coordY = 0;
        for (int i = 0; i < NUM_OF_ACCESS_POINTS; i++) {

            if (NUM_OF_ACCESS_POINTS == 1) {// for 1 AccesPoint
                coordX = MAP_SIZE / 2;
                coordY = MAP_SIZE / 2;
            } else {
                coordX = fogDeviceList.get(i).getCoord().getCoordX();
                coordY = fogDeviceList.get(i).getCoord().getCoordY();
            }
            int id = i;

            int downLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkBw = 100 * 1024 * 1024; // 100Mbits
            int upLinkLatency = 1;
            int energy = 200; // energy consumption
            int maxMobDevInAP = MaxAndMin.MAX_ST_IN_AP; //max number of MobileDevice in AccessPoint range

            ApDevice accessPoint = new ApDevice("AcessPoint" + id, coordX, coordY, id, downLinkBw, energy, maxMobDevInAP, upLinkBw, upLinkLatency);
            accessPointList.add(accessPoint);

            Log.printLine(accessPoint.getName() + " created at " + accessPoint.getCoord().toString());
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
                int coordX = 0; //getRand().nextInt(MaxAndMin.MAX_X);
                int coordY = 0; //getRand().nextInt(MaxAndMin.MAX_Y);
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


                MobileDevice mobileDevice = new MobileDevice(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, upLinkBw, downLinkBw, upLinkLatency, ratePerMips, coordX, coordY, id, direction, speed, maxServiceValue, vmSize, migrationTechnique);
                //MobileDevice mobileDevice = new MobileDevice(name, coordX, coordY, id, direction, speed);


                /* add Sensors and Actuators to the Device */
                mobileDevice.setSensors(sensorSet);
                mobileDevice.setActuators(actuatorSet);


                /* more setup */
                mobileDevice.setTempSimulation(0);
                mobileDevice.setTimeFinishDeliveryVm(-1);
                mobileDevice.setTimeFinishHandoff(0);
//            st.setTravelPredicTime(getTravelPredicTimeForST());
//            st.setMobilityPredictionError(getMobilityPrecitionError());


                mobileDeviceList.add(mobileDevice);

                Log.printLine(mobileDevice.getName() + " created at " + mobileDevice.getCoord().toString() + " with Direction " + mobileDevice.getDirection() + " and Speed " + mobileDevice.getSpeed());

                /* create User */
                User user = new User("userOf" + name, mobileDevice);
                mobileDevice.setDeviceUser(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Errors happen...");
        }

    }

    private static void createFogDevices(int parentId) {


        try {
            for (int i = 0; i < NUM_OF_FOG_DEVICES; i++) {

                /* Fog Device properties */
                // TODO: Verteilung (AppExemplo2 1207)

                //int n = 0;
                //n = MAP_SIZE / ( NUM_OF_FOG_DEVICES + 1);
                //int

                int coordX = getRand().nextInt(MaxAndMin.MAX_X);
                int coordY = getRand().nextInt(MaxAndMin.MAX_Y);

                int id = FogUtils.generateEntityId();
                String name = "FogDevice" + id;

                long storage = 1000 * 1024 * 1024;
                int bandwith = 1000 * 1024 * 1024;
                int ram = 25000;
                int schedulingInterval = 10;
                FogLinearPowerModel powerModel = new FogLinearPowerModel(107.339d, 83.433d);


                /* set migration strategy */
                DecisionMigration migrationStrategy = null;
                if (migrationStrategyPolicy == Policies.LOWEST_DIST_BW_SMARTTING_SERVERCLOUDLET) {
                    migrationStrategy = new LowestDistBwSmartThingServerCloudlet(fogDeviceList, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_LATENCY) {
                    migrationStrategy = new LowestLatency(fogDeviceList, accessPointList, migrationPointPolicy, policyReplicaVm);
                } else if (migrationStrategyPolicy == Policies.LOWEST_DIST_BW_SMARTTING_AP) {
                    migrationStrategy = new LowestDistBwSmartThingAP(fogDeviceList, accessPointList, migrationPointPolicy, policyReplicaVm);
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
                double upLinkRandom = MAX_BANDWITH * getRand().nextDouble();
                double downLinkRandom = MAX_BANDWITH * getRand().nextDouble();
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

                FogDevice fogDevice = new FogDevice(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, upLinkRandom, downLinkRandom, upLinkLatency, ratePerMips, coordX, coordY, id, serviceOffer, migrationStrategy, policyReplicaVm, beforeMigration);
//               FogDevice fogDevice = new FogDevice(name, coordX, coordY, id);
                fogDevice.setParentId(parentId);


                fogDeviceList.add(fogDevice.getMyId(), fogDevice);

                Log.printLine(fogDevice.getName() + " created at " + fogDevice.getCoord().toString());

                /* create Owner */
                Owner owner = new Owner("ownerOf" + name, fogDevice);
                fogDevice.setDeviceOwner(owner);

            }
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

        AppModule testVm = new AppModule(mobileDevice.getMyId(), appModuleName, appId, mobileDevice.getMyId(), mips, ram, bw, size, "VmOf" + mobileDevice.getName(), scheduler, new HashMap<Pair<String, String>, SelectivityModel>());


        /* more setup for MobileDevice */
        mobileDevice.setVmMobileDevice(testVm);
        mobileDevice.getSourceServerCloudlet().getHost().vmCreate(testVm);
        mobileDevice.setVmLocalServerCloudlet(mobileDevice.getSourceServerCloudlet());


        Log.printLine(mobileDevice.getName() + " -- Position: " + mobileDevice.getCoord() + ", Direction: " + mobileDevice.getDirection() + ", Speed: " + mobileDevice.getSpeed());
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

        //Log.printLine("VM " + vm.getId() + " for " + broker.getName() + " created");
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
        TestExample.rand = rand;
    }

    public static int getSCENARIO() {
        return SCENARIO;
    }

    public static int getMAP_SIZE() {
        return MAP_SIZE;
    }

    public static DeviceMap getDeviceMap() {
        return deviceMap;
    }

    public static ArrayList<FogDevice> getFogDeviceList() {
        return fogDeviceList;
    }
    
    public static int getNumOfFogDevices() {
        return NUM_OF_FOG_DEVICES;
    }
    
    public static void setAveragePathLength(double averagePathLength) {
        TestExample.averagePathLength = averagePathLength;
    }
    
    public static double getGridInterval() {
        return GRID_INTERVAL;
    }
}