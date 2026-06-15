package si.vajnartech.moonstalker;

import static si.vajnartech.moonstalker.C.CALIBRATOR;
import static si.vajnartech.moonstalker.C.MD_CALIBRATING;
import static si.vajnartech.moonstalker.C.MD_MOVING;
import static si.vajnartech.moonstalker.C.SERVER_NAME;
import static si.vajnartech.moonstalker.C.ST_CONNECTION_ERROR;
import static si.vajnartech.moonstalker.C.ST_NOT_READY;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_CALIBRATED;
import static si.vajnartech.moonstalker.OpCodes.MSG_CALIBRATING;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONNECT;
import static si.vajnartech.moonstalker.OpCodes.MSG_MOVE_END;
import static si.vajnartech.moonstalker.OpCodes.MSG_MOVE_START;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import si.vajnartech.moonstalker.processor.DataAstroObj;
import si.vajnartech.moonstalker.processor.Ping;
import si.vajnartech.moonstalker.processor.Processor;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    public DataAstroObj astroData;

    public AstroObject curObject;
    protected Processor machine = new Processor(this);

    MyFragment currentFragment = null;
    Menu menu;

    TerminalWindow terminal;
    Monitor monitor;
    FloatingActionButton fab;
    DrawerLayout drawer;

    public void setPosMessage()
    {
        terminal.writePosition(curObject);
    }

    public void setInfoMessage(int val)
    {
        terminal.setText(tx(val));
    }

    public void logMessage(String val)
    {
        monitor.update(val);
    }

    public void updateFab(int color)
    {
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(color, null)));
    }

    public void updateMenu(boolean ca, boolean ma, boolean tr, boolean mo)
    {
        runOnUiThread(() -> {
            menu.findItem(R.id.calibrate).setEnabled(ca);
            menu.findItem(R.id.manual).setEnabled(ma);
            menu.findItem(R.id.track).setEnabled(tr);
            menu.findItem(R.id.move).setEnabled(mo);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        });
    }

    public void promptToCalibration()
    {
        runOnUiThread(() -> myMessage(tx(R.string.calibration_ntfy)));
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        terminal = new TerminalWindow(this);
        C.curMessage = tx(R.string.not_connected);
        terminal.setText(C.curMessage);

        SharedPref.setDefault("device_name", SERVER_NAME);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if (machine.mode.get() == MD_MOVING &&
                    machine.status.get() == ST_READY) {
                moveEnd();
            } else if (machine.status.get() == ST_CONNECTION_ERROR ||
                    machine.status.get() == ST_NOT_READY
            ) {
                connect();
            } else if (machine.mode.get() == MD_CALIBRATING) {
                calibrated();
            }
        });

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

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        monitor = new Monitor(inflater.inflate(R.layout.frag_monitor, null, false));

        new Ping(machine);
    }

    private void connect()
    {
        machine.set(MSG_CONNECT);
    }

    private void calibrated()
    {
        machine.set(MSG_CALIBRATED, CALIBRATOR.toString());
    }

    private void calibrating()
    {
        machine.set(MSG_CALIBRATING);
    }

    public void moveStart(String direction)
    {
        machine.set(MSG_MOVE_START, direction);
    }

    public void moveEnd()
    {
        machine.set(MSG_MOVE_END);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
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

        if (id == R.id.calibrate) {
            calibrating();
        }
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
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

    @SuppressWarnings("unused")
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
        MyFragment frag = (MyFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (frag == null && cls != null) try {
            frag = MyFragment.instantiate(cls, this);
            frag.setArguments(params);
        } catch (Exception e) {
            Log.e("MainActivity", "Error creating fragment", e);
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
}
