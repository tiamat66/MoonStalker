package com.robic.zoran.moonstalker;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

import java.util.GregorianCalendar;

/**
 * Created by zoran on 7.3.2016.
 */
public class Telescope {
    Position position;

    private static final double POLARIS_RA =  0;
    private static final double POLARIS_DEC = 90;
    private static final double PRECISION = 2.0;
    // Mechanical characteristics
    private static final double MOTOR_STEPS_NUM = 200.0;
    private static final double REDUCTOR_TRANSMITION = 30.0;
    private static final double BELT_TRANSMITION = 48.0 / 14.0;
    private static final double K = MOTOR_STEPS_NUM *
            REDUCTOR_TRANSMITION *
            BELT_TRANSMITION;

    boolean isCalibrated;
    double hSteps;
    double vSteps;


    public Telescope() {
        position = new Position();
        isCalibrated = false;
        hSteps = 0;
        vSteps = 0;
    }

    public boolean isCalibrated() {
        return isCalibrated;
    }

    public void calibration()
    {
        // The default calibration position is POLARIS
        position.setRa(POLARIS_RA);
        position.setDec(POLARIS_DEC);
        position.equatorialToTelescope();
        isCalibrated = true;
    }

    public Position getPosition() {
        return position;
    }

    public void Move(double ra, double dec) {
        position.setRa(ra);
        position.setDec(dec);
        Move();
    }

    public void Move()
    {
        double dif_az;
        double dif_hi;
        double azimuth_tmp;
        double height_tmp;
        int cur_h_steps = 0;
        int cur_v_steps = 0;

        azimuth_tmp = position.getAzimuth();
        height_tmp = position.getHeight();

        position.equatorialToTelescope();
        dif_az = position.getAzimuth() - azimuth_tmp;
        dif_hi = position.getHeight() - height_tmp;

        hSteps += (dif_az * K) / 360.0;
        vSteps += (dif_hi * K) / 360.0;

        if((Math.abs(hSteps) >= PRECISION) ||
                (Math.abs(vSteps) >= PRECISION))
        {
            cur_h_steps = (int)hSteps;
            cur_v_steps = (int)vSteps;
            hSteps -= cur_h_steps;
            vSteps -= cur_v_steps;
            //vtsk_mv(cur_h_steps, cur_v_steps);
        }
    }
}
