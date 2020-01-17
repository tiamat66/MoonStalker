package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import si.vajnartech.moonstalker.rest.GetSkyObjList;
import si.vajnartech.moonstalker.rest.GetStarInfo;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static si.vajnartech.moonstalker.C.SERVER_NAME;
import static si.vajnartech.moonstalker.C.ST_CALIBRATED;
import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_MANUAL;
import static si.vajnartech.moonstalker.C.ST_MOVE_TO_OBJECT;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_TRACING;
import static si.vajnartech.moonstalker.C.calObj;
import static si.vajnartech.moonstalker.C.curObj;

// V 'inicializiram' se zatakne
// kako dolociti max speed
// preveri battery send/response

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
  MyFragment currentFragment = null;
  Control    ctrl;
  Menu       menu;

  TerminalWindow terminal;
  Monitor        monitor;
  ImageView      statusLight;

  @SuppressLint("InflateParams") @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    statusLight = findViewById(R.id.status_light);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("");
    C.curMessage = tx(R.string.not_connected);

    SharedPref.setDefault("device_name", SERVER_NAME);
    SharedPref.setDefault("calibration_obj", calObj);
    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener()
    {
      @SuppressWarnings("SameParameterValue")
      void update (int newStat, int newMode)
      {
        TelescopeStatus.setMode(newMode);
        TelescopeStatus.set(newStat);
      }

      @Override
      public void onClick(View view)
      {
        if (currentFragment instanceof SettingsFragment) {
          setFragment("main", MainFragment.class, new Bundle());
        }
        else if (TelescopeStatus.get() == ST_NOT_CONNECTED) {
          connect(true);
        }
        else if (TelescopeStatus.getMode() == ST_MANUAL) {
          update(ST_READY, ST_READY);
          setFragment("main", MainFragment.class, new Bundle());
        }
        else if (TelescopeStatus.getMode() == ST_TRACING) {
          update(ST_READY, ST_MOVE_TO_OBJECT);
        }
        else if (TelescopeStatus.getMode() == ST_CALIBRATING) {
          ctrl.calibrate();
          update(ST_READY, ST_CALIBRATED);
          setFragment("main", MainFragment.class, new Bundle());
        }
        else if (TelescopeStatus.get() == ST_READY && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
          ctrl.move(C.curObj);
        }
      }
    });

    final DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    menu = navigationView.getMenu();
    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    setFragment("main", MainFragment.class, new Bundle());

    // init terminal window
    terminal = new TerminalWindow(this);
    LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    monitor = new Monitor(inflater.inflate(R.layout.frag_monitor, null, false));
    monitor.update("$ ");

    // init astro database
    SelectFragment.initAstroObjDatabase(this);
    final MainActivity ctx = this;
    // start state machine
    new StatusSM(new Nucleus()
    {
      @Override
      public void initTelescope()
      {
        initControl();
      }

      @SuppressWarnings("SameParameterValue")
      void update(Integer title, Integer icon, boolean ca, boolean ma, boolean tr, boolean mo)
      {
        if (title != null)
          terminal.setText(tx(title));
        if (icon != null)
          statusLight.setImageDrawable(getResources().getDrawable(icon));
        menu.findItem(R.id.calibrate).setEnabled(ca);
        menu.findItem(R.id.manual).setEnabled(ma);
        menu.findItem(R.id.track).setEnabled(tr);
        menu.findItem(R.id.move).setEnabled(mo);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
      }

      @SuppressWarnings("SameParameterValue")
      void update(Integer title, Integer icon)
      {
        if (title != null)
          terminal.setText(tx(title));
        if (icon != null)
          statusLight.setImageDrawable(getResources().getDrawable(icon));
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
      }

      @Override
      public void updateStatus()
      {
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (TelescopeStatus.getMode() == ST_TRACING) {
              terminal.setBackgroundColor(getResources().getColor(R.color.colorAccent2));
              update(R.string.tracing, R.drawable.ic_mv_s);
            } else if (TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
              update(R.string.ready, R.drawable.ic_ok_s, false, true, true, false);
              terminal.setBackgroundColor(getResources().getColor(R.color.colorOk2));
              if (currentFragment instanceof SelectFragment)
                ((SelectFragment) currentFragment).setPositionString();
            } else if (TelescopeStatus.getMode() == ST_MANUAL) {
              update(R.string.ready, R.drawable.ic_ok_s);
              terminal.setText(tx(R.string.manual));
            } else if (TelescopeStatus.getMode() == ST_CALIBRATED) {
              update(R.string.calibrated, R.drawable.ic_ok_s, false, true, true, true);
            } else if (TelescopeStatus.getMode() == ST_CALIBRATING) {
              update(R.string.calibrating, R.drawable.ic_cal_s);
            } else if (TelescopeStatus.get() == ST_READY) {
              if (TelescopeStatus.getMode() != ST_TRACING)
                update(R.string.ready, R.drawable.ic_ok_s, true, true, false, false);
            }
          }
        });
      }

      @Override
      public void startProgress(ProgressType pt)
      {
        pOn(pt);
      }

      @Override
      public void stopProgress()
      {
        pOff();
      }

      @Override
      public void move()
      {
        ctrl.move(curObj);
        runOnUiThread(new Runnable()
        {
          @Override public void run()
          {
            ((SelectFragment) currentFragment).setPositionString();
          }
        });
      }

      @Override
      public void dump(final String str)
      {
        runOnUiThread(new Runnable() {
          @Override public void run()
          {
            monitor.update(str);
          }
        });
      }
    });
    // init current astro object
    new GetStarInfo(C.calObj, null);
    connect(false);

    new GetSkyObjList(3);
  }

  private void connect(boolean exe)
  {
    BlueTooth b = new BlueTooth(new SharedPref(this).getString("device_name"), this, new BTInterface()
    {
      @Override
      public void exit(String msg)
      {
        myMessage(msg);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        finish();
      }

      @Override
      public void progressOn()
      {
        pOn(ProgressType.CONNECTING);
      }

      @Override
      public void progressOff()
      {
        pOff();
      }

      @Override
      public void onOk()
      {
        runOnUiThread(new Runnable()
        {
          public void run()
          {
            Toast.makeText(MainActivity.this, tx(R.string.connected), Toast.LENGTH_SHORT).show();
          }
        });
      }

      @Override
      public void onError()
      {
        myMessage(tx(R.string.connection_failed));
      }
    });

    if (exe)
      b.executeOnExecutor(THREAD_POOL_EXECUTOR);
  }

  private void promptToCalibration()
  {
    runOnUiThread(new Runnable()
    {
      @Override public void run()
      {
        myMessage(tx(R.string.calibration_ntfy));
      }
    });
  }

  private void initControl()
  {
    runOnUiThread(new Runnable()
    {
      public void run()
      {
        ctrl = new Control(MainActivity.this);
        ctrl.init();
      }
    });
  }

  @Override
  public void onBackPressed()
  {
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      setFragment("settings", SettingsFragment.class, new Bundle());
      return true;
    }
    else if(id == R.id.action_monitor) {
      if (!C.monitoring) {
        C.monitoring = true;
        monitor.showAtLocation(this.findViewById(R.id.content), Gravity.BOTTOM | Gravity.START, 0, 0);
      }
      else {
        monitor.dismiss();
        C.monitoring = false;
      }
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onNavigationItemSelected(MenuItem item)
  {
    int id = item.getItemId();

    if (id == R.id.move) {
      if (TelescopeStatus.get() != ST_READY)
        Snackbar.make(currentFragment.getView(), tx(R.string.not_calibrated), Snackbar.LENGTH_LONG)
            .setAction("Calibrated", null).show();
      else {
        TelescopeStatus.setMode(ST_MOVE_TO_OBJECT);
        setFragment("move", SelectFragment.class, new Bundle());
      }
    } else if (id == R.id.track) {
      if (TelescopeStatus.get() != ST_READY)
        Snackbar.make(currentFragment.getView(), tx(R.string.not_calibrated), Snackbar.LENGTH_LONG)
            .setAction("Calibrated", null).show();
      else {
        TelescopeStatus.setMode(ST_TRACING);
        if (!(currentFragment instanceof SelectFragment)) {
          setFragment("move", SelectFragment.class, new Bundle());
        }
      }
    } else if (id == R.id.calibrate) {
      TelescopeStatus.setMode(ST_CALIBRATING);
      setFragment("manual", ManualFragment.class, new Bundle());
      promptToCalibration();
    } else if (id == R.id.manual) {
      if (TelescopeStatus.getMode() != ST_CALIBRATED && TelescopeStatus.getMode() != ST_MOVE_TO_OBJECT) {
        setFragment("manual control", ManualFragment.class, new Bundle());
        TelescopeStatus.setMode(ST_MANUAL);
      }
      else
        myMessage(tx(R.string.to_manual_move), new Runnable()
        {
          @Override public void run()
          {
            setFragment("manual control", ManualFragment.class, new Bundle());
            TelescopeStatus.setMode(ST_MANUAL);
          }
        });
    }
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  void myMessage(final String msg)
  {
    runOnUiThread(new Runnable()
    {
      public void run()
      {

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(tx(R.string.warning));
        alertDialog.setMessage(msg);
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL, "OK",
            new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                dialog.dismiss();
              }
            });
        alertDialog.show();
      }
    });
  }

  void myMessage(final String msg, final Runnable action)
  {
    runOnUiThread(new Runnable()
    {
      public void run()
      {

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(tx(R.string.warning));
        alertDialog.setMessage(msg);
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, tx(R.string.ok),
            new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialog, int which)
              {
                action.run();
              }
            });
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, tx(android.R.string.cancel),
            new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialog, int which)
              {
                dialog.dismiss();
              }
            });
        alertDialog.show();
      }
    });
  }

  public String tx(int stringId, Object... formatArgs)
  {
    if (formatArgs.length > 0)
      return getString(stringId, formatArgs);
    return getString(stringId);
  }

  /***********************************************************************************************
   *
   * Progress indicator section
   *
   ***********************************************************************************************/
  public LinearLayout ll_progress = null;

  public enum ProgressType
  {
    CONNECTING,
    INITIALIZING,
    MOVING
  }

  public void progressOn(ProgressType type)
  {
    ProgressBar progress = new ProgressBar(this);
    LinearLayout.LayoutParams lp_progress = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp_progress.gravity = Gravity.CENTER;
    lp_progress.weight = 0;

    progress.setLayoutParams(lp_progress);

    int color = getResources().getColor(R.color.colorAccent);
    if (progress.getIndeterminateDrawable() != null)
      progress.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

    TextView loadingText = new TextView(this);
    LinearLayout.LayoutParams lp_loading = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp_loading.weight = 0;
    lp_loading.gravity = Gravity.CENTER;
    if (type == ProgressType.CONNECTING)
      loadingText.setText(tx(R.string.connecting));
    else if (type == ProgressType.INITIALIZING)
      loadingText.setText(tx(R.string.initializing));
    else if (type == ProgressType.MOVING)
      loadingText.setText(tx(R.string.moving));

    loadingText.setTextColor(getResources().getColor(R.color.colorAccent));
    loadingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
    loadingText.setLayoutParams(lp_loading);

    ll_progress = new LinearLayout(this);
    ll_progress.setOrientation(LinearLayout.VERTICAL);
    ll_progress.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    ll_progress.addView(progress, lp_progress);
    ll_progress.addView(loadingText, lp_loading);
    final FrameLayout.LayoutParams lp_ll = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    );
    lp_ll.gravity = Gravity.CENTER;
    ll_progress.setLayoutParams(lp_ll);
    runOnUiThread(new Runnable()
    {
      @Override public void run()
      {
        ((ConstraintLayout) findViewById(R.id.content)).addView(ll_progress, lp_ll);
      }
    });
  }

  public void pOn(ProgressType type)
  {
    progressOn(type);
  }

  public void pOff()
  {
    if (ll_progress != null) {
      runOnUiThread(new Runnable()
      {
        @Override public void run()
        {
          ((ConstraintLayout) findViewById(R.id.content)).removeView(ll_progress);
          ll_progress = null;
        }
      });
    }
  }

  /***********************************************************************************************
   *
   * Fragment section
   *
   ***********************************************************************************************/

  private MyFragment createFragment(String tag, Class<? extends MyFragment> cls, Bundle params)
  {
    MyFragment frag;
    frag = (MyFragment) getSupportFragmentManager().findFragmentByTag(tag);
    if (frag == null && cls != null) try {
      frag = MyFragment.instantiate(cls, this);
      frag.setArguments(params);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return frag;
  }

  public void setFragment(String tag, Class<? extends MyFragment> cls, Bundle params)
  {
    currentFragment = createFragment(tag, cls, params);
    if (currentFragment == null) return;

    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.content, currentFragment);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  public void refreshCurrentFragment()
  {
    MyFragment          f           = currentFragment;
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.detach(f);
    transaction.commit();

    transaction = getSupportFragmentManager().beginTransaction();
    transaction.attach(f);
    transaction.commit();
  }
}

