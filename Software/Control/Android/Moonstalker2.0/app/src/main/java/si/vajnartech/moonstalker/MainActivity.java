package si.vajnartech.moonstalker;

import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import si.vajnartech.moonstalker.rest.GetStarInfo;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static si.vajnartech.moonstalker.C.ST_NOT_CAL;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
  MyFragment currentFragment = null;
  Control ctrl;

  TerminalWindow terminal;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        if (TelescopeStatus.get() == ST_NOT_CAL)
        {
          Snackbar.make(view, tx(R.string.calibrated), Snackbar.LENGTH_LONG).setAction("Calibrated", null).show();
          ctrl.calibrate();
          setFragment("move", MoveFragment.class, new Bundle());
        }
        else if (TelescopeStatus.get() == ST_READY)
        {
          ctrl.move(C.curObj);
        }
        else if (TelescopeStatus.get() == ST_NOT_CONNECTED)
          connect(true);
      }
    });

    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    // init terminal window
    terminal = new TerminalWindow(this);

    // init astro database
    MoveFragment.initAstroObjDatabase(this);

    // start state machine
    new StatusSM(new Nucleus() {
      @Override
      public void initTelescope()
      {
        initControl();
      }

      @Override
      public void updateStatus()
      {
        runOnUiThread(new Runnable() {
          @Override
          public void run()
          {
            if (TelescopeStatus.get() == ST_READY) {
              terminal.setBackgroundColor(getResources().getColor(R.color.colorOk));
              refreshCurrentFragment();
            }
            else if (TelescopeStatus.get() == ST_NOT_CAL)
            {
              terminal.setBackgroundColor(getResources().getColor(R.color.colorError));
              terminal.setText(tx(R.string.not_calibrated));
            }
            else if (TelescopeStatus.get() == ST_NOT_CONNECTED)
            {
              terminal.setBackgroundColor(getResources().getColor(R.color.colorError));
              terminal.setText(tx(R.string.not_connected));
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
    });
    // init current astro object
    new GetStarInfo(C.calObj, null);
    connect(false);
  }

  private void connect(boolean exe)
  {
    BlueTooth b = new BlueTooth(C.SERVER_NAME, this, new BTInterface()
    {
      @Override
      public void showMessage(String msg)
      {
        myMessage(msg);
      }

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
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
          }
        });
      }
    });

    if (exe)
      b.executeOnExecutor(THREAD_POOL_EXECUTOR);
  }

  private void promptToCalibration()
  {
    runOnUiThread(new Runnable() {
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
    } else {
      super.onBackPressed();
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

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item)
  {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.move) {
      setFragment("move", MoveFragment.class, new Bundle());
    } else if (id == R.id.track) {

    } else if (id == R.id.calibrate) {
      setFragment("manual control", ManualFragment.class, new Bundle());
      promptToCalibration();
    } else if (id == R.id.nav_share) {

    } else if (id == R.id.nav_send) {

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
        alertDialog.setTitle("Alert");
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

  /*
  type: 0 - Connecting
   */
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
    ll_progress.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
    ll_progress.addView(progress, lp_progress);
    ll_progress.addView(loadingText, lp_loading);
    final FrameLayout.LayoutParams lp_ll = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
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
    MyFragment       f           = currentFragment;
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.detach(f);
    transaction.commit();

    transaction = getSupportFragmentManager().beginTransaction();
    transaction.attach(f);
    transaction.commit();
  }
}

