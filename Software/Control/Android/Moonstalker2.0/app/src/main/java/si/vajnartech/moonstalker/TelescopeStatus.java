package si.vajnartech.moonstalker;

import android.util.Log;

import static si.vajnartech.moonstalker.C.ST_BTRY_LOW;
import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.TRSHLD_BTRY;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
final class TelescopeStatus
{
  private static float  btryVoltage = -1;
  private static int status = -1;
  private static int mode = -1;
  private static String error = "";

  static void setBatteryVoltage(float voltage)
  {
    btryVoltage = voltage;
    if (btryVoltage < TRSHLD_BTRY)
      set(ST_BTRY_LOW);
    Log.i("STATUS", "BTR=" + getBtryVoltage());
  }

  static float getBtryVoltage()
  {
    return btryVoltage;
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
