package si.vajnartech.moonstalker;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.TAG;

@SuppressWarnings("FieldCanBeLocal")
public final class TelescopeStatus
{
  private static int btryVoltage = -1;

  private static int    status = -1;
  private static int    mode   = -1;
  private static String error  = "";
  private static String ack = "";
  private static String misc = "";

  private static final AtomicBoolean lck = new AtomicBoolean(false);

  public static void lock()
  {
    lck.set(true);
  }

  public static void unlock()
  {
    lck.set(false);
  }

  public static boolean locked()
  {
    return lck.get();
  }

  static void setBatteryVoltage(int voltage)
  {
    btryVoltage = voltage;
    Log.i(TAG, "Battery=" + btryVoltage);
  }

  public static String getAck()
  {
    return ack;
  }

  public static void setAck(String val)
  {
    ack = val;
  }

  public static void setMode(int m)
  {
    mode = m;
  }

  public static int getMode()
  {
    return mode;
  }

  public static void set(int st)
  {
    status = st;
    Log.i("STATUS", "ST=" + get());
  }

  public static int get()
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

  public static void setMisc(String val)
  {
    misc = val;
  }

  static String getMisc()
  {
    return misc;
  }
}
