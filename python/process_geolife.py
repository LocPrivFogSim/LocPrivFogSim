import os
import re
import sqlite3
import subprocess
import numba.np.extensions
import numpy
from datetime import datetime
from math import sin, cos, sqrt, atan2, radians
from decimal import Decimal
from numba import njit

directory = "D:/BA/Geolife Trajectories 1.3/Data"
#directory = "D:/BA/002"
newDir = "D:/BA/adjustedData3"
duplicatesDir = "D:/BA/duplikate2"
logFile = "D:/BA/log.txt"
duplicatesFile = "D:/BA/duplicates1.csv"



max_distance_between_coords = 20
max_time_diff_between_coords = 10
min_nr_coords = 40
min_dist_start_end = 200


def calc_dist_in_m(coordinate1, coordinate2):
    x = numpy.array([numpy.float64(coordinate1[0]), numpy.float64(coordinate1[1])])
    y = numpy.array([numpy.float64(coordinate2[0]), numpy.float64(coordinate2[1])])
    return calc_dist_njit(x, y)


@njit()
def calc_dist_njit(x, y):
    radius_earth = 6371.0

    lat1 = radians(x[0])
    lon1 = radians(x[1])
    lat2 = radians(y[0])
    lon2 = radians(y[1])

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    distance = radius_earth * c * 1000
    return distance


def is_in_peking(coordinate):
    # point to check
    x = numpy.array([numpy.float64(coordinate[0]), numpy.float64(coordinate[1])])
    return is_in_peking_njit(x)


@njit()
def is_in_peking_njit(Z):
    A = numpy.array([numpy.float64(41.229556359747455), numpy.float64(116.32212585037877)])
    B = numpy.array([numpy.float64(40.56688298442188), numpy.float64(117.73143927643063)])
    C = numpy.array([numpy.float64(39.099855), numpy.float64(116.519898)])
    D = numpy.array([numpy.float64(39.75667), numpy.float64(115.13559)])

    # vectors (a,b) (b,c) (c,d) (d,a) -> point has to be on the right of each => crossprodukt > 0

    cross_ABxAZ = numba.np.extensions.cross2d(B - A, Z - A)
    cross_BCxBZ = numba.np.extensions.cross2d(C - B, Z - B)
    cross_CDxCZ = numba.np.extensions.cross2d(D - C, Z - C)
    cross_DAxDZ = numba.np.extensions.cross2d(A - D, Z - D)

    if (cross_ABxAZ > 0 and cross_BCxBZ > 0 and cross_CDxCZ > 0 and cross_DAxDZ > 0):
        return True
    else:
        return False


def get_coord_list_from_file(old_file_path):
    coordinates_as_list = []

    # read full content & read all lines
    tempDatei = open(old_file_path)
    inhalt = tempDatei.read()
    tempDatei.close()

    tempDatei = open(old_file_path)
    allLines = tempDatei.readlines()
    tempDatei.close()

    for i in range(len(allLines)):
        if "\n" in allLines[i]:
            allLines[i] = allLines[i].replace('\r', '').replace('\n', '')

    # 39.8593516,116.25777,0,236.2,39927.4890162037,2009-04-24,11:44:11
    pattern = "-?[0-9]{1,3}\.[0-9]*,-?[0-9]{1,3}\.[0-9]*,0,-?[0-9]*\.?[0-9]*,[0-9]*\.[0-9]*,[0-9]{4}-[0-9]{2}-[0-9]{2},[0-9]{2}:[0-9]{2}:[0-9]{2}"
    coordinateLines = re.findall(pattern, inhalt)

    time_format = "%Y-%m-%d %H:%M:%S"

    coordinateLines = re.findall(pattern, inhalt)
    if (len(coordinateLines) > 0):
        coordinateLine_Split = str(coordinateLines[0]).split(",")
        startTime = datetime.strptime(coordinateLine_Split[5] + " " + coordinateLine_Split[6], time_format)

        previous_coord = None

        for i in range(len(coordinateLines)):
            coordinateLine_Split = str(coordinateLines[i]).split(",")
            lat = coordinateLine_Split[0]
            lon = coordinateLine_Split[1]
            time = datetime.strptime(coordinateLine_Split[5] + " " + coordinateLine_Split[6], time_format)
            time_dif = (time - startTime).seconds

            coordinate_arr = [lat, lon, time_dif]
            if(coordinate_arr != previous_coord):
                coordinates_as_list.append(coordinate_arr)
            previous_coord = coordinate_arr
        return coordinates_as_list


