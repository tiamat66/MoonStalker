package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends MyFragment
{
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
