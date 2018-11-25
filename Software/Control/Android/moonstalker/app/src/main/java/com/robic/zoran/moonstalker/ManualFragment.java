package com.robic.zoran.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

public class ManualFragment extends VajnarFragment
{
  CursorKeysBar cb;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_cal, container, false);
    cb = new CursorKeysBar(act, res);
    sb = new StatusBar(act, res);

    initAstroObjDropDown();
    return res;
  }

  @Override protected void setStatus(int status)
  {

  }

  @Override
  protected void updateButtons()
  {

  }
}

