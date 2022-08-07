package si.vajnartech.moonstalker;

@SuppressWarnings("unused")
public final class OpCodes
{
  static final int OUT_MSG = 1;
  static final int IN_MSG  = 2;

  // in messages
  static final String WAITING = "NA";
  static final String INIT    = "INIT";

  static final String READY     = "RDY";      // <RDY> ob MVST
  static final String BATTERY   = "BTRY";     // <BTRY voltage> v= voltage in millivolts
  static final String ERROR     = "ERROR";    // <ERROR error_msg>
  static final String WARNING   = "WARNING";  // <WARNING warning_msg>
  static final String INFO      = "INFO";     // <INFO info_msg>
  static final String NOT_READY = "NOT_RDY";  // <NOT_RDY> pri zasedenosti ob MV, MVS, ME, MVST
  public static final String MOVE_ACK  = "MV_ACK";   // <MV_ACK a b s> potrditev ob MV
  public static final String MVS_ACK   = "MVS_ACK";  // <MVS_ACK d s> potrditev ob MVS
  public static final String MVE_ACK   = "MVE_ACK";  // <MVE_ACK> ob potrditvi ob MVE

  // out messages
  static final String MOVE_START  = "MVS";    // <MVS d s> d=direction (N, S, W, E, NW, NE, SW, SE); s=speed in RPM
  static final String MOVE_STOP   = "MVE";    // <MVE>
  static final String MOVE        = "MV";     // <MV a b s> a=h steps; b=v steps; s=max speed in RPM
  static final String GET_STATUS  = "MVST?";  // <MVST?>
  static final String GET_BATTERY = "BTRY?";  // <BTRY?>
}
