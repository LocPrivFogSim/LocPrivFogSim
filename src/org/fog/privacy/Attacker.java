package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.vmmobile.TestExample2;
import org.fog.vmmobile.constants.MaxAndMin;

import java.util.*;

/**
 * The Attacker class
 * - observes the (path of the) mobile device(s)
 * - computes the controlled area
 * - evaluates the possible matches with the known paths and the possible accuracy of a position mapping
 * - holds a list of 50 precomputed (due to execution time) paths
 */
public class Attacker {

    private static final int SCENARIO = TestExample2.getSCENARIO();
    private static final int MOBILE_CAN_BE_TURNED_OFF = TestExample2.getMobileCanBeTurnedOff();

    private List<MobileDevice> mobileDeviceList = new ArrayList<>();

    private String name;
    private List<FogDevice> controlledDeviceList = new ArrayList<>();
    private ArrayList<FogDevice> allFogDevicesList = new ArrayList<>();

    private HashMap<Integer, MobileDeviceInformation> knowledge = new HashMap<>();
    private double controlledArea = 0.0;
    private double totalArea_num = 0.0;
    private double[] areaOfFogNode = new double[TestExample2.getNumOfFogDevices()];
    private MobileDeviceInformation deviceInformation = new MobileDeviceInformation();

    private TreeMap<Integer, LinkedList<Integer>> knownPaths = new TreeMap<>();

    private LinkedList<Integer> path = new LinkedList<>();
    private TreeMap<Long, Position> allPositions = new TreeMap<>();

    // Scenario 2
    private DeviceMap deviceMap = new DeviceMap(TestExample2.getMAP_SIZE());
    private boolean hasDeviceMap = false;

    private long startTime;
    private long endTime;
    
    private int mobileDeviceActualPaths;

    private TreeMap<Long, Position> realPositionMap = new TreeMap<>();
    private double areaHitDuration;
    
    public Attacker(String name, List<FogDevice> controlledDevices, ArrayList<FogDevice> allFogDevicesList) {
        this.name = name;
        this.controlledDeviceList = controlledDevices;
        this.allFogDevicesList = allFogDevicesList;

        Log.printLine("Attacker " + name + " created");
        observe();
        if (SCENARIO == 1) {
            hasDeviceMap = false;
        }
        if (SCENARIO == 2) {
            hasDeviceMap = true;
            deviceMap = TestExample2.getDeviceMap();
        }

        if (hasDeviceMap) {

        }

        controlledArea = computeControlledArea();

        addPrecomputedPaths();
    }

    /* Observer */
    public void observe() {
//        for (FogDevice device : controlledDeviceList)
        for (FogDevice device : allFogDevicesList) {
            device.addObserver(this);
        }
    }

