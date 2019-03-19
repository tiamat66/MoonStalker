package si.vajnartech.moonstalker;

import java.util.UUID;

public class C
{
  public static final String TAG = "IZAABELA";

  static final float TRSHLD_BTRY = 11.0f;

  static UUID token = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  static final int MINIMUM_TIME = 10000;  // 10s
  static final int MINIMUM_DISTANCE = 50; // 50m

  static final double DEF_LONGITUDE = 13.82;
  static final double DEF_LATITUDE = 46.45;

  // Named of paired BT device which acts like telescope
  static final String SERVER_NAME = "Zoran Robič Tablca";

  // Mechanical characteristics
  private static final double MOTOR_STEPS_NUM      = 200.0;
  private static final double REDUCTOR_TRANSLATION = 30.0;
  private static final double BELT_TRANSLATION     = 48.0 / 14.0;
  static final         double K                    = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION;

  // Telescope status values
  static final int ST_READY =    1;
  static final int ST_ERROR =    2;
  static final int ST_TRACING =  5;
  static final int ST_MOVING =   6;
  static final int ST_BTRY_LOW = 7;
  static final int ST_NOT_CAL =  9;
  static final int ST_NOT_CONNECTED = 10;
  static final int ST_CONNECTED     = 12;

  public static AstroObject curObj = null;
  public static String curConstellation = "";
  static String calObj = "Polaris";
  static String calConstellation = "Ursa Major";
}



