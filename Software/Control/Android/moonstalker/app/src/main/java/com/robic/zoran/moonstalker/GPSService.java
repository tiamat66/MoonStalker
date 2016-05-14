package com.robic.zoran.moonstalker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class GPSService implements LocationListener {

    private static final String TAG = "gps1";
    private static final double DEF_LONGITUDE = 13.82;
//    private static final double DEF_LONGITUDE = -1.9166667;
    private static final double DEF_LATITUDE = 46.45;
//    private static final double DEF_LATITUDE = 52.5;
    private static final int MINIMUM_TIME = 10000;  // 10s
    private static final int MINIMUM_DISTANCE = 50; // 50m

    double latitude;
    double longitude;
    private LocationManager mLocationManager;
    MainActivity mainActivity;
    private boolean gotLocation = false;

    public GPSService(MainActivity myMainActivity) {
        mainActivity = myMainActivity;
        latitude = DEF_LATITUDE;
        longitude = DEF_LONGITUDE;

        enableGPSService();
    }

    private void enableGPSService() {

        mLocationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);

        if (mLocationManager == null) {
            Log.d(TAG, "Cannot get the LocationManager");
        } else {
            Log.d(TAG, "The LocationManager succesfuly granted");
        }

        // We have to check if ACCESS_FINE_LOCATION and/or ACCESS_COARSE_LOCATION permission are granted
        if (ContextCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        gotLocation = true;
        mainActivity.showLocation();
        mainActivity.updateStatus();
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

    public double getLatitude() {

        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isGotLocation() {
        return gotLocation;
    }
}