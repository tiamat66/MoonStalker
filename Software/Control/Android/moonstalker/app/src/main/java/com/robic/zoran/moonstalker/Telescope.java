package com.robic.zoran.moonstalker;

class Telescope
{
    static final String TAG = "Telescope";
    static final int OK    = 1;
    static final int ERROR = 2;
    static final int BUSY  = 3;
    private Position     pos;
    private MainActivity act;
    private static final double PRECISION = 2.0;
    // Mechanical characteristics
    private static final double MOTOR_STEPS_NUM      = 200.0;
    private static final double REDUCTOR_TRANSLATION = 30.0;
    private static final double BELT_TRANSLATION     = 48.0 / 14.0;

    private static final double K = MOTOR_STEPS_NUM *
            REDUCTOR_TRANSLATION *
            BELT_TRANSLATION;

    private static final int    H_NEGATIVE  = 0;
    private static final double TRSHLD_BTRY = 11.0;

    boolean calibrated = false;
    int     ready =      ERROR;
    boolean tracing =    false;
    boolean batteryOk =  false;
    boolean hNegative =  false;

    private double hSteps = 0;
    private double vSteps = 0;

    Telescope(GPSService gps, MainActivity act)
    {
        pos = new Position(gps);
        this.act = act;

        Thread traceThread = new Thread(new Runnable() {
            public void run()
            {
                trace();
            }
        });
        traceThread.start();
    }

    Position getPos()
    {
        return pos;
    }

    void calibrate()
    {
        // The default calibration position is POLARIS, the first object in table
        calibrated = true;
        act.updateStatus();
    }

    void onMove(double ra, double dec)
    {
        pos.set(ra, dec);
        move();
    }

    private void setBatteryVoltage(double btryVoltage)
    {
        if (btryVoltage < TRSHLD_BTRY) {
            batteryOk = false;
            setReady(ERROR);
        } else {
            batteryOk = true;
            setReady(OK);
        }
    }

    private void move()
    {
        double dif_az;
        double dif_hi;
        double azimuth_tmp;
        double height_tmp;
        int cur_h_steps;
        int cur_v_steps;

        azimuth_tmp = pos.az;
        height_tmp = pos.h;

        pos.RaDec2AltAz();
        dif_az = pos.az - azimuth_tmp;
        dif_hi = pos.h  - height_tmp;

        hSteps += (dif_az * K) / 360.0;
        vSteps += (dif_hi * K) / 360.0;

        if ((Math.abs(hSteps) >= PRECISION) || (Math.abs(vSteps) >= PRECISION)) {
            if (ready != ERROR) {
                cur_h_steps = (int) hSteps;
                cur_v_steps = (int) vSteps;
                hSteps -= cur_h_steps;
                vSteps -= cur_v_steps;

                if (pos.h <= H_NEGATIVE)
                    act.getCtr().inMsgProcess(Control.ALT_NEGATIVE);
                else {
                    act.getCtr().inMsgProcess(Control.ALT_POSITIVE);
                    act.getCtr().outMessage(Control.MOVE,
                            String.valueOf(cur_h_steps), String.valueOf(cur_v_steps));
                }
            }
        }
    }

    private void trace()
    {
        while (true) {
            try {
                if (tracing) move();
                Thread.sleep(1000);
            } catch (InterruptedException e) {break;}
        }
    }

    void batteryVoltage(String recMsg)
    {
        String batteryVoltage = recMsg.substring(
                2 + Control.MSG_BATTERY_RES.length(), recMsg.length() - 2);
        setBatteryVoltage(Double.valueOf(batteryVoltage));
        act.updateStatus();
    }

    void setTrace(boolean val)
    {
        tracing = val;
        act.updateStatus();
    }

    void setReady(int val)
    {
        ready = val;
        act.updateStatus();
    }

    void setHNegative(boolean val)
    {
        hNegative = val;
        act.updateStatus();
    }

    void setBatteryOk(boolean val)
    {
        this.batteryOk = val;
        act.updateStatus();
    }
}
