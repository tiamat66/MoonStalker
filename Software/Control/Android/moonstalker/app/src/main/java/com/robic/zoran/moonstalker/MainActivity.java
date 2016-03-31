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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "main";

    TextView posTextView;
    TextView locTextView;
    TextView statusTextView;

    Button traceButton;
    Button moveButton;
    Button connectButton;

    Spinner mStarDropDown;
    ArrayAdapter<CharSequence> mStarAdapter;

    Telescope telescope;
    BlueToothService btService;
    GPSService gpsService;
    AstroObject curAstroObject = null;
    MyView myView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout L1 = new LinearLayout(this);
        L1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout L2 = new LinearLayout(this);
        L2.setOrientation(LinearLayout.HORIZONTAL);

        //Text Views
        posTextView = new TextView(this);
        L2.addView(posTextView);

        locTextView = new TextView(this);
        L2.addView(locTextView);

        statusTextView = new TextView(this);
        L2.addView(statusTextView);

        //Buttons
        traceButton = new Button(this);
        traceButton.setText("TRACE");
        traceButton.setId(R.id.trace_button);
        L1.addView(traceButton);

        moveButton = new Button(this);
        moveButton.setId(R.id.move_button);
        moveButton.setText("MOVE");
        L1.addView(moveButton);

        connectButton = new Button(this);
        connectButton.setText("CONNECT");
        connectButton.setId(R.id.connect_button);
        L1.addView(connectButton);

        //DropDown
        mStarDropDown = new Spinner(this);
        L1.addView(mStarDropDown);

        root.addView(L1);
        root.addView(L2);

        myView = new MyView(this);
        root.addView(myView);

        setContentView(root);

        btService = new BlueToothService(this);
        gpsService = new GPSService(this);
        telescope = new Telescope(btService, this);
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


        //Calibrate the telescope
        telescope.position.setLongitude(gpsService.getLongitude());
        telescope.position.setLatitude(gpsService.getLatitude());
        telescope.calibration();

        traceButton.setOnClickListener(this);
        disableButton(traceButton);

        moveButton.setOnClickListener(this);
        disableButton(moveButton);

        connectButton.setOnClickListener(this);

        mStarAdapter = ArrayAdapter
                .createFromResource(this, R.array.stars,
                        android.R.layout.simple_spinner_item);

        mStarDropDown.setAdapter(mStarAdapter);
        mStarDropDown.setOnItemSelectedListener(this);

        showPosition();
        showLocation();
        showStatus();
    }

    public void showPosition() {

        String output = "";

        output += String.format("RA = %.2f\n", telescope.getPosition().getRa()) +
                String.format("DEC= %.2f\n", telescope.getPosition().getDec());

        output +=
                String.format("Height:  %.2f\n", telescope.getPosition().getHeight()) +
                        String.format("Azimuth: %.2f\n", telescope.getPosition().getAzimuth());
        posTextView.setText(output);
        myView.invalidate();
    }

    public void showLocation() {

        String output;

        output = String.format("LAT=%.2f\n", gpsService.getLatitude()) +
                String.format("LON=%.2f\n", gpsService.getLongitude());
        locTextView.setText(output);
        myView.invalidate();
    }

    public void showStatus() {

        String output = "";

        if (gpsService.isGotLocation())
            output += "GPS: locked\n";
        else
            output += "GPS: not locked\n";

        if (telescope.isTracing())
            output += "Tracing: on\n";
        else
            output += "Tracing: off\n";

        if (telescope.isReady())
            output += "Telescope: ready\n";
        else
            output += "Telescope: busy\n";

        statusTextView.setText(output);
        updateStatus();
        myView.invalidate();
    }

    private void updateStatus() {

        if (!btService.isConnected() ||
                !telescope.isReady ||
                curAstroObject == null) {

            disableButton(traceButton);
            disableButton(moveButton);

        } else {
            enableButton(traceButton);
            enableButton(moveButton);
        }

        if (btService.isConnected()) {

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
                if (!telescope.isTracing) {

                    traceButton.setText("Trace OFF");
                    disableButton(moveButton);
                    telescope.onTrace();
                } else {

                    traceButton.setText("Trace");
                    enableButton(moveButton);
                    telescope.offTrace();
                }
                showPosition();
                showStatus();
                break;

            case R.id.move_button:
                if (curAstroObject != null) {

                    double ra = Double.valueOf(curAstroObject.getmRa());
                    double dec = Double.valueOf(curAstroObject.getmDec());
                    telescope.onMove(ra, dec);
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

        if (curAstroObject == null) {

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

    public class MyView extends View {

        private static final int D = 300;
        private static final int O = 10;

        public MyView(Context context) {
            super(context);

        }

        protected void drawHeight(int X, int Y, double x, double y, Canvas canvas) {

            Paint paint = new Paint();

            paint.setColor(Color.GREEN);
            canvas.drawCircle(X + (D / 2) + (float) x, Y + (D / 2) - (float) y, O, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor("#CD5C5C"));
            // Height
            canvas.drawLine(X, Y + (D / 2), X + D, Y + (D / 2), paint);
            canvas.drawLine(X + (D / 2), Y, X + (D / 2), Y + D, paint);
            canvas.drawLine(X + (D / 2), Y + (D / 2), X + (D / 2) + (float) x, Y + (D / 2) - (float) y, paint);
            canvas.drawCircle(X + (D / 2), Y + (D / 2), D / 2, paint);
            canvas.drawRect(X, Y, X + D, Y + D, paint);

            paint.setColor(Color.BLACK);
            canvas.drawText("HEIGHT", X, Y - O, paint);
            canvas.drawText("+90", X + (D / 2), Y - O, paint);
            canvas.drawText("-90", X + (D / 2), Y + D + O, paint);
            canvas.drawText("0", X - O, Y + (D / 2), paint);
            canvas.drawText("0", X + D + O, Y + (D / 2), paint);
        }

        protected void drawAzimuth(int X, int Y, double x, double y, Canvas canvas) {

            Paint paint = new Paint();

            paint.setColor(Color.GREEN);
            canvas.drawCircle(X + (D / 2) + (float) x, Y + (D / 2) - (float) y, O, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.parseColor("#CD5C5C"));
            // Height
            canvas.drawLine(X, Y + (D / 2), X + D, Y + (D / 2), paint);
            canvas.drawLine(X + (D / 2), Y, X + (D / 2), Y + D, paint);
            canvas.drawLine(X + (D / 2), Y + (D / 2), X + (D / 2) + (float) x, Y + (D / 2) - (float) y, paint);
            canvas.drawCircle(X + (D / 2), Y + (D / 2), D / 2, paint);
            canvas.drawRect(X, Y, X + D, Y + D, paint);

            paint.setColor(Color.BLACK);
            canvas.drawText("AZIMUTH", X, Y - O, paint);
            canvas.drawText("0", X + (D / 2), Y - O, paint);
            canvas.drawText("180", X + (D / 2), Y + D + O, paint);
            canvas.drawText("270", X - O, Y + (D / 2), paint);
            canvas.drawText("90", X + D + O, Y + (D / 2), paint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            double x, y, h, az;

            az = telescope.getPosition().getAzimuth();
            h = telescope.getPosition().getHeight();

            if ((az > 180) && (az < 360))
                h = Math.toRadians(270 - (h - 270));
            else
                h = Math.toRadians(h);

            az = Math.toRadians((360 - az) + 90);

            Paint paint = new Paint();
            paint.setColor(Color.CYAN);
            canvas.drawPaint(paint);

            x = (D / 2) * Math.cos(h);
            y = (D / 2) * Math.sin(h);
            drawHeight(100, 100, x, y, canvas);

            x = (D / 2) * Math.cos(az);
            y = (D / 2) * Math.sin(az);
            drawAzimuth(500, 100, x, y, canvas);
        }
    }
}
