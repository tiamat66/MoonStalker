package si.vajnartech.moonstalker.rest;


import android.widget.ArrayAdapter;

import si.vajnartech.moonstalker.SkyInterface;

@SuppressWarnings("FieldCanBeLocal")
public abstract class GetSkyObjInfo<T>
{
  protected String name;
  protected SkyInterface skyInterface;

  ArrayAdapter<T> adapter;

  GetSkyObjInfo(String name, String URL, SkyInterface skyInterface)
  {
    this(name, URL);
    this.skyInterface = skyInterface;
  }

  GetSkyObjInfo(String name, String URL)
  {
    HTTP.GetCompleteEvent ce = (http, data) -> process(data);
    this.name = name;
    new HTTP(URL + name, ce).executeOnExecutor(TPE.THREAD_POOL_EXECUTOR);
  }

  GetSkyObjInfo(String URL)
  {
    HTTP.GetCompleteEvent ce = (http, data) -> process(data);
    new HTTP(URL, ce).executeOnExecutor(TPE.THREAD_POOL_EXECUTOR);
  }

  protected abstract void process(String data);

  protected abstract String parse(String txt, String start, String end);

  protected abstract String parse(String txt, String start);
}

