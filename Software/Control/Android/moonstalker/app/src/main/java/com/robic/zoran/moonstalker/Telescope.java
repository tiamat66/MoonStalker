package com.robic.zoran.moonstalker;

import android.util.Log;

/**
 * Created by zoran on 7.3.2016.
 */
public class Telescope {
    Position position;
    Control  control;

    private static final String TAG="control1";
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
    boolean isReady = true;
    boolean isTracing = false;
    double hSteps;
    double vSteps;
    Thread traceThread;


    public Telescope(BlueToothService myBtService) {
        control = new Control(myBtService, this);
        position = new Position();
        isCalibrated = false;
        hSteps = 0;
        vSteps = 0;

        traceThread = new Thread(new Runnable() {
            public void run(){

               trace();
            }
        });
        traceThread.start();
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

    public void onMove(double ra, double dec) {
        position.setRa(ra);
        position.setDec(dec);
        move();
    }

    private void move()
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
            if(isReady) {
                cur_h_steps = (int) hSteps;
                cur_v_steps = (int) vSteps;
                hSteps -= cur_h_steps;
                vSteps -= cur_v_steps;
                control.move(cur_h_steps, cur_v_steps);

                clearReady();
            } else {
                Log.d(TAG, "Telescope is busy");
            }
        }
    }

    private void trace() {
        while (true) {

            try {
                if(isTracing) {
                    move();
                }
                traceThread.sleep(1000);
            } catch (InterruptedException e) {

                break;
            }
        }
    }

    public void onTrace() {

        isTracing = true;
    }

    public void offTrace() {

        isTracing = false;
    }

    public void setReady() {

        isReady = true;
    }

    public void clearReady() {

        isReady = false;
    }

    public Control getControl() {

        assert control == null;
        return control;
    }
}
