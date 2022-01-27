package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.localization.Coordinate;
import org.fog.localization.Distances;
import org.fog.offloading.IOffloadingResponseTimeCalculator;
import org.fog.offloading.OffloadingEvents;
import org.fog.offloading.OffloadingTask;
import org.fog.placement.MobileController;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.privacy.Attacker;
import org.fog.privacy.EventType;
import org.fog.privacy.Owner;
import org.fog.privacy.Position;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.fog.vmmigration.BeforeMigration;
import org.fog.vmmigration.DecisionMigration;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.Service;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.TestExample4;
import org.fog.vmmobile.constants.MobileEvents;
import org.fog.vmmobile.constants.Policies;

import java.io.*;
import java.util.*;

public class FogDevice extends PowerDatacenter {
    /**
     * Observable
     */
    private final List<Attacker> observerList = new ArrayList<>();
    protected Queue<Tuple> northTupleQueue;
    protected Queue<Pair<Tuple, Integer>> southTupleQueue;
    protected List<String> activeApplications;

    protected Map<String, Application> applicationMap;
    protected Map<String, List<String>> appToModulesMap;
    protected Map<Integer, Double> childToLatencyMap;
    protected Map<Integer, Integer> cloudTrafficMap;
    protected double lockTime;
    /**
     * ID of the parent Fog Device
     */
    protected int parentId;
    protected int volatilParentId;
    /**
     * ID of the Controller
     */
    protected int controllerId;
    /**
     * IDs of the children Fog devices
     */
    protected List<Integer> childrenIds;
    protected Map<Integer, List<String>> childToOperatorsMap;
    /**
     * Flag denoting whether the link southwards from this FogDevice is busy
     */
    protected boolean isSouthLinkBusy;
    /**
     * Flag denoting whether the link northwards from this FogDevice is busy
     */
    protected boolean isNorthLinkBusy;
    protected double uplinkBandwidth;
    protected double downlinkBandwidth;
    protected double uplinkLatency;
    protected List<Pair<Integer, Double>> associatedActuatorIds;
    protected double energyConsumption;
    protected double lastUtilizationUpdateTime;
    protected double lastUtilization;
    protected double ratePerMips;
    protected double totalCost;
    protected Map<String, Map<String, Integer>> moduleInstanceCount;
    //protected Coordinate coord;// = new Coordinate();//myiFogSim
    protected Position position;

    protected Set<ApDevice> apDevices;//myiFogSim
    protected Set<MobileDevice> smartThings;
    protected Set<MobileDevice> smartThingsWithVm;
    protected Set<FogDevice> serverCloudlets;
    protected boolean available;
    protected Service service;
    protected DecisionMigration migrationStrategy;
    protected int policyReplicaVM;
    protected BeforeMigration beforeMigration;
    protected int startTravelTime;
    protected int travelTimeId;
    protected int travelPredicTime;
    protected int mobilityPrecitionError;
    protected int myId;
    protected IOffloadingResponseTimeCalculator offloadingResponseTimeCalculator;
    protected HashMap<MobileDevice, Integer> offloadingTasks;
    int numClients = 0;
    /**
     * owner of fog device
     */
    private Owner deviceOwner;
    /**
     * possible Attackers
     */
    private List<Attacker> attackerList = new ArrayList<>();
    private int level;
    //	protected NetworkBwServerCloulets networkBwServerCloulets;
    private HashMap<Integer, Double> netServerCloudlets;
    private FogDevice serverCloudletToVmMigrate;

    public FogDevice() {//myiFogSim

    }

    public FogDevice(String name, Position position, int id) { //myiFogSim
        //	public FogDevice(String name, Coordinate coord, int coordX, int coordY, int id) { //myiFogSim
        // TODO Auto-generated constructor stub
        super(name);
        this.position = position;

        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.setMyId(id);
        smartThings = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        serverCloudlets = new HashSet<>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();
        this.setAvailable(true);
    }

    public FogDevice(String name) {
        super(name);
    }

    public FogDevice( //myiFogSim - for ServerCloulet -> it addition Service in the Construction
                      String name,
                      FogDeviceCharacteristics characteristics,
                      VmAllocationPolicy vmAllocationPolicy,
                      List<Storage> storageList,
                      double schedulingInterval,
                      double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips
            , Coordinate coord, int id, Service service
            , DecisionMigration migrationStrategy
            , int policyReplicaVM
            , BeforeMigration beforeMigration
    ) throws Exception {

        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.position = new Position();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.position.setCoordinate(coord);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();
        setVolatilParentId(-1);
        this.setAvailable(true);
        this.setService(service);

        setBeforeMigrate(beforeMigration);
        setPolicyReplicaVM(policyReplicaVM);
        setMigrationStrategy(migrationStrategy);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }

    public FogDevice( //myiFogSim - for ServerCloulet -> it addition Service in the Construction
                      String name,
                      FogDeviceCharacteristics characteristics,
                      VmAllocationPolicy vmAllocationPolicy,
                      List<Storage> storageList,
                      double schedulingInterval,
                      double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips
            , float coordX, float coordY, int id, Service service
            , DecisionMigration migrationStrategy
            , int policyReplicaVM
            , BeforeMigration beforeMigration
    ) throws Exception {

        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.position = new Position();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.position.setCoordinate(Coordinate.createCartesianCoordinate(coordX,coordY));
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();
        setVolatilParentId(-1);
        this.setAvailable(true);
        this.setService(service);

        setBeforeMigrate(beforeMigration);
        setPolicyReplicaVM(policyReplicaVM);
        setMigrationStrategy(migrationStrategy);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }

    public FogDevice( //myiFogSim - for ServerCloulet -> it addition Service in the Construction
                      String name,
                      FogDeviceCharacteristics characteristics,
                      VmAllocationPolicy vmAllocationPolicy,
                      List<Storage> storageList,
                      double schedulingInterval,
                      double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips
            , Coordinate coord, int id, Service service
            , DecisionMigration migrationStrategy
            , int policyReplicaVM
            , BeforeMigration beforeMigration
            , IOffloadingResponseTimeCalculator responseTimeCalculator
    ) throws Exception {

        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.position = new Position();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.position.setCoordinate(coord);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();
        setVolatilParentId(-1);
        this.setAvailable(true);
        this.setService(service);

        setBeforeMigrate(beforeMigration);
        setPolicyReplicaVM(policyReplicaVM);
        setMigrationStrategy(migrationStrategy);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setOffloadingResponseTimeCalculator(responseTimeCalculator);
    }

