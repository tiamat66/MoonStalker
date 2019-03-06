package si.vajnartech.moonstalker.rest;


import si.vajnartech.moonstalker.MainActivity;

@SuppressWarnings("FieldCanBeLocal")
public abstract class GetSkyObjInfo
{
  protected String       name;
  protected MainActivity act;
  protected Object       adapter;


  GetSkyObjInfo(String name, MainActivity act, String URL)
  {
    HTTP.GetCompleteEvent ce = new HTTP.GetCompleteEvent()
    {
      @Override public void complete(HTTP http, String data)
      {
        process(data);
      }
    };
    this.name = name;
    this.act = act;
    new HTTP(URL + name, ce).executeOnExecutor(TPE.THREAD_POOL_EXECUTOR);
  }

  GetSkyObjInfo(MainActivity act, String URL)
  {
    HTTP.GetCompleteEvent ce = new HTTP.GetCompleteEvent()
    {
      @Override public void complete(HTTP http, String data)
      {
        process(data);
      }
    };
    this.act = act;
    new HTTP(URL, ce).executeOnExecutor(TPE.THREAD_POOL_EXECUTOR);
  }

  protected abstract void process(String data);
  protected abstract String parse(String txt, String start, String end);
  protected abstract String parse(String txt, String start);

  protected class Parser
  {
    private String txt, start, end;

    Parser(String txt, String start, String end)
    {
      this.txt = txt;
      this.start = start;
      this.end = end;
    }

    Parser(String txt, String start)
    {
      this.txt = txt;
      this.start = start;
    }

//    String parse()
//    {
//      String u = txt.replaceAll("\\s+", "");
//      String s = start.replaceAll("\\s+", "");
//
//      if (end != null) {
//        String e = end.replaceAll("\\s+", "");
//        if (u.contains(s) && u.contains(e)) {
//          return u.substring(u.indexOf(s), u.indexOf(e));
//        }
//      } else {
//        if (u.contains(s))
//          return u.substring(u.indexOf(s), u.length());
//      }
//      return "";
//    }


  }
}

