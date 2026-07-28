package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.main_layout, container, false);
  }
}
