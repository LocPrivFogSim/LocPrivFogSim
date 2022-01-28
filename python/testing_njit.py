from numba import njit
import numpy




@njit
def ray_tracing(x,y,poly):
    n = len(poly)
    inside = False
    p2x = 0.0
    p2y = 0.0
    xints = 0.0
    p1x,p1y = poly[0]
    for i in range(n+1):
        p2x,p2y = poly[i % n]
        if y > min(p1y,p2y):
            if y <= max(p1y,p2y):
                if x <= max(p1x,p2x):
                    if p1y != p2y:
                        xints = (y-p1y)*(p2x-p1x)/(p2y-p1y)+p1x
                    if p1x == p2x or x <= xints:
                        inside = not inside
        p1x,p1y = p2x,p2y

    return inside




def main():
    location = [40.28926792288004, 116.29271430962295]

    polygon = [[40.29623783,116.30429705], [ 40.30129903,116.29966327],[ 40.28926263,116.2927432 ],[ 40.28664348,116.29322647],[ 40.28498914,116.30166237],[ 40.29494151,116.30440366]]

    polygon = numpy.asarray(polygon)
    
    x = numpy.float64(location[0])
    y = numpy.float64(location[1])

    print(ray_tracing(x,y,polygon))


if __name__ == '__main__':
    main()