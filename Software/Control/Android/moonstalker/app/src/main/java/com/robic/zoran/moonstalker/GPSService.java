package com.robic.zoran.moonstalker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class GPSService implements LocationListener
{
  private static final String TAG = "IZAA";
  private static final double DEF_LONGITUDE = 13.82;
  private static final double DEF_LATITUDE = 46.45;
  private static final int MINIMUM_TIME = 10000;  // 10s
  private static final int MINIMUM_DISTANCE = 50; // 50m

  private double latitude;
  private double longitude;
  private MainActivity act;
  private boolean gotLocation = false;

  GPSService(MainActivity mainActivity)
  {
    this.act = mainActivity;
    latitude = DEF_LATITUDE;
    longitude = DEF_LONGITUDE;
    enableGPSService();
  }

  private void enableGPSService()
  {
    LocationManager locationManager = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);

    if (locationManager == null) {
      Log.d(TAG, "Cannot get the LocationManager");
    } else {
      Log.d(TAG, "The LocationManager succesfuly granted");
    }

    if (ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      || ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

      if (locationManager != null) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, this);
      }
    }
  }

  @Override
  public void onLocationChanged(Location location)
  {
    latitude = location.getLatitude();
    longitude = location.getLongitude();
    StatusBar sb = act.curentFragment.sb;
    if (sb != null)
      sb.setGps();
    gotLocation = true;
  }

  @Override
  public void onStatusChanged(String s, int i, Bundle bundle)
  {
  }

  @Override
  public void onProviderEnabled(String s)
  {
  }

  @Override
  public void onProviderDisabled(String s)
  {
  }

  double getLatitude()
  {
    return latitude;
  }

  double getLongitude()
  {
    return longitude;
  }

  boolean isGotLocation()
  {
    return gotLocation;
  }
}