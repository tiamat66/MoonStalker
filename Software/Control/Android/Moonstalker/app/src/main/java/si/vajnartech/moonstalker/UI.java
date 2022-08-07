package si.vajnartech.moonstalker;

import android.os.Bundle;

import java.text.DecimalFormat;

import androidx.drawerlayout.widget.DrawerLayout;


public class UI implements UpdateUI
{
  MainActivity act;

  Bundle modes = new Bundle();

  public UI(MainActivity act)
  {
    this.act = act;
  }

  public void updateUI(int telescopeStatus, int telescopeMode)
  {
    act.runOnUiThread(() -> {
      switch (telescopeStatus) {
      case C.ST_CONNECTED:
        updateMessage(act.tx(R.string.connected));
        updateMessageColor(R.color.colorAccent2);
        break;
      case C.ST_READY:
        if (telescopeMode == C.ST_MOVE_TO_OBJECT) {
          stMoveToObject();
          updateMessage(act.tx(R.string.select_object));
          updateMessageColor(R.color.colorBase);
          showFab(true);
        } else if (telescopeMode == C.ST_CALIBRATED) {
          stCalibrated();
          showFab(false);
        } else if (telescopeMode == C.ST_CALIBRATING) {
          updateMessage(act.tx(R.string.calibrating));
          updateMessageColor(R.color.colorNeutral);
          showFab(true);
        } else {
          stReady();
          updateMessage(act.tx(R.string.ready));
          updateMessageColor(R.color.colorOk);
        }
        updateSideDrawer();
        break;
      case C.ST_CONNECTING:
        updateMessage(act.tx(R.string.connecting));
        updateMessageColor(R.color.colorNeutral);
        showFab(false);
        break;
      case C.ST_NOT_CONNECTED:
        updateMessage(act.tx(R.string.not_connected));
        updateMessageColor(R.color.colorError);
        showFab(true);
        break;
      case C.ST_INIT:
        updateMessage(act.tx(R.string.initializing));
        updateMessageColor(R.color.colorNeutral);
      }
    });
  }

  private void stReady()
  {
    modes.putBoolean("calibration", true);
    modes.putBoolean("manual", true);
    modes.putBoolean("trace", false);
    modes.putBoolean("move", false);
  }

  private void stCalibrated()
  {
    modes.putBoolean("calibration", false);
    modes.putBoolean("manual", true);
    modes.putBoolean("trace", true);
    modes.putBoolean("move", true);
  }

  private void stMoveToObject()
  {
    modes.putBoolean("calibration", false);
    modes.putBoolean("manual", true);
    modes.putBoolean("trace", true);
    modes.putBoolean("move", false);
  }

  private String formatPositionString(double azimuth, double height, SkyObject.Coordinates object)
  {
    DecimalFormat df = new DecimalFormat("000.00");
    String        az = "A:" + df.format(azimuth);
    String        h  = "H:" + df.format(height);

    return String.format("%s (%s)\n%s | %s", object.name, object.constellation, az, h);
  }

  @Override
  public void updateMessage(String msg)
  {
    act.updateMessage(msg);
  }

  @Override
  public void updateMessageColor(int color)
  {
    act.updateMessageColor(color);
  }

  @Override public void updateSideDrawer()
  {
    act.menu.findItem(R.id.calibrate).setEnabled(modes.getBoolean("calibration"));
    act.menu.findItem(R.id.manual).setEnabled(modes.getBoolean("manual"));
    act.menu.findItem(R.id.track).setEnabled(modes.getBoolean("trace"));
    act.menu.findItem(R.id.move).setEnabled(modes.getBoolean("move"));
    act.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
  }

  @Override
  public void setPositionString(int color, SkyObject skyObject)
  {
    updateMessage(formatPositionString(skyObject.getAzimuth(), skyObject.getHeight(), skyObject.get()));
    updateMessageColor(color);
  }

  @Override
  public void showFab(boolean show)
  {
    act.showFab(show);
  }
}

