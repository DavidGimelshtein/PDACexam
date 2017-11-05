package com.portfolio.david.pdacexam;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by david on 02.11.2017.
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PolygonView polygonView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //creating polygon area object from it's KML local file regard to current map
        polygonView = new PolygonView(mMap, R.raw.allowed_area, getApplicationContext());
        //moving camera - focus on polygon area while displaying map
        //polygonArea.displayOnMap();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng latLng) {
                //creating a new marker on the map, regard to screen touch area
                MarkerOptions marker = new MarkerOptions().position(latLng);
                int mtrDistsance = polygonView.isMarkerInside(marker);
                //checking if marker is inside polygon area and if it is, setting it's color to green
                if (mtrDistsance == 0)
                    marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                else {
                    //if not, displaying marker with red color and info about the shortest distance to polygon
                    marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    marker.title(String.valueOf(mtrDistsance) + "m");
                }
                //displaying marker on the map
                mMap.addMarker(marker);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            }
        });
    }
}
