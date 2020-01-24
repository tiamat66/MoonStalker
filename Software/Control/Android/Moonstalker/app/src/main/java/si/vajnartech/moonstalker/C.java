package si.vajnartech.moonstalker;

import java.util.UUID;

public final class C
{
  public static final String TAG = "IZAABELA";

  static final int TRSHLD_BTRY = 11000;  // milivolts

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
  static final int ST_CALIBRATING   = 13;
  static final int ST_MANUAL   = 14;
  static final int ST_MOVING_E = 15;
  static final int ST_MOVING_S = 16;
  static final int ST_CALIBRATED   = 17;
  static final int ST_MOVE_TO_OBJECT = 18;
  static final int ST_NOT_READY = 19;

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

//  public enum Directions
//  {
//    UP(1),
//    DOWN(2),
//    LEFT(3),
//    RIGHT(4),
//    NONE(5);
//
//    private final int value;
//    Directions(int value)
//    {
//      this.value = value;
//    }
//
//    public int getValue()
//    {
//      return value;
//    }
//  }

  public static String curConstellation = "";
  static String calObj = "Polaris";
  static String calConstellation = "Ursa Major";
  public static AstroObject curObj = new AstroObject(calObj, 0.0, 0.0, "", "");

  static boolean monitoring = false;
  static boolean mStatus = false;
  static String  curMessage = "";
}



