package si.vajnartech.moonstalker;

import static si.vajnartech.moonstalker.C.MD_MOVING;
import static si.vajnartech.moonstalker.C.SERVER_NAME;
import static si.vajnartech.moonstalker.OpCodes.CALIBRATED;
import static si.vajnartech.moonstalker.OpCodes.CALIBRATING;
import static si.vajnartech.moonstalker.OpCodes.CONNECT;
import static si.vajnartech.moonstalker.OpCodes.MOVE_END;
import static si.vajnartech.moonstalker.OpCodes.TRACK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import si.vajnartech.moonstalker.processor.CelestialObj;
import si.vajnartech.moonstalker.processor.CmdTrack;
import si.vajnartech.moonstalker.processor.Ping;
import si.vajnartech.moonstalker.processor.Processor;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjAstroData;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    public RObjAstroData objectsDatabase = new RObjAstroData(null);

    public CelestialObj curObject;
    public String curObjName = "";
    public String toObjName = "";

    protected Processor machine = new Processor(this);
    private final Ping scheduler = new Ping(machine);

    MyFragment currentFragment = null;
    Menu menu;

    TerminalWindow terminal;
    Monitor monitor;
    FloatingActionButton fab;
    DrawerLayout drawer;

    public void setPosMessage(double elevation, double azimuth)
    {
        if (currentFragment instanceof ControlFragment) {
            ((ControlFragment) currentFragment).update(elevation, azimuth);
            curObjName = toObjName;
        }
    }

    public void setInfoMessage(int val)
    {
        terminal.setText(tx(val));
    }

    public void logMessage(String val)
    {
        if (val == null) return;
        monitor.update(val);
    }

    public void showFab(boolean visible)
    {
        if (visible)
            fab.setVisibility(View.VISIBLE);
        else
            fab.setVisibility(View.GONE);
    }

    public void updateFab(int color)
    {
        runOnUiThread(() -> {
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(color, null)));

            // Update icon based on state
            if (machine.status.get() == OpCodes.MOVING) {
                fab.setImageResource(android.R.drawable.ic_media_pause);
            } else if (machine.status.get() == OpCodes.CONNECTION_ERROR || machine.status.get() == OpCodes.NOT_READY) {
                fab.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
            } else if (machine.status.get() == CALIBRATING) {
                fab.setImageResource(android.R.drawable.ic_menu_save);
            } else if (machine.status.get() == TRACK) {
                fab.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                fab.setImageResource(android.R.drawable.ic_menu_directions);
            }
        });
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
            if (machine.status.mode.get() == MD_MOVING &&
                    machine.status.get() == OpCodes.READY) {
                moveEnd();
            } else if (machine.status.get() == OpCodes.READY) {
                connect();
            } else if (machine.status.mode.get() == CALIBRATING) {
                calibrated();
            } else if (machine.status.mode.get() == CALIBRATED &&
                    machine.status.get() == OpCodes.CONNECTED) {
                move();
            } else if (machine.status.get() == OpCodes.TRACK) {
                track(false);
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

        scheduler.start();
    }

    private void connect()
    {
        machine.set(CONNECT);
    }

    private void calibrated()
    {
        machine.set(CALIBRATED);
    }

    private void calibrating()
    {
        machine.set(CALIBRATING, CALIBRATING);
    }

    private void manual()
    {
        machine.set(OpCodes.MANUAL);
    }

    private void auto()
    {
        machine.set(OpCodes.AUTO_CONTROL);
    }

    public void moveStart(String direction)
    {
        // TODO: speed is 500 RPM, both horizontal and vertical steppers will move this speed
        String speed = "500";
        machine.set(OpCodes.MOVE_START, new ObjController(direction, speed, ""));
    }

    public void moveEnd()
    {
        machine.set(MOVE_END);
    }

    public void move()
    {

        ObjController obj = new ObjController(toObjName, "", "");
        machine.set(OpCodes.MOVE, obj);
    }

    private void track(boolean track)
    {
        String action;
        if (track)
            action = "start_track";
        else
            action = "stop_track";
        ObjController obj = new ObjController(action, "", "");

        machine.set(OpCodes.TRACK, obj);
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
        } else if (id == R.id.manual) {
            manual();
        } else if (id == R.id.track) {
            track(true);
        } else if (id == R.id.move) {
            auto();
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

    @Override
    protected void onDestroy()
    {
        scheduler.stop();
        machine.quit();
        super.onDestroy();
    }
}
