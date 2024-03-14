package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    View res = inflater.inflate(R.layout.content_main, container, false);
    TextView tv = res.findViewById(R.id.msg_window);
    tv.setVisibility(View.VISIBLE);
    tv.setText(C.curMessage);
    return res;
  }
}