    public void update(FogDevice source, MobileDevice mobileDevice, int eventType, String event) {
    	Log.formatLine("Attacker was notified: MobileDevice '%s' was %sed to '%s' due to a %s event.", mobileDevice.getName(),
    			event, source.getName(), EventType.eventTypeToString(eventType));

    	if (eventType != EventType.OFFLOADING)
    		return;
    	
    	MobileDeviceInformation deviceInformation = knowledge.getOrDefault(mobileDevice.getMyId(), null);

        if (deviceInformation == null) {
            deviceInformation = new MobileDeviceInformation(mobileDevice);
        }

        Log.printLine("\n*************************************************");

        Position position = new Position();
        // position.setClosestFogDevice(deviceMap.findClosestFogDevice(TestExample2.getFogDeviceList(), mobileDevice));
        position.setClosestFogDevice(source);
        int id = 0;
        
        Position actualPosition = new Position();
        FogDevice actualFogNode = mobileDevice.getSourceServerCloudlet();
        if (actualFogNode == null) // in handoff
        	actualFogNode = mobileDevice.getDestinationServerCloudlet();
        if (actualFogNode == null) // in handoff
        	actualFogNode = deviceMap.findClosestFogDevice(TestExample2.getFogDeviceList(), mobileDevice);
        actualPosition.setClosestFogDevice(actualFogNode);

        long time = Calendar.getInstance().getTimeInMillis();

        switch (event) {
            case "add":
                position.setState("entering");
                actualPosition.setState("entering");
                allPositions.put(time, position);
                realPositionMap.put(time, actualPosition);
                id = position.getClosestFogDevice().getMyId();

                if (controlledDeviceList.contains(position.getClosestFogDevice())) {
                    deviceInformation.addPosition(time, position);
                    knowledge.put(mobileDevice.getMyId(), deviceInformation);
                    mobileDeviceList.add(mobileDevice);
                    if (path.isEmpty() || path.getLast() == null || path.getLast() != id) {
                        path.add(id);
                    }
                    Log.printLine("Attacker got information about entering device " + deviceInformation.getDeviceName() + " on Node " + id);
                }
                else {
                    if (path.isEmpty() || path.getLast() != null) {    // null means 'x' or 'not connected'
                        path.add(null);
                    }
                }

                break;

            case "remove":
                position.setState("leaving");
                actualPosition.setState("leaving");
                allPositions.put(time, position);
                realPositionMap.put(time, actualPosition);
                id = position.getClosestFogDevice().getMyId();

                if (controlledDeviceList.contains(position.getClosestFogDevice())) {
                    deviceInformation.addPosition(time, position);
                    knowledge.put(mobileDevice.getMyId(), deviceInformation);
                    mobileDeviceList.remove(mobileDevice);
                    if (path.isEmpty() || path.getLast() == null || path.getLast() != id) {
                        path.add(id);
                    }
                    Log.printLine("Attacker got information about leaving device " + deviceInformation.getDeviceName() + " on Node " + id);
                }
                else {
                    if (path.isEmpty() || path.getLast() != null) {    // null means 'x' or 'not connected'
                        path.add(null);
                    }
                }

                break;

            default:
                Log.printLine("Attacker update error: unknown operation " + event + " on Node " + id);
        }

        printKnowledge();
        
        if (actualPosition.getClosestFogDevice() == position.getClosestFogDevice())
        	Log.formatLine("Actual fog node %s is equals to attackers suspected fog node %s", actualPosition.getClosestFogDevice().getName(), position.getClosestFogDevice().getName());
        else
        	Log.formatLine("Actual fog node %s is NOT equals to attackers suspected fog node %s", actualPosition.getClosestFogDevice().getName(), position.getClosestFogDevice().getName());
    }





//    public void addControlledDevice(FogDevice controlledDevice) {
//        controlledDeviceList.add(controlledDevice);
//        Log.printLine("Attacker controls " + controlledDevice.getName());
//        getMobileDeviceList();
//        observe();
//
//        for (MobileDevice mobileDevice : controlledDevice.getSmartThings()) {
//            update(mobileDevice, "add");
//        }
//
//
//    }

    public double getAreaOfFogDevice(FogDevice fogDevice) {

        if (controlledArea == 0) {
            computeControlledArea();
        }

        return areaOfFogNode[fogDevice.getMyId()];
    }


    public double computeControlledArea() {
        double area = 0.0;

        totalArea_num = (MaxAndMin.MAX_X / TestExample2.getGridInterval()) * (MaxAndMin.MAX_Y / TestExample2.getGridInterval());
        totalArea_num = totalArea_num * TestExample2.getGridInterval() * TestExample2.getGridInterval();

        int[] fogNode_coverage = new int[TestExample2.getNumOfFogDevices()];

        double dx=0, dy=0, distance=0;
        double min_distance = MaxAndMin.MAX_X * MaxAndMin.MAX_Y;
        FogDevice closestDevice = null;

        if (SCENARIO == 1) {
            for (double i=0.0; i <= MaxAndMin.MAX_X; i+=TestExample2.getGridInterval()) {      // x
                for (double j = 0.0; j <= MaxAndMin.MAX_Y; j += TestExample2.getGridInterval()) {  // y

                    min_distance = MaxAndMin.MAX_X * MaxAndMin.MAX_Y;

                    for (FogDevice fogDevice : controlledDeviceList) {
                        dx = Math.pow((i - fogDevice.getCoord().getCoordX()), 2);     // (x_p - x_q)^2
                        dy = Math.pow((j - fogDevice.getCoord().getCoordY()), 2);     // (y_p - y_q)^2
                        distance = Math.sqrt((dx + dy));                            // sqrt(dx + dy)

                        if (distance < min_distance) {
                            min_distance = distance;
                            closestDevice = fogDevice;
                        }
                    }
                    if ((min_distance <= MaxAndMin.CLOUDLET_COVERAGE) && (closestDevice != null)) {
                        fogNode_coverage[closestDevice.getMyId()] += 1;
                    }
                }
            }
        }

        else if (SCENARIO == 2) {

            for (double i=0.0; i <= MaxAndMin.MAX_X; i += TestExample2.getGridInterval()) {      // x
                for (double j = 0.0; j <= MaxAndMin.MAX_Y; j += TestExample2.getGridInterval()) {  // y

                    min_distance = MaxAndMin.MAX_X * MaxAndMin.MAX_Y;

                    for (FogDevice fogDevice : allFogDevicesList) {
                        dx = Math.pow((fogDevice.getCoord().getCoordX() - i), 2);     // (x_p - x_q)^2
                        dy = Math.pow((fogDevice.getCoord().getCoordY() - j), 2);     // (y_p - y_q)^2
                        distance = Math.sqrt((dx + dy));                              // sqrt(dx + dy)

                        if (distance < min_distance) {
                            min_distance = distance;
                            closestDevice = fogDevice;
                        }
                    }

                    if ((min_distance <= MaxAndMin.CLOUDLET_COVERAGE) && (closestDevice != null)) {
                        fogNode_coverage[closestDevice.getMyId()] += 1;
                    }
                }
            }
        }

        for (FogDevice fogDevice : controlledDeviceList) {
            areaOfFogNode[fogDevice.getMyId()] = fogNode_coverage[fogDevice.getMyId()] * TestExample2.getGridInterval() * TestExample2.getGridInterval();
            area += areaOfFogNode[fogDevice.getMyId()];
        }

        // WARNING(markus): Returning the value to use it and setting the value in a private field is not a good practice
        // and leads to errors. I don't know what the reason for this was but it should probably be cleaned up i.e.,
        // either return the value and dont set or make the method void and set the value at the end.
        controlledArea = area;
        return area;
    }



