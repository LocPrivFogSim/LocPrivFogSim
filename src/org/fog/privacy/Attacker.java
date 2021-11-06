package org.fog.privacy;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.localization.Path;
import org.fog.localization.SimField;
import org.fog.vmmobile.TestExample4;
import org.fog.vmmobile.constants.MaxAndMin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Attacker class
 * - observes the (path of the) mobile device(s)
 * - computes the controlled area
 * - evaluates the possible matches with the known paths and the possible accuracy of a position mapping
 * - holds a list of 50 precomputed (due to execution time) paths
 */
public class Attacker {

    private static final int SCENARIO = TestExample4.getSCENARIO();
    private static final int MOBILE_CAN_BE_TURNED_OFF = TestExample4.getMobileCanBeTurnedOff();
    private List<MobileDevice> mobileDeviceList = new ArrayList<>();
    private String name;
    private List<FogDevice> controlledDeviceList;
    private ArrayList<FogDevice> allFogDevicesList;
    private HashMap<LinkedList<Integer>, Integer> tracesForAllPaths;


    private HashMap<Integer, MobileDeviceInformation> knowledge = new HashMap<>();
    private double controlledArea = -1.0;
    private double totalArea_num = 0.0;
    private HashMap<Integer, Double> areaOfFogNode = new HashMap<>();

    ArrayList<Path> allPaths;
    private HashMap<Integer, LinkedList<Integer>> trackedTracesForPathIds = new HashMap<>();
    private HashMap<Integer, ArrayList<Position>> trackedPositionsForPathIds = new HashMap<>();

    // Scenario 2
    private DeviceMap deviceMap = new DeviceMap();
    private boolean hasDeviceMap = false;

    private HashMap<Integer, Integer> endTimesForPaths = new HashMap<>();

    public Attacker(String name, List<FogDevice> controlledDevices, ArrayList<FogDevice> allFogDevicesList, ArrayList<Path> allPaths) {
        this.name = name;
        this.controlledDeviceList = controlledDevices;
        this.allFogDevicesList = allFogDevicesList;

        this.allPaths = allPaths;
        tracesForAllPaths = this.getAttackersTracesForAllPaths(allPaths, controlledDeviceList);

        Log.printLine("Attacker " + name + " created");
        observe();
        if (SCENARIO == 1) {
            hasDeviceMap = false;
        }
        if (SCENARIO == 2) {
            hasDeviceMap = true;
            deviceMap = TestExample4.getDeviceMap();
        }
        controlledArea = computeControlledArea();
    }

    /* Observer */
    public void observe() {
        for (FogDevice device : TestExample4.getRelevantCompromisedDevices()) {
            device.addObserver(this);
        }
    }

