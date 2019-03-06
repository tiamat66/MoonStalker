package si.vajnartech.moonstalker;

import java.util.UUID;

public class C
{
  public static final String TAG = "IZAABELA";

  public static UUID token = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  public static final int MINIMUM_TIME = 10000;  // 10s
  public static final int MINIMUM_DISTANCE = 50; // 50m

  // Named of paired BT device which acts like telescope
  public static final String SERVER_NAME = "Zoran Robič Tablca";

  // Mechanical characteristics
  public static final double MOTOR_STEPS_NUM      = 200.0;
  public static final double REDUCTOR_TRANSLATION = 30.0;
  public static final double BELT_TRANSLATION     = 48.0 / 14.0;
  public static final double K = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION;

  // Telescope status values
  public static final int ST_READY =    1;
  public static final int ST_ERROR =    2;
  public static final int ST_TRACING =  5;
  public static final int ST_MOVING =   6;
  public static final int ST_BTRY_LOW = 7;
  public static final int ST_NOT_CAL =  9;
  public static final int ST_NOT_CONNECTED = 10;
  public static final int ST_CONNECTING    = 11;
  public static final int ST_CONNECTED     = 12;

  public static AstroObject curObj = null;
}



