package si.vajnartech.moonstalker;

import java.util.UUID;

@SuppressWarnings("unused")
public final class C
{
  public static final String TAG = "PEPE";
  public static final String CALIBRATOR = "Polaris";

  static final int TRSHLD_BTRY = 11000;  // milivolts

  static UUID token = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  static final int MINIMUM_TIME = 10000;  // 10s
  static final int MINIMUM_DISTANCE = 50; // 50m

  public static final double DEF_LONGITUDE = 13.82;
  public static final double DEF_LATITUDE = 46.45;

  // Named of paired BT device which acts like telescope
  static final String SERVER_NAME = "Naprava A32 uporabnika Zoran";

  // Mechanical characteristics
  private static final double MOTOR_STEPS_NUM      = 200.0;
  private static final double REDUCTOR_TRANSLATION = 30.0;
  private static final double BELT_TRANSLATION     = 48.0 / 14.0;
  static final         double K                    = MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION;

  // Telescope status values

  // Telescope mode values
  static final int MD_TRACING =  1;
  public static final int MD_MOVING =   2;
  public static final int MD_NOT_CAL =  3;
  public static final int MD_CALIBRATING   = 4;
  public static final int MD_MANUAL   = 5;
  public static final int MD_CALIBRATED   = 6;
  public static final int MD_MOVE_TO_OBJECT = 7;
  public static final int MD_NOT_CALIBRATED   = 8;

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
  static final String NONE = "NONE";
  static final String CLEAR = "";
  static boolean monitoring = false;
  static boolean mStatus = false;
  static String  curMessage = "";
}