def get_valid_paths_from_coord_list(coord_list: list):
    if coord_list == None:
        return
    if len(coord_list) == 0:
        return

    valid_paths: list = []



    while len(coord_list) >= min_nr_coords:
        # lat,lon,time
        startcoord = coord_list[0]
        current_coord = coord_list[0]
        prev_coord = []
        current_path_list = []

        curr_max_dist = 0
        time_dif_below_max = True
        in_peking = is_in_peking(current_coord)

        while time_dif_below_max and in_peking:
            current_path_list.append(current_coord)
            if len(coord_list) <= len(current_path_list):
                break
            if (coord_list[len(current_path_list)]) == None:
                break
            curr_max_dist = max(curr_max_dist, calc_dist_in_m(current_coord, startcoord))
            prev_coord = current_coord
            current_coord = coord_list[len(current_path_list)]
            time_dif_below_max = (current_coord[2] - prev_coord[2] < max_time_diff_between_coords)
            in_peking = is_in_peking(current_coord)

        if len(current_path_list) == 0:
            del coord_list[0]
        else:
            del coord_list[0:len(current_path_list)]
            if curr_max_dist >= min_dist_start_end and len(
                    current_path_list) >= min_nr_coords:
                valid_paths.append(current_path_list)
    return valid_paths


def create_files_for_path_list(paths: list, directory_path):
    for path in paths:

        currentLatPrefix = str(path[0][0]).split(".")[0]
        currentLonPrefix = str(path[0][1]).split(".")[0]
        start_time = path[0][2]

        text = currentLatPrefix + ";" + currentLonPrefix + "\n"

        for coord in path:
            lat = str(coord[0])
            tempLatPrefix = lat.split(".")[0]
            tempLatSufix = lat.split(".")[1]

            lon = str(coord[1])
            tempLonPrefix = lon.split(".")[0]
            tempLonSufix = lon.split(".")[1]

            time_dif = coord[2] - start_time

            if tempLatPrefix == currentLatPrefix and tempLonPrefix == currentLonPrefix:
                text += tempLatSufix + "," + tempLonSufix + "," + str(time_dif) + "\n"

            else:
                currentLonPrefix = tempLonPrefix
                currentLatPrefix = tempLatPrefix
                text += "\n\n\n\n" + currentLatPrefix + ";" + currentLonPrefix + "\n\n\n"

        dir_len = len(os.listdir(directory_path))

        # print (text)
        tempDatei = open(directory_path + "/path_" + str(dir_len) + ".txt", "w+")
        tempDatei.write(text)
        tempDatei.close()

def create_path_string(path:list):
    result_string = ""
    time_start = path[0][2]
    for coord in path:
        lat = str(coord[0])
        lon = str(coord [1])
        timestamp = str(coord[2]-time_start)
        result_string = result_string + lat +","+lon+","+timestamp+"||"
    return result_string


def createFileForEachPath():
    starttime = datetime.now()
    counter = 0
    for dirpath, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".plt"):
                counter = counter + 1
                print(os.path.join(dirpath, file), "        fortschritt: ", counter, "/19000", "   time: ",
                      datetime.now() - starttime)
                coord_list_for_file = get_coord_list_from_file(os.path.join(dirpath, file))
                paths_for_coord_list = get_valid_paths_from_coord_list(coord_list_for_file)
                create_files_for_path_list(paths_for_coord_list, newDir)


def paths_are_similar(path1, path2):
    startCoord1 = path1[0]
    endCoord1 = path1[-1]

    startCoord2 = path2[0]
    endCoord2 = path2[-1]

    dist_start = calc_dist_in_m(startCoord1, startCoord2)
    dist_end = calc_dist_in_m(endCoord1, endCoord2)
    nr_pairs = max(len(path1), len(path2))

    if dist_start <= 40 and dist_end <= 40:
        dtw_val = dtw(path1, path2)
        if dtw_val / nr_pairs < 19.9:
            return True
    return False