    public void update(FogDevice source, MobileDevice mobileDevice, int timestamp,int eventType, String event) {

        LinkedList<Integer> trace = trackedTracesForPathIds.get(mobileDevice.getPath().getPathId());
        ArrayList<Position> trackedPositions = trackedPositionsForPathIds.get(mobileDevice.getPath().getPathId());


        endTimesForPaths.put(mobileDevice.getPath().getPathId(), mobileDevice.getPath().getPositions().get(mobileDevice.getPath().getPositions().size() - 1).getTimestamp());

        Log.formatLine("Attacker was notified: MobileDevice '%s' was %sed to '%s' due to a %s event.", mobileDevice.getName(),
                event, source.getName(), EventType.eventTypeToString(eventType));

        MobileDeviceInformation deviceInformation = knowledge.getOrDefault(mobileDevice.getMyId(), null);
        if (deviceInformation == null) {
            deviceInformation = new MobileDeviceInformation(mobileDevice);
        }

        Log.printLine("\n*************************************************");

        Position position = new Position();
        //position.setTimestamp(mobileDevice.getPosition().getTimestamp());
        position.setTimestamp(timestamp);
        position.setClosestFogDevice(source);
        int id = 0;

        FogDevice actualFogNode = mobileDevice.getSourceServerCloudlet();
        if (actualFogNode == null) { // in handoff
            actualFogNode = deviceMap.findClosestFogDevice(TestExample4.getRelevantFogDevicesList(), mobileDevice);
        }

        long time = position.getTimestamp();

        switch (event) {
            case "add":

                //System.out.println("---------added at ts: "+timestamp+ "   fogNodeId: "+source.getMyId());

                position.setState("entering");


                if (controlledDeviceList.contains(position.getClosestFogDevice())) {

                    trackedPositions.add(position);
                    id = position.getClosestFogDevice().getMyId();

                    deviceInformation.addPosition(time, position);
                    knowledge.put(mobileDevice.getMyId(), deviceInformation);

                    if(!mobileDeviceList.contains(mobileDevice))mobileDeviceList.add(mobileDevice);  //should not happen but sometimes events are send twice

                    // && !trace.contains(id)
                    if (trace.isEmpty() || trace.getLast() == null || trace.getLast() != id) {
                        trace.add(id);
                    }

                    Log.printLine("Attacker got information about entering device " + deviceInformation.getDeviceName() + " on Node " + id);
                } else {
                    if (trace.isEmpty() || trace.getLast() != null) {    // null means 'x' or 'not connected'
                        trace.add(null);
                    }
                }

                break;

            case "remove":

                //System.out.println("---------removed at ts: "+ timestamp+"      fogNode: "+source.getMyId());

                position.setState("leaving");


                if (controlledDeviceList.contains(position.getClosestFogDevice())) {
                    trackedPositions.add(position);
                    id = position.getClosestFogDevice().getMyId();
                    deviceInformation.addPosition(time, position);
                    knowledge.put(mobileDevice.getMyId(), deviceInformation);
                    mobileDeviceList.remove(mobileDevice);
                    if (trace.isEmpty() || trace.getLast() == null || trace.getLast() != id) {
                        trace.add(id);
                    }
                    Log.printLine("Attacker got information about leaving device " + deviceInformation.getDeviceName() + " on Node " + id);
                } else {
                    if (trace.isEmpty() || trace.getLast() != null) {    // null means 'x' or 'not connected'
                        trace.add(null);
                    }
                }
                break;

            default:
                Log.printLine("Attacker update error: unknown operation " + event + " on Node " + id);
        }

    }


    public double getAreaOfFogDevice(FogDevice fogDevice) {

        if (controlledArea == 0) {
            computeControlledArea();
        }

        return areaOfFogNode.get(fogDevice.getMyId());
    }


    public double computeControlledArea() {
        if (controlledArea < 0) {
            calcControlledArea();
        }
        return controlledArea;
    }

