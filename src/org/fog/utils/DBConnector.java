package org.fog.utils;

import org.fog.localization.Coordinate;
import org.fog.localization.Path;
import org.fog.privacy.Position;

import java.sql.*;
import java.util.*;


public class DBConnector {


    String dbURL = "jdbc:sqlite:geoLifePaths.db";


    private Connection connect() {
            try {
                Class.forName("org.sqlite.JDBC");
                return DriverManager.getConnection(dbURL);
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();

        }
        return null;
    }

    public int getMaxPathCount() {
        int count = -1;
        try {
            Connection conn = connect();
            String query = "SELECT COUNT(*) FROM paths";

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            count = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    public Path getPathById(int pathId) {
        Path path = null;
        try {
            Connection conn = connect();
            String query = "SELECT * FROM paths WHERE path_id=" + pathId;

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            int id = resultSet.getInt("path_id");
            String pathString = resultSet.getString("path");
            int duplicates = resultSet.getInt("duplicates");
            double distance = resultSet.getDouble("distance");
            double minLat = resultSet.getDouble("min_lat");
            double maxLat = resultSet.getDouble("max_lat");
            double minLon = resultSet.getDouble("min_lon");
            double maxLon = resultSet.getDouble("max_lon");

            String traceString = resultSet.getString("fog_nodes_trace");
            String[] traceSplit = traceString.split(",");
            LinkedList<Integer> trace = new LinkedList<>();
            for (int i = 0; i < traceSplit.length; i++) {
                int idNum = Integer.parseInt(traceSplit[i]);
                trace.add(idNum);
            }

            ArrayList<Position> positions = getPositionsForPathString(pathString);

            path = new Path(id, positions, duplicates,  minLat, maxLat, minLon, maxLon, distance,trace);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //System.out.println(path);

        return path;
    }

    public ArrayList<Path> getAllPaths() {
        ArrayList<Path> paths = new ArrayList<>();
        try {
            Connection conn = connect();
            String query = "SELECT * FROM paths";

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("path_id");
                String pathString = resultSet.getString("path");
                int duplicates = resultSet.getInt("duplicates");
                double distance = resultSet.getDouble("distance");
                double minLat1 = resultSet.getDouble("min_lat");
                double maxLat1 = resultSet.getDouble("max_lat");
                double minLon1 = resultSet.getDouble("min_lon");
                double maxLon1 = resultSet.getDouble("max_lon");

                String traceString = resultSet.getString("fog_nodes_trace");
                LinkedList<Integer> trace = new LinkedList<>();
                if (traceString != null) {
                    String[] traceSplit = traceString.split(",");

                    for (int i = 0; i < traceSplit.length; i++) {
                        int idNum = Integer.parseInt(traceSplit[i]);
                        trace.add(idNum);
                    }
                }

                ArrayList<Position> positions = getPositionsForPathString(pathString);

                Path path = new Path(id, positions, duplicates,  minLat1, maxLat1, minLon1, distance, maxLon1, trace);
                paths.add(path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return paths;
    }

    public ArrayList<Path> getPathsWithingBorders(double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<Path> paths = new ArrayList<>();
        try {
            Connection conn = connect();
            String query = "SELECT * FROM paths WHERE min_lat >=" + minLat
                    + " AND max_lat <=" + maxLat
                    + " AND min_lon >= " + minLon
                    + " AND max_lon <=" + maxLon;

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("path_id");
                String pathString = resultSet.getString("path");
                int duplicates = resultSet.getInt("duplicates");
                double distance = resultSet.getDouble("distance");
                double minLat1 = resultSet.getDouble("min_lat");
                double maxLat1 = resultSet.getDouble("max_lat");
                double minLon1 = resultSet.getDouble("min_lon");
                double maxLon1 = resultSet.getDouble("max_lon");

                LinkedList<Integer> trace = new LinkedList<>();
                String traceString = resultSet.getString("fog_nodes_trace");
                if(traceString != null){
                    String[] traceSplit = traceString.split(",");

                    for (int i = 0; i < traceSplit.length; i++) {
                        int idNum = Integer.parseInt(traceSplit[i]);
                        trace.add(idNum);
                    }
                }

                ArrayList<Position> positions = getPositionsForPathString(pathString);

                Path path = new Path(id, positions, duplicates,  minLat1, maxLat1, minLon1, maxLon1, distance, trace);
                paths.add(path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return paths;
    }

    public ArrayList<Path> getPathsWithingWhereTraceIsNull(double minLat, double maxLat, double minLon, double maxLon) {
        ArrayList<Path> paths = new ArrayList<>();
        try {
            Connection conn = connect();
            String query = "SELECT * FROM paths WHERE min_lat >=" + minLat
                    + " AND max_lat <=" + maxLat
                    + " AND min_lon >= " + minLon
                    + " AND max_lon <=" + maxLon
                    + " AND fog_nodes_trace IS NULL";

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("path_id");
                String pathString = resultSet.getString("path");
                int duplicates = resultSet.getInt("duplicates");
                double distance = resultSet.getDouble("distance");
                double minLat1 = resultSet.getDouble("min_lat");
                double maxLat1 = resultSet.getDouble("max_lat");
                double minLon1 = resultSet.getDouble("min_lon");
                double maxLon1 = resultSet.getDouble("max_lon");

                LinkedList<Integer> trace = new LinkedList<>();
                String traceString = resultSet.getString("fog_nodes_trace");
                if(traceString != null){
                    String[] traceSplit = traceString.split(",");

                    for (int i = 0; i < traceSplit.length; i++) {
                        int idNum = Integer.parseInt(traceSplit[i]);
                        trace.add(idNum);
                    }
                }

                ArrayList<Position> positions = getPositionsForPathString(pathString);

                Path path = new Path(id, positions, duplicates,  minLat1, maxLat1, minLon1, maxLon1, distance, trace);
                paths.add(path);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return paths;
    }

    private ArrayList<Position> getPositionsForPathString(String path) {

        ArrayList<Position> positions = new ArrayList<>();

        path = path.substring(0, path.length() - 2);  //remove trailing Seperator

        String[] subStrings = path.split("[||]{1}");

        for (int i = 0; i < subStrings.length; i++) {
            if (subStrings[i].length() == 0) continue;

            String[] innerSubStrings = subStrings[i].split(",");
            double lat = Double.parseDouble(innerSubStrings[0]);
            double lon = Double.parseDouble(innerSubStrings[1]);
            int timeStamp = Integer.parseInt(innerSubStrings[2]);
            Coordinate coord = Coordinate.createGPSCoordinate(lat, lon);

            Position pos = new Position(coord, timeStamp, 2, 12);
            positions.add(pos);
        }

        return positions;
    }


    /**
     * creates a randomized grid where each gridpoint represents the position of one fog node and stores those positions in DB
     * further it sets the traces for each path according to the new distribution of fog nodes
     * <p>
     * takes a long time to execute (ca 35 minutes) due to the amount of sql statements/queries
     *
     * @param bottomLeft
     * @param bottomRight
     * @param topLeft
     * @param sizeX        in km
     * @param sizeY        in km
     * @param intervall    in km
     * @param distToEdges  in km
     * @param maxRandShift in km
     */
    public void generateRandomFogNodePositionInDB(Coordinate bottomLeft, Coordinate bottomRight, Coordinate topLeft, double sizeX, double sizeY, double intervall, double distToEdges, double maxRandShift) {

        try {
            Connection conn = this.connect();
            conn.setAutoCommit(false);
            String dropTable = "DROP TABLE IF EXISTS node_positions";
            String createTable = "CREATE TABLE IF NOT EXISTS node_positions(node_id INTEGER, lat REAL, lon REAL)";

            Statement stmt = conn.createStatement();
            stmt.execute(dropTable);
            stmt.execute(createTable);
            conn.commit();


            String index = "CREATE INDEX node_id_index ON node_positions(node_id)";
            stmt.execute(index);
            conn.commit();
            System.out.println("index created");


            HashMap<String, Coordinate> grid = new HashMap<>();

            double gridSizeX = sizeX - 2 * distToEdges;
            double gridSizeY = sizeY - 2 * distToEdges;

            int nrPositionsVert = (int) (gridSizeX / intervall);
            int nrPositionsHoriz = (int) (gridSizeY / intervall);

            double bearingAngleY = Coordinate.calcBearingAngle(bottomLeft, topLeft, false);
            double bearingAngleX = Coordinate.calcBearingAngle(bottomLeft, bottomRight, false);

            Coordinate bottomLeftOfGrid = Coordinate.findCoordinateForBearingAndDistance(bottomLeft, bearingAngleX, distToEdges * 1000);
            bottomLeftOfGrid = Coordinate.findCoordinateForBearingAndDistance(bottomLeftOfGrid, bearingAngleY, distToEdges * 1000);

            for (int i = 0; i <= nrPositionsVert; i++) {
                for (int j = 0; j <= nrPositionsHoriz; j++) {

                    Coordinate nextPosition = null;

                    Coordinate belowAndLeft = grid.get((i - 1) + "," + (j - 1));
                    Coordinate left = grid.get((i - 1) + "," + (j));
                    Coordinate below = grid.get(i + "," + (j - 1));

                    //dist to each has to be >300 metres

                    boolean movedCorrectly = false;
                    while (!movedCorrectly) {

                        Coordinate verticallyMoved = Coordinate.findCoordinateForBearingAndDistance(bottomLeftOfGrid, bearingAngleX, i * intervall * 1000 - (maxRandShift * 1000) + new Random().nextInt(1000));
                        Coordinate horizontallyMoved = Coordinate.findCoordinateForBearingAndDistance(verticallyMoved, bearingAngleY, j * intervall * 1000 - (maxRandShift * 1000) + new Random().nextInt(1000));

                        ArrayList<Double> distances = new ArrayList<>();

                        if (belowAndLeft != null)
                            distances.add(Coordinate.calcDistance(horizontallyMoved, belowAndLeft));
                        if (left != null) distances.add(Coordinate.calcDistance(horizontallyMoved, left));
                        if (below != null) distances.add(Coordinate.calcDistance(horizontallyMoved, below));

                        if (distances.size() == 0) {
                            nextPosition = horizontallyMoved;
                            movedCorrectly = true;
                        }

                        boolean allAbove300 = true;
                        for (double d : distances) {
                            if (d < 300) {
                                allAbove300 = false;
                                break;
                            }
                        }
                        if (allAbove300) {
                            nextPosition = horizontallyMoved;
                            movedCorrectly = true;
                        }
                    }
                    grid.put(i + "," + j, nextPosition);
                }
            }

            System.out.println("done filling map");

            long s1 = System.currentTimeMillis();

            HashMap<Integer, Coordinate> allCoordsWithId= new HashMap<>();


            System.out.println("starting to fill coords in  db");
            int id = 0;
            for (Coordinate c : grid.values()) {

                allCoordsWithId.put(id, c);
                String addPosition = "INSERT INTO node_positions VALUES (" + id + "," + c.getLat() + "," + c.getLon() + ")";

                stmt.execute(addPosition);
                id++;

                if (id % 500 == 0) {
                    conn.commit();
                    System.out.println(id);
                }
            }
            conn.commit();

            long s2 = System.currentTimeMillis();

            System.out.println("coordinates stored in db, took " + (s2 - s1) / 1000 + " seconds");

            ArrayList<Path> allPaths = this.getAllPaths();



            System.out.println("now drop all traces");
            long t1 = System.currentTimeMillis();

            String dropAllTraces = "UPDATE paths SET fog_nodes_trace = NULL WHERE fog_nodes_trace IS NOT NUll ";
            stmt.execute(dropAllTraces);
            conn.commit();

            System.out.println("dropping took: "+(System.currentTimeMillis()-t1)/1000 + " seconds");




            long time1 = System.currentTimeMillis();

            int counter = 0;

            String updateString = "UPDATE paths SET fog_nodes_trace = ? WHERE path_id= ?";
            PreparedStatement updatePathTrace = conn.prepareStatement(updateString);



            System.out.println("erste: "+ (int) (sizeX / intervall) + "         zweite: "+(int) (sizeY / intervall));

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int ij = 10;
            while (ij <= 111) {

                for (int i = 1; i < (int) (sizeX / ij)+1; i++) {

                    for (int j = 1; j < (int)(sizeY / ij)+1; j++) {

                        Coordinate botLeft = Coordinate.findCoordinateForBearingAndDistance(bottomLeft, bearingAngleX, (i-1) * 1000 * ij);;
                        botLeft = Coordinate.findCoordinateForBearingAndDistance(botLeft, bearingAngleY, j * 1000 * ij);
                        Coordinate botRight = Coordinate.findCoordinateForBearingAndDistance(botLeft, bearingAngleX,  1000 * ij);
                        Coordinate topLeft1 = Coordinate.findCoordinateForBearingAndDistance(botLeft, bearingAngleY,  1000 * ij);
                        Coordinate topRigth1 = Coordinate.findCoordinateForBearingAndDistance(botRight, bearingAngleY,  1000 * ij);

                        double minLat = botRight.getLat();
                        double minLon = botLeft.getLon();
                        double maxLat = topLeft1.getLat();
                        double maxLon = topRigth1.getLon();

                        //offset bounding box by 2km to each side
                        double minLatOff = Coordinate.getOffsetLat(botRight, -2000);
                        double maxLatOff = Coordinate.getOffsetLat(topLeft1, 2000);
                        double minLonOff = Coordinate.getOffsetLon(botLeft, -2000);
                        double maxLonOff = Coordinate.getOffsetLon(topRigth1, 2000);


                        ArrayList<Path> pathsInBorder = this.getPathsWithingWhereTraceIsNull(minLat, maxLat, minLon, maxLon);
                        HashMap<Integer, Coordinate> coordWithId = this.getFogNodesInBorders(minLatOff, maxLatOff, minLonOff, maxLonOff);



                        System.out.println( "Intervall:"+ ij + "      Loop:" + i + "," + j);

                        int foundPaths = pathsInBorder.size();
                        if (foundPaths != 0) {

                            System.out.println(foundPaths + " pfade gefunden. Intervall:"+ ij + "      Loop:" + i + "," + j);
                            System.out.println("nodes in borders: "+coordWithId.keySet().size());
                        }

                        for (Path path : pathsInBorder) {
                            counter++;
                            if (counter % 2000 == 0) {
                                long time2 = System.currentTimeMillis();
                                updatePathTrace.executeBatch();
                                conn.commit();
                                time1 = time2;
                            }

                            LinkedList<Integer> trace = new LinkedList();

                            for (int l = 0; l < path.getPositions().size(); l += 1) {
                                Position pos = path.getPositions().get(l);

                                double currMinDist = Double.MAX_VALUE;
                                int currentClosesId = 0;

                                for (int key : coordWithId.keySet()) {
                                    double dist = Coordinate.calcDistance(pos.getCoordinate(), coordWithId.get(key));
                                    if (dist < currMinDist) {
                                        currMinDist = dist;
                                        currentClosesId = key;
                                    }
                                }
                                if (trace.size() == 0) {
                                    trace.add(currentClosesId);
                                    continue;
                                }

                                if (trace.getLast() != currentClosesId) {
                                    trace.add(currentClosesId);
                                }
                            }

                            String toStore = "";
                            for (int k = 0; k < trace.size(); k++) {
                                if (k == trace.size() - 1) {
                                    toStore += trace.get(k);
                                    break;
                                }
                                toStore += trace.get(k) + ",";
                            }
                            updatePathTrace.setString(1, toStore);
                            updatePathTrace.setInt(2, path.getPathId());
                            updatePathTrace.addBatch();
                        }
                    }
                }
                ij += 10;
            }

            conn.commit();


            //now set all the left over paths (which werent inside boders

            ArrayList<Path> leftOverPaths = new ArrayList<>();

            String selectAllLeftOvers = "SELECT * FROM paths WHERE fog_nodes_trace IS NULL OR fog_nodes_trace = 0";
            ResultSet resultSet =  stmt.executeQuery(selectAllLeftOvers);
            while(resultSet.next()){
                int id1 = resultSet.getInt("path_id");
                String pathString = resultSet.getString("path");
                int duplicates = resultSet.getInt("duplicates");
                double distance = resultSet.getDouble("distance");
                double minLat1 = resultSet.getDouble("min_lat");
                double maxLat1 = resultSet.getDouble("max_lat");
                double minLon1 = resultSet.getDouble("min_lon");
                double maxLon1 = resultSet.getDouble("max_lon");
                LinkedList<Integer> trace = new LinkedList<>();
                String traceString = resultSet.getString("fog_nodes_trace");
                if(traceString != null){
                    String[] traceSplit = traceString.split(",");

                    for (int i = 0; i < traceSplit.length; i++) {
                        int idNum = Integer.parseInt(traceSplit[i]);
                        trace.add(idNum);
                    }
                }

                ArrayList<Position> positions = getPositionsForPathString(pathString);

                Path path = new Path(id1, positions, duplicates,  minLat1, maxLat1, minLon1, maxLon1, distance, trace);
                leftOverPaths.add(path);
            }

            System.out.println("++++++++++++++++++++++++++leftoverPaths: "+leftOverPaths.size());
            int c = 0;
            for(Path path : leftOverPaths){
                c++;
                if(c % 10 == 0){
                    System.out.println(c + " von "+leftOverPaths.size());
                }

                LinkedList<Integer> trace = new LinkedList();

                for (Position pos : path.getPositions()) {


                    double currMinDist = Double.MAX_VALUE;
                    int currentClosesId = 0;

                    for (int key : allCoordsWithId.keySet()) {
                        double dist = Coordinate.calcDistance(pos.getCoordinate(), allCoordsWithId.get(key));
                        if (dist < currMinDist) {
                            currMinDist = dist;
                            currentClosesId = key;
                        }
                    }
                    if (trace.size() == 0) {
                        trace.add(currentClosesId);
                        continue;
                    }

                    if (trace.getLast() != currentClosesId) {
                        trace.add(currentClosesId);
                    }
                }

                String toStore = "";
                for (int k = 0; k < trace.size(); k++) {
                    if (k == trace.size() - 1) {
                        toStore += trace.get(k);
                        break;
                    }
                    toStore += trace.get(k) + ",";
                }
                updatePathTrace.setString(1, toStore);
                updatePathTrace.setInt(2, path.getPathId());
                updatePathTrace.addBatch();

                if (counter % 300 == 0) {
                    updatePathTrace.executeBatch();
                    conn.commit();
                    System.out.println("300  commited");
                }
            }
            updatePathTrace.executeBatch();
            conn.commit();

            conn.setAutoCommit(true);

            long s3 = System.currentTimeMillis();
            System.out.println("all traces set, took " + (s3 - s2) / 1000 + " seconds");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, Coordinate> getFogNodesInBorders(double minLat, double maxLat, double minLon, double maxLon) {

        HashMap<Integer, Coordinate> nodesWithinBorder = new HashMap<>();
        try {
            Connection conn = connect();
            String query = "SELECT * FROM node_positions WHERE lat >=" + minLat
                    + " AND lat <=" + maxLat
                    + " AND lon >= " + minLon
                    + " AND lon <=" + maxLon;

            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("node_id");
                double lat = resultSet.getDouble("lat");
                double lon = resultSet.getDouble("lon");

                Coordinate coord = Coordinate.createGPSCoordinate(lat, lon);
                nodesWithinBorder.put(id, coord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodesWithinBorder;
    }

    public HashMap<Integer, Coordinate> getAllFogNodePositions() {
        HashMap<Integer, Coordinate> allPositionsmap = new HashMap<>();
        try {
            Connection conn = this.connect();
            String allPositions = "SELECT * FROM node_positions";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(allPositions);

            while (rs.next()) {
                int id = rs.getInt("node_id");
                double lat = rs.getDouble("lat");
                double lon = rs.getDouble("lon");
                allPositionsmap.put(id, Coordinate.createGPSCoordinate(lat, lon));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allPositionsmap;
    }
}
