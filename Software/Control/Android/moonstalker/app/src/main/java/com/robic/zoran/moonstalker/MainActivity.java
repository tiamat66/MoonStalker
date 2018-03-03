package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener
{
    private static final String TAG = "Telescope-Main";

    TextView posTextView;
    TextView locTextView;
    TextView statusTextView;

    Button traceButton;
    Button moveButton;
    Button connectButton;
    Button calibrateButton;
    Button exitButton;
    Button up;
    Button down;
    Button left;
    Button right;

    Spinner mStarDropDown;
    ArrayAdapter<CharSequence> mStarAdapter;

    private DeviceIO  deviceIO;
    private Telescope t;
    private BlueToothService bt;
    private Control ctr;
    private GPSService gps;
    private AstroObject curObj = null;
    private MyView myView = null;
    private Paint drawPaint = new Paint();

    DeviceIO getDevice()
    {
        return deviceIO;
    }

    BlueToothService getBt()
    {
        return bt;
    }

    Control getCtr()
    {
        return ctr;
    }

    GPSService get_gps()
    {
        return gps;
    }

    Telescope getTelescope()
    {
        return t;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
        calibrateButton.setText("CALIBRATED");
        L1.addView(calibrateButton);

        exitButton = new Button(this);
        exitButton.setId(R.id.exit_button);
        exitButton.setText("EXIT");
        L1.addView(exitButton);

        up = new Button(this);
        up.setId(R.id.up_button);
        up.setText("U");
        L1.addView(up);

        down = new Button(this);
        down.setId(R.id.down_button);
        down.setText("D");
        L1.addView(down);

        left = new Button(this);
        left.setId(R.id.left_button);
        left.setText("L");
        L1.addView(left);

        right = new Button(this);
        right.setId(R.id.right_button);
        right.setText("R");
        L1.addView(right);

        //DropDown
        mStarDropDown = new Spinner(this);
        L1.addView(mStarDropDown);

        root.addView(L1);
        root.addView(L2);

        myView = new MyView(this);
        root.addView(myView);
        setContentView(root);

        bt = new BlueToothService(this);
        gps = new GPSService(this);
        t = new Telescope(gps, this);
        ctr = new Control(t, this);
        deviceIO = new DeviceIO(this);
        deviceIO.start();
        try {moonstalkerMain();}
        catch (InterruptedException e) {e.printStackTrace();}
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    private void disableButton(Button button)
    {
        button.setAlpha(0.5f);
        button.setClickable(false);
    }

    private void enableButton(Button button)
    {
        button.setAlpha(1.0f);
        button.setClickable(true);
    }

    private void moonstalkerMain() throws InterruptedException
    {
        exitButton.setOnClickListener(this);
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
        mStarDropDown.setEnabled(false);
        showLocation();
        updateStatus();
    }

    public void showLocation()
    {
        String output = String.format("LAT=%s", t.getPos().convertDec2Hour(gps.getLatitude())) +
                String.format("LON=%s", t.getPos().convertDec2Hour (gps.getLongitude()));
        locTextView.setText(output);
        myView.invalidate();
    }

    @SuppressLint("SetTextI18n")
    public void updateStatus()
    {
        if (!bt.connected ||
                t.ready==Telescope.ERROR ||
                !t.calibrated ||
                curObj == null) {
            if (!t.tracing) disableButton(traceButton);
            disableButton(moveButton);
            mStarDropDown.setEnabled(false);
        }
        else if (!t.tracing) {
            enableButton(traceButton);
            enableButton(moveButton);
            mStarDropDown.setEnabled(true);
        }
        else {
            enableButton(traceButton);
            disableButton(moveButton);
            mStarDropDown.setEnabled(false);
        }

        if (!bt.connected)
            disableButton(calibrateButton);
        else if (!t.calibrated)
            enableButton(calibrateButton);

        if (bt.connected) {
            disableButton(connectButton);
            connectButton.setText("CONNECTED");
        }
        else {
            enableButton(connectButton);
            connectButton.setText("CONNECT");
        }

        if(t.tracing) traceButton.setText("TRACE OFF");
        else traceButton.setText("TRACE");

        myView.invalidate();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.connect_button:
                bt.connect();
                updateStatus();
                break;
            case R.id.trace_button:
                if (!t.tracing) {
                    traceButton.setText("TRACE OFF");
                    t.setTrace(true);
                } else {
                    traceButton.setText("TRACE");
                    t.setTrace(false);
                }
                break;
            case R.id.move_button:
                if (curObj != null) {
                    move();
                    updateStatus();
                }
                break;
            case R.id.calibrate_button:
                disableButton(calibrateButton);
                t.calibrate();
                move();
                break;
            case R.id.exit_button:
                errorExit("Application terminated", "");
                break;
        }
    }

    public void move()
    {
        double ra = curObj.getRa();
        double dec = curObj.getDec();
        t.onMove(ra, dec);
    }

    private void scanAstroLine(int position, AstroObject obj)
    {
        String buf = mStarDropDown.getItemAtPosition(position).toString();
        Log.d(TAG, position + ": " + buf);

        Scanner sc = new Scanner(buf);
        String name = sc.next();
        //ra h min sec
        String ra = sc.next() + " ";
        ra += sc.next() + " ";
        ra += sc.next() + " ";
        //dec h min sec
        String dec = sc.next() + " ";
        dec += sc.next() + " ";
        dec += sc.next() + " ";
        obj.set(name, ra, dec);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if (curObj == null) curObj = new AstroObject("", "", "");
        scanAstroLine(position, curObj);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {}

    private class AstroObject
    {
        private String name;
        private String ra;
        private String dec;

        AstroObject(String name, String ra, String dec)
        {
            set(name, ra, dec);
        }

        void set(String name, String ra, String dec)
        {
            this.name = name;
            this.ra = ra;
            this.dec = dec;
        }

        double getRa()
        {
            Scanner sc = new Scanner(ra);
            return convertHour2Dec(Double.valueOf(sc.next()),
                    Double.valueOf(sc.next()),
                    Double.valueOf(sc.next()));
        }

        double getDec()
        {
            Scanner sc = new Scanner(dec);
            return convertHour2Dec(Double.valueOf(sc.next()),
                    Double.valueOf(sc.next()),
                    Double.valueOf(sc.next()));
        }
    }

    public class MyView extends View
    {
        private static final int D = 300;
        private static final int O = 10;

        public MyView(Context context)
        {
            super(context);
        }

        protected void drawHeight(int X, int Y, double x, double y, Canvas canvas)
        {
            String output;
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
            output = String.format("ALTITUDE: %s\n", t.getPos().convertDec2Hour(t.getPos().h));
            canvas.drawText(output, X, Y - O, paint);
            canvas.drawText("+90", X + (D / 2), Y - O, paint);
            canvas.drawText("-90", X + (D / 2), Y + D + O, paint);
            canvas.drawText("0", X - O, Y + (D / 2), paint);
            canvas.drawText("0", X + D + O, Y + (D / 2), paint);
        }

        protected void drawAzimuth(int X, int Y, double x, double y, Canvas canvas)
        {
            String output;
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
            output = String.format("AZIMUTH: %s\n", t.getPos().convertDec2Hour(t.getPos().az));
            paint.setFakeBoldText(true);
            canvas.drawText(output, X, Y - O, paint);
            canvas.drawText("N", X + (D / 2), Y - O, paint);
            canvas.drawText("S", X + (D / 2), Y + D + 2 * O, paint);
            canvas.drawText("W", X - 2 * O, Y + (D / 2), paint);
            canvas.drawText("E", X + D + O, Y + (D / 2), paint);
        }

        protected void drawStatus(int X, int Y, Canvas canvas)
        {
            String out;
            int i = 1;
            int offset = 20;
            Paint paint = new Paint();

            paint.setColor(Color.BLACK);
            canvas.drawRect(X, Y, X + D / 2, Y + D / 2, paint);

            if (t.ready == Telescope.OK) {
                paint.setColor(Color.GREEN);
                out = "OK";
            }
            else if (t.ready == Telescope.BUSY) {
                paint.setColor(Color.BLUE);
                out = "BUSY";
            }
            else {
                paint.setColor(Color.RED);
                out = "ERROR";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (gps.isGotLocation()) {
                paint.setColor(Color.GREEN);
                out = "GPS LOCKED";
            } else {
                paint.setColor(Color.RED);
                out = "GPS NOT LOCKED";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (t.calibrated) {
                paint.setColor(Color.GREEN);
                out = "CALIBRATED";
            } else {
                paint.setColor(Color.RED);
                out = "NOT CALIBRATED";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (bt.connected) {
                paint.setColor(Color.GREEN);
                out = "CONNECTED";
                disableButton(connectButton);
            } else if (bt.connecting) {
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

            if (t.tracing) {
                paint.setColor(Color.YELLOW);
                out = "TRACING";
                canvas.drawText(out, X + offset, Y + i * offset, paint);
                i++;
            }

            if (t.batteryOk) {
                paint.setColor(Color.GREEN);
                out = "BATTERY OK";
            } else {
                paint.setColor(Color.RED);
                out = "BATTERY ERROR";
            }
            canvas.drawText(out, X + offset, Y + i * offset, paint);
            i++;

            if (t.hNegative) {
                paint.setColor(Color.YELLOW);
                out = "NEGATIVE ALTITUDE";
                canvas.drawText(out, X + offset, Y + i * offset, paint);
            }
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);

            double x, y;
            double az = t.getPos().az;
            double h =  t.getPos().h;

            if ((az > 180) && (az < 360))
                h = Math.toRadians(270 - (h - 270));
            else
                h = Math.toRadians(h);

            az = Math.toRadians((360 - az) + 90);

            drawPaint.setColor(Color.CYAN);
            canvas.drawPaint(drawPaint);

            x = (D / 2) * Math.cos(h);
            y = (D / 2) * Math.sin(h);
            drawHeight(250, 100, x, y, canvas);

            x = (D / 2) * Math.cos(az);
            y = (D / 2) * Math.sin(az);
            drawAzimuth(250 + D, 100, x, y, canvas);

            drawStatus(10, 10, canvas);
        }
    }

    private double convertHour2Dec(double h, double min, double s)
    {
        return (h + (min / 60.0) + (s / 3600.0));
    }

    public void connectionTimedOutMessage()
    {
        Toast.makeText(this, "BlueTooth connection to server timed out.\n" +
                "Please check connection.", Toast.LENGTH_LONG).show();
        updateStatus();
    }

    public void connectionCanceled()
    {
        Toast.makeText(this, "Connection lost!", Toast.LENGTH_LONG).show();
        updateStatus();
    }

    public void messagePrompt(String title, String message)
    {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle(title);
        builder1.setMessage(message);
        builder1.setCancelable(true);
        builder1.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    void errorExit(String title, String message)
    {
        Log.d(TAG, title + " - " + message);
        finish();
        System.exit(0);
    }
}
