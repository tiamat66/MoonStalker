package si.vajnartech.moonstalker;

import androidx.fragment.app.DialogFragment;

public abstract class MyFragment extends DialogFragment
{
  MainActivity act;

  public static <T extends MyFragment> T instantiate(Class<T> cls, MainActivity act)
  {
    T res = null;
    try {
      res = cls.newInstance();
      res.act = act;
    } catch (java.lang.InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return res;
  }
}