    public void addKnownPath(int way, LinkedList<Integer> path) {
        knownPaths.put(way, path);
    }


    public ArrayList<Integer> getWaysForPath(LinkedList<Integer> path) {


        if (MOBILE_CAN_BE_TURNED_OFF == 1) {
            /* remove null in path */
            LinkedList<Integer> newPath = new LinkedList<>();
            for (Integer p : path) {
                if (p != null && (newPath.isEmpty() || newPath.getLast() != p)) {
                    newPath.add(p);
                }
            }
            path = newPath;
        } else {
            /* cut path if beginning or end is null */
            while (!path.isEmpty() && path.getLast() == null) {
                path.removeLast();
            }
            while (!path.isEmpty() && path.getFirst() == null) {
                path.removeFirst();
            }
        }
        this.path = path;

        /* get possible ways */
        ArrayList<Integer> controlledDeviceIds = new ArrayList<>();
        ArrayList<Integer> ways = new ArrayList<>();

        for (FogDevice fogDevice : controlledDeviceList) {
            controlledDeviceIds.add(fogDevice.getMyId());
        }

        Log.printLine();
        for (Integer key : knownPaths.keySet()) {

            LinkedList<Integer> trace = knownPaths.get(key);
            LinkedList<Integer> cutTrace = new LinkedList<>();

            if (MOBILE_CAN_BE_TURNED_OFF == 0) {

                for (int i : trace) {
                    if (controlledDeviceIds.contains(i)) {
                        cutTrace.add(i);
                    } else if (!cutTrace.isEmpty() && cutTrace.getLast() != null) {  // null means 'x' or 'not connected'
                        cutTrace.add(null);
                    }
                }
                while (!cutTrace.isEmpty() && cutTrace.getLast() == null) {
                    cutTrace.removeLast();
                }

            } else if (MOBILE_CAN_BE_TURNED_OFF == 1) {
                trace.retainAll(controlledDeviceIds);
                for (int i : trace) {
                    if (cutTrace.isEmpty() || cutTrace.getLast() != i) {
                        cutTrace.add(i);
                    }
                }
            }

            knownPaths.put(key, cutTrace);
            Log.printLine(key + ": " + cutTrace.toString());
        }

        double avg_length = 0;

        for (Integer key : knownPaths.keySet()) {
            avg_length += knownPaths.get(key).size();
            if (knownPaths.get(key).equals(path)) {
                ways.add(key);
            }
        }

        avg_length = avg_length / (double)knownPaths.size();
        TestExample2.setAveragePathLength(avg_length);
        Log.printLine("Average path length: " + avg_length);

        return ways;
    }
    
    public int actualPath() {
    	return mobileDeviceActualPaths;
	}