    public double calcControlledArea() {
        if (controlledDeviceList.size() == 0) {
            return 0;
        }

        double area = 0.0;

        SimField field = TestExample4.getSimfield();
        List<Coordinate> corners = field.sortCornersClockwise(field.getCorners());
        double x = Coordinate.calcDistance(corners.get(0), corners.get(1));
        double y = Coordinate.calcDistance(corners.get(1), corners.get(2));
        totalArea_num = x * y;

        double bearingX = Coordinate.calcBearingAngle(corners.get(0), corners.get(1), false);
        double bearingY = Coordinate.calcBearingAngle(corners.get(1), corners.get(2), false);

        Coordinate corner = corners.get(0);

        int[] fogNode_coverage = new int[allFogDevicesList.size()];

        long start = System.currentTimeMillis();

        controlledDeviceList.sort(new Comparator<FogDevice>() {
            @Override
            public int compare(FogDevice o1, FogDevice o2) {
                double d1 = Coordinate.calcDistance(o1.getPosition().getCoordinate(), corner);
                double d2 = Coordinate.calcDistance(o2.getPosition().getCoordinate(), corner);
                if (d1 < d2) return -1;
                if (d1 > d2) return 1;
                else return 0;
            }
        });
        allFogDevicesList.sort((o1, o2) -> {
            double d1 = Coordinate.calcDistance(o1.getPosition().getCoordinate(), corner);
            double d2 = Coordinate.calcDistance(o2.getPosition().getCoordinate(), corner);
            if (d1 < d2) return -1;
            if (d1 > d2) return 1;
            else return 0;
        });

        ArrayList<Coordinate> gridPoints = new ArrayList<>();
        for (double i = 0.0; i <= x; i += 100) {      // x
            for (double j = 0.0; j <= y; j += 100) {  // y
                Coordinate moveDirectionX = Coordinate.findCoordinateForBearingAndDistance(corner, bearingX, i);
                Coordinate moveDirectionY = Coordinate.findCoordinateForBearingAndDistance(moveDirectionX, bearingY, j);
                gridPoints.add(moveDirectionY);
            }
        }
        gridPoints.sort(new Comparator<Coordinate>() {
            @Override
            public int compare(Coordinate o1, Coordinate o2) {
                double d1 = Coordinate.calcDistance(o1, corner);
                double d2 = Coordinate.calcDistance(o2, corner);
                if (d1 < d2) return -1;
                if (d1 > d2) return 1;
                else return 0;
            }
        });

        long afterSort = System.currentTimeMillis();

        System.out.println("afterSort: " + (afterSort - start) / 1000 + " sekunden" + "        gridpoints: " + gridPoints.size());

        int leftIndex = 0, rightIndex = 1;

        for (int i = 0; i < gridPoints.size(); i++) {

            FogDevice closestDevice = null;

            double currentDistance = Coordinate.calcDistance(gridPoints.get(i), corner);

            if (SCENARIO == 1) {

                while (Coordinate.calcDistance(corner, controlledDeviceList.get(rightIndex).getPosition().getCoordinate()) - currentDistance < 750) {
                    if (rightIndex + 1 == controlledDeviceList.size()) break;
                    rightIndex++;
                }

                while (Coordinate.calcDistance(corner, controlledDeviceList.get(leftIndex).getPosition().getCoordinate()) - currentDistance < -750) {
                    if (leftIndex + 1 == rightIndex) break;
                    leftIndex++;
                }

                List<FogDevice> toCompare = controlledDeviceList.subList(leftIndex, rightIndex);

                double currentMin = Double.MAX_VALUE;

                for (FogDevice fogDevice : toCompare) {

                    double distance = Coordinate.calcDistance(fogDevice.getPosition().getCoordinate(), gridPoints.get(i));

                    if (distance < currentMin) {
                        currentMin = distance;
                        closestDevice = fogDevice;
                    }
                }
                if (currentMin < MaxAndMin.CLOUDLET_COVERAGE && closestDevice != null) {
                    fogNode_coverage[closestDevice.getMyId()]++;
                }
            } else if (SCENARIO == 2) {
                while (Coordinate.calcDistance(corner, allFogDevicesList.get(rightIndex).getPosition().getCoordinate()) - currentDistance < 700) {
                    if (rightIndex + 1 == allFogDevicesList.size()) break;
                    rightIndex++;
                }

                while (Coordinate.calcDistance(corner, allFogDevicesList.get(leftIndex).getPosition().getCoordinate()) - currentDistance < -700) {
                    if (leftIndex + 1 == rightIndex) break;
                    leftIndex++;
                }

                List<FogDevice> toCompare = allFogDevicesList.subList(leftIndex, rightIndex);

                double currentMin = Double.MAX_VALUE;

                for (FogDevice fogDevice : toCompare) {

                    double distance = Coordinate.calcDistance(fogDevice.getPosition().getCoordinate(), gridPoints.get(i));

                    if (distance < currentMin) {
                        currentMin = distance;
                        closestDevice = fogDevice;
                    }
                }
                if (currentMin < MaxAndMin.CLOUDLET_COVERAGE && closestDevice != null) {
                    fogNode_coverage[closestDevice.getMyId()]++;
                }
            }
        }
        long afterLoop = System.currentTimeMillis();
       System.out.println("afterLoop: " + (afterLoop - afterSort) / 1000 + " sekunden");

        for (FogDevice fogDevice : controlledDeviceList) {
            double tmpArea = fogNode_coverage[fogDevice.getMyId()] * 100 * 100;
            area += tmpArea;
        }

        for (FogDevice fogDevice : allFogDevicesList) {
            double tmpArea = fogNode_coverage[fogDevice.getMyId()] * 100 * 100;
            areaOfFogNode.put(fogDevice.getMyId(), tmpArea);
        }

        // WARNING(markus): Returning the value to use it and setting the value in a private field is not a good practice
        // and leads to errors. I don't know what the reason for this was but it should probably be cleaned up i.e.,
        // either return the value and dont set or make the method void and set the value at the end.
        controlledArea = area;

        //      System.out.println("area: " + 100 * area / (x * y) + " %");
        return area;
    }