def calcLenOfPath(path):
    length = 0
    for i in range(1,path.__len__()):
        lat1 = float(path[i-1][0])
        lat2 = float(path[i][0])
        lon1 = float(path[i-1][1])
        lon2 = float(path[i][1])
        coord1 = [lat1, lon1]
        coord2 = [lat2, lon2]
        length = length + calc_dist_in_m(coord1, coord2)

    return length

def dtw(path_X, path_Y):
    x = []
    y = []
    for i in range(len(path_X)):
        typedA = numpy.float64(path_X[i][0])
        typedB = numpy.float64(path_X[i][1])
        x.append([typedA, typedB])

    for i in range(len(path_Y)):
        typedA = numpy.float64(path_Y[i][0])
        typedB = numpy.float64(path_Y[i][1])
        y.append([typedA, typedB])

    npArrA = numpy.array(x)
    npArrB = numpy.array(y)

    return dtw_njit(npArrA, npArrB)


@njit()
def dtw_njit(path_X, path_Y):
    X = len(path_X)
    Y = len(path_Y)

    # store distances for all pairs of both paths in matrix
    distance_mat = numpy.zeros((X, Y))

    for i in range(X):
        for j in range(Y):
            distance_mat[i, j] = calc_dist_njit(path_X[i], path_Y[j])

    # init cost matrix
    dist_mat = numpy.zeros((X + 1, Y + 1))
    for i in range(1, X + 1):
        dist_mat[i, 0] = numpy.inf
    for i in range(1, Y + 1):
        dist_mat[0, i] = numpy.inf

    # fill cost matrix
    for i in range(1, X + 1):
        for j in range(1, Y + 1):
            loc_dist = distance_mat[i - 1, j - 1]
            dist_mat[i, j] = loc_dist + min([dist_mat[i - 1, j - 1], dist_mat[i, j - 1], dist_mat[i - 1, j]])

    return dist_mat[X, Y]


def createGPX(coords: list):
    # header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?> \n\n"
    follow = "<gpx>\n<trk>\n<trkseg>\n"
    end = "</trkseg>\n</trk>\n</gpx>"

    coordstmp = ""
    for coord1 in coords:
        lat1 = coord1[0]
        lon1 = coord1[1]
        coordstmp = coordstmp + "<trkpt lat=\"" + lat1 + "\" lon=\"" + lon1 + "\"> </trkpt>\n"

    content = follow + coordstmp + end
    # print(content)
    return content


def dist_to_corner(path: list):
    corner = [41.229556359747455, 116.32212585037877]
    x = calc_dist_in_m(corner, path[0])
    return x


