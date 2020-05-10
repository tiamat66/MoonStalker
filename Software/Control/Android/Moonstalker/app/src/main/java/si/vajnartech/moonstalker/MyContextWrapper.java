package si.vajnartech.moonstalker;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

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
        mResources = new SVGResources(super.getResources());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return mResources;
  }
}
