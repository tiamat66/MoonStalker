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

    private static final String PREFS = "prefs";
    private static final String PREF_NAME = "Zoran";
    SharedPreferences mSharedPreferences;

    TextView mainTextView;
    EditText raEditText;
    EditText decEditText;
    //EditText mainEditText;
    Button traceButton;
    Button traceOffButton;
    Button updateButton;
    Button moveButton;
    Button mButton;
    //Button button6;
    Button button7;

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
        telescope = new  Telescope(btService);
        Thread myThread;

        if(telescope != null &&
                btService != null &&
                gpsService != null) {

            myThread = new Thread(new Runnable() {
                public void run(){
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

        //btService.onResume();
    }

    //@Override
    public void onPause() {
        super.onPause();

        //btService.onPause();
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

        // Create an Intent to share your content
        setShareIntent();

        return true;
    }

    private void setShareIntent() {

        if (mShareActionProvider != null) {

            // create an Intent with the contents of the TextView
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Development");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mainTextView.getText());

            // Make sure the provider knows
            // it should work with that Intent
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    private void myTest() {

        String output;

        mainTextView = (TextView) findViewById(R.id.main_textview);

        //Calibrate the telescope
        telescope.position.setLongitude(gpsService.getLongitude());
        telescope.position.setLatitude(gpsService.getLatitude());
        telescope.calibration();

        // 2. Access the Button defined in layout XML
        // and listen for it here
        traceButton = (Button) findViewById(R.id.trace_button);
        traceButton.setOnClickListener(this);

        traceOffButton = (Button) findViewById(R.id.trace_off_button);
        traceOffButton.setOnClickListener(this);

        updateButton = (Button) findViewById(R.id.update_button);
        updateButton.setOnClickListener(this);

        moveButton = (Button) findViewById(R.id.move_button);
        moveButton.setOnClickListener(this);

        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);

        /*button6 = (Button) findViewById(R.id.button6);
        button6.setOnClickListener(this); */

        button7 = (Button) findViewById(R.id.button7);
        button7.setOnClickListener(this);


        // 3. Access the EditText defined in layout XML
        //mainEditText = (EditText) findViewById(R.id.main_edittext);
        raEditText = (EditText) findViewById(R.id.ra_edittext);
        decEditText = (EditText) findViewById(R.id.dec_edittext);

        // 4. Access the ListView
        mainListView = (ListView) findViewById(R.id.main_listview);

        // Create an ArrayAdapter for the ListView
        mArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                mNameList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(mArrayAdapter);

        // 5. Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        // 7. Greet the user, or ask for their name if new
        //displayWelcome();

        updateView();
    }

    public void displayWelcome() {

        // Access the device's key-value storage
        mSharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Read the user's name,
        // or an empty string if nothing found
        String name = mSharedPreferences.getString(PREF_NAME, "");

        if (name.length() > 0) {

            // If the name is valid, display a Toast welcoming them
            Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_LONG).show();
        } else {

            // otherwise, show a dialog to ask for their name
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Hello!");
            alert.setMessage("What is your name?");

            // Create EditText for entry
            final EditText input = new EditText(this);
            alert.setView(input);

            // Make an "OK" button to save the name
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                // Grab the EditText's input
                String inputName = input.getText().toString();

                // Put it into memory (don't forget to commit!)
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putString(PREF_NAME, inputName);
                e.commit();

                // Welcome the new user
                Toast.makeText(getApplicationContext(), "Welcome, " + inputName + "!", Toast.LENGTH_LONG).show();
                }
            });

            // Make a "Cancel" button
            // that simply dismisses the alert
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {}
            });

            alert.show();
        }
    }

    private String showPosition()
    {

        long timeH = telescope.getPosition().getTime() / 1000 / 3600;
        long timeS = telescope.getPosition().getTime() / 1000;


        String output = "Height=" +
                telescope.getPosition().getHeight() +
                "\nAzimuth=" +
                telescope.getPosition().getAzimuth() +
                "\nTimeFromVernalEquinox=" +
                timeS + " s, " + timeH + " hours" +
                "\nRA=" +
                telescope.getPosition().getRa() +
                "\nDEC=" +
                telescope.getPosition().getDec() + "\n";


        return(output);
    }

    private String showLocation()
    {
        String output = "Latitude=" +
                gpsService.getLatitude() +
                "\nLongitude=" +
                gpsService.getLongitude() + "\n";

        return(output);
    }

    private void updateView()
    {

        String output = showLocation() + showPosition();
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
                telescope.offTrace();
                break;

            case R.id.update_button:
                updateView();
                break;

            case R.id.move_button:
                String sRa = raEditText.getText().toString();
                String sDec = decEditText.getText().toString();
                double ra = Double.valueOf(sRa).doubleValue();
                double dec = Double.valueOf(sDec).doubleValue();

                telescope.onMove(ra, dec);
                break;

            /* case R.id.button6:
                btService.getPairedDevices();
                break; */

            case R.id.button7:
                btService.connect();
                break;

            case R.id.button:
                /*
                mainTextView.setText(mainEditText.getText().toString()
                        + " is learning Android development!");
                // Also add that value to the list shown in the ListView
                mNameList.add(mainEditText.getText().toString());
                mArrayAdapter.notifyDataSetChanged();

                // 6. The text you'd like to share has changed,
                // and you need to update
                setShareIntent();
                */
                //btService.write("Zoran+Maruša");
                telescope.getControl().btry();

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

}
