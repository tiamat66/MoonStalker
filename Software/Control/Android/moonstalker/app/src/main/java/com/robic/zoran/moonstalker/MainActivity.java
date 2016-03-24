package com.robic.zoran.moonstalker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    private static final String TAG = "main";

    TextView objTextView;
    TextView posTextView;
    TextView locTextView;
    TextView statusTextView;

    EditText raEditText;
    EditText decEditText;

    Button traceButton;
    Button moveButton;
    Button connectButton;
    Button traceOffButton;

    ListView mainListView;
    ArrayAdapter mArrayAdapter;
    ArrayList mNameList = new ArrayList();

    ShareActionProvider mShareActionProvider;

    Telescope telescope;
    BlueToothService btService;
    GPSService gpsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btService = new  BlueToothService(this);
        gpsService = new GPSService(this);
        telescope = new  Telescope(btService, this);
        moonstalkerMain();

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

    }

    //@Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu.
        // Adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Access the Share Item defined in menu XML
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);

        // Access the object responsible for
        // putting together the sharing submenu
        if (shareItem != null) {
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        }

        return true;
    }

    private void moonstalkerMain() {

        objTextView = (TextView) findViewById(R.id.obj_textview);
        posTextView = (TextView) findViewById(R.id.pos_textview);
        locTextView = (TextView) findViewById(R.id.loc_textview);
        statusTextView = (TextView) findViewById(R.id.status_textview);

        //Calibrate the telescope
        telescope.position.setLongitude(gpsService.getLongitude());
        telescope.position.setLatitude(gpsService.getLatitude());
        telescope.calibration();

        traceButton = (Button) findViewById(R.id.trace_button);
        traceButton.setOnClickListener(this);

        moveButton = (Button) findViewById(R.id.move_button);
        moveButton.setOnClickListener(this);

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(this);

        traceOffButton = (Button) findViewById(R.id.trace_off_button);
        traceOffButton.setOnClickListener(this);

        raEditText = (EditText) findViewById(R.id.ra_edittext);
        decEditText = (EditText) findViewById(R.id.dec_edittext);

        // Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);
        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                mNameList);
        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);
        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        showPosition();
        showLocation();
        showStatus();
    }

    private void addAstroObjects() {

        AstroObject obj;

        obj = new AstroObject("Sirius", "star", 6.75, -16.72);
        mNameList.add(obj.print());
        mArrayAdapter.notifyDataSetChanged();
    }

    public void showPosition()
    {
        String output;

        output = String.format("RA = %.2f\n", telescope.getPosition().getRa()) +
                 String.format("DEC= %.2f\n", telescope.getPosition().getDec());
        objTextView.setText(output);

        output +=
                String.format("Height:  %.2f\n", telescope.getPosition().getHeight()) +
                String.format("Azimuth: %.2f\n", telescope.getPosition().getAzimuth());
        posTextView.setText(output);
    }

    public void showLocation()
    {

        String output;

        output = String.format("LAT=%.2f\n", gpsService.getLatitude()) +
                 String.format("LON=%.2f\n", gpsService.getLongitude());
        locTextView.setText(output);
    }

    public void showStatus() {

        String output="";

        if(gpsService.isGotLocation())
            output += "GPS: locked\n";
        else
            output += "GPS: not locked\n";

        if(btService.isConnected())
            output += "BT: connected\n";
        else
            output += "BT: not connected\n";

        if(telescope.isTracing())
            output += "Tracing: on\n";
        else
            output += "Tracing: off\n";

        if (telescope.isReady())
            output += "Telescope: ready\n";
        else
            output += "Telescope: busy\n";

        statusTextView.setText(output);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.connect_button:
                btService.connect();
                showStatus();
                break;

            case R.id.trace_button:
                telescope.onTrace();
                showPosition();
                showStatus();
                break;

            case R.id.move_button:
                String sRa = raEditText.getText().toString();
                String sDec = decEditText.getText().toString();
                double ra = Double.valueOf(sRa);
                double dec = Double.valueOf(sDec);
                telescope.onMove(ra, dec);
                break;

            case R.id.trace_off_button:
                telescope.offTrace();
                showPosition();
                showStatus();

            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Log the item's position and contents
        // to the console in Debug
        Log.d("moonstalker", position + ": " + mNameList.get(position));
    }

    public Telescope getTelescope() {
        return telescope;
    }

    private class AstroObject {

        private String sName = "";
        private String sType = "";
        private double dRa = 0.0;
        private double dDec = 0.0;

        AstroObject(String name, String type, double ra, double dec) {

            sName = name;
            sType = type;
            dRa = ra;
            dDec = dec;
        }

        public String print() {

            String output =
                    sName + "  " + dRa + "  " + dDec;
            return output;
        }
    }

}
