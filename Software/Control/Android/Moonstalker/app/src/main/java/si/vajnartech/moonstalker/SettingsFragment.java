package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class SettingsFragment extends MyFragment
{
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    final SharedPref p   = new SharedPref(act);
    View             res = inflater.inflate(R.layout.frag_settings, container, false);
    EditText         et  = res.findViewById(R.id.device_name);
    et.setText(p.getString("device_name"));
    et.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after)
      {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {}

      @Override
      public void afterTextChanged(Editable s)
      {
        p.put("device_name", s.toString());
      }
    });

    et = res.findViewById(R.id.calibration_object);
    et.setText(p.getString("calibration_obj"));
    et.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after)
      {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {}

      @Override
      public void afterTextChanged(Editable s)
      {
        p.put("calibration_obj", s.toString());
      }
    });
    return res;
  }
}
