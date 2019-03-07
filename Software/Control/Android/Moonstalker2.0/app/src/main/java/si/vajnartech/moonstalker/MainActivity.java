package si.vajnartech.moonstalker;

import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
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
        if (TelescopeStatus.get() == C.ST_CONNECTED) {
          initControl();
          Snackbar.make(view, "Init control", Snackbar.LENGTH_LONG)
              .setAction("Action", null).show();
        }
        else
          connect();
      }
    });

    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }

  private void connect()
  {
    new BlueTooth(C.SERVER_NAME, this, new BTInterface()
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
        pOn();
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
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
          }
        });
      }
    });
  }

  private void initControl()
  {
    new Control(this);
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

    if (id == R.id.nav_camera) {
      // Handle the camera action
    } else if (id == R.id.nav_gallery) {

    } else if (id == R.id.nav_slideshow) {

    } else if (id == R.id.nav_manage) {

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

  /***********************************************************************************************
   *
   * Progress indicator section
   *
   ***********************************************************************************************/
  public LinearLayout ll_progress = null;

  public enum ProgressType
  {
    CONNECTING,
  }

  /*
  type: 0 - Loading data
        1 - Saving data
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
      loadingText.setText("Connecting");
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

  public void pOn()
  {
    progressOn(ProgressType.CONNECTING);
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

}

