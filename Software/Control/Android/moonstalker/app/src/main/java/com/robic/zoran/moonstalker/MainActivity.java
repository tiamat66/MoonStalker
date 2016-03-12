package com.robic.zoran.moonstalker;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private final static int REQUEST_ENABLE_BT = 1;

    TextView  mainTextView;
    Button traceButton;
    Button traceOffButton;
    Button updateButton;

    Telescope telescope;
    BlueToothService btService;
    GPSService gpsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btService = new  BlueToothService(this);
        gpsService = new GPSService(this);
        telescope = new  Telescope(btService);
        Thread myThread;

        if(telescope != null &&
                btService != null &&
                gpsService != null) {

            myThread = new Thread(new Runnable() {
                public void run(){
                    //txt1.setText("Thread!!");
                    myTest();
                }
            });

            myThread.start();
        }

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
    public void onResume() {
        super.onResume();

        btService.onResume();
    }

    //@Override
    public void onPause() {
        super.onPause();

        btService.onPause();
    }

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

    private void myTest() {

        // Access the TextView defined in layout XML
        // and then set its text
        mainTextView = (TextView) findViewById(R.id.main_textview);

        //Calibrate the telescope
        telescope.position.setLongitude(gpsService.getLongitude());
        telescope.position.setLatitude(gpsService.getLatitude());
        telescope.calibration();

        //Move the telescope
        telescope.onMove(15, 0);

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

        // 2. Access the Button defined in layout XML
        // and listen for it here
        traceButton = (Button) findViewById(R.id.trace_button);
        traceButton.setOnClickListener(this);

        traceOffButton = (Button) findViewById(R.id.trace_off_button);
        traceOffButton.setOnClickListener(this);

        updateButton = (Button) findViewById(R.id.update_button);
        updateButton.setOnClickListener(this);
    }

    private void updateView()
    {

        long time = telescope.getPosition().getTime() / 1000 / 86400;

        String output = "Height=" +
                telescope.getPosition().getHeight() +
                "\nAzimuth=" +
                telescope.getPosition().getAzimuth() +
                "\nTimeFromVernalEquinox=" +
                time;

        mainTextView.setText(output);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.trace_button:
                mainTextView.setText("Trace on");
                telescope.onTrace();
                break;

            case R.id.trace_off_button:
                mainTextView.setText("Trace Off");
                break;

            case R.id.update_button:
                updateView();
                break;

            default:
                break;
        }
    }
}
