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

import junit.framework.Assert;

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
    Button calibrateButton;

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

        calibrateButton = new Button(this);
        calibrateButton.setId(R.id.calibrate_button);
        calibrateButton.setText("CALIBRATE");
        L1.addView(calibrateButton);

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
        telescope = new Telescope(btService, gpsService, this);
        try {
            moonstalkerMain();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    private void moonstalkerMain() throws InterruptedException {

        calibrateButton.setOnClickListener(this);

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

        showLocation();
        updateStatus();
    }

    public void showLocation() {

        String output;

        output = String.format("LAT=%.2f\n", gpsService.getLatitude()) +
                String.format("LON=%.2f\n", gpsService.getLongitude());
        locTextView.setText(output);
        myView.invalidate();
    }

    public void updateStatus() {

        if (!btService.isConnected() ||
                !telescope.isReady ||
                !telescope.isCalibrated ||
                curAstroObject == null ) {

            if(!telescope.isTracing) disableButton(traceButton);
            disableButton(moveButton);

        } else if (btService.isConnected() &&
                telescope.isReady &&
                telescope.isCalibrated &&
                !telescope.isTracing()) {

            enableButton(traceButton);
            enableButton(moveButton);
        } else if (btService.isConnected() &&
                telescope.isReady &&
                telescope.isCalibrated &&
                telescope.isTracing()) {

            enableButton(traceButton);
            disableButton(moveButton);
        }

        if(!btService.isConnected())
            disableButton(calibrateButton);
        else
            enableButton(calibrateButton);

        if (btService.isConnected()) {

            disableButton(connectButton);
            connectButton.setText("CONNECTED");
        } else {
            enableButton(connectButton);
            connectButton.setText("CONNECT");
        }

        myView.invalidate();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.connect_button:
                btService.connect();
                updateStatus();
                break;

            case R.id.trace_button:
                if (!telescope.isTracing) {

                    traceButton.setText("TRACE OFF");
                    telescope.onTrace();
                } else {

                    traceButton.setText("TRACE");
                    telescope.offTrace();
                }
                updateStatus();
                break;

            case R.id.move_button:
                if (curAstroObject != null) {

                    double ra = Double.valueOf(curAstroObject.getmRa());
                    double dec = Double.valueOf(curAstroObject.getmDec());
                    telescope.onMove(ra, dec);
                    updateStatus();
                }
                break;

            case R.id.calibrate_button:
                //Calibrate the telescope
                telescope.calibration();
                disableButton(calibrateButton);
                try {
                    telescope.getBattery();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
            String output;

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
            output = String.format("HEIGHT: %.2f\n", telescope.getPosition().getHeight());
            canvas.drawText(output, X, Y - O, paint);
            canvas.drawText("+90", X + (D / 2), Y - O, paint);
            canvas.drawText("-90", X + (D / 2), Y + D + O, paint);
            canvas.drawText("0", X - O, Y + (D / 2), paint);
            canvas.drawText("0", X + D + O, Y + (D / 2), paint);
        }

        protected void drawAzimuth(int X, int Y, double x, double y, Canvas canvas) {

            Paint paint = new Paint();
            String output;

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
            paint.setFakeBoldText(true);
            output = String.format("AZIMUTH: %.2f\n", telescope.getPosition().getAzimuth());
            canvas.drawText(output, X, Y - O, paint);
            canvas.drawText("N", X + (D / 2), Y - O, paint);
            canvas.drawText("S", X + (D / 2), Y + D + 2 * O, paint);
            canvas.drawText("W", X - 2 * O, Y + (D / 2), paint);
            canvas.drawText("E", X + D + O, Y + (D / 2), paint);
        }

        protected void drawStatus(int X, int Y, Canvas canvas) {

            Paint paint = new Paint();
            String out = "";
            int i = 1;
            int offset = 20;

            paint.setColor(Color.BLACK);
            canvas.drawRect(X, Y, X + D / 2, Y + D / 2, paint);

            if (gpsService.isGotLocation()) {

                paint.setColor(Color.GREEN);
                out = "GPS LOCKED";
            } else {

                paint.setColor(Color.RED);
                out = "GPS NOT LOCKED";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (telescope.isCalibrated) {
                paint.setColor(Color.GREEN);
                out = "CALIBRATED";
            } else {

                paint.setColor(Color.RED);
                out = "NOT CALIBRATED";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (telescope.isReady) {
                paint.setColor(Color.GREEN);
                out = "READY";
            } else {

                paint.setColor(Color.RED);
                out = "BUSY";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (btService.isConnected()) {
                paint.setColor(Color.GREEN);
                out = "CONNECTED";
                disableButton(connectButton);
            } else if(btService.isConnecting()) {
                paint.setColor(Color.YELLOW);
                out = "CONNECTING...";
                disableButton(connectButton);
                canvas.drawText(out, X + offset, Y + 4 * offset, paint);
            } else {

                paint.setColor(Color.RED);
                out = "NOT CONNECTED";
                enableButton(connectButton);
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (telescope.isTracing) {

                paint.setColor(Color.YELLOW);
                out = "TRACING";
                canvas.drawText(out, X + offset, Y + i * offset, paint);
                i++;
            }

            if(telescope.isBatteryOk()) {

                paint.setColor(Color.GREEN);
                out = "BATTERY OK";
            } else {

                paint.setColor(Color.RED);
                out = "BATTERY LOW";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

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
            drawHeight(250, 100, x, y, canvas);

            x = (D / 2) * Math.cos(az);
            y = (D / 2) * Math.sin(az);
            drawAzimuth(250 + D, 100, x, y, canvas);

            drawStatus(10, 10, canvas);
        }
    }
}
