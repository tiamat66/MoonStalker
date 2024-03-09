package si.vajnartech.moonstalker;

@SuppressWarnings("unused")
public final class OpCodes
{
  public static final int OUT_MSG = 1;
  public static final int IN_MSG  = 2;

  // in messages
  static final String WAITING = "NA";
  static final String INIT    = "INIT";

  static final String READY     = "RDY";      // <RDY> ob MVST
  static final String BATTERY   = "BTRY";     // <BTRY voltage> v= voltage in millivolts
  static final String ERROR     = "ERROR";    // <ERROR error_msg>
  static final String WARNING   = "WARNING";  // <WARNING warning_msg>
  static final String INFO      = "INFO";     // <INFO info_msg>
  static final String NOT_READY = "NOT_RDY";  // <NOT_RDY> pri zasedenosti ob MV, MVS, ME, MVST
  static final String MOVE_ACK  = "MV_ACK";   // <MV_ACK a b s> potrditev ob MV
  static final String MVS_ACK   = "MVS_ACK";  // <MVS_ACK d s> potrditev ob MVS
  static final String MVE_ACK   = "MVE_ACK";  // <MVE_ACK> ob potrditvi ob MVE

  // out messages
  static final String MOVE_START  = "MVS";    // <MVS d s> d=direction (N, S, W, E, NW, NE, SW, SE); s=speed in RPM
  static final String MOVE_STOP   = "MVE";    // <MVE>
//  public static final String MOVE        = "MV";     // <MV a b s> a=h steps; b=v steps; s=max speed in RPM
  public static final String GET_STATUS  = "MVST?";  // <MVST?>
  static final String GET_BATTERY = "BTRY?";  // <BTRY?>

  // out messages
  public static final int MSG_CONNECT = 1;
  public static final int MSG_MOVE = 2;
  // in messages
  public static final int MSG_CONN_ERROR = 3;
  public static final int MSG_READY = 4;
  public static final int MSG_NOT_READY = 5;
  public static final int MSG_MV_ACK = 6;
  public static final int MSG_ERROR = 7;
  public static final int MSG_WARNING = 8;
  public static final int MSG_INFO = 9;
  public static final int MSG_BATTERY = 10;
  public static final int MSG_BATTERY_RES = 11;
  public static final int MSG_PING = 12;


}
