package si.vajnartech.moonstalker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;
import si.vajnartech.moonstalker.rest.GetStarInfo;

import static si.vajnartech.moonstalker.C.MINIMUM_DISTANCE;
import static si.vajnartech.moonstalker.C.MINIMUM_TIME;
import static si.vajnartech.moonstalker.C.ST_CONNECTING;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.INIT;

public abstract class Telescope implements LocationListener
{
  protected MainActivity act;

  private final Location curLocation = new Location("GPS");

  protected SkyObject newObject = new SkyObject(() -> {
    curLocation.setLatitude(C.DEF_LATITUDE);
    curLocation.setLongitude(C.DEF_LONGITUDE);
    return curLocation;
  });

  protected final SkyObject skyObject = new SkyObject(() -> {
    curLocation.setLatitude(C.DEF_LATITUDE);
    curLocation.setLongitude(C.DEF_LONGITUDE);
    return curLocation;
  });

  protected static final int PRECISION = 1;

  Telescope(MainActivity act)
  {
    this.act = act;
  }

  private void enableGPSService(MainActivity act)
  {
    LocationManager locationManager = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);

    if (locationManager == null)
      Log.d(TAG, "Cannot get the LocationManager");
    else
      Log.d(TAG, "The LocationManager successfully granted");
    if (ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED)
      if (locationManager != null)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, this);
  }

  void calibrate()
  {
    new GetStarInfo(C.calObj, (name, constellation, ra, dec) -> {
      skyObject.set(name, constellation, ra, dec);
      TelescopeStatus.set(ST_READY);
      act.userInterface.setPositionString(R.color.colorOk, skyObject);
    });
  }

  protected int setMaxSpeedRPM()
  {
    return 500;
  }

  protected void connect()
  {
    TelescopeStatus.set(ST_CONNECTING);
  }

  void init()
  {
    inMsgProcess(INIT, new Bundle());
  }

  public abstract void inMsgProcess(String msg, Bundle bundle);

  abstract void move();

  abstract void st();

  public SkyObject getNewObject()
  {
    return newObject;
  }

  public SkyObject getSkyObject()
  {
    return skyObject;
  }
}