def run():


    print("exit")
    exit
    conn = sqlite3.connect('D://BA/geoLifePaths.db')

    c = conn.cursor()
    c.execute("DROP table IF EXISTS paths")
    conn.commit()


    #pathID | | path | | duplicatesNumber | | minLat | | maxLat | | minLon | | maxLon
    c.execute("""CREATE TABLE paths(
        path_id INTEGER,
        path BLOB,
        duplicates INTEGER,
        distance REAL,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL)""")


    log = open(logFile, "w")
    starttime = datetime.now()
    log.write("start: " + str(starttime))
    counter = 0
    all_valid_paths: list = []

    all_paths_len = 0
    all_valid_paths_len = 0

    # iterate all files
    for dirpath, dirs, files in os.walk(directory):

        for file in files:
            if file.endswith(".plt"):
                counter = counter + 1
                if (counter % 50 == 0):
                    print("fortschritt: ", counter, "/19000")

                coord_list_for_file = get_coord_list_from_file(os.path.join(dirpath, file))

                all_paths_len = all_paths_len + calcLenOfPath(coord_list_for_file)

                valid_paths = get_valid_paths_from_coord_list(coord_list_for_file)

                for valid in valid_paths:
                    all_valid_paths_len = all_valid_paths_len + calcLenOfPath(valid)

                for e in valid_paths:
                    all_valid_paths.append(e)




    log.write("\nAnzahl valide Pfade: " + len(all_valid_paths).__str__())
    loadingTime = datetime.now()
    log.write("\nPfade geladen: " + str(loadingTime) + "     dauer: " + str(loadingTime - starttime))

    log.write("\n Länge aller Pfade: "+str(all_paths_len) +  "   Länge der validen Pfade: "+str(all_valid_paths_len))

    # sort
    all_valid_paths = sorted(all_valid_paths, key=dist_to_corner)

    sorting_time = datetime.now()
    log.write("\nsortiert:  " + str(sorting_time) + "     dauer:  " + str(sorting_time - loadingTime))

    # for logging
    total_duplicates = 0

    # [ [path, nr_duplicates], ...]
    result_list = []

    # each [path, is_alrd_duplicate_bool]
    paths = []
    for p in all_valid_paths:
        paths.append([p, False])

    # fill result list
    for i in range(len(paths)):
        j = 1
        if paths[i][1] is True:
            continue

        result_list.append([paths[i][0], 0])

        if not i + j < len(paths):
            break

        tmp_gpx_list = []
        tmp_gpx_list.append(paths[i][0])

        # remove duplicates
        while (calc_dist_in_m(paths[i][0][0], paths[i + j][0][0]) < 19.9):
            if (paths[i + j][1] is False):
                if paths_are_similar(paths[i][0], paths[i + j][0]):
                    result_list[-1][1] = result_list[-1][1] + 1
                    paths[i + j][1] = True
                    total_duplicates = total_duplicates + 1

                    tmp_gpx_list.append(paths[i + j][0])

            if (i + j + 1 < paths.__len__()):
                j = j + 1
            else:
                break

        # create gpx-files for duplicates -> to visualize some later on
        if len(tmp_gpx_list) > 1:
            os.mkdir(duplicatesDir + "/duplikat_" + str(len(result_list) - 1) + "_" + str(len(tmp_gpx_list)))
            for k in range(len(tmp_gpx_list)):
                gpx = createGPX(tmp_gpx_list[k])
                tempDateiA = open(
                    duplicatesDir + "/duplikat_" + str(len(result_list) - 1) + "_" + str(len(tmp_gpx_list)) + "/" + str(
                        k) + ".gpx", "w+")
                tempDateiA.write(gpx)
                tempDateiA.close()

    log.write("\nAnzahl Duplikate: " + total_duplicates.__str__())

    # put all paths in DB -> pathTable contains pathID  || path || duplicatesNumber || minLat || maxLat || minLon || maxLon

    for k in range(result_list.__len__()):
        id = int(k)


        path = result_list[k][0]

        distance = calcLenOfPath(path)

        path_string = create_path_string(path)

        nrOfDuplicates = int(result_list[k][1])
        minLat =float( 10000)
        maxLat = float(-10000)
        minLon = float(10000)
        maxLon = float(-10000)

        for coord in path:
            lat = float(coord[0])
            lon = float(coord[1])

            if (lat > maxLat):
                maxLat = lat
            if (lat < minLat):
                minLat = lat
            if (lon > maxLon):
                maxLon = lon
            if (lon < minLon):
                minLon = lon



        c.execute("INSERT INTO paths (path_id, path, duplicates, distance,min_lat, max_lat, min_lon, max_lon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", (id, path_string, nrOfDuplicates, distance, minLat, maxLat, minLon, maxLon))


        #print("minLat: ", str(minLat), "    maxLat: ", str(maxLat), "       minLon: ",str(minLon), "    maxLon: ",str(maxLon) )

    conn.commit()
    conn.close()

    log.write("\nende: " + datetime.now().__str__() + "    dauer: "  + str(datetime.now() - starttime))

    log.close()
    return





    # duplicate "dictionary" for later trace_comp_val
    duplicates = open(duplicatesFile, "w")
    nr = 0
    for k in range(result_list.__len__()):
        duplicates.write(str(nr) + ":" + str(result_list[k][1]) + "\n")
        create_files_for_path_list([result_list[k][0]], newDir)
        nr = nr + 1

    duplicates.close()


run()


# debugging
def file_to_Gpx():
    global coordList
    tmp = open("D:/BA/adjustedData/newFile76.txt")
    allLines = tmp.readlines()
    coordList = []
    pre_Lat = 0
    pre_Lon = 0
    for line in allLines:
        if line.__contains__(";"):
            pre_Lat = line.split(";")[0]
            pre_Lon = line.split(";")[1].replace("\n", "")
        if line.__contains__(","):
            lat = pre_Lat.__str__() + "." + line.split(",")[0]
            lon = pre_Lon.__str__() + "." + line.split(",")[1]
            coordList.append([lat, lon])
    print(coordList)
    createGPX(coordList)
