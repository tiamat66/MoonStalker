package si.vajnartech.moonstalker;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import static si.vajnartech.moonstalker.C.*;
import static si.vajnartech.moonstalker.OpCodes.NA;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"}) final class TelescopeStatus
{
  private static int btryVoltage = -1;

  private static int    status = -1;
  private static int    mode   = -1;
  private static String error  = "";
  private static String ack = "";

  private static AtomicBoolean lck = new AtomicBoolean(false);

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

  static void set(int st)
  {
    status = st;
    Log.i("STATUS", "ST=" + get());
    if (st == ST_MOVING_S)
      setAck(NA);
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
