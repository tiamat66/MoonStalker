package si.vajnartech.moonstalker;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;

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
      if (mResources == null) {
        mResources = new SVGResources(super.getResources(), getPackageName());
        SharedPref p           = new SharedPref(this);
        int        themeUse    = p.getInt("theme_use");
        int        themeBase   = p.getInt("theme_base");
        int        themeAccent = p.getInt("theme_accent");
        if (themeUse == 0 || themeUse == 1)
          mResources.setThemeBase(themeBase, themeAccent, themeUse == 0);
      }
    } catch (Exception e) {
      Log.i("IZAA", "Error: " + e);
    }
    return mResources;
  }
}

