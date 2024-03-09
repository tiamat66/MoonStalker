package si.vajnartech.moonstalker;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.TAG;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"}) final public class TelescopeStatus
{
  private static int btryVoltage = -1;

  private static int    status = -1;

  private static String   status1;
  private static int    mode   = -1;
  private static String error  = "";
  private static String ack = "";
  private static String misc = "";

  private static final AtomicBoolean lck = new AtomicBoolean(false);

  static void lock()
  {
    lck.set(true);
  }

  static void unlock()
  {
    lck.set(false);
  }

  static boolean locked()
  {
    return lck.get();
  }

  static void setBatteryVoltage(int voltage)
  {
    btryVoltage = voltage;
    Log.i(TAG, "Battery=" + btryVoltage);
  }

  static String getAck()
  {
    return ack;
  }

  static void setAck(String val)
  {
    ack = val;
  }

  static void setMode(int m)
  {
    mode = m;
  }

  static int getMode()
  {
    return mode;
  }

  public static void set(int st)
  {
    status = st;
    Log.i("STATUS", "ST=" + get());
  }

  public static void set(String st)
  {
    status1 = st;
  }

  static int get()
  {
    return status;
  }

  static String getError()
  {
    return error;
  }

  static void setError(String e)
  {
    set(ST_ERROR);
    error = e;
    Log.i("STATUS", "ERR=" + getError());
  }

  static void setMisc(String val)
  {
    misc = val;
  }

  static String getMisc()
  {
    return misc;
  }
}
