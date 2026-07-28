package si.vajnartech.moonstalker;

// Actions and states
public final class OpCodes
{
    public static final int CONNECT = 1;
    public static final int MSG_MOVE = 2;
    public static final int MSG_CONN_ERROR = 3;
    public static final int READY = 4;
    public static final int NOT_READY = 5;
    public static final int MSG_MV_ACK = 6;
    public static final int MSG_ERROR = 7;
    public static final int MSG_WARNING = 8;
    public static final int MSG_INFO = 9;
    public static final int MSG_BATTERY = 10;
    public static final int MSG_BATTERY_RES = 11;
    public static final int GET_ASTRO_DATA = 13;
    public static final int CALIBRATED = 14;
    public static final int CALIBRATING = 15;
    public static final int MSG_POSITION = 16;
    public static final int MOVE_START = 17;
    public static final int MOVE_END = 18;
    public static final int MSG_MVS_ACK = 19;
    public static final int MSG_MVE_ACK = 20;
    public static final int MSG_CONN_TIMEOUT = 21;
    public static final int GOT_ASTRO_DATA = 22;
    public static final int CONNECTING = 23;
    public static final int CONNECTED = 24;
    public static final int ERROR = 25;
    public static final int MOVING = 26;
}
