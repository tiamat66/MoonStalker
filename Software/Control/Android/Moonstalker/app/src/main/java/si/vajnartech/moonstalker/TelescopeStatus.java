package si.vajnartech.moonstalker;

import android.util.Log;

import static si.vajnartech.moonstalker.C.ST_BTRY_LOW;
import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.TRSHLD_BTRY;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "unused"})
final class TelescopeStatus
{
  private static int  btryVoltage = -1;
  private static int status = -1;
  private static int mode = -1;
  private static boolean lck = true;
  private static String error = "";

  static void lock()
  {
    lck = true;
  }

  static void unlock()
  {
    lck = false;
  }

  static boolean locked()
  {
    return lck;
  }

  static void setBatteryVoltage(int voltage)
  {
    btryVoltage = voltage;
  }

  static void setMode(int m)
  {
    mode = m;
  }

  static int getMode()
  {
    return mode;
  }

  static void set(int st)
  {
    status = st;
    Log.i("STATUS", "ST=" + get());
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
}
