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
        // TODO: vajnar.
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
