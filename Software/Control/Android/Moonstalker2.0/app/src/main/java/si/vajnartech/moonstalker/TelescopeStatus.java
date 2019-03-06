package si.vajnartech.moonstalker;

import android.os.Bundle;

import static si.vajnartech.moonstalker.C.ST_BTRY_LOW;
import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.TRSHLD_BTRY;

final class TelescopeStatus
{
  private static float  btryVoltage;
  private static Bundle status = new Bundle();

  static void setBatteryVoltage(float voltage)
  {
    btryVoltage = voltage;
    if (voltage < TRSHLD_BTRY)
      set(ST_BTRY_LOW);
  }

  static float getBtryVoltage()
  {
    return btryVoltage;
  }

  static void set(int st)
  {
    status.putInt("st", st);
  }

  static int get()
  {
    return status.getInt("st");
  }

  static void setError(String e)
  {
    Bundle b = new Bundle();
    b.putInt("st", ST_ERROR);
    b.putString("arg1", e);
  }
}
