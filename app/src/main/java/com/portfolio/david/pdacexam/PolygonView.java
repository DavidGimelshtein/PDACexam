package com.portfolio.david.pdacexam;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlPlacemark;
import com.google.maps.android.kml.KmlPolygon;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by david on 02.11.2017.
 */

public class PolygonView {

    private ArrayList<LatLng> polygonCoordinates;
    private GoogleMap map;
    private Context context;

    public PolygonView(GoogleMap map, int kmlLocalResource, Context context) {
        //initialize polygon necessary fields such as map, context, layer, polygon vertexes
        this.map = map;
        this.context = context;
        try {
            //creating KML layer based on KML file
            KmlLayer layer = new KmlLayer(map, kmlLocalResource, context);
            //Retrieve the first container in the KML layer
            KmlContainer container = layer.getContainers().iterator().next();
            //Retrieve the first placemark in the nested container
            KmlPlacemark placemark = container.getPlacemarks().iterator().next();
            //Retrieve a polygon object in a placemark
            KmlPolygon polygon = (KmlPolygon)placemark.getGeometry();
            //Retrieve polygon points - coordinates from polygon object
            polygonCoordinates = polygon.getOuterBoundaryCoordinates();
            displayOnMap(polygonCoordinates, Color.BLACK, 10);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //The algorithm of checking whether point is inside of polygon based on comparing current area of polygon
    // with area of new polygon that includes new point between two closest polygon points to it.
    // If that point is inside polygon then area of new polygon is less than area of base polygon,
    // and vice versa the area of new polygon is greater than the base one.
    public int isMarkerInside(MarkerOptions marker) {
        //creating coordinates of new polygon based on the first one
        ArrayList<LatLng> newPolygonCoordinates = (ArrayList<LatLng>)polygonCoordinates.clone();
        //indexes of "closest" polygon points to marker point
        int firstPointIndex = 0; int secondPointIndex = 0;
        float minDistance = Float.MAX_VALUE;
        float distance;
        LatLng markerPoint = marker.getPosition();
        //get the firstPointIndex and it's distance
        for (LatLng polygonPoint : polygonCoordinates) {
            distance = getDistance(markerPoint, polygonPoint);
            if (distance < minDistance) {
                minDistance = distance;
                firstPointIndex = polygonCoordinates.indexOf(polygonPoint);
            }
        }
        //the secondPointIndex is beside the firstPointIndex on the left or right hand
        // and it's must be closest to marker point but also the straight line connecting to secondPointIndex
        // should not cross ribs of polygon
        //http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
        secondPointIndex = getSecondPointIndex(markerPoint, firstPointIndex, newPolygonCoordinates);
        //checking if area of new polygon less or great from base one
        if (getPolygonSquare(newPolygonCoordinates) < getPolygonSquare(polygonCoordinates)) {
            map.clear();
            displayOnMap(newPolygonCoordinates, Color.GREEN, 5);
            displayOnMap(polygonCoordinates, Color.BLACK, 10);
            return 0;//there no distance to polygon. The marker point is in...
        }
        //The marker point is outside so let's compute shortest distance from it to polygon
        //based on distances of each from three it's sides https://www.enotes.com/homework-help/determine-length-height-triangle-abc-that-191277
        map.clear();
        displayOnMap(newPolygonCoordinates, Color.RED, 5);
        displayOnMap(polygonCoordinates, Color.BLACK, 10);
        float a = minDistance;
        float b = getDistance(markerPoint, polygonCoordinates.get(secondPointIndex));
        float base = getDistance(polygonCoordinates.get(firstPointIndex), polygonCoordinates.get(secondPointIndex));
        return Math.round(getShortestDistance(a, b, base));
    }

    private int getSecondPointIndex(LatLng markerPoint, int firstPointIndex, ArrayList<LatLng> newPolygonCoordinates) {
        //the special case is where the firstPoint index is 0 because the 0 and the last index is the same point
        //so the closest point is or index 1 or last - 1 and not just last
        //index of first "closest" polygon point to marker point
        int secondPointIndex = 0;
        if (firstPointIndex == 0){
            if (getDistance(markerPoint, polygonCoordinates.get(polygonCoordinates.size() - 2 )) <
                    getDistance(markerPoint, polygonCoordinates.get((firstPointIndex + 1)))) {
                if (!intersect(markerPoint, polygonCoordinates.get(polygonCoordinates.size() - 2 ),
                        polygonCoordinates.get(firstPointIndex), polygonCoordinates.get((firstPointIndex + 1)))) {
                    secondPointIndex = polygonCoordinates.size() - 2 ;
                    newPolygonCoordinates.add(polygonCoordinates.size() - 1, markerPoint);
                }
                else {
                    secondPointIndex = firstPointIndex + 1;
                    newPolygonCoordinates.add(secondPointIndex, markerPoint);
                }
            }
            else {
                if (!intersect(markerPoint, polygonCoordinates.get((firstPointIndex + 1)),
                        polygonCoordinates.get(polygonCoordinates.size() - 1), polygonCoordinates.get(polygonCoordinates.size() - 2))) {
                    secondPointIndex = firstPointIndex + 1;
                    newPolygonCoordinates.add(secondPointIndex, markerPoint);
                }
                else {
                    secondPointIndex = polygonCoordinates.size() - 2;
                    newPolygonCoordinates.add(polygonCoordinates.size() - 1, markerPoint);
                }
            }
        }
        else {
            if (getDistance(markerPoint, polygonCoordinates.get(firstPointIndex - 1)) <
                    getDistance(markerPoint, polygonCoordinates.get(firstPointIndex + 1))) {
                if (!intersect(markerPoint, polygonCoordinates.get(firstPointIndex - 1),
                        polygonCoordinates.get(firstPointIndex), polygonCoordinates.get(firstPointIndex + 1))) {
                    secondPointIndex = firstPointIndex - 1;
                    newPolygonCoordinates.add(firstPointIndex, markerPoint);
                } else {
                    secondPointIndex = firstPointIndex + 1;
                    newPolygonCoordinates.add(secondPointIndex, markerPoint);
                }
            } else {
                if (!intersect(markerPoint, polygonCoordinates.get(firstPointIndex + 1),
                        polygonCoordinates.get(firstPointIndex), polygonCoordinates.get(firstPointIndex - 1))) {
                    secondPointIndex = firstPointIndex + 1;
                    newPolygonCoordinates.add(secondPointIndex, markerPoint);
                } else {
                    secondPointIndex = firstPointIndex - 1;
                    newPolygonCoordinates.add(firstPointIndex, markerPoint);
                }
            }
        }
        return secondPointIndex;
    }

    private float getShortestDistance(float a, float b, float base) {
        float p = (a + b + base) / 2;
        return (float)(2 * (Math.sqrt(p * (p - a) * (p - b) * (p - base)) / base));
    }

    private void displayOnMap(ArrayList<LatLng> polygonCoordinates, int color, float w) {
        map.addPolygon(new PolygonOptions().addAll(polygonCoordinates).strokeColor(color).strokeWidth(w));
        //Create LatLngBounds of the outer coordinates of the polygon
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : polygonCoordinates) {
            builder.include(latLng);
        }
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 1));
    }

    private float getDistance(LatLng from, LatLng to) {
        float[] distance = new float[3];
        //first method
        //double result, opposite, adjacent;
        //adjacent = Math.abs(Math.abs(from.latitude) - Math.abs(to.latitude));
        //opposite = Math.abs(Math.abs(from.longitude) - Math.abs(to.longitude));
        //result = Math.sqrt(adjacent * adjacent + opposite * opposite);
        //second method https://developer.android.com/reference/android/location/Location.html#distanceBetween(double, double, double, double, float[])
        Location.distanceBetween(from.latitude, from.longitude,
                to.latitude, to.longitude, distance);
        return distance[0];
    }

    private double getPolygonSquare(ArrayList<LatLng> polygonCoordinates) {
        double result = 0;
        int index = 0;
        //formula of finding polygon area: 1/2 * [for i=0 to n](a[i].x * a[i+1].y - a[i].y * a[i+1].x)
        //https://www.mathopenref.com/coordpolygonarea.html
        for (LatLng polygonLatLng : polygonCoordinates) {
            //if it's the last vertex = the first vertex, there is no more to compute
            if (index == polygonCoordinates.size() - 1) {
                continue;
            }
            else {
                result = result +
                        (polygonLatLng.latitude * polygonCoordinates.get(index + 1).longitude -
                                polygonLatLng.longitude * polygonCoordinates.get(index + 1).latitude);
                index++;
            }
        }
        return Math.abs(result / 2);
    }

    private boolean onSegment(LatLng p, LatLng q, LatLng r)
    {
        if (q.latitude <= max(p.latitude, r.latitude) && q.latitude >= min(p.latitude, r.latitude) &&
                q.longitude <= max(p.longitude, r.longitude) && q.longitude >= min(p.longitude, r.longitude))
            return true;

        return false;
    }

    private int orientation(LatLng p, LatLng q, LatLng r) {
        double val = (q.longitude - p.longitude) * (r.latitude - q.latitude)
                - (q.latitude - p.latitude) * (r.longitude - q.longitude);

        if (val == 0.0)
            return 0; // colinear
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    private boolean intersect(LatLng p1, LatLng q1, LatLng p2, LatLng q2) {

        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) return true;

        // p1, q1 and p2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false; // Doesn't fall in any of the above cases
    }
}