    public ArrayList<Double> calcSizeOfUncertaintyRegion() {

        ArrayList<Double> sizeOfUncertaintyList = new ArrayList<>();

       // System.out.println("sizeOfUn   size hier: "+trackedPositionsForPathIds);

        for(Integer key: trackedPositionsForPathIds.keySet()) {

            int endTime = endTimesForPaths.get(key);

            ArrayList<Position> positionList = new ArrayList<>();
            for (Position p : trackedPositionsForPathIds.get(key)) {
                positionList.add(p);
            }

            double totalArea = 0d;

            if (positionList.isEmpty() || positionList.get(0) == null) {
                if(SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0){

                    sizeOfUncertaintyList.add((totalArea_num-controlledArea)/totalArea_num);}
                else{
                    sizeOfUncertaintyList.add(1d);
                }
               continue;
            }

            Position previousPos = positionList.get(0);

            //add first bit (first point of path - first trackedPosition
            Position firstPos = positionList.get(0);
            if (firstPos.getState().equals("leaving")) {

                totalArea += getAreaOfFogDevice(firstPos.getClosestFogDevice()) * (firstPos.getTimestamp());
            } else {
                if (SCENARIO == 1) {
                    // Connected to a non-compromised fog node; Knows only compromised nodes
                    totalArea += totalArea_num * (firstPos.getTimestamp());
                } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                    // Connected to a non-compromised fog node; Knows all nodes positions; Can disconnect
                    totalArea += totalArea_num * (firstPos.getTimestamp());
                } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                    // Connected to a non-compromised fog node; Knows all nodes positions; Cannot disconnect
                    totalArea += (totalArea_num - controlledArea) * (firstPos.getTimestamp());
                }
            }