    public double calculateAccuracy() {

    	double numAttackerWasRight = 0;
    	int numInRange = 0;
        double accuracy;

        MobileDeviceInformation deviceInfo = this.deviceInformation;
        //deviceInfo.setPositionMap(cleanupPositionMap());
        deviceInfo.setPositionMap(knowledge.get(0).getPositionMap());
        if (deviceInfo.getPositionMap().isEmpty()) {
            return 1;
        }
        TreeMap<Long, Position> positionMap = deviceInfo.getPositionMap();


        boolean isInRange = false;
        long key = positionMap.firstKey();
        Position position = positionMap.get(key);

        computeControlledArea();
        double areaSum = 0.0;
        int n = 0;

        for (long i = startTime; i <= endTime ; i+=1) {

            if (i < key && !isInRange) {
                if (SCENARIO == 1) {
                	// Connected to a non-compromised fog node; Knows only compromised nodes 
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                }
                else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                	// Connected to a non-compromised fog node; Knows all nodes positions; Can disconnect
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                }
                else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                	// Connected to a non-compromised fog node; Knows all nodes positions; Cannot disconnect
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y) - controlledArea);
                }
            } else if (i < key && isInRange) {
            	// connected to fog node; getting area of fog node; 
                areaSum += getAreaOfFogDevice(position.getClosestFogDevice());
            } else if (i >= key && !positionMap.isEmpty()) {
                if (position.getState().equals("entering")) {
                    isInRange = true;
                    // connecting (entering) to fog node
                    areaSum += getAreaOfFogDevice(position.getClosestFogDevice());
                    numInRange++;
                } else {
                    isInRange = false;
                    if (SCENARIO == 1) {
                        // disconnecting (leaving) from fog node; Knows only compromised nodes
                        areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                    }
                    else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                    	// disconnecting (leaving) from fog node; Knows all nodes positions; Can disconnect
                        areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                    }
                    else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                    	// disconnecting (leaving) from fog node; Knows all nodes positions; Cannot disconnect
                        areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y) - controlledArea);
                    }
                }
                positionMap.pollFirstEntry();
                if (!positionMap.isEmpty()) {
                    key = positionMap.firstKey();
                    position = positionMap.get(key);
                }
            } else {
            	// position map empty; last time slot
                if (SCENARIO == 1) {
                	// Connected to a non-compromised fog node; Knows only compromised nodes
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                }
                else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                	// Connected to a non-compromised fog node; Knows all nodes positions; Can disconnect
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y));
                }
                else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                	// Connected to a non-compromised fog node; Knows all nodes positions; Cannot disconnect
                    areaSum += ((MaxAndMin.MAX_X * MaxAndMin.MAX_Y) - controlledArea);
                }
            }
            
            Position p = realPositionMap.getOrDefault(i, null);
            if (p != null && position.getClosestFogDevice() == p.getClosestFogDevice())
            	numAttackerWasRight++;
