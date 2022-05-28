package si.vajnartech.moonstalker;

import java.util.UUID;

@SuppressWarnings("unused")
public final class C
{
  public static final String TAG = "MOONSTALKER";

  static final int TRSHLD_BTRY = 11000;  // milivolts

  static UUID token = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  static final int MINIMUM_TIME = 10000;  // 10s
  static final int MINIMUM_DISTANCE = 50; // 50m

  static final double DEF_LONGITUDE = 13.82;
  static final double DEF_LATITUDE = 46.45;

  // Named of paired BT device which acts like telescope
  static final String SERVER_NAME = "LG Zoran";

  // Mechanical characteristics
  private static final double MOTOR_STEPS_NUM      = 200.0;
  private static final double REDUCTOR_TRANSLATION = 30.0;
  private static final double BELT_TRANSLATION     = 48.0 / 14.0;
  static final         double K                    = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION;

  // Telescope status values
  public static final int ST_READY =    1;
  static final int ST_ERROR =    2;
  static final int ST_TRACING =  5;
  public static final int ST_MOVING =   6;
  static final int ST_BTRY_LOW = 7;
  static final int ST_NOT_CAL =  9;
  public static final int ST_NOT_CONNECTED = 10;
  public static final int ST_CONNECTED     = 12;
  static final int ST_CALIBRATING   = 13;
  static final int ST_MANUAL   = 14;
  static final int ST_CALIBRATED   = 17;
  static final int ST_MOVE_TO_OBJECT = 18;
  public static final int ST_NOT_READY = 19;
  public static final int ST_WAITING_ACK = 20;
  public static final int ST_INIT = 21;
  public static final int ST_CONNECTING = 22;

  // Triggers
  static final int ST_MOVING_S = 16;
  static final int ST_MOVING_E = 15;

  // Moving directions
  static final String N = "N";
  static final String E = "E";
  static final String S = "S";
  static final String W = "W";
  static final String NE = "NE";
  static final String SE = "SE";
  static final String SW = "SW";
  static final String NW = "NW";
  public static final String NONE = "NONE";
  public static final String CLEAR = "";

  public static String curConstellation = "";
  static String calObj = "Polaris";
  static String calConstellation = "Ursa Major";
  public static AstroObject curObj = new AstroObject(calObj, 0.0, 0.0, "", "");

  static boolean monitoring = false;
  public static boolean mStatus = false;
  static String  curMessage = "";
}



