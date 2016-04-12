package com.robic.zoran.moonstalker;

import android.os.Handler;
import android.util.Log;

/**
 * Created by zoran on 7.3.2016.
 */
public class Telescope {
    Position position;
    Control control;
    MainActivity mainActivity = null;
    private static final int UPDATE_MESSAGE = 1;
    private static final int BUSY_MESSAGE = 2;

    private static final String TAG = "control";
    private static final double POLARIS_RA = 0;
    private static final double POLARIS_DEC = 90;
    private static final double PRECISION = 2.0;
    // Mechanical characteristics
    private static final double MOTOR_STEPS_NUM = 200.0;
    private static final double REDUCTOR_TRANSMITION = 30.0;
    private static final double BELT_TRANSMITION = 48.0 / 14.0;
    private static final double K = MOTOR_STEPS_NUM *
            REDUCTOR_TRANSMITION *
            BELT_TRANSMITION;

    private static final int H_NEGATIVE = 30;
    private static final double TRSHLD_BTRY = 12.0;

    boolean isCalibrated = false;
    boolean isReady = false;
    boolean isTracing = false;
    boolean batteryOk = false;

    double hSteps;
    double vSteps;
    double btryVoltage;
    Thread traceThread;
    Handler h;

    public Telescope(BlueToothService myBtService, GPSService myGpsService, MainActivity myMainActivity) {
        control = new Control(myBtService, this);
        position = new Position(myGpsService);
        isCalibrated = false;
        mainActivity = myMainActivity;
        hSteps = 0;
        vSteps = 0;

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case UPDATE_MESSAGE:
                        mainActivity.updateStatus();
                        break;
                    case BUSY_MESSAGE:
                        mainActivity.updateStatus();
                        break;
                }
            }
        };

        traceThread = new Thread(new Runnable() {
            public void run() {

                trace();
            }
        });
        traceThread.start();
    }

    public void calibration() {
        // The default calibration position is POLARIS
        position.setRa(POLARIS_RA);
        position.setDec(POLARIS_DEC);
        position.equatorialToTelescope();
        isCalibrated = true;
        mainActivity.updateStatus();
    }

    public Position getPosition() {
        return position;
    }

    public void onMove(double ra, double dec) {
        position.setRa(ra);
        position.setDec(dec);
        move();
    }

    public void setBtryVoltage(double btryVoltage) {

        this.btryVoltage = btryVoltage;
        if (btryVoltage < TRSHLD_BTRY) {
            batteryOk = false;
            clearReady();
        } else {
            batteryOk = true;
            setReady();
        }
    }

    public double getBtryVoltage() {
        return btryVoltage;
    }

    private void move() {
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

        if ((Math.abs(hSteps) >= PRECISION) ||
                (Math.abs(vSteps) >= PRECISION)) {
            if (isReady) {
                cur_h_steps = (int) hSteps;
                cur_v_steps = (int) vSteps;
                hSteps -= cur_h_steps;
                vSteps -= cur_v_steps;
                control.move(cur_h_steps, cur_v_steps);

                clearReady();
                h.obtainMessage(UPDATE_MESSAGE).sendToTarget();
            } else {
                h.obtainMessage(BUSY_MESSAGE).sendToTarget();
            }
        }
    }

    private void trace() {
        while (true) {

            try {
                if (isTracing) {
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
        mainActivity.updateStatus();
    }

    public void offTrace() {

        isTracing = false;
        mainActivity.updateStatus();
    }

    public void setReady() {

        isReady = true;
        mainActivity.updateStatus();
    }

    public void clearReady() {

        isReady = false;
    }

    public Control getControl() {

        assert control == null;
        return control;
    }

    public boolean isTracing() {
        return isTracing;
    }

    public boolean isReady() {
        return isReady;
    }

    public boolean getBattery() throws InterruptedException {

        mainActivity.telescope.control.btry();
        Thread.sleep(1000);

        return batteryOk;
    }

    public boolean isBatteryOk() {
        return batteryOk;
    }
}
