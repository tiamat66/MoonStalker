package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
  private static final String  TAG       = "IZAA";
  private static final boolean BLUETOOTH = true;
  private static final boolean GPS       = true;

  private DeviceIO         deviceIO;
  private Telescope        t;
  private BlueToothService bt;
  private Control          ctr;
  private GPSService       gps;

  VajnarFragment curentFragment = null;

  MsView3D    view3D;
  AstroObject curObj = new AstroObject();

  DeviceIO getDevice()
  {
    return deviceIO;
  }

  BlueToothService getBt()
  {
    return bt;
  }

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
      bt = new BlueToothService(this);
    if (GPS)
      gps = new GPSService(this);
    t = new Telescope(gps, this);
    ctr = new Control(t, this);
    deviceIO = new DeviceIO(this);
    deviceIO.start();
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


  public void connectionTimedOutMessage()
  {
    Toast.makeText(this, "BlueTooth connection to server timed out.\n" +
                         "Please check connection.", Toast.LENGTH_LONG).show();
  }

  public void connectionCanceled()
  {
    Toast.makeText(this, "Connection lost!", Toast.LENGTH_LONG).show();
  }

  void errorExit()
  {
    finish();
    System.exit(0);
  }

  public VajnarFragment createFragment(String tag, Class<? extends VajnarFragment> cls, Bundle params)
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
}

class MSDialog extends AlertDialog.Builder
{
  MSDialog(final MainActivity ctx, String msg, int to)
  {
    super(ctx);

    setTitle(msg);
    AlertDialog dlg = create();
    show();
    try {
      Thread.sleep(to);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    dlg.dismiss();
  }

  MSDialog(final MainActivity ctx)
  {
    super(ctx);

    setTitle(ctx.getResources().getString(R.string.conn_warn));
    setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                          @Override public void onClick(DialogInterface dialogInterface, int i)
                          {
                            ctx.getBt().connect();
                          }
                        });
    setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                          @Override public void onClick(DialogInterface dialogInterface, int i)
                          {
                            ctx.errorExit();
                          }
                        });

    create();
    show();
  }
}
