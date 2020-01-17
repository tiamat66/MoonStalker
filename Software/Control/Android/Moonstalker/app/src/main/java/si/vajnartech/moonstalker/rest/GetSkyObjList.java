package si.vajnartech.moonstalker.rest;

import android.util.Log;

import java.util.List;

public class GetSkyObjList extends REST<SkyObjList>
{
  public GetSkyObjList(int objType)
  {
    super(String.format(REST.GET_OBJECTS, objType), "vajnar", "vajnar", REST.SERVER_ADDRESS, new OnFail() {
      @Override public void execute()
      {
        Log.i("IZAA", "Something went wrong...................................................");
      }
    });
  }

  @Override
  SkyObjList backgroundFunc()
  {
    return callServer(null, REST.OUTPUT_TYPE_JSON);
  }

  protected void onPostExecute(SkyObjList j)
  {
    super.onPostExecute(j);

    if (j != null)
      for (String s: j.list)
        Log.i("IZAA", "*=" + s);
  }
}

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
class SkyObjList
{
  public List<String> list;

  @Override
  public String toString()
  {
    return "SkyObjList{" +
           "list=" + list +
           '}';
  }
}