            for (int i = 1; i < positionList.size(); i++) {
                Position position = positionList.get(i);
                FogDevice closest = position.getClosestFogDevice();

                if (position.getState().equals("leaving")) {
                    totalArea += getAreaOfFogDevice(closest) * (position.getTimestamp() - previousPos.getTimestamp());
                } else {
                    if (SCENARIO == 1) {
                        // Connected to a non-compromised fog node; Knows only compromised nodes
                        totalArea += totalArea_num * (position.getTimestamp() - previousPos.getTimestamp());
                    } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                        // Connected to a non-compromised fog node; Knows all nodes positions; Can disconnect
                        totalArea += totalArea_num * (position.getTimestamp() - previousPos.getTimestamp());
                    } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                        // Connected to a non-compromised fog node; Knows all nodes positions; Cannot disconnect
                        totalArea += (totalArea_num - controlledArea) * (position.getTimestamp() - previousPos.getTimestamp());
                    }
                }
                previousPos = position;
            }

            //add the final bit (lastTracked Position - final Point of path):
            Position finalPos = positionList.get(positionList.size() - 1);
            if (finalPos.getState().equals("entering")) {
                totalArea += getAreaOfFogDevice(finalPos.getClosestFogDevice()) * (endTime - finalPos.getTimestamp());
            } else {
                if (SCENARIO == 1) {
                    // Connected to a non-compromised fog node; Knows only compromised nodes
                    totalArea += totalArea_num * (endTime - finalPos.getTimestamp());
                } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 1) {
                    // Connected to a non-compromised fog node; Knows all nodes positions; Can disconnect
                    totalArea += totalArea_num * (endTime - finalPos.getTimestamp());
                } else if (SCENARIO == 2 && MOBILE_CAN_BE_TURNED_OFF == 0) {
                    // Connected to a non-compromised fog node; Knows all nodes positions; Cannot disconnect
                    totalArea +=  (totalArea_num - controlledArea) * (endTime - finalPos.getTimestamp());
                }
            }

           // System.out.println("totalArea:  " + totalArea + "   time: " + (endTime) + "     totalAreaNum: " + totalArea_num + "    avg Area: " + (totalArea / endTime) + "   pathId: "+key);

            double accuracy = totalArea / ((endTime) * totalArea_num);



            sizeOfUncertaintyList.add(accuracy);
        }

        return sizeOfUncertaintyList;
    }


    /**
     *
     * returns the traces for each Path
     *
     */
    private HashMap<LinkedList<Integer>, Integer> getAttackersTracesForAllPaths(ArrayList<Path> paths, List<FogDevice> fogNodes) {

      //  System.out.println("begin setting Traces!!!");
        long start = System.currentTimeMillis();

        List<Integer> controlledIDs = fogNodes.stream().map(fogDevice -> fogDevice.getMyId()).collect(Collectors.toList());

        HashMap<LinkedList<Integer>, Integer> frequencyOfTraces = new HashMap<>(); // path as linkedList + frequency

        int x = 0, z = paths.size();

        for (Path path : paths) {
            LinkedList<Integer> trace = new LinkedList<>();

            for(int i : path.getTrace()){
                if(!controlledIDs.contains(i))continue;
                if(trace.size() == 0){
                    trace.add(i);
                    continue;
                }
                if(trace.getLast()!= i){
                    trace.add(i);
                }
            }

            if(frequencyOfTraces.keySet().contains(trace)){
                int prev = frequencyOfTraces.get(trace);
                frequencyOfTraces.put(trace, prev + path.getNrOfDuplicates() + 1);
            }else{
                frequencyOfTraces.put(trace, 1);
            }
        }

        //    System.out.println("ende trace Setzen: " + ((System.currentTimeMillis() - start) / 1000));


        return frequencyOfTraces;
    }

    public ArrayList<Integer> calcTraceCompVal() {
        ArrayList<Integer> allTraceVals = new ArrayList<>();

        if(MOBILE_CAN_BE_TURNED_OFF == 0){
            for(Integer key: trackedTracesForPathIds.keySet()) {
                LinkedList<Integer> trace = trackedTracesForPathIds.get(key);

                //System.out.println("trace for "+ key +" :"+trace);
                boolean traceInKnownTraces = tracesForAllPaths.keySet().contains(trace);
                if (traceInKnownTraces) {
                    int tcv = tracesForAllPaths.get(trace);
                    allTraceVals.add(tcv);
                }else{
                    allTraceVals.add(1);
                }
            }
        }
        else if(MOBILE_CAN_BE_TURNED_OFF ==1){
            System.out.println("ä################# actual traces: "+ trackedTracesForPathIds);

            for (Integer key : trackedTracesForPathIds.keySet()){

                LinkedList<Integer> trackedTrace = trackedTracesForPathIds.get(key);

                int count = 0;
                for(Path path: allPaths){

                   LinkedList<Integer> actualTrace = path.getTrace();

                    int i = 0;
                    int j = 0;

                    while(i < actualTrace.size() && j <trackedTrace.size()){

                        int toCheck = actualTrace.get(i);
                        int toCompareTo = trackedTrace.get(j);

                        if (toCheck == toCompareTo ){
                            j++;
                        }
                       i++;
                    }

                    if (j == trackedTrace.size()){
                        count += path.getNrOfDuplicates()+1;
                    }

                }
                allTraceVals.add(count);
            }
        }
        return allTraceVals;
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
        Log.printLine();
        Log.printLine("\n\n*************************************************\n\n");
    }



    public void initMaps(List<MobileDevice> mobileDeviceList) {
       for(MobileDevice m: mobileDeviceList){
            trackedPositionsForPathIds.put(m.getPath().getPathId(), new ArrayList<>());
            trackedTracesForPathIds.put(m.getPath().getPathId(), new LinkedList<>());
            int endtime = m.getPath().getPositions().get(m.getPath().getPositions().size()-1).getTimestamp();
            endTimesForPaths.put(m.getPath().getPathId(), endtime);
        }
    }

    public void setHasDeviceMap(boolean hasDeviceMap) {
        this.hasDeviceMap = hasDeviceMap;
    }
}
