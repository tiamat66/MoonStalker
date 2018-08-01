package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.robic.zoran.moonstalker.rest.Drive;
import com.robic.zoran.moonstalker.rest.LoginBlueTooth;

public class MainActivity extends AppCompatActivity
{
  private static final String  TAG       = "IZAA";
  private static final boolean BLUETOOTH = true;
  private static final boolean GPS       = true;

  private Telescope  t;
  private Control    ctr;
  private GPSService gps;

  VajnarFragment curentFragment = null;

  MsView3D view3D;
  AstroObject curObj = new AstroObject();

  Control getCtr()
  {
    return ctr;
  }

  GPSService getGps()
  {
    return gps;
  }

  Telescope getTelescope()
  {
    return t;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // TODO: Text v string it to gre v calibration screen
    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    assert inflater != null;
    @SuppressLint("InflateParams")
    LinearLayout root = (LinearLayout) inflater.inflate(R.layout.content_main, null);

    if (BLUETOOTH)
      if (GPS)
        gps = new GPSService(this);
    t = new Telescope(gps, this);
    ctr = new Control(t, this);
    new MSDialog(this);

    view3D = new MsView3D(this);
    setContentView(root);

    setFragment("main", MainFragment.class, new Bundle());
  }

  @Override
  public void onResume()
  {
    super.onResume();
  }

  @Override
  public void onPause()
  {
    super.onPause();
  }

  public void errorExit()
  {
    finish();
    System.exit(0);
  }

  public VajnarFragment createFragment(String tag, Class<? extends VajnarFragment> cls,
                                       Bundle params)
  {
    VajnarFragment frag;
    frag = (VajnarFragment) getSupportFragmentManager().findFragmentByTag(tag);
    if (frag == null && cls != null) try {
      frag = VajnarFragment.instantiate(cls, this);
      frag.setArguments(params);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return frag;
  }

  public void setFragment(String tag, Class<? extends VajnarFragment> cls, Bundle params)
  {
    curentFragment = createFragment(tag, cls, params);
    if (curentFragment == null) return;

    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.container, curentFragment);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  public void showAlert(String msg, int to, Runnable run)
  {
    new MSDialog(this, msg, to, run);
  }
}

class MSDialog extends AlertDialog.Builder
{
  MSDialog(final MainActivity ctx, String msg, int to, Runnable run)
  {
    super(ctx);

    setTitle(msg);
    final AlertDialog dlg = create();
    show();

    if (run != null)
      new Handler().postDelayed(run, to);
    dlg.dismiss();
  }

  MSDialog(final MainActivity ctx)
  {
    super(ctx);

    setTitle(ctx.getResources().getString(R.string.conn_warn));
    setPositiveButton(R.string.ok,
                      new DialogInterface.OnClickListener()
                      {
                        @Override public void onClick(DialogInterface dialogInterface, int i)
                        {
                          ctx.getCtr().inMsgProcess(Control.INIT, null);
                        }
                      });
    setNegativeButton(R.string.cancel,
                      new DialogInterface.OnClickListener()
                      {
                        @Override public void onClick(DialogInterface dialogInterface, int i)
                        {
                          ctx.errorExit();
                        }
                      });

    create();
    show();
  }
}