    public FogDevice( //myiFogSim - for MobileDevice
                      String name,
                      FogDeviceCharacteristics characteristics,
                      VmAllocationPolicy vmAllocationPolicy,
                      List<Storage> storageList,
                      double schedulingInterval,
                      double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips
            , Coordinate coord, int id

    ) throws Exception {

        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

        this.position = new Position();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.position.setCoordinate(coord);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();

        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        setVolatilParentId(-1);

        this.setAvailable(true);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);

        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }

    public FogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);

        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        offloadingTasks = new HashMap<MobileDevice, Integer>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }

    public FogDevice(
            String name, long mips, int ram,
            double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, null, null, new LinkedList<Storage>(), 0);

        offloadingTasks = new HashMap<MobileDevice, Integer>();

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                powerModel
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));

        String arch = Config.FOG_DEVICE_ARCH;
        String os = Config.FOG_DEVICE_OS;
        String vmm = Config.FOG_DEVICE_VMM;
        double time_zone = Config.FOG_DEVICE_TIMEZONE;
        double cost = Config.FOG_DEVICE_COST;
        double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
        double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
        double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        setCharacteristics(characteristics);

        setLastProcessTime(0.0);
        setVmList(new ArrayList<Vm>());
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host1 : getCharacteristics().getHostList()) {
            host1.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }

        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);

        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
    }

    private static void saveMigration(MobileDevice st) {
        Log.printLine("MIGRATION " + st.getMyId() + " Position: " + st.getPosition().getCoordinate().getCoordX() + ", " + st.getPosition().getCoordinate().getCoordY() + " Direction: " + st.getPosition().getDirection() + " Speed: " + st.getPosition().getSpeed());
        Log.printLine("Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": " +
                Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate()) + " Migration time: " + st.getMigTime());
        Log.printLine("Consumo de rede a" + NetworkUsageMonitor.getNetworkUsage());
        NetworkUsageMonitor.migrationTrafficUsage(st.getVmLocalServerCloudlet().getUplinkBandwidth(), st.getVmMobileDevice().getSize());
        Log.printLine("Consumo de rede d" + NetworkUsageMonitor.getNetworkUsage());
        try (FileWriter fw = new FileWriter(st.getMyId() + "migration.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(st.getMyId() + "\t" + st.getPosition().getCoordinate().getCoordX() + "\t" +
                    st.getPosition().getCoordinate().getCoordY() + "\t" + st.getPosition().getDirection() + "\t" +
                    st.getPosition().getSpeed() + "\t" + st.getVmLocalServerCloudlet().getName() + "\t" +
                    st.getDestinationServerCloudlet().getName() + "\t" +
                    CloudSim.clock() + "\t" + st.getMigTime() + "\t" + (CloudSim.clock() + st.getMigTime()));
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

    private void notifyObservers(MobileDevice mobileDevice, int timestamp, int eventId,int eventType, String event) {
        if (!CloudSim.running())
            return;

        for (Attacker observer : observerList) {
            observer.update(this, mobileDevice, timestamp, eventId,eventType, event);
        }
    }

    public void addObserver(Attacker observer) {
        observerList.add(observer);
    }

    public void removeObserver(Attacker observer) {
        observerList.remove(observer);
    }

    public void setSmartThings(MobileDevice st, int timestamp, int eventType, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.smartThings.add(st);
            notifyObservers(st, timestamp,action,eventType, "add");
        } else if (action == Policies.REMOVE) {
            this.smartThings.remove(st);
            notifyObservers(st, timestamp, action,eventType, "remove");

        }
    }

    public Set<MobileDevice> getSmartThings() {//myiFogSim
        return smartThings;
    }

    private void beginOffloadingTaskExecution(SimEvent event) {
        OffloadingTask task = (OffloadingTask) event.getData();

        if (task.getSource() == null)
            throw new IllegalStateException("A offloading task to be executed needs a source mobile device.");

        // Update task list
        addOffloadingTask(task.getSource(), task);

        // Schedule a event for the offloading task end...
        double delay = getOffloadingResponseTimeCalculator().calculateResponseTime(
                MobileController.getServerCloudlets(),
                MobileController.getApDevices(),
                task.getSource(),
                this,
                task);
        schedule(getId(), delay, OffloadingEvents.END_OFFLOAD_TASK_EXECUTION, task);
    }

    private void endOffloadingTaskExecution(SimEvent event) {
        OffloadingTask task = (OffloadingTask) event.getData();

        if (task.getSource() == null)
            throw new IllegalStateException("A offloading task to be executed needs a source mobile device.");

        removeOffloadingTask(task.getSource(), task);

        // Notify mobile device that the task has finished executing...
        scheduleNow(task.getSource().getId(), OffloadingEvents.FINISHED_OFFLOADING, task);
    }

    public void addOffloadingTask(MobileDevice mobileDevice, OffloadingTask task) {
        double availableMips = getHost().getPeList().get(0).getPeProvisioner().getAvailableMips();
        TestExample4.jsonHelper.addEvent(
                getMyId(),
                "add",
                6001,
                EventType.OFFLOADING,
                mobileDevice.getPosition().getTimestamp(),
                availableMips,
                task
        );

        task.setTarget(this);

        getHost().getPeList().get(0).getPeProvisioner().allocateMipsForOffloadingTask(task, task.getMi());

        // Update task list
        int count = this.offloadingTasks.getOrDefault(mobileDevice, 0);
        this.offloadingTasks.put(mobileDevice, ++count);

        // The first task of the mobile device was added. Notify Observers
        if (count == 1)
            notifyObservers(mobileDevice, mobileDevice.getPosition().getTimestamp(), 6001,EventType.OFFLOADING, "add");
    }

    public void removeOffloadingTask(MobileDevice mobileDevice, OffloadingTask task) {
        task.setTarget(null);

        getHost().getPeList().get(0).getPeProvisioner().deallocateMipsForOffloadingTask(task);

        int count = this.offloadingTasks.getOrDefault(mobileDevice, 0) - 1;

        if (count == -1)
            return;
        else if (count == 0)
            this.offloadingTasks.remove(mobileDevice);
        else
            this.offloadingTasks.put(mobileDevice, count);

        // Last task for the mobile device was removed. Notify Observers
        if (count == 0)
            notifyObservers(mobileDevice, mobileDevice.getPosition().getTimestamp(), 6004,EventType.OFFLOADING, "remove");

        double availableMips = getHost().getPeList().get(0).getPeProvisioner().getAvailableMips();
        TestExample4.jsonHelper.addEvent(
                getMyId(),
                "remove",
                6004,
                EventType.OFFLOADING,
                mobileDevice.getPosition().getTimestamp(),
                availableMips,
                task
        );
    }

    public int getMyId() {
        return myId;
    }

    public void setMyId(int myId) {
        this.myId = myId;
    }

    public HashMap<Integer, Double> getNetServerCloudlets() {
        return netServerCloudlets;
    }

    public void setNetServerCloudlets(HashMap<Integer, Double> netServerCloudlets) {
        this.netServerCloudlets = netServerCloudlets;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Set<ApDevice> getApDevices() { //myiFogSim
        return apDevices;
    }

    public void setApDevices(ApDevice ap, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.apDevices.add(ap);
        } else {
            this.apDevices.remove(ap);
        }
    }


    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getStartTravelTime() {
        return startTravelTime;
    }

    public void setStartTravelTime(int startTravelTime) {
        this.startTravelTime = startTravelTime;
    }

    public int getTravelTimeId() {
        return travelTimeId;
    }

    public void setTravelTimeId(int travelTimeId) {
        this.travelTimeId = travelTimeId;
    }

    public int getTravelPredicTime() {
        return travelPredicTime;
    }

    public void setTravelPredicTime(int travelPredicTime) {
        this.travelPredicTime = travelPredicTime;
    }

    public int getMobilityPrecitionError() {
        return mobilityPrecitionError;
    }

    public void setMobilityPredictionError(int mobilityPrecitionError) {
        this.mobilityPrecitionError = mobilityPrecitionError;
    }

    /**
     * Overrides this method when making a new and different type of resource. <br>
     * <b>NOTE:</b> You do not need to override {@link # body()} method, if you use this method.
     *
     * @pre $none
     * @post $none
     */
    @Override
    protected void registerOtherEntity() {

    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        if (Log.TRACE_EVNETS)
            Log.print("FogDevice.java: " + this.getName() + " => Event: " + ev.toString() + "; Tag: ");

        switch (ev.getTag()) {
            case FogEvents.TUPLE_ARRIVAL:
                if (Log.TRACE_EVNETS)
                    Log.printLine("FogEvents.TUPLE_ARRIVAL (" + ev.getTag() + ")");
               // processTupleArrival(ev);  todo leads to an error
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
            case MobileEvents.MAKE_DECISION_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.MAKE_DECISION_MIGRATION (" + ev.getTag() + ")");
                invokeDecisionMigration(ev);
                break;
            case MobileEvents.TO_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.TO_MIGRATION (" + ev.getTag() + ")");
                invokeBeforeMigration(ev);
                break;
            case MobileEvents.NO_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.NO_MIGRATION (" + ev.getTag() + ")");
                invokeNoMigration(ev);
                break;
            case MobileEvents.START_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.START_MIGRATION (" + ev.getTag() + ")");
                invokeStartMigration(ev);
                break;
            case MobileEvents.ABORT_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.ABORT_MIGRATION (" + ev.getTag() + ")");
                invokeAbortMigration(ev);
                break;
            case MobileEvents.REMOVE_VM_OLD_CLOUDLET:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.REMOVE_VM_OLD_CLOUDLET (" + ev.getTag() + ")");
                removeVmOldServerCloudlet(ev);
                break;
            case MobileEvents.ADD_VM_NEW_CLOUDLET:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.ADD_VM_NEW_CLOUDLET (" + ev.getTag() + ")");
                addVmNewServerCloudlet(ev);
                break;
            case MobileEvents.DELIVERY_VM:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.DELIVERY_VM (" + ev.getTag() + ")");
                deliveryVM(ev);
                break;
            case MobileEvents.CONNECT_ST_TO_SC:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.CONNECT_ST_TO_SC (" + ev.getTag() + ")");
                connectServerCloudletSmartThing(ev);
                break;
            case MobileEvents.DESCONNECT_ST_TO_SC:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.DESCONNECT_ST_TO_SC (" + ev.getTag() + ")");
                desconnectServerCloudletSmartThing(ev);
                break;
            case MobileEvents.UNLOCKED_MIGRATION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.UNLOCKED_MIGRATION (" + ev.getTag() + ")");
                unLockedMigration(ev);
                break;
            case MobileEvents.VM_MIGRATE:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.VM_MIGRATE (" + ev.getTag() + ")");
                myVmMigrate(ev);
                break;
            case MobileEvents.SET_MIG_STATUS_TRUE:
                if (Log.TRACE_EVNETS)
                    Log.printLine("MobileEvents.SET_MIG_STATUS_TRUE (" + ev.getTag() + ")");
                migStatusToLiveMigration(ev);
                break;

            case OffloadingEvents.BEGIN_OFFLOAD_TASK_EXECUTION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("OffloadingEvents.BEGIN_OFFLOAD_TASK_EXECUTION (" + ev.getTag() + ")");
                beginOffloadingTaskExecution(ev);
                break;
            case OffloadingEvents.END_OFFLOAD_TASK_EXECUTION:
                if (Log.TRACE_EVNETS)
                    Log.printLine("OffloadingEvents.END_OFFLOAD_TASK_EXECUTION (" + ev.getTag() + ")");
                endOffloadingTaskExecution(ev);
                break;

            default:
                break;
        }
    }

    private void myVmMigrate(SimEvent ev) {
        // TODO Auto-generated method stub

        MobileDevice smartThing = (MobileDevice) ev.getData();
        Log.printLine("local " + smartThing.getVmLocalServerCloudlet().getName() + " " + smartThing.getVmLocalServerCloudlet().getActiveApplications() +
                " apps " + smartThing.getVmLocalServerCloudlet().getApplicationMap().values().toString());
        Log.printLine("dest: " + smartThing.getDestinationServerCloudlet().getName() + " " + smartThing.getDestinationServerCloudlet().getActiveApplications() +
                " apps " + smartThing.getDestinationServerCloudlet().getApplicationMap().values().toString());
        Log.printLine("smartthing id: " + smartThing.getMyId());
        smartThing.getVmLocalServerCloudlet().applicationMap.values();
        Application app = smartThing.getVmLocalServerCloudlet().applicationMap.get("MyApp_vr_game" + smartThing.getMyId());
        if (app == null) {
            Log.printLine("Clock: " + CloudSim.clock() + " - FogDevice.java - App == Null");
            System.exit(0);
        }
        //smartThing.getDestinationServerCloudlet().
        getApplicationMap().put(app.getAppId(), app);
        //		Log.printLine("Clock: "+CloudSim.clock()+" - Add applicationMap "+app.getAppId()+ " on "+//smartThing.getDestinationServerCloudlet().
        //				getName());
        if (smartThing.getVmLocalServerCloudlet().getApplicationMap().remove(app.getAppId()) == null) {
            Log.printLine("FogDevice.java - applicationMap did not remove. return == null");
            System.exit(0);
        }

        MobileController mobileController = (MobileController) CloudSim.getEntity("MobileController");

        mobileController.getModuleMapping().addModuleToDevice(((AppModule) smartThing.getVmMobileDevice()).getName(), getName(), 1);//smartThing.getDestinationServerCloudlet().
        Log.printLine("Antes de entrar no submitApplicationMigration - " + getName());
        mobileController.getModuleMapping().getModuleMapping().remove(smartThing.getVmLocalServerCloudlet().getName());
        if (!mobileController.getModuleMapping().getModuleMapping().containsKey(getName())) {
            mobileController.getModuleMapping().getModuleMapping().put(getName(), new HashMap<String, Integer>());
            mobileController.getModuleMapping().getModuleMapping().get(getName()).put("AppModuleVm_" + smartThing.getName(), 1);
            //CONFERIR ISSO AQUI
        }
        mobileController.submitApplicationMigration(smartThing, app, 1);

//						sendNow(mobileController.getId(), FogEvents.APP_SUBMIT, app);
        sendNow(mobileController.getId(), MobileEvents.APP_SUBMIT_MIGRATE, app);
        //		if(!smartThing.getSourceServerCloudlet().equals(smartThing.getVmLocalServerCloudlet())){
        //			smartThing.getVmLocalServerCloudlet().getChildToLatencyMap().put(smartThing.getId(), smartThing.getUplinkLatency()+getUplinkLatency());
        //			smartThing.getVmLocalServerCloudlet().addChild(smartThing.getId());
        //		}
    }

    private void unLockedMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        smartThing.setLockedToMigration(false);
        smartThing.setTimeFinishDeliveryVm(-1);
        LogMobile.debug("FogDevice.java", smartThing.getName() + " had the migration unlocked");
    }

    private void saveConnectionCloudletSmartThing(MobileDevice st, String conType) {

        try (FileWriter fw = new FileWriter("results/"+st.getMyId() + "ConClSmTh.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(CloudSim.clock() + "\t" + st.getMyId() + "\t" + conType);
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

    private void desconnectServerCloudletSmartThing(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        desconnectServerCloudletSmartThing(smartThing);
        MyStatistics.getInstance().startWithoutConnetion(smartThing.getMyId(), CloudSim.clock());
        saveConnectionCloudletSmartThing(smartThing, "desconnectServerCloudletSmartThing");
    }

    private void connectServerCloudletSmartThing(SimEvent ev) {

        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();

        connectServerCloudletSmartThing(smartThing);
        MyStatistics.getInstance().finalWithoutConnection(smartThing.getMyId(), CloudSim.clock());
        saveConnectionCloudletSmartThing(smartThing, "connectServerCloudletSmartThing");

        //		if(smartThing.isLockedToHandoff() && !smartThing.isHandoffStatus()){ //Handoff has been occurred first than delivery
        if (smartThing.getTimeFinishDeliveryVm() == -1) {
            MyStatistics.getInstance().startDelayAfterNewConnection(smartThing.getMyId(), CloudSim.clock());// Delivery hasn't been ocu
        } else {
            smartThing.setMigStatus(false);
            smartThing.setPostCopyStatus(false);
            smartThing.setMigStatusLive(false);
            if (MyStatistics.getInstance().getInitialWithoutVmTime().get(smartThing.getMyId()) != null) {
                MyStatistics.getInstance().finalWithoutVmTime(smartThing.getMyId(), CloudSim.clock());
                MyStatistics.getInstance().getInitialWithoutVmTime().remove(smartThing.getMyId());
            }
            LogMobile.debug("FogDevice.java", smartThing.getName() + " had migStatus to false - connectServerCloudlet");
            MyStatistics.getInstance().startDelayAfterNewConnection(smartThing.getMyId(), 0.0);
            MyStatistics.getInstance().finalDelayAfterNewConnection(smartThing.getMyId(), getCharacteristics().getCpuTime(smartThing.getVmMobileDevice().getSize() * 1024 * 1024 * 8, 0.0));//MaxAndMin.DELAY_PROCESS);
            //			MyStatistics.getInstance().finalWithoutVmTime(smartThing.getMyId(), CloudSim.clock()+getCharacteristics().getCpuTime(smartThing.getVmMobileDevice().getSize()*1024*1024*8, 0.0));
        }

        /*
        if (!smartThing.getSourceServerCloudlet().equals(smartThing.getVmLocalServerCloudlet())) {
            smartThing.getSourceServerCloudlet().desconnectServerCloudletSmartThing(smartThing);

            smartThing.getVmLocalServerCloudlet().connectServerCloudletSmartThing(smartThing);
            MyStatistics.getInstance().setMyCountLowestLatency(1);
        }
         */
        //		}
    }

    private void addVmNewServerCloudlet(SimEvent ev) {
        // TODO Auto-generated method stub

    }

    private void removeVmOldServerCloudlet(SimEvent ev) {
        // TODO Auto-generated method stub

    }

    private void invokeAbortMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        Log.printLine("*_*_*_*_*_*_*_*_*_*_*_*_*_ABORT MIGRATION -> beforeMigration*_*_*_*_*_*_*_*_*_*_*_*: " + smartThing.getName());
        MyStatistics.getInstance().getInitialWithoutVmTime().remove(smartThing.getMyId());
        MyStatistics.getInstance().getInitialTimeDelayAfterNewConnection().remove(smartThing.getMyId());
        MyStatistics.getInstance().getInitialTimeWithoutConnection().remove(smartThing.getMyId());
        smartThing.setMigStatus(false);
        smartThing.setPostCopyStatus(false);
        smartThing.setMigStatusLive(false);
        smartThing.setLockedToMigration(false);
        smartThing.setTimeFinishDeliveryVm(-1.0);
        smartThing.setAbortMigration(true);
        smartThing.setDestinationServerCloudlet(smartThing.getVmLocalServerCloudlet());
    }

    public boolean connectServerCloudletSmartThing(MobileDevice st) {
        st.setSourceServerCloudlet(this);

        setSmartThings(st, st.getPosition().getTimestamp(), EventType.MIGRATION, Policies.ADD);
        //		st.setVmLocalServerCloudlet(this);
        st.setParentId(getId());
        double latency = st.getUplinkLatency();

        getChildToLatencyMap().put(st.getId(), latency);
        addChild(st.getId());
        //		for(MobileSensor s: st.getSensors()){
        //			addChild(s.getId());
        //			latency = s.getLatency();
        //			getChildToLatencyMap().put(s.getId(), latency);
        //		}
        //		for(MobileActuator a: st.getActuators()){
        //			addChild(a.getId());
        //			latency = a.getLatency();
        //			getChildToLatencyMap().put(a.getId(), latency);
        //		}
        setUplinkLatency(getUplinkLatency() + 0.123812950236);//
        //		this.getChildrenIds().add(st.getId());
        //		NetworkTopology.addLink(this.getId(), st.getId(), st.getDownlinkBandwidth(), 0.05);//0.02+0.03
        LogMobile.debug("FogDevice.java", st.getName() + " was connected to " + getName());

        return true;
    }

    public boolean desconnectServerCloudletSmartThing(MobileDevice st) {
        //		for(MobileSensor s: st.getSensors()){
        //			st.getSourceServerCloudlet().getChildrenIds().remove((Integer)s.getId());
        //		}
        //		for(MobileActuator a: st.getActuators()){
        //			st.getSourceServerCloudlet().getChildrenIds().remove((Integer)a.getId());
        //		}
        setSmartThings(st, st.getPosition().getTimestamp(), EventType.MIGRATION, Policies.REMOVE); //it'll remove the smartThing from serverCloudlets-smartThing's set
        st.setSourceServerCloudlet(null);
//		NetworkTopology.addLink(this.getId(), st.getId(), 0.0, 0.0);
        setUplinkLatency(getUplinkLatency() - 0.123812950236);
        removeChild(st.getId());
        LogMobile.debug("FogDevice.java", st.getName() + " was desconnected to " + getName());
        return true;
    }

    // CHECK MIGRATION_TIME

    private void invokeStartMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        //		Log.printLine("******StarMigration*****");
        MobileDevice smartThing = (MobileDevice) ev.getData();

        if (MobileController.getSmartThings().contains(smartThing)) {//the smartThing is outside of the map
            if (!smartThing.isAbortMigration()) {
                if (smartThing.getSourceAp() != null) {//the smartThing isn't connected in any ap right now
                    //					Log.enable();

                    int srcId = getId();
                    int entityId = smartThing.getDestinationServerCloudlet().getId();
                    Double delay = 1.0;
                    if (entityId != srcId) {// does not delay self messages
                        delay += getNetworkDelay(srcId, entityId);
                    }

                    send(smartThing.getVmLocalServerCloudlet().getId(), delay, MobileEvents.DELIVERY_VM, smartThing);
                    LogMobile.debug("FogDevice.java", smartThing.getName() + " was scheduled the DELIVERY_VM  from " +
                            smartThing.getVmLocalServerCloudlet().getName() + " to "
                            + smartThing.getDestinationServerCloudlet().getName());
                    Log.printLine("FogDevice.java" + smartThing.getName() + " was scheduled the DELIVERY_VM  from " +
                            smartThing.getVmLocalServerCloudlet().getName() + " to "
                            + smartThing.getDestinationServerCloudlet().getName());

                    sendNow(smartThing.getDestinationServerCloudlet().getId(), MobileEvents.VM_MIGRATE, smartThing);
                    Map<String, Object> ma;
                    ma = new HashMap<String, Object>();

                    //				for(Vm vm: smartThing.getSourceServerCloudlet().getHost().getVmList()){
                    //					Log.printLine("Vm # "+vm.getId());
                    //					if(vm.getId()==smartThing.getMyId()){
                    //						ma.put("vm", vm);
                    //						break;
                    //					}
                    //				}
                    if (smartThing.getVmMobileDevice() == null) {
                        Log.printLine(smartThing.getName() + " has a null VM");
                    }
                    ma.put("vm", smartThing.getVmMobileDevice());
                    ma.put("host", smartThing.getDestinationServerCloudlet().getHost());
                    if (ma.size() < 2) {
                        sendNow(getId(), MobileEvents.ABORT_MIGRATION, smartThing);
                        Log.printLine("FogDevice.java ma.size()<2");
                        System.exit(0);
                    } else {
                        sendNow(smartThing.getVmLocalServerCloudlet().getId(), CloudSimTags.VM_MIGRATE, ma);
                        LogMobile.debug("FogDevice.java", "CloudSim.VM_MIGRATE was scheduled  to VM#: " + smartThing.getVmMobileDevice().getId() + " HOST#: " +
                                smartThing.getDestinationServerCloudlet().getHost().getId());
                        Log.printLine("FogDevice.java" + " CloudSim.VM_MIGRATE was scheduled  to VM#: " + smartThing.getVmMobileDevice().getId() + " HOST#: " +
                                smartThing.getDestinationServerCloudlet().getHost().getId());
                        //	sendNow(smartThing.getDestinationServerCloudlet().getId(), FogEvents.APP_SUBMIT, applicationMap.get("MyApp_vr_game"+smartThing.getMyId()));
                    }
                } else {
                    sendNow(smartThing.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, smartThing);
                }
            } else {
                smartThing.setAbortMigration(false);
            }
        } else {
            LogMobile.debug("FogDevice.java", smartThing.getName() + " was excluded from List of SmartThings!");
        }
    }

    private void deliveryVM(SimEvent ev) {// pode ser um bom ponto de medição
        MobileDevice smartThing = (MobileDevice) ev.getData();
        if (MobileController.getSmartThings().contains(smartThing)) {

            LogMobile.debug("FogDevice.java", "DELIVERY VM: " + smartThing.getName() + " (id: " + smartThing.getId() + ") from " + smartThing.getVmLocalServerCloudlet().getName()
                    + " to " + smartThing.getDestinationServerCloudlet().getName());
            //			smartThing.getVmLocalServerCloudlet().setUplinkLatency(smartThing.getVmLocalServerCloudlet().getUplinkLatency()-(smartThing.getMigTime()/10000.0));

            smartThing.getVmLocalServerCloudlet().setSmartThingsWithVm(smartThing, Policies.REMOVE);

            smartThing.setVmLocalServerCloudlet(smartThing.getDestinationServerCloudlet());
            smartThing.setDestinationServerCloudlet(null);

            smartThing.getVmLocalServerCloudlet().setSmartThingsWithVm(smartThing, Policies.ADD);
//			if(smartThing.getSourceServerCloudlet() == null){
//				smartThing.setSourceServerCloudlet(smartThing.getVmLocalServerCloudlet());
//			}
//			if(!smartThing.getSourceServerCloudlet().equals(smartThing.getVmLocalServerCloudlet())){
//				smartThing.getSourceServerCloudlet().desconnectServerCloudletSmartThing(smartThing);
//				smartThing.getVmLocalServerCloudlet().connectServerCloudletSmartThing(smartThing);
//				//					smartThing.setParentId(smartThing.getVmLocalServerCloudlet().getId());
//			}
            if (MyStatistics.getInstance().getInitialTimeDelayAfterNewConnection().containsKey(smartThing.getMyId())) {
                smartThing.setMigStatus(false);
                smartThing.setPostCopyStatus(false);
                smartThing.setMigStatusLive(false);
                if (MyStatistics.getInstance().getInitialWithoutVmTime().get(smartThing.getMyId()) != null) {
                    MyStatistics.getInstance().finalWithoutVmTime(smartThing.getMyId(), CloudSim.clock());
                    MyStatistics.getInstance().getInitialWithoutVmTime().remove(smartThing.getMyId());
                }
                LogMobile.debug("FogDevice.java", smartThing.getName() + " had migStatus to false - deliveryVM");
                //				MyStatistics.getInstance().finalWithoutVmTime(smartThing.getMyId(), CloudSim.clock()+getCharacteristics().getCpuTime(smartThing.getVmMobileDevice().getSize()*1024*1024*8, 0.0));
                MyStatistics.getInstance().finalDelayAfterNewConnection(smartThing.getMyId(), CloudSim.clock() + getCharacteristics().getCpuTime(smartThing.getVmMobileDevice().getSize() * 1024 * 1024 * 8, 0.0));//handoff has been occurred first than delivery
                if (smartThing.getSourceServerCloudlet() == null) {
                    smartThing.setSourceServerCloudlet(smartThing.getVmLocalServerCloudlet());
                    Log.printLine("CRASH " + smartThing.getMyId() + "\t source c " + smartThing.getSourceServerCloudlet()
                            + "\t local server " + smartThing.getVmLocalServerCloudlet());
                }
                if (!smartThing.getSourceServerCloudlet().equals(smartThing.getVmLocalServerCloudlet())) {
                    smartThing.getSourceServerCloudlet().desconnectServerCloudletSmartThing(smartThing);

                    smartThing.getVmLocalServerCloudlet().connectServerCloudletSmartThing(smartThing);
                    //					smartThing.setParentId(smartThing.getVmLocalServerCloudlet().getId());
                }
            } else {
                //	MyStatistics.getInstance().startDelayAfterNewConnection(smartThing.getMyId(),CloudSim.clock());//handoff hasn't been occurred yet
            }

            float migrationLocked = (smartThing.getVmMobileDevice().getSize() * (smartThing.getPosition().getSpeed() + 1)) + 20000;
            if (migrationLocked < smartThing.getTravelPredicTime() * 1000) {
                migrationLocked = smartThing.getTravelPredicTime() * 1000;
            }
            send(smartThing.getVmLocalServerCloudlet().getId(), migrationLocked, MobileEvents.UNLOCKED_MIGRATION, smartThing);
            MyStatistics.getInstance().countMigration();
            MyStatistics.getInstance().historyMigrationTime(smartThing.getMyId(), smartThing.getMigTime());

            smartThing.setTimeFinishDeliveryVm(CloudSim.clock());
        } else {
            LogMobile.debug("FogDevice.java", smartThing.getName() + " was excluded by List of SmartThings! (inside Delivery Vm)");
        }
    }

    private void invokeNoMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();

        if (smartThing.isLockedToMigration()) {//isMigStatus()){
            LogMobile.debug("FogDevice.java", "IN MIGRATE: " + smartThing.getName() + " already is in migration Process or the migration is locked");
        } else {
            LogMobile.debug("FogDevice.java", "IN MIGRATE: " + smartThing.getName() + " is not in Migrate");
        }
    }

    private void invokeBeforeMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        if (MobileController.getSmartThings().contains(smartThing)) {
            //if(BeforeMigration.dataPepare(getPolicyReplicaVM(), smartThing)){//the smartThing is outside of the map
            if (smartThing.getSourceAp() != null && !smartThing.isMigStatus()) {//the smartThing isn't connected in any ap right now
                double delayProcess = getBeforeMigrate().dataprepare(smartThing);
                if (delayProcess >= 0) {
                    if (getPolicyReplicaVM() == Policies.LIVE_MIGRATION) {
                        smartThing.setPostCopyStatus(true);
                        smartThing.setTimeStartLiveMigration(CloudSim.clock());
                    } else {
                        smartThing.setMigStatus(true);
                        MyStatistics.getInstance().startWithoutVmTime(smartThing.getMyId(), CloudSim.clock());
                        smartThing.setTimeFinishDeliveryVm(-1.0);
                        send(smartThing.getVmLocalServerCloudlet().getId(), smartThing.getMigTime() + delayProcess, MobileEvents.START_MIGRATION, smartThing);//It'll happen according the Migration Time
                    }
                    smartThing.setLockedToMigration(true);
                }
            } else {
                sendNow(smartThing.getVmLocalServerCloudlet().getId(), MobileEvents.NO_MIGRATION, smartThing);
                MyStatistics.getInstance().getInitialWithoutVmTime().remove(smartThing.getMyId());
                MyStatistics.getInstance().getInitialTimeDelayAfterNewConnection().remove(smartThing.getMyId());
                MyStatistics.getInstance().getInitialTimeWithoutConnection().remove(smartThing.getMyId());
                smartThing.setMigStatus(false);
                smartThing.setPostCopyStatus(false);
                smartThing.setMigStatusLive(false);
                smartThing.setLockedToMigration(false);
                smartThing.setTimeFinishDeliveryVm(-1.0);
                smartThing.setAbortMigration(true);
            }
        } else {
            sendNow(smartThing.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, smartThing);
        }
    }

    private void migStatusToLiveMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        sendNow(smartThing.getVmLocalServerCloudlet().getId(), MobileEvents.START_MIGRATION, smartThing);//It'll happen according the Migration Time
    }

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

    private void invokeDecisionMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        for (MobileDevice st : getSmartThings()) {
            if (st.getSourceAp() != null && (!st.isLockedToMigration())) {// (!st.isMigStatus())){//Only the connected smartThings
                if (st.getVmLocalServerCloudlet().getMigrationStrategy().shouldMigrate(st)) {
                    if (!st.getVmLocalServerCloudlet().equals(st.getDestinationServerCloudlet())) {
                        Log.printLine("====================ToMigrate================== " + st.getName() + " " + st.getId());// to do something
                        LogMobile.debug("FogDevice.java", "Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": " +
                                Distances.checkDistance(st.getPosition().getCoordinate(), st.getSourceAp().getPosition().getCoordinate()));
                        Log.printLine("Migration time: " + st.getMigTime());
                        LogMobile.debug("FogDevice.java", "Made the decisionMigration for " + st.getName());
                        LogMobile.debug("FogDevice.java", "from " + st.getVmLocalServerCloudlet().getName() + " to " + st.getDestinationServerCloudlet().getName() +
                                " -> Connected by: " + st.getSourceServerCloudlet().getName());
                        sendNow(st.getVmLocalServerCloudlet().getId(), MobileEvents.TO_MIGRATION, st);
                        MyStatistics.getInstance().getInitialWithoutVmTime().remove(st.getMyId());
                        MyStatistics.getInstance().getInitialTimeDelayAfterNewConnection().remove(st.getMyId());
                        MyStatistics.getInstance().getInitialTimeWithoutConnection().remove(st.getMyId());
                        st.setLockedToMigration(true);
                        st.setTimeFinishDeliveryVm(-1.0);
                        saveMigration(st);
                    } else {
                        sendNow(getId(), MobileEvents.NO_MIGRATION, st);
                    }
                } else {
                    sendNow(getId(), MobileEvents.NO_MIGRATION, st);
                }
            } else {
                sendNow(getId(), MobileEvents.NO_MIGRATION, st);
            }
        }
    }

    /**
     * Perform miscellaneous resource management tasks
     *
     * @param ev
     */
    private void manageResources(SimEvent ev) {
        updateEnergyConsumption();
        //send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }

    /**
     * Updating the number of modules of an application module on this device
     *
     * @param ev instance of SimEvent containing the module and no of instances
     */
    private void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        if (!moduleInstanceCount.containsKey(appId))
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        Log.printLine(getName() + " Creating " + config.getInstanceCount() + " instances of module " + config.getModule().getName());
    }

    /**
     * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
     *
     * @param ev SimEvent instance containing the edge to send tuple on
     */
    private void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge) ev.getData();
        String srcModule = edge.getSource();
        AppModule module = null;
        for (Vm vm : getHost().getVmList()) {
            //if(vm.getVmm().equals("Xen")){
            if (((AppModule) vm).getName().equals(srcModule)) {
                module = (AppModule) vm;
                break;
            }
            //}
        }
        if (module == null)
            return;

        int instanceCount = getModuleInstanceCount().get(module.getAppId()).get(srcModule);

        /*
         * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
         */
        for (int i = 0; i < ((edge.getDirection() == Tuple.UP) ? instanceCount : 1); i++) {
            if (applicationMap.isEmpty()) {
                continue;
            } else {
                Application app = getApplicationMap().get(module.getAppId());
                if (app == null) {
//					Log.printLine("*sendPeriodicTuple*");
//					Log.printLine("Clock: "+CloudSim.clock()+" - "+ getName());
//					Log.printLine("FogDevice.java - App == null");
//					Log.printLine("FogDevice.java - module.getAppId: "+module.getAppId());
//					Log.printLine("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
//					for(MobileDevice st: MobileController.getSmartThings()){
//						if(module.getVmm().equals(st.getVmMobileDevice().getVmm())){
//							Tuple tuple = st.getVmLocalServerCloudlet().applicationMap.get(module.getAppId()).createTuple(edge, st.getVmLocalServerCloudlet().getId());
//							st.getVmLocalServerCloudlet().updateTimingsOnSending(tuple);
//							st.getVmLocalServerCloudlet().sendToSelf(tuple);
//							send(st.getVmLocalServerCloudlet().getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
//							Log.printLine("Resend "+ module.getAppId()+ " to "+st.getVmLocalServerCloudlet().getName());
//							break;
//						}
//					}
                    continue;
                }
                Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
                updateTimingsOnSending(tuple);
                sendToSelf(tuple);
            }
        }
        if (applicationMap.isEmpty()) {
            return;
        } else {
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }

    protected void processActuatorJoined(SimEvent ev) {
        int actuatorId = ev.getSource();
        double delay = (double) ev.getData();
        getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
    }

    protected void updateActiveApplications(SimEvent ev) {
        Application app = (Application) ev.getData();
        Log.printLine("FogDevice " + this.getName() + " Apps " + getActiveApplications() + " Adding app " + app.getAppId());
        if (!getActiveApplications().contains(app.getAppId()))
            getActiveApplications().add(app.getAppId());
        Log.printLine(" Apps " + getActiveApplications());
    }

    public String getOperatorName(int vmId) {
        for (Vm vm : this.getHost().getVmList()) {
            if (vm.getId() == vmId)
                return ((AppModule) vm).getName();
        }
        return null;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    @Override
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                if (utilizationOfCpu < 0 || utilizationOfCpu > 1) {
                    Log.printLine("utilizationOfCpu: " + utilizationOfCpu);
                }
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);

        checkCloudletCompletion();

        /** Remove completed VMs **/
        /**
         * Change made by HARSHIT GUPTA
         */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }

    @Override
    protected void checkCloudletCompletion() {
        boolean cloudletCompleted = false;
        List<Vm> removeVmList = new ArrayList<>();
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        cloudletCompleted = true;
                        Tuple tuple = (Tuple) cl;
                        TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        if (application == null) {
//							Log.printLine("*checkCloudletCompletion*");
//							Log.printLine("Clock: "+CloudSim.clock()+" - "+ getName());
//							Log.printLine("FogDevice.java - Application == null");
//							Log.printLine("FogDevice.java - tuple.getAppId: "+tuple.getAppId());
//							Log.printLine("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
                            removeVmList.add(vm);
//							for(MobileDevice st: MobileController.getSmartThings()){
//								for(Sensor s: st.getSensors()){
//									if(tuple.getAppId().equals(s.getAppId())){
//										st.getVmLocalServerCloudlet().checkCloudletCompletion();
//										application = st.getVmLocalServerCloudlet().getApplicationMap().get(tuple.getAppId());
//										List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId());
//										for(Tuple resTuple : resultantTuples){
//											resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
//											resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
//											st.getVmLocalServerCloudlet().updateTimingsOnSending(resTuple);
//											st.getVmLocalServerCloudlet().sendToSelf(resTuple);
//										}
//										sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
//										Logger.debug(st.getVmLocalServerCloudlet().getName(), "Completed execution of tuple "+tuple.getCloudletId()+" on "+tuple.getDestModuleName());
//										LogMobile.debug(st.getVmLocalServerCloudlet().getName(), "Completed execution of tuple "+tuple.getCloudletId()+" on "+tuple.getDestModuleName());
//
//									}
//
//								}
//							}
                            continue;
                        }
                        //	Logger.ENABLED=true;
                        //						Log.printLine("FogDevice.java - tuple.getAppId: "+tuple.getAppId());

                        Logger.debug(getName(), "Completed execution of tuple " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
                        //	Logger.ENABLED=false;

                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId());
                        for (Tuple resTuple : resultantTuples) {
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule) vm).getName(), vm.getId());
                            updateTimingsOnSending(resTuple);
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
            for (Vm vm : removeVmList) {
                host.getVmList().remove(vm);
            }
            removeVmList.clear();
        }

        if (cloudletCompleted)
            updateAllocatedMips(null);
        //		Logger.ENABLED=false;

    }

    protected void updateTimingsOnSending(Tuple resTuple) {
        // TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE.
        // WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
        String srcModule = resTuple.getSrcModuleName();
        String destModule = resTuple.getDestModuleName();
        for (AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()) {
            if (loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)) {
                int tupleId = TimeKeeper.getInstance().getUniqueId();
                resTuple.setActualTupleId(tupleId);
                if (!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
                    TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
                TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
                TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());

                //Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);

            }
        }
    }

    protected int getChildIdWithRouteTo(int targetDeviceId) {
        for (Integer childId : getChildrenIds()) {
            if (targetDeviceId == childId)
                return childId;
            if (((FogDevice) CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
                return childId;
        }
        return -1;
    }

    protected int getChildIdForTuple(Tuple tuple) {
        if (tuple.getDirection() == Tuple.ACTUATOR) {
            int gatewayId = ((Actuator) CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
            return getChildIdWithRouteTo(gatewayId);
        }
        return -1;
    }

    protected void updateAllocatedMips(String incomingOperator) {
        getHost().getVmScheduler().deallocatePesForAllVms();
        for (final Vm vm : getHost().getVmList()) {
            //if(vm.getVmm().equals("Xen")){
            if (vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule) vm).getName().equals(incomingOperator)) {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            } else {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add(0.0);
                    }
                });
            }
            //}
        }

        updateEnergyConsumption();
    }

    private void updateEnergyConsumption() {
        double totalMipsAllocated = 0;
        for (final Vm vm : getHost().getVmList()) {
            //			AppModule operator = (AppModule)vm;
            //			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
            //					.getAllocatedMipsForVm(operator));
            totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
        }

        double timeNow = CloudSim.clock();
        double currentEnergyConsumption = getEnergyConsumption();
        double newEnergyConsumption = currentEnergyConsumption + (timeNow - lastUtilizationUpdateTime) * getHost().getPowerModel().getPower(lastUtilization);
        setEnergyConsumption(newEnergyConsumption);

		/*if(getName().equals("d-0")){
			Log.printLine("------------------------");
			Log.printLine("Utilization = "+lastUtilization);
			Log.printLine("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			Log.printLine(timeNow-lastUtilizationUpdateTime);
		}*/
        double currentCost = getTotalCost();
        double newcost = currentCost + (timeNow - lastUtilizationUpdateTime) * getRatePerMips() * lastUtilization * getHost().getTotalMips();
        setTotalCost(newcost);

        lastUtilization = Math.min(1, totalMipsAllocated / getHost().getTotalMips());
        lastUtilizationUpdateTime = timeNow;
    }

    protected void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        //		Log.printLine("*************FogDevice.java********* app.getAppId: "+app.getAppId());
        applicationMap.put(app.getAppId(), app);
    }

    protected void addChild(int childId) {
        if (CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
            return;
        if (!getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().add(childId);
        if (!getChildToOperatorsMap().containsKey(childId))
            getChildToOperatorsMap().put(childId, new ArrayList<String>());
    }

    protected void removeChild(int childId) {
        getChildrenIds().remove(CloudSim.getEntity(childId));
        getChildToOperatorsMap().remove(childId);
    }

    protected void updateCloudTraffic() {
        int time = (int) CloudSim.clock() / 1000;
        if (!cloudTrafficMap.containsKey(time))
            cloudTrafficMap.put(time, 0);
        cloudTrafficMap.put(time, cloudTrafficMap.get(time) + 1);
    }

    protected void sendTupleToActuator(Tuple tuple) {

        /*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
        for (Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()) {
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator) CloudSim.getEntity(actuatorId)).getActuatorType();
            if (tuple.getDestModuleName().equals(actuatorType)) {
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }
        for (int childId : getChildrenIds()) {
            sendDown(tuple, childId);
        }
    }

    public void saveLostTupple(String a, String filename) {
        try (FileWriter fw1 = new FileWriter(filename, true);
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

    protected void processTupleArrival(SimEvent ev) {
        Tuple tuple = (Tuple) ev.getData();
        MyStatistics.getInstance().setMyCountTotalTuple(1);

        boolean flagContinue = false;
        for (MobileDevice st : MobileController.getSmartThings()) {// verifica se o smartthing ainda esta na lista
            for (Sensor s : st.getSensors()) {
                if (tuple.getAppId().equals(s.getAppId())) {
                    flagContinue = true;
                    break;
                }
            }
        }
        if (!flagContinue) {
            return;
        }

        if (tuple.getInitialTime() == -1) {
            //			if(tuple.getTupleType().contains("EEG")){
            MyStatistics.getInstance().getTupleLatency().put(tuple.getMyTupleId(), CloudSim.clock() - getUplinkLatency());
            tuple.setInitialTime(CloudSim.clock() - getUplinkLatency());
            //			}
        }

        for (MobileDevice st : getSmartThings()) {
            if (st.getId() == ev.getSource()) {
                if ((!st.isHandoffStatus() && !st.isMigStatus())) {//&& (ev.getSource()!=ev.getDestination())){
//					if(MyStatistics.getInstance().getInitialWithoutVmTime().get(st.getMyId())!=null){
//						MyStatistics.getInstance().finalWithoutVmTime(st.getMyId(), CloudSim.clock());
//						MyStatistics.getInstance().getInitialWithoutVmTime().remove(st.getMyId());
//					}

                    break;
                } else {
                    MyStatistics.getInstance().setMyCountLostTuple(1);
                    saveLostTupple(String.valueOf(CloudSim.clock()), st.getId() + "fdlostTupple.txt");
                    if (st.isMigStatus()) {//send again
                        //fazer mediçao aqui.
//						if(MyStatistics.getInstance().getInitialWithoutVmTime().get(st.getMyId())==null){
//							MyStatistics.getInstance().startWithoutVmTime(st.getMyId(), CloudSim.clock());
//						}

                        LogMobile.debug("FogDevice.java", st.getName() + " is in Migration");
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - FogDevice.java - Destination: "+CloudSim.getEntityName(ev.getDestination())+": Tuple "+tuple.getCloudletId());
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - FogDevice.java - Source: "+CloudSim.getEntityName(ev.getSource()));
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - TupleId: "+tuple.getMyTupleId()+" FogDevice.java - migration");
//						double delay = st.getMigTime() - (CloudSim.clock() - MyStatistics.getInstance().getInitialWithoutVmTime().get(st.getMyId()));
//						if(delay < 1){
//							delay = 100;
//						}
//						send(st.getDestinationServerCloudlet().getId(),1000, FogEvents.TUPLE_ARRIVAL, tuple);
                        return;
                    } else {//send again
                        //						LogMobile.debug("FogDevice.java", st.getName()+" is in Handoff");
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - FogDevice.java - Destination: "+CloudSim.getEntityName(ev.getDestination())+": Tuple "+tuple.getCloudletId());
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - FogDevice.java - Source: "+CloudSim.getEntityName(ev.getSource()));
                        //						Log.printLine("Clock: "+CloudSim.clock()+" - TupleId: "+tuple.getMyTupleId()+" FogDevice.java - handoff");
//						send(ev.getDestination(), 100, FogEvents.TUPLE_ARRIVAL, tuple);
                        return;
                    }
                }
            }
        }
        if (getName().equals("cloud")) {
            updateCloudTraffic();
        }

		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			Log.printLine(++numClients);
		}*/
        Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + " with tupleType = " + tuple.getTupleType() + "\t| Source : " +
                CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));
        //		if(tuple.getTupleType().contains("EEG")){
        //			Logger.ENABLED=true;
        //			Logger.debug(getName(), "Received tuple "+tuple.getMyTupleId()+" with tupleType = "+tuple.getTupleType()+"\t| Source : "+
        //				CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
        //			Logger.ENABLED=false;
        //		}
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

        if (FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())) {
        }

        if (tuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(tuple);
            return;
        }
        //		int index=0;
        if (getHost().getVmList().size() > 0) {
            for (Vm vm : getHost().getVmList()) {
                //if(vm.getVmm().equals("Xen")){//if(tuple.getAppId().equals("MyApp_vr_game"+vm.getId())){
                final AppModule operator = (AppModule) vm;//getHost().getVmList().get(index);
                if (CloudSim.clock() > 0) {
                    getHost().getVmScheduler().deallocatePesForVm(operator);
                    getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>() {
                        protected static final long serialVersionUID = 1L;

                        {
                            add((double) getHost().getTotalMips());
                        }
                    });
                }

                break;
                //}
                //				Log.printLine("tuple.getAppId(): "+tuple.getAppId());
                //				Log.printLine("Vm #: "+getHost().getVmList().get(index).getId()+ " UserId: "+getHost().getVmList().get(index).getUserId()+" Name: "+getHost().getVmList().get(index).getVmm());
                //				index++;
            }
        }
        if (getName().equals("cloud") && tuple.getDestModuleName() == null) {
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
        }

        if (appToModulesMap.containsKey(tuple.getAppId())) {
            if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
                int vmId = -1;
                for (Vm vm : getHost().getVmList()) {
                    //if(vm.getVmm().equals("Xen")){
                    if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
                        vmId = vm.getId();
                    //}
                }
                if (vmId < 0
                        || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                        tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
                    return;
                }
                tuple.setVmId(vmId);
                //Logger.error(getName(), "Executing tuple for operator " + moduleName);

                updateTimingsOnReceipt(tuple);

                executeTuple(ev, tuple.getDestModuleName());
            } else {
                if (tuple.getDestModuleName() != null) {
                    if (tuple.getDirection() == Tuple.UP)
                        sendUp(tuple);
                    else if (tuple.getDirection() == Tuple.DOWN) {
                        for (int childId : getChildrenIds()) {
                            MobileDevice tempSt = (MobileDevice) CloudSim.getEntity(childId);
                            if (tuple.getAppId().equals(((AppModule) tempSt.getVmMobileDevice()).getAppId())) {
                                //						Log.printLine("FogDevice: "+CloudSim.getEntityName(getId())+" ChildId: "+CloudSim.getEntityName(childId)+" "+tuple.getTupleType());
                                sendDown(tuple, childId);
                            }
                        }
                    }
                } else {
                    sendUp(tuple);
                }
            }
        } else {
            if (tuple.getDirection() == Tuple.UP)
                sendUp(tuple);
            else if (tuple.getDirection() == Tuple.DOWN) {
                for (int childId : getChildrenIds()) {
                    MobileDevice tempSt = (MobileDevice) CloudSim.getEntity(childId);
                    if (tuple.getAppId().equals(((AppModule) tempSt.getVmMobileDevice()).getAppId())) {
                        //						Log.printLine("FogDevice: "+CloudSim.getEntityName(getId())+" ChildId: "+CloudSim.getEntityName(childId)+" "+tuple.getTupleType());
                        sendDown(tuple, childId);
                    }
                }
            }
        }
    }

    public void printResults(String a, String filename) {
        try (FileWriter fw1 = new FileWriter(filename, true);
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

    protected void updateTimingsOnReceipt(Tuple tuple) {
        Application app = getApplicationMap().get(tuple.getAppId());
        if (app == null) {
//			Log.printLine("*updateTimingsOnReceipt*");
//			Log.printLine("Clock: "+CloudSim.clock()+" - "+ getName());
//			Log.printLine("FogDevice.java - App == null");
//			Log.printLine("FogDevice.java - tuple.getAppId: "+tuple.getAppId());
//			Log.printLine("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
//						for(MobileDevice st: MobileController.getSmartThings()){
//							for(Sensor s: st.getSensors()){
//								if(tuple.getAppId().equals(s.getAppId())){
//									st.getVmLocalServerCloudlet().updateTimingsOnReceipt(tuple);
//								}
//							}
//						}
            return;
        }
        String srcModule = tuple.getSrcModuleName();
        String destModule = tuple.getDestModuleName();
        List<AppLoop> loops = app.getLoops();
        for (AppLoop loop : loops) {
            //			double plusLatency = 0.0;
            //			if(this.getName().contains("SmartThing")){
            //				MobileDevice st = (MobileDevice) this;
            //				if(st.getSourceServerCloudlet()!=null){
            //					if(!st.getSourceServerCloudlet().equals(st.getVmLocalServerCloudlet())){
            //						plusLatency = st.getSourceServerCloudlet().getUplinkLatency();
            //					}
            //				}
            //			}
            if (loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)) {
                Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                if (startTime == null)
                    break;
                if (!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())) {
                    TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
                    TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
                    TimeKeeper.getInstance().getMaxLoopExecutionTime().put(loop.getLoopId(), 0.0);
                    printResults(String.valueOf(0), loop.getLoopId() + "LoopId.txt");
                    printResults(String.valueOf(0), loop.getLoopId() + "LoopMaxId.txt");
                }
                double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
                int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
                double delay = CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());//+plusLatency);
                if (delay > TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loop.getLoopId())) {
                    TimeKeeper.getInstance().getMaxLoopExecutionTime().put(loop.getLoopId(), delay);
                    printResults(String.valueOf(delay), loop.getLoopId() + "LoopMaxId.txt");
                }
                TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
                double newAverage = (currentAverage * currentCount + delay) / (currentCount + 1);
                TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
                TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount + 1);
                printResults(String.valueOf(newAverage), loop.getLoopId() + "LoopId.txt");
                break;
            }
        }
    }


    protected void processSensorJoining(SimEvent ev) {
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
    }

    protected void executeTuple(SimEvent ev, String operatorId) {
        //TODO Power funda
        Tuple tuple = (Tuple) ev.getData();

        boolean flagContinue = false;
        for (MobileDevice st : MobileController.getSmartThings()) {// verifica se o smartthing ainda esta na lista
            for (Sensor s : st.getSensors()) {
                if (tuple.getAppId().equals(s.getAppId())) {
                    flagContinue = true;
                    break;
                }
            }
        }
        if (!flagContinue) {
            return;
        }

        Logger.debug(getName(), "Executing tuple " + tuple.getCloudletId() + " on module " + operatorId);

        if (MyStatistics.getInstance().getTupleLatency().get(tuple.getMyTupleId()) != null) {//if(tuple.getTupleType().contains("EEG")){
            tuple.setFinalTime(CloudSim.clock() + getUplinkLatency());
            //			MyStatistics.getInstance().putLatencyFileValue(tuple.getFinalTime()- MyStatistics.getInstance().getTupleLatency().get(tuple.getMyTupleId())//tuple.getInitialTime()+(2*getUplinkLatency())
            //					, CloudSim.clock(),tuple.getTupleType(),tuple.getMyTupleId());
        }

        Application app = getApplicationMap().get(tuple.getAppId());
        if (app == null) {
//			Log.printLine("*executeTuple*");
//			Log.printLine("Clock: "+CloudSim.clock()+" - "+ getName());
//			Log.printLine("FogDevice.java - App == null");
//			Log.printLine("FogDevice.java - tuple.getAppId: "+tuple.getAppId());
//			Log.printLine("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
//						for(MobileDevice st: MobileController.getSmartThings()){
//							for(Sensor s: st.getSensors()){
//								if(tuple.getAppId().equals(s.getAppId())){
//									st.getVmLocalServerCloudlet().executeTuple(ev, operatorId);
//								}
//							}
//						}
            return;
        }

        TimeKeeper.getInstance().tupleStartedExecution(tuple);
        updateAllocatedMips(operatorId);
        processCloudletSubmit(ev, false);
        updateAllocatedMips(operatorId);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
    }

    protected void processModuleArrival(SimEvent ev) {
        AppModule module = (AppModule) ev.getData();
        String appId = module.getAppId();
        if (!appToModulesMap.containsKey(appId)) {
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        appToModulesMap.get(appId).add(module.getName());
        processVmCreate(ev, false);
        if (module.isBeingInstantiated()) {
            module.setBeingInstantiated(false);
        }

        initializePeriodicTuples(module);

        module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                .getAllocatedMipsForVm(module));
    }

    private void initializePeriodicTuples(AppModule module) {
        String appId = module.getAppId();
        Application app = getApplicationMap().get(appId);
        List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
        for (AppEdge edge : periodicEdges) {
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }

    protected void processOperatorRelease(SimEvent ev) {
        this.processVmMigrate(ev, false);
    }


    protected void updateNorthTupleQueue() {

        if (!getNorthTupleQueue().isEmpty()) {
            Tuple tuple = getNorthTupleQueue().poll();
            sendUpFreeLink(tuple);
        } else {
            setNorthLinkBusy(false);
        }
    }

    protected void sendUpFreeLink(Tuple tuple) {

        double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
        setNorthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        send(parentId, networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
    }

    protected void sendUp(Tuple tuple) {

        if (parentId > 0) {
            if (!isNorthLinkBusy()) {
                sendUpFreeLink(tuple);
            } else {
                northTupleQueue.add(tuple);
            }
        }
    }

    protected void updateSouthTupleQueue() {
        if (!getSouthTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getSouthTupleQueue().poll();
            sendDownFreeLink(pair.getFirst(), pair.getSecond());
        } else {
            setSouthLinkBusy(false);
        }
    }

    protected void sendDownFreeLink(Tuple tuple, int childId) {
        double networkDelay = tuple.getCloudletFileSize() / getDownlinkBandwidth();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        setSouthLinkBusy(true);
        double latency = getChildToLatencyMap().get(childId);
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
        send(childId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void sendDown(Tuple tuple, int childId) {
        if (getChildrenIds().contains(childId)) {
            if (!isSouthLinkBusy()) {
                sendDownFreeLink(tuple, childId);
            } else {
                southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
            }
        }
    }


    protected void sendToSelf(Tuple tuple) {

        send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
    }

    public PowerHost getHost() {
        return (PowerHost) getHostList().get(0);
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public List<Integer> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }

    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }

    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }

    public double getUplinkLatency() {
        return uplinkLatency;
    }

    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }

    public boolean isSouthLinkBusy() {
        return isSouthLinkBusy;
    }

    public void setSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }

    public boolean isNorthLinkBusy() {
        return isNorthLinkBusy;
    }

    public void setNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }

    public int getControllerId() {
        return controllerId;
    }

    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
    }

    public List<String> getActiveApplications() {
        return activeApplications;
    }

    public void setActiveApplications(List<String> activeApplications) {
        this.activeApplications = activeApplications;
    }

    public Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }

    public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }

    public Map<String, Application> getApplicationMap() {
        return applicationMap;
    }

    public void setApplicationMap(Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
    }

    public Queue<Tuple> getNorthTupleQueue() {
        return northTupleQueue;
    }

    public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
        this.northTupleQueue = northTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
        return southTupleQueue;
    }

    public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
        this.southTupleQueue = southTupleQueue;
    }

    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
        return associatedActuatorIds;
    }

    public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
        this.associatedActuatorIds = associatedActuatorIds;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }

    public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Map<String, Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }

    public void setModuleInstanceCount(
            Map<String, Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
    }

    public DecisionMigration getMigrationStrategy() {
        return migrationStrategy;
    }

    public void setMigrationStrategy(DecisionMigration migrationStrategy) {
        this.migrationStrategy = migrationStrategy;
    }

    public int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public void setPolicyReplicaVM(int policyReplicaVM) {
        this.policyReplicaVM = policyReplicaVM;
    }

    public Set<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public void setServerCloudlets(FogDevice sc, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.serverCloudlets.add(sc);
        } else {
            this.serverCloudlets.remove(sc);
        }
    }

    public FogDevice getServerCloudletToVmMigrate() {
        return serverCloudletToVmMigrate;
    }

    public void setServerCloudletToVmMigrate(FogDevice serverCloudletToVmMigrate) {
        this.serverCloudletToVmMigrate = serverCloudletToVmMigrate;
    }

    public Set<MobileDevice> getSmartThingsWithVm() {
        return smartThingsWithVm;
    }

    public void setSmartThingsWithVm(MobileDevice st, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.smartThingsWithVm.add(st);
        } else {
            this.smartThingsWithVm.remove(st);
        }
    }

    public int getVolatilParentId() {
        return volatilParentId;
    }

    public void setVolatilParentId(int volatilParentId) {
        this.volatilParentId = volatilParentId;
    }

    public BeforeMigration getBeforeMigrate() {
        return beforeMigration;
    }

    public void setBeforeMigrate(BeforeMigration beforeMigration) {
        this.beforeMigration = beforeMigration;
    }

    public Owner getDeviceOwner() {
        return deviceOwner;
    }

    public void setDeviceOwner(Owner deviceOwner) {
        this.deviceOwner = deviceOwner;
    }

    public void addAttacker(Attacker attacker) {
        attackerList.add(attacker);
    }

    public IOffloadingResponseTimeCalculator getOffloadingResponseTimeCalculator() {
        return this.offloadingResponseTimeCalculator;
    }

    public void setOffloadingResponseTimeCalculator(IOffloadingResponseTimeCalculator responseTimeCalculator) {
        this.offloadingResponseTimeCalculator = responseTimeCalculator;
    }
}
