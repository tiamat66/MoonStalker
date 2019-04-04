package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainFragment extends MyFragment
{
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.content_main, container, false);
    res.findViewById(R.id.logo).setVisibility(View.VISIBLE);
    res.findViewById(R.id.msg_window).setVisibility(View.GONE);
    res.findViewById(R.id.sky_object).setVisibility(View.GONE);
    return res;
  }
}
