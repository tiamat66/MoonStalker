package si.vajnartech.moonstalker;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.drawerlayout.widget.DrawerLayout;


public class UI implements UpdateUI
{
  MainActivity act;

  Bundle modes = new Bundle();

  public UI(MainActivity act)
  {
    this.act = act;
  }

  public void updateStatus(FloatingActionButton fab)
  {
    act.runOnUiThread(() -> {
      if (act.currentFragment instanceof ManualFragment) {
        ManualFragment frag = (ManualFragment) act.currentFragment;
        frag.updateArrows();
      }

      if (TelescopeStatus.get() == C.ST_READY)
        act.updateFab(R.color.colorOk2, fab);
      else if (TelescopeStatus.get() == C.ST_MOVING)
        act.updateFab(R.color.colorMoving, fab);

      if (TelescopeStatus.get() == C.ST_MOVING)
        update(String.format("%s: %s", act.tx(R.string.moving), TelescopeStatus.getMisc()));
      else if (TelescopeStatus.get() == C.ST_NOT_READY)
        update(R.string.not_ready);
      else if (TelescopeStatus.getMode() == C.ST_TRACING)
        update(R.string.tracing);
      else if (TelescopeStatus.getMode() == C.ST_MOVE_TO_OBJECT) {
        stMoveToObject();
        update(R.string.ready, modes);
      } else if (TelescopeStatus.getMode() == C.ST_MANUAL)
        update(R.string.manual);
      else if (TelescopeStatus.getMode() == C.ST_CALIBRATED) {
        stCalibrated();
        update(R.string.calibrated, modes);
      } else if (TelescopeStatus.getMode() == C.ST_CALIBRATING)
        update(R.string.calibrating);
      else if (TelescopeStatus.get() == C.ST_READY) {
        stReady();
        update(R.string.ready, modes);
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

  @Override
  public void update(String title)
  {
    if (title != null)
      act.terminal.setText(title);
  }

  @Override
  public void update(Integer title)
  {
    if (title != null)
      act.terminal.setText(act.tx(title));
  }

  @Override
  public void update(Integer title, Bundle modes)
  {
    if (title != null) {
      act.terminal.setText(act.tx(title));
      if (title == R.string.calibrated)
        act.setPositionString();
    }
    act.menu.findItem(R.id.calibrate).setEnabled(modes.getBoolean("calibration"));
    act.menu.findItem(R.id.manual).setEnabled(modes.getBoolean("manual"));
    act.menu.findItem(R.id.track).setEnabled(modes.getBoolean("trace"));
    act.menu.findItem(R.id.move).setEnabled(modes.getBoolean("move"));
    act.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
  }
}
