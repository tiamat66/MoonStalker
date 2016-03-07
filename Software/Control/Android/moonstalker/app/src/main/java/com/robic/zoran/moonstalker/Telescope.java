package com.robic.zoran.moonstalker;

/**
 * Created by zoran on 7.3.2016.
 */
public class Telescope {

    //Constants
    private static final double POLARIS_RA =  0;
    private static final double POLARIS_DEC = 90;

    boolean isCalibrated = false;

    Control control;
    Position position;

    public void calibration()
    {
        double lat, lon;

        // The default calibration position is POLARIS
        position.setRa(POLARIS_RA);
        position.setDec(POLARIS_DEC);
        // Get globe coordinates from GPS module
        position.setLatitude(40); //TODO
        position.setLongitude(40);//TODO
        position.equatorialToTelescope();
    }

}