//            	Log.formatLine("%s = %s: YESSSSSSSS", position.getClosestFogDevice().getName(), p.getClosestFogDevice().getName());
//            else if (p != null && position.getClosestFogDevice() != p.getClosestFogDevice())
//            	Log.formatLine("%s = %s: NOOOOOOOOO", position.getClosestFogDevice().getName(), p.getClosestFogDevice().getName());
            
            n++;
        }

        accuracy = areaSum / (n * totalArea_num);
        areaHitDuration = Math.max(0.0d, Math.min(1.0d, numAttackerWasRight/numInRange));
        
        Log.printLine("Attacker was right: " + numAttackerWasRight + "; Num in range: " + numInRange + "; Area hit duration: " + areaHitDuration);
        
        return accuracy;
    }

    public double getAreaHitDuration() {
    	return areaHitDuration;
    }

    public TreeMap<Long, Position> cleanupPositionMap() {

        /* adjust position map (cut idle time) */
        //TreeMap<Long, Position> positionMap = knowledge.get(0).getPositionMap();
        TreeMap<Long, Position> positionMap = allPositions;
        TreeMap<Long, Position> newPositionMap = new TreeMap<>();

        startTime = positionMap.firstKey();

        Long time = positionMap.firstKey();
        Long previousTime = time;

        Position position = new Position(positionMap.firstEntry().getValue());
        Position previousPosition = new Position(positionMap.firstEntry().getValue());


        previousPosition.setState("entering");
        newPositionMap.put(previousTime, new Position(previousPosition));

        for (Long key : positionMap.keySet()) {

            if (key == positionMap.firstKey()) {
                continue;
            }

            time = key;
            position = new Position(positionMap.get(key));

            previousTime = time-1;
            previousPosition.setState("leaving");
            newPositionMap.put(previousTime, new Position(previousPosition));

            position.setState("entering");
            newPositionMap.put(time, new Position(position));

            previousPosition = position;
        }

        endTime = time + 20;
        allPositions = newPositionMap;


        ArrayList<Integer> controlledDeviceIds = new ArrayList<>();

        for (FogDevice fogDevice : controlledDeviceList) {
            controlledDeviceIds.add(fogDevice.getMyId());
        }

        newPositionMap = new TreeMap<>();

        for (Long key : allPositions.keySet()) {

            position = allPositions.get(key);
            if (controlledDeviceIds.contains(position.getClosestFogDevice().getMyId())) {
                newPositionMap.put(key, position);
            }
        }


        if (!newPositionMap.isEmpty()) {
            knowledge.get(0).setPositionMap(newPositionMap);
        } else {
            MobileDeviceInformation noInformation = new MobileDeviceInformation();
            noInformation.setPositionMap(newPositionMap);
            knowledge.put(0, noInformation);
        }

        cleanupPositionMap2();

        return newPositionMap;
    }
    
    private void cleanupPositionMap2()
    {
    	TreeMap<Long, Position> positionMap = realPositionMap;
        TreeMap<Long, Position> newPositionMap = new TreeMap<>();

        startTime = positionMap.firstKey();

        Long time = positionMap.firstKey();
        Long previousTime = time;

        Position position = new Position(positionMap.firstEntry().getValue());
        Position previousPosition = new Position(positionMap.firstEntry().getValue());

        previousPosition.setState("entering");
        newPositionMap.put(previousTime, new Position(previousPosition));

        for (Long key : positionMap.keySet()) {

            if (key == positionMap.firstKey()) {
                continue;
            }

            time = key;
            position = new Position(positionMap.get(key));

            previousTime = time-1;
            previousPosition.setState("leaving");
            newPositionMap.put(previousTime, new Position(previousPosition));

            position.setState("entering");
            newPositionMap.put(time, new Position(position));

            previousPosition = position;
        }

        realPositionMap = newPositionMap;
    }

    public List<FogDevice> getControlledDeviceList() {
        return controlledDeviceList;
    }

    public LinkedList<Integer> getPath() {
        return path;
    }


    public double getControlledArea() {
        return controlledArea;
    }

    public void printKnowledge() {

        Log.printLine("*************************************************");
        Log.printLine("****  Attacker controlled devices:  *****");
        controlledDeviceList.forEach(device -> Log.print(device.getName() + ","));
        Log.printLine();
        Log.printLine("*************************************************");
        Log.printLine("****  Attacker knows:  *****");

        knowledge.forEach((key, value) -> {
            value.printDeviceInformation();
            Log.printLine("*************************************************");
        });
        Log.printLine("*************************************************\n\n");

        Log.printLine("Path of Mobile Device (Attacker): \n");
        Log.print("Start");
        path.forEach(value -> Log.print(" -> " + value));
        Log.printLine();
        Log.printLine("\n\n*************************************************\n\n");
    }


    public MobileDeviceInformation getDeviceInformationById(int id) {
        MobileDeviceInformation devInfo = knowledge.getOrDefault(id, null);
        if (devInfo == null) {
            Log.printLine("No knowledge about device with id " + id);
        }
        return devInfo;
    }

    public long getHighestTime() {
        MobileDeviceInformation deviceInformation = knowledge.get(0);

        long max = deviceInformation.getPositionMap().lastKey();

        return max;
    }

    public List<MobileDevice> getMobileDeviceList() {
        return mobileDeviceList;
    }

    public void setMobileDeviceList(List<MobileDevice> mobileDeviceList) {
        this.mobileDeviceList = mobileDeviceList;
    }

    public void addMobileDevice(MobileDevice device) {
        mobileDeviceList.add(device);
    }


    public boolean isHasDeviceMap() {
        return hasDeviceMap;
    }

    public void setHasDeviceMap(boolean hasDeviceMap) {
        this.hasDeviceMap = hasDeviceMap;
    }


    public LinkedList<Integer> getAnyPathForMobileDevice(MobileDevice mobileDevice) {
    	int n = TestExample2.getRand().nextInt(50);
    	int x = (n % knownPaths.size()) + 1;
    	Log.printLine(mobileDevice.getName() + " path: " + x);
    	mobileDeviceActualPaths = x;
        return knownPaths.get(x);
    }

    public int getKnownPathsSize() {
        return knownPaths.size();
    }
    
    public TreeMap<Integer, LinkedList<Integer>> getKnownPaths() {
    	return knownPaths;
    }


    public void addPrecomputedPaths() {
        /* precomputed paths because of long execution time...
         * could also be automated */

        int way = 0;
        LinkedList<Integer> path = new LinkedList<>();

        way=1;
        Collections.addAll(path, 1, 11, 9, 3, 15, 0, 9, 13, 14, 13, 17, 9, 1, 4, 14, 4, 7, 15, 17, 0, 3, 13, 2, 3, 15, 5, 18, 11, 9, 16, 9, 11, 4);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=2;
        Collections.addAll(path, 2, 14, 3, 14, 9, 13, 4, 1, 7, 18, 6, 4, 10, 3, 2, 18, 0, 1, 9, 1, 13, 14, 13, 9, 7, 15, 5);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=3;
        Collections.addAll(path, 11, 3, 19, 9, 13, 0, 4, 14, 16, 4, 14, 9, 16, 7, 5, 11, 16, 9, 12, 14, 11, 14, 13, 9, 17, 3, 13, 12, 11, 10, 9, 14, 7, 13, 11, 3, 16, 3, 6, 9, 1, 9, 14);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=4;
        Collections.addAll(path, 9, 13, 1, 6, 3, 13, 6, 16, 2, 9, 4, 16, 19, 4, 7, 6, 9, 10, 1, 16, 15, 13, 9, 6, 17, 11, 7, 11, 10);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=5;
        Collections.addAll(path, 13, 11, 13, 14, 9, 7, 6, 15, 11, 12, 13, 4, 7, 17, 12, 9, 13, 1, 10, 17, 7, 19, 17, 0, 11, 9, 14, 11, 7, 1, 3, 1, 11, 12, 2);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=6;
        Collections.addAll(path, 6, 7, 3, 2, 4, 11, 10, 16, 12, 17, 11, 10, 17, 11, 1, 17, 16, 15, 7, 8, 13, 9, 16, 1, 14, 7, 14, 10, 6, 5, 10, 7, 18, 12, 9, 11, 0, 6, 13);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=7;
        Collections.addAll(path, 0, 3, 12, 13, 16, 7, 19, 17, 7, 19, 13, 17, 7, 11, 2, 4, 15, 13, 10, 11, 1, 13, 16, 1, 7, 3, 10, 14, 10);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=8;
        Collections.addAll(path, 14, 17, 18, 1, 4, 11, 2, 17, 4, 0, 1, 13, 3, 6, 1, 4, 11, 6, 13, 14, 4, 9, 18, 12, 14, 0, 1, 9, 6, 4);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=9;
        Collections.addAll(path, 7, 12, 3, 9, 6, 16, 3, 7, 3, 0, 14, 1, 6, 4, 2, 13, 11, 1, 17, 14, 13, 12, 11, 3, 14, 4, 11, 6);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=10;
        Collections.addAll(path, 12, 2, 9, 14, 9, 12, 16, 13, 8, 7, 17, 9, 16, 11, 4, 13, 9, 13, 18, 1, 12, 5, 0, 4, 17, 6, 14, 9, 1, 9, 18, 1, 17, 11, 16, 15, 4, 14, 6);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=11;
        Collections.addAll(path, 4, 3, 1, 11, 4, 10, 13, 6, 1, 3, 11, 6, 13, 19, 7, 13, 8, 13, 11, 7, 2, 13, 12, 7, 18, 7, 9, 1, 17, 11, 0, 2, 14, 18);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=12;
        Collections.addAll(path, 13, 17, 12, 6, 11, 13, 17, 10, 3, 1, 16, 1, 7, 14, 11, 14, 9, 3, 16, 9, 19, 6, 14, 9, 11, 0, 17, 11, 4, 1, 9, 8, 5, 15, 4, 9, 11, 16, 19, 11, 8);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=13;
        Collections.addAll(path, 16, 12, 19, 16, 14, 11, 18, 11, 1, 9, 11, 17, 11, 16, 3, 11, 12, 9, 11, 4, 6, 13, 9, 12, 7, 14, 17, 11, 2, 18, 11);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=14;
        Collections.addAll(path, 17, 2, 6, 2, 7, 6, 2, 11, 4, 11, 9, 12, 18, 6, 15, 18, 11, 2, 7, 1, 16, 6, 14, 9, 1, 15, 16, 10, 13, 3, 11, 12, 14, 1, 3, 15, 6, 8, 11);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=15;
        Collections.addAll(path, 6, 0, 13, 2, 10, 9, 1, 14, 3, 5, 6, 10, 16, 1, 10, 16, 4, 7, 2, 6, 14, 11, 9, 4, 1, 13, 1, 17);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=16;
        Collections.addAll(path, 1, 8, 15, 9, 11, 0, 15, 2, 6, 13, 0, 14, 0, 6, 17, 0, 14, 13, 1, 13, 12, 13, 5, 8, 6, 11, 9, 2, 3, 16, 0, 2, 4, 16, 13, 2, 6, 7, 10, 14, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=17;
        Collections.addAll(path, 1, 4, 16, 13, 7, 6, 17, 14, 7, 3, 10, 12, 2, 7, 2, 9, 11, 6, 7, 13, 10, 14, 18, 6, 3, 11, 17);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=18;
        Collections.addAll(path, 1, 18, 1, 16, 14, 4, 1, 11, 10, 12, 4, 6, 15, 10, 2, 1, 16, 9, 12, 3, 7, 13, 17, 7, 9, 6, 11, 14, 6, 0, 14, 9, 12, 14, 18);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=19;
        Collections.addAll(path, 11, 10, 4, 6, 1, 13, 7, 6, 7, 16, 9, 14, 3, 11, 1, 14, 4, 13, 1, 7, 3, 9, 14, 11, 6, 9, 7, 6, 10, 11);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=20;
        Collections.addAll(path, 12, 5, 9, 1, 14, 6, 10, 14, 17, 11, 6, 17, 13, 9, 11, 19, 3, 12, 1, 18, 1, 16, 1, 3, 11, 6, 2, 10, 3);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=21;
        Collections.addAll(path, 13, 7, 3, 13, 10, 18, 17, 15, 13, 18, 12, 16, 12, 13, 16, 3, 12, 6, 2, 15, 3, 8, 15, 14, 6, 18, 1, 16, 14, 4);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=22;
        Collections.addAll(path, 3, 14, 13, 0, 11, 4, 1, 4, 13, 15, 13, 7, 14, 18, 6, 2, 11, 9, 13, 12, 1, 14, 9, 6, 17, 1, 5, 14, 3, 9, 15, 4, 13, 15);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=23;
        Collections.addAll(path, 18, 13, 14, 1, 6, 14, 11, 3, 2, 4, 11, 17, 1, 15, 6, 7, 0, 13, 14, 10, 14, 1, 10, 6, 3, 7, 0, 12);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=24;
        Collections.addAll(path, 14, 19, 6, 4, 9, 13, 11, 6, 11, 16, 6, 2, 18, 0, 9, 7, 9, 2, 13, 9, 3, 8, 15, 11, 1, 6, 7, 16, 6, 0, 16, 15, 14, 15, 11, 1, 15, 19, 18, 17, 14);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=25;
        Collections.addAll(path, 7, 5, 15, 6, 7, 2, 17, 12, 13, 11, 6, 18, 11, 17, 16, 14, 1, 3, 0, 11, 13, 4, 17, 6, 16, 4, 11, 7, 11, 6, 9, 17, 6, 15, 9);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=26;
        Collections.addAll(path, 5, 14, 0, 8, 13, 14, 2, 7, 14, 11, 2, 18, 9, 14, 6, 14, 13, 16, 9, 13, 1, 9, 18, 4, 11, 9, 11, 4, 17, 6, 15, 10, 7, 18, 15, 7, 13);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=27;
        Collections.addAll(path, 2, 6, 9, 14, 16, 9, 7, 14, 10, 14, 6, 7, 14, 11, 5, 2, 18, 12, 10, 3, 4, 9, 10, 14, 1, 14, 9, 11, 16, 0, 10, 4, 9, 0, 10, 14, 13, 2);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=28;
        Collections.addAll(path, 13, 16, 14, 9, 13, 8, 12, 13, 11, 14, 13, 11, 4, 6, 16, 17, 9, 14, 6, 4, 9, 14, 6, 1, 11, 9, 17, 2);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=29;
        Collections.addAll(path, 16, 7, 17, 4, 7, 0, 13, 16, 0, 17, 11, 7, 9, 2, 14, 9, 6, 11, 14, 16, 7, 4, 17, 3, 9, 19, 13, 1, 17, 11, 7, 13, 1, 13, 3, 0);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=30;
        Collections.addAll(path, 17, 14, 19, 6, 13, 11, 3, 11, 17, 6, 11, 4, 11, 15, 5, 7, 12, 1, 9, 12, 11, 7, 13, 3);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=31;
        Collections.addAll(path, 6, 12, 0, 12, 1, 13, 12, 13, 16, 4, 2, 9, 1, 13, 1, 2, 1, 16, 3, 10, 15, 9, 14, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=32;
        Collections.addAll(path, 7, 16, 9, 14, 4, 16, 14, 11, 12, 17, 9, 14, 7, 1, 4, 2, 10, 14, 9, 16, 6, 18, 6, 9, 3, 1, 12, 18, 6, 1, 13);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=33;
        Collections.addAll(path, 4, 17, 1, 14, 9, 15, 0, 1, 6, 9, 5, 15, 14, 7, 4, 6, 14, 6, 12, 16, 11, 14, 1, 12, 16, 11, 8, 9, 4, 9, 7, 4, 3, 19, 6, 14, 11, 7);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=34;
        Collections.addAll(path, 10, 6, 0, 7, 6, 11, 13, 14, 17, 3, 16, 1, 6, 18, 2, 3, 4, 10, 0, 17, 4, 14, 12, 13, 12, 7);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=35;
        Collections.addAll(path, 11, 4, 9, 10, 6, 13, 17, 13, 3, 14, 15, 6, 1, 7, 18, 7, 4, 10, 1, 7, 17, 18, 14, 12, 9, 2, 8, 4, 6, 10, 6, 3);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=36;
        Collections.addAll(path, 9, 1, 4, 16, 9, 3, 14, 0, 6, 12, 4, 9, 15, 8, 11, 9, 6, 12, 2, 7, 10, 11, 9, 8, 3, 11, 13, 17, 16, 7, 12, 14, 1, 13, 17, 3, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=37;
        Collections.addAll(path, 6, 17, 13, 1, 14, 1, 0, 9, 4, 10, 7, 14, 10, 17, 3, 19, 17, 12, 15, 19, 7, 6, 11, 13, 6, 14);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=38;
        Collections.addAll(path, 6, 11, 0, 11, 17, 13, 10, 6, 0, 16, 18, 14, 11, 14, 4, 0, 12, 13, 6, 17, 10, 11, 2, 6, 19);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=39;
        Collections.addAll(path, 3, 4, 6, 12, 14, 1, 15, 11, 4, 6, 11, 4, 9, 13, 3, 6, 15, 2, 1, 11, 15, 19, 6, 13, 1, 13, 12, 9, 5, 2, 11, 19);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=40;
        Collections.addAll(path, 14, 1, 9, 13, 6, 9, 2, 1, 9, 10, 17, 12, 17, 7, 3, 11, 18, 6, 15, 0, 3, 9, 11, 14, 6, 7, 6, 4, 14, 11, 7, 12);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=41;
        Collections.addAll(path, 11, 17, 0, 16, 7, 10, 14, 7, 1, 14, 9, 16, 19, 0, 5, 13, 6, 13, 0, 13, 6, 11, 14, 13, 9, 13, 7, 14, 7, 1, 12, 3, 10, 8);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=42;
        Collections.addAll(path, 11, 3, 8, 3, 7, 18, 7, 4, 10, 4, 11, 10, 14, 13, 1, 11, 9, 16, 4, 17, 9, 17, 12, 9, 11, 15, 6, 2, 0, 9, 0, 2, 17);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=43;
        Collections.addAll(path, 19, 4, 13, 10, 13, 3, 6, 13, 6, 17, 16, 15, 8, 19, 15, 1, 6, 4, 1, 3, 1, 15, 6);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=44;
        Collections.addAll(path, 17, 11, 1, 5, 4, 7, 13, 16, 14, 2, 14, 11, 3, 1, 13, 14, 9, 4, 2, 16, 9, 11, 4, 0, 13, 2, 7, 14, 11, 17, 7, 16, 13, 3, 13, 16);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=45;
        Collections.addAll(path, 9, 17, 11, 10, 16, 3, 14, 13, 14, 11, 10, 8, 11, 14, 13, 9, 2, 12, 19, 9, 16, 13, 6, 14, 1, 14, 13, 7, 16, 7, 1);
        knownPaths.put(way, path);


        path = new LinkedList<>();
        way=46;
        Collections.addAll(path, 14, 3, 14, 16, 3, 13, 3, 11, 1, 9, 17, 3, 9, 11, 5, 4, 7, 3, 14, 18, 9, 17, 14, 17, 11, 1, 3, 2, 6, 14, 3, 4, 3, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=47;
        Collections.addAll(path, 14, 10, 9, 7, 10, 2, 16, 9, 2, 6, 12, 1, 3, 11, 13, 14, 13, 14, 0, 1, 17, 1, 14, 2, 8, 13, 6, 14, 8, 15, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=48;
        Collections.addAll(path, 15, 11, 6, 4, 11, 9, 7, 1, 14, 1, 16, 11, 4, 11, 12, 3, 11, 9, 6, 11, 18, 17, 14, 18, 14, 17, 4, 11, 14, 13, 18, 7, 1);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=49;
        Collections.addAll(path, 14, 12, 9, 13, 11, 3, 14, 3, 13, 2, 11, 13, 1, 10, 7, 3, 4, 13, 9, 4, 14, 4, 9, 11, 16, 12, 9, 14, 7, 1, 9, 10);
        knownPaths.put(way, path);

        path = new LinkedList<>();
        way=50;
        Collections.addAll(path, 2, 10, 12, 2, 6, 13, 0, 6, 12, 11, 9, 13, 16, 6, 11, 13, 14, 7, 12, 16, 19, 10, 1, 9, 13, 7, 4, 11, 1, 5, 9);
        knownPaths.put(way, path);



    }
}