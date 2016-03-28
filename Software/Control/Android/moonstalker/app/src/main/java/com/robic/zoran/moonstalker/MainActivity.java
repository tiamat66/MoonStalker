package com.robic.zoran.moonstalker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "main";

    TextView posTextView;
    TextView locTextView;
    TextView statusTextView;
//    boolean  isMoved = false;

    Button traceButton;
    Button moveButton;
    Button connectButton;

    Spinner mStarDropDown;
    ArrayAdapter<CharSequence> mStarAdapter;

    Telescope telescope;
    BlueToothService btService;
    GPSService gpsService;
    AstroObject curAstroObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setContentView(new MyView(this));

        btService = new  BlueToothService(this);
        gpsService = new GPSService(this);
        telescope = new  Telescope(btService, this);
        moonstalkerMain();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void disableButton(Button button) {

        button.setAlpha(0.5f);
        button.setClickable(false);
    }

    private void enableButton(Button button) {

        button.setAlpha(1.0f);
        button.setClickable(true);
    }

    private void moonstalkerMain() {

        posTextView = (TextView) findViewById(R.id.pos_textview);
        locTextView = (TextView) findViewById(R.id.loc_textview);
        statusTextView = (TextView) findViewById(R.id.status_textview);

        //Calibrate the telescope
        telescope.position.setLongitude(gpsService.getLongitude());
        telescope.position.setLatitude(gpsService.getLatitude());
        telescope.calibration();

        traceButton = (Button) findViewById(R.id.trace_button);
        traceButton.setOnClickListener(this);
        disableButton(traceButton);

        moveButton = (Button) findViewById(R.id.move_button);
        moveButton.setOnClickListener(this);
        disableButton(moveButton);

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(this);

        mStarDropDown = (Spinner) findViewById(R.id.spinner1);
        mStarAdapter = ArrayAdapter
                .createFromResource(this, R.array.stars,
                        android.R.layout.simple_spinner_item);

        mStarDropDown.setAdapter(mStarAdapter);
        mStarDropDown.setOnItemSelectedListener(this);

        showPosition();
        showLocation();
        showStatus();
    }

    public void showPosition()
    {
        String output = "";

        output += String.format("RA = %.2f\n", telescope.getPosition().getRa()) +
                 String.format("DEC= %.2f\n", telescope.getPosition().getDec());

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

        if(telescope.isTracing())
            output += "Tracing: on\n";
        else
            output += "Tracing: off\n";

        if (telescope.isReady())
            output += "Telescope: ready\n";
        else
            output += "Telescope: busy\n";

        if(curAstroObject != null)
            output += "Object: " + curAstroObject.print() + "\n";
        else
            output += "Object: Not selected";

        statusTextView.setText(output);
        updateStatus();
    }

    private void updateStatus() {

        if(!btService.isConnected() ||
                !telescope.isReady ||
                curAstroObject == null) {

            disableButton(traceButton);
            disableButton(moveButton);

        } else {
            enableButton(traceButton);
            enableButton(moveButton);
        }

        if(btService.isConnected()) {

            disableButton(connectButton);
            connectButton.setText("CONNECTED");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.connect_button:
                btService.connect();
                showStatus();
                break;

            case R.id.trace_button:
                if(!telescope.isTracing) {

                    traceButton.setText("Trace OFF");
                    telescope.onTrace();
                } else {

                    traceButton.setText("Trace");
                    telescope.offTrace();
                }
                showPosition();
                showStatus();
                break;

            case R.id.move_button:
                if(curAstroObject != null) {

                    double ra = Double.valueOf(curAstroObject.getmRa());
                    double dec = Double.valueOf(curAstroObject.getmDec());
                    //isMoved = true;
                    telescope.onMove(ra, dec);
                    //showStatus();
                }
                break;

            default:
                break;
        }
    }

    private void scanAstroLine(int position, AstroObject obj) {

        Scanner sc;
        String name;
        String ra;
        String dec;
        String buf = mStarDropDown.getItemAtPosition(position).toString();
        Log.d(TAG, position + ": " + buf);

        sc = new Scanner(buf);
        name = sc.next();
        ra = sc.next();
        dec = sc.next();
        obj.setAll(name, ra, dec);
        showStatus();
    }

    public Telescope getTelescope() {
        return telescope;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if(curAstroObject == null) {

            curAstroObject = new AstroObject();
        }
        scanAstroLine(position, curAstroObject);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class AstroObject {

        private String mName;
        private String mType;
        private String mRa;
        private String mDec;
        private String mDescription;


        public void setAll(String name, String ra, String dec) {

            mName = name;
            mRa = ra;
            mDec = dec;
        }

        public String print() {

            String output =
                    mName + "  " + mRa + "  " + mDec;
            return output;
        }

        public String getmRa() {
            return mRa;
        }

        public String getmDec() {
            return mDec;
        }
    }

    //TODO
    public class MyView extends View {
        public MyView(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);
            int x = getWidth();
            int y = getHeight();
            int radius;
            radius = 100;
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawPaint(paint);
            // Use Color.parseColor to define HTML colors
            paint.setColor(Color.parseColor("#CD5C5C"));
            canvas.drawCircle(x / 2, y / 2, radius, paint);
        }
    }
}
