package si.vajnartech.moonstalker.rest;


import android.widget.ArrayAdapter;

@SuppressWarnings("FieldCanBeLocal")
public abstract class GetSkyObjInfo<T>
{
  // Interface for manipulating with Sky Data
  public interface SkyInterface
  {
    void updateConstellation();
  }

  protected String name;

  ArrayAdapter<T> adapter;


  GetSkyObjInfo(String name, String URL)
  {
    HTTP.GetCompleteEvent ce = (http, data) -> process(data);
    this.name = name;
    new HTTP(URL + name, ce).execute();
  }

  GetSkyObjInfo(String URL)
  {
    HTTP.GetCompleteEvent ce = (http, data) -> process(data);
    new HTTP(URL, ce).execute();
  }

  protected abstract void process(String data);

  protected abstract String parse(String txt, String start, String end);

  protected abstract String parse(String txt, String start);
}

