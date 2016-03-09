package com.robic.zoran.moonstalker;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private final static int REQUEST_ENABLE_BT = 1;

    /* Position */
    private static final int MINIMUM_TIME = 10000;  // 10s
    private static final int MINIMUM_DISTANCE = 50; // 50m

    /* GPS */
    private LocationManager mLocationManager;
    private GPSService      mLocationListener;

    TextView  mainTextView;
    Telescope telescope;
    BlueToothService btService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        telescope = new Telescope();
        btService = new BlueToothService(this);

        enableGPSService();

        myTest();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    private void enableGPSService() {

        mLocationListener = new GPSService();
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // We have to check if ACCESS_FINE_LOCATION and/or ACCESS_COARSE_LOCATION permission are granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, mLocationListener);
        }
        telescope.position.setLongitude(mLocationListener.getLongitude());
        telescope.position.setLatitude(mLocationListener.getLatitude());
    }

    private void myTest() {

        // Access the TextView defined in layout XML
        // and then set its text
        mainTextView = (TextView) findViewById(R.id.main_textview);

        //Calibrate the telescope
        telescope.calibration();
        telescope.Move(15, 0);

        long time = telescope.getPosition().getTime() / 1000 / 86400;

        String output = "Height=" +
                telescope.getPosition().getHeight() +
                "\nAzimuth=" +
                telescope.getPosition().getAzimuth() +
                "\nTimeFromVernalEquinox=" +
                time;

        if (btService.isBtPresent()) {
            output += "\n BlueTooth is present on this device\n";
        } else {
            output += "\n BlueTooth not present on this device\n";
        }

        if (btService.isBtPOn()) {
            output += "BlueTooth is ON\n";
        } else {
            output += "BlueTooth is OFF\n";
        }
        mainTextView.setText(output);
    }
}
