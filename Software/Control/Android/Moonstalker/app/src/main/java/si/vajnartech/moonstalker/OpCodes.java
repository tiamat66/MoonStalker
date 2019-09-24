package si.vajnartech.moonstalker;

final class OpCodes
{
  static final int OUT_MSG = 1;
  static final int IN_MSG = 2;

  // in messages
  static final String READY   = "RDY";
  static final String BATTERY = "BTRY";
  static final String ERROR   = "FATAL_ERROR";
  static final String INIT    = "INIT";

  static final String NOT_READY    = "NOT_RDY";
  static final String MOVE_ACK    = "MV_ACK";

  // out messages
  static final String MOVE_START  = "MVS";
  static final String MOVE_STOP   = "MVE";
  static final String MOVE        = "MV";
  static final String GET_STATUS  = "MVST?";
  static final String GET_BATTERY = "BTRY?";
}
