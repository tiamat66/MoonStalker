package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static si.vajnartech.moonstalker.C.SERVER_NAME;
import static si.vajnartech.moonstalker.C.ST_CALIBRATED;
import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_MANUAL;
import static si.vajnartech.moonstalker.C.ST_MOVE_TO_OBJECT;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_TRACING;
import static si.vajnartech.moonstalker.C.calObj;
// preveri ce faila sploh init bluetooth, naslednje ce faila connect, in potem se ce faila init telescope!!!
// ko ugasnem emulator nic kient ne zazna da se je kaj zgodilo, javi naj se prekinjena BT povezava
// pri rocnem premikanju kako narediti da ustavimo premikanje, sedaj je to finger up event, a se da v FAB?
// od zgornje postacke FAB rata moder ko premikamo in je kljukica dajmo rajsi krizec
// naredi premakni na, da bo delalo, in izgled
// dodaj v rocne komande tudi diagonalne premike in lepso slikco s puscicami krog!!!!
// ko je operabilen in naenkrat se prekine BT, connection lost
// ko je operabilen in naenkrat dobi error
// daj statuse v enum
// kako dolociti max speed
// delam na MVS/MVE in hendlanje responsev (acknowledges delajo!!!! preveri se NOT_READY),
// takoj postimat kako bo sploh tole premikanje zgledalo
// naredi samo eno opcijo rocno vodenje z moznostjo kalibracijenehal premikati se ni zgodil move end
// rocno vodenje, vcasih se puscica ugasne teleskop pa se ni
// select fragment popravi
// naj javi, da se ni mogel povezati s podatkovno bazo
// ko je teleskop kalibriran naj to vpise kot toast, v terminal window pa naj gre ime objekta?
// animacije
// astro data base

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
  FloatingActionButton fab;
  DrawerLayout         drawer;
  MyFragment           currentFragment = null;
  Control              ctrl;
  Menu                 menu;
  ProgressIndicator    progressIndicator;

  TerminalWindow terminal;
  Monitor        monitor;

  @SuppressLint("InflateParams")
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle("");
    C.curMessage = tx(R.string.not_connected);

    SharedPref.setDefault("device_name", SERVER_NAME);
    SharedPref.setDefault("calibration_obj", calObj);
    fab = findViewById(R.id.fab);
    updateFab(R.color.colorError, fab);
    fab.setOnClickListener(new View.OnClickListener()
    {
      @SuppressWarnings("SameParameterValue")
      void update(int newStat, int newMode)
      {
        TelescopeStatus.setMode(newMode);
        TelescopeStatus.set(newStat);
      }

      @Override
      public void onClick(View view)
      {
        if (TelescopeStatus.get() == ST_MOVING &&
            (TelescopeStatus.getMode() == ST_CALIBRATING || TelescopeStatus.getMode() == ST_MANUAL)) {
          ctrl.moveStop();
        } else if (currentFragment instanceof SettingsFragment) {
          setFragment("main", MainFragment.class, new Bundle());
        } else if (TelescopeStatus.get() == ST_NOT_CONNECTED) {
          ctrl.connect();
        } else if (TelescopeStatus.getMode() == ST_MANUAL) {
          update(ST_READY, ST_READY);
          setFragment("main", MainFragment.class, new Bundle());
        } else if (TelescopeStatus.getMode() == ST_TRACING) {
          update(ST_READY, ST_MOVE_TO_OBJECT);
        } else if (TelescopeStatus.get() == ST_READY && TelescopeStatus.getMode() == ST_CALIBRATING) {
          ctrl.calibrate();
          update(ST_READY, ST_CALIBRATED);
          setFragment("main", MainFragment.class, new Bundle());
        } else if (TelescopeStatus.get() == ST_READY && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
          ctrl.move(C.curObj);
        }
      }
    });

    progressIndicator = new ProgressIndicator(this);

    drawer = findViewById(R.id.drawer_layout);
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

    // init astro database TODO
    //  SelectFragment.initAstroObjDatabase(this);

    // start state machine
    new MyStateMachine(this);

    // create control mechanism
    ctrl = new Control(MainActivity.this);

    //  init current astro object TODO
    //  new GetStarInfo(C.calObj, null);

    BlueTooth.initBluetooth(this);
  }


  public void setPositionString()
  {
    SelectFragment.setPositionString(this);
  }

  public void updateFab(int color, FloatingActionButton fab)
  {
    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(color, null)));
  }

  public void connect()
  {
    new MyBlueTooth(new SharedPref(this).getString("device_name"), this).executeOnExecutor(THREAD_POOL_EXECUTOR);
  }

  private void promptToCalibration()
  {
    runOnUiThread(() -> myMessage(tx(R.string.calibration_ntfy)));
  }

  public void initControl()
  {
    runOnUiThread(() -> ctrl.init());
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
    } else if (id == R.id.action_monitor) {
      if (!C.monitoring) {
        C.monitoring = true;
        monitor.showAtLocation(this.findViewById(R.id.content), Gravity.BOTTOM | Gravity.START, 0, 0);
      } else {
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
      } else
        myMessage(tx(R.string.to_manual_move), () -> {
          setFragment("manual control", ManualFragment.class, new Bundle());
          TelescopeStatus.setMode(ST_MANUAL);
        });
    }
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  void myMessage(final String msg)
  {
    runOnUiThread(() -> {

      AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
      alertDialog.setTitle(tx(R.string.warning));
      alertDialog.setMessage(msg);
      alertDialog.setButton(
          AlertDialog.BUTTON_NEUTRAL, "OK",
          (dialog, which) -> dialog.dismiss());
      alertDialog.show();
    });
  }

  void myMessage(final String msg, final Runnable action)
  {
    runOnUiThread(() -> {

      AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
      alertDialog.setTitle(tx(R.string.warning));
      alertDialog.setMessage(msg);
      alertDialog.setButton(
          AlertDialog.BUTTON_POSITIVE, tx(R.string.ok),
          (dialog, which) -> action.run());
      alertDialog.setButton(
          AlertDialog.BUTTON_NEGATIVE, tx(android.R.string.cancel),
          (dialog, which) -> dialog.dismiss());
      alertDialog.show();
    });
  }

  public String tx(int stringId, Object... formatArgs)
  {
    if (formatArgs.length > 0)
      return getString(stringId, formatArgs);
    return getString(stringId);
  }

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
    MyFragment f = currentFragment;

    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.detach(f);
    transaction.commit();

    transaction = getSupportFragmentManager().beginTransaction();
    transaction.attach(f);
    transaction.commit();
  }

  @Override
  protected void attachBaseContext(Context newBase)
  {
    super.attachBaseContext(new MyContextWrapper(newBase));
  }
}
