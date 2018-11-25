package com.robic.zoran.moonstalker.rest;

import android.os.Bundle;
import android.util.Log;

import com.robic.zoran.moonstalker.Control;
import com.robic.zoran.moonstalker.MainActivity;
import com.robic.zoran.moonstalker.R;

public class Drive extends REST<Response>
{
  private Control     ctrl;
  private Instruction opCode;

  public Drive(String url, MainActivity act, Control ctrl, Instruction opCode)
  {
    super(url, act);
    this.ctrl = ctrl;
    this.opCode = opCode;
  }

  @Override Response backgroundFunc()
  {
    return deusExMachina(opCode);
  }

  @Override void fail(int responseCode)
  {
    Log.i(TAG, "fail, response code is " + responseCode);
    act.showAlert(act.getResources().getString(R.string.no_conn), 4000, new Runnable()
    {
      @Override public void run()
      {
        errorExit();
      }
    });
  }

  private void process(Response j)
  {
    switch (j.opCode) {
    case OpCodes.RDY:
      Log.i(TAG, "processing RDY from response ");
      ctrl.inMsgProcess(j.opCode, null);
      break;
    case OpCodes.BTRY:
      Log.i(TAG, "processing BTRY from response with p1 = " + j.p1);
      Bundle b = new Bundle();
      b.putFloat("p1", Float.parseFloat(j.p1));
      ctrl.inMsgProcess(j.opCode, b);
      break;
    default:
      Log.i(TAG, "unknown response received");
    }
  }

  @Override
  protected void onPostExecute(Response j)
  {
    ctrl.release();
    if (j != null) {
      Log.i(TAG, "on post execute OK");
      process(j);
    } else {
      Log.i(TAG, "on post execute ERROR");
    }
    Log.i("IZAA", "release socket");
  }

  public static class OpCodes
  {
    public final static int ST   = 1;
    public final static int RDY  = 2;
    public final static int BTRY = 3;
    public final static int MOVE = 7;
  }
}

@SuppressWarnings("WeakerAccess") class Response
{
  public int    opCode;
  public String p1;
  public String p2;

  @Override
  public String toString()
  {
    return "Response{" +
           "opCode=" + opCode +
           ", p1=" + p1 +
           ", p2=" + p2 +
           '}';
  }
}
