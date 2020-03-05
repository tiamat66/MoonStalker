package si.vajnartech.moonstalker;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;

public class MyContextWrapper extends ContextWrapper
{
  private SVGResources mResources;

  public MyContextWrapper(Context base)
  {
    super(base);
  }

  @Override
  public Resources getResources()
  {
    try {
      if (mResources == null)
        mResources = new SVGResources(super.getResources(), getPackageName());
    } catch (Exception e) {
      Log.i("IZAA", "Error: " + e);
    }
    return mResources;
  }
}
