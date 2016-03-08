package com.robic.zoran.moonstalker;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class GPSService implements LocationListener {

    private static final double DEF_LONGITUDE = 42.0;
    private static final double DEF_LATITUDE = 42.0;

    String myLocation;
    double latitude;
    double longitude;

    public GPSService() {
        latitude = DEF_LATITUDE;
        longitude = DEF_LONGITUDE;
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        myLocation = "Latitude = " + location.getLatitude() + " Longitude = " + location.getLongitude();

        //I make a log to see the results
        Log.e("MY CURRENT LOCATION", myLocation);

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public String getMyLocation() {
        return myLocation;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}