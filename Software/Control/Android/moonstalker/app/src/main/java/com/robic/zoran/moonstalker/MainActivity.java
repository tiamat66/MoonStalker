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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Scanner;

import static com.robic.zoran.moonstalker.MSUtil.convertDec2Hour;
import static com.robic.zoran.moonstalker.Telescope.ST_READY;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
    AdapterView.OnItemSelectedListener
{
  private static final String  TAG       = "Telescope-Main";
  private static final boolean IS3D      = true;
  private static final boolean BLUETOOTH = true;
  private static final boolean GPS       = true;
  private static final int     TS        = 30;

  TextView locTextView;

  Button traceButton;
  Button moveButton;
  Button exitButton;

  Spinner skyObjDropDown;

  private DeviceIO         deviceIO;
  private Telescope        t;
  private BlueToothService bt;
  private Control          ctr;
  private GPSService       gps;
  public  AstroObject curObj    = null;
  private MyView      myView    = null;
  private Paint       drawPaint = new Paint();

  MsStatusBar statusBar;
  PositionBar positionBar;

  LinearLayout  cursorKeysBar2;
  CursorKeysBar cursorKeysBar1, cursorKeysBar3;
  Buttons buttons;

  int a, b, c;



  private MsView3D view3D = null;

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
    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout root = (LinearLayout) inflater.inflate(R.layout.content_main, null);

    // First line is status line
    LinearLayout L1 = new LinearLayout(this);
    L1.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams prms = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    L1.setGravity(Gravity.END);
    L1.setLayoutParams(new LinearLayout.LayoutParams(prms));

    statusBar = new MsStatusBar(this);
    buttons = new Buttons(this);
    buttons.setPadding(TS, 0, 0, 0);
    L1.addView(statusBar);
    L1.addView(buttons);
    /******************************************/

    LinearLayout L2 = new LinearLayout(this);
    L2.setOrientation(LinearLayout.HORIZONTAL);

    skyObjDropDown = new Spinner(this);

    // Position
    positionBar = new PositionBar(this);

    // Cursor keys
    cursorKeysBar1 = new CursorKeysBar(this, R.drawable.ic_up);

    cursorKeysBar2 = new LinearLayout(this);
    cursorKeysBar2.setOrientation(LinearLayout.HORIZONTAL);
    cursorKeysBar2.addView(new CursorKeysBar(this, R.drawable.ic_left));
    cursorKeysBar2.addView(new CursorKeysBar(this, R.drawable.ic_right));

    cursorKeysBar3 = new CursorKeysBar(this, R.drawable.ic_down);

    // Commands
    LinearLayout LControls = new LinearLayout(this);
    LControls.setOrientation(LinearLayout.VERTICAL);


    a = 1;
    b = 2;
    c = 3;
    LinearLayout LButtons = new LinearLayout(this);
    LButtons.setOrientation(LinearLayout.HORIZONTAL);

    traceButton = new Button(this);
    traceButton.setText("TRACE");
    traceButton.setId(a);
    LButtons.addView(traceButton);

    moveButton = new Button(this);
    moveButton.setId(b);
    moveButton.setText("MOVE");
    LButtons.addView(moveButton);

    exitButton = new Button(this);
    exitButton.setId(c);
    exitButton.setText("EXIT");
    LButtons.addView(exitButton);

//    LControls.addView(LButtons);
    LControls.addView(skyObjDropDown);

    LControls.addView(positionBar);

    LControls.addView(cursorKeysBar1);
    LControls.addView(cursorKeysBar2);
    LControls.addView(cursorKeysBar3);

    // Buttons
    L2.addView(LControls);

    // Graphic View
    if (!IS3D) {
      myView = new MyView(this);
      L2.addView(myView);
    } else {
      view3D = new MsView3D(this);
      L2.addView(view3D);
    }

    root.addView(L1);
    root.addView(L2);

    setContentView(root);

    // ---------------------------------------------------------------------------------------------
    if (BLUETOOTH)
      bt = new BlueToothService(this);
    if (GPS)
      gps = new GPSService(this);
    t = new Telescope(gps, this);
    ctr = new Control(t, this);
    deviceIO = new DeviceIO(this);
    deviceIO.start();
    moonstalkerMain();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    if (IS3D) view3D.onResume();
  }

  @Override
  public void onPause()
  {
    super.onPause();
    if (IS3D) view3D.onPause();
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

  private void moonstalkerMain()
  {
    exitButton.setOnClickListener(this);
    traceButton.setOnClickListener(this);
    moveButton.setOnClickListener(this);

    ArrayAdapter<CharSequence> skyObjAdapter =
        ArrayAdapter.createFromResource(
            this, R.array.stars, android.R.layout.simple_spinner_item);
    skyObjDropDown.setAdapter(skyObjAdapter);
    skyObjDropDown.setOnItemSelectedListener(this);
    skyObjDropDown.setEnabled(true);

    new MSDialog(this);
  }

  public void showLocation()
  {
    String output = String.format("LAT=%s", convertDec2Hour(gps.getLatitude())) +
                    String.format("LON=%s", convertDec2Hour(gps.getLongitude()));
    locTextView.setText(output);
    myView.invalidate();
  }

//  @SuppressLint("SetTextI18n")
//  public void updateStatus1()
//  {
//    myView.invalidate();
//
//    if (bt.getStatus() == BlueToothService.NOT_CONNECTED) {
//      initUI();
//      return;
//    }
//    if (bt.getStatus() == BlueToothService.CONNECTED) {
//      if (t.p.getStatus() == Telescope.ST_ERROR) {
//        initUI();
//        return;
//      }
//      if (t.p.getStatus() == Telescope.ST_NOT_CAL) {
//        initUI();
//        enableButton(calibrateButton);
//        return;
//      }
//      if (t.p.getStatus() == Telescope.ST_READY) {
//        initUI();
//        skyObjDropDown.setEnabled(true);
//        enableButton(moveButton);
//        enableButton(traceButton);
//        return;
//      }
//      if (t.p.getStatus() == Telescope.ST_MOVING) {
//        initUI();
//        return;
//      }
//      if (t.p.getStatus() == Telescope.ST_TRACING) {
//        initUI();
//        enableButton(traceButton);
//        return;
//      }
//      if (t.p.getStatus() == Telescope.ST_BTRY_LOW) {
//        initUI();
//        return;
//      }
//    }
//  }

  // TODO:
  @SuppressLint("SetTextI18n")
  @Override
  public void onClick(View v)
  {
    switch (v.getId()) {
    case 1:
      if (t.p.getStatus() == ST_READY) {
        traceButton.setText("TRACE OFF");
        t.startTrace();
      } else {
        traceButton.setText("TRACE");
        t.p.setStatus(ST_READY);
      }
      break;
    case 2:
      if (curObj != null)
        t.move(curObj.getRa(), curObj.getDec());
      break;
    case 3:
      errorExit("Application terminated", "");
      break;
    }
  }

  private void scanAstroLine(int position, AstroObject obj)
  {
    String buf = skyObjDropDown.getItemAtPosition(position).toString();
    Log.d(TAG, position + ": " + buf);

    Scanner sc   = new Scanner(buf);
    String  name = sc.next();
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
  {
  }

  class AstroObject
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
      Paint  paint = new Paint();

      paint.setColor(Color.GREEN);
      canvas.drawCircle(X + (D / 2) + (float) x, Y + (D / 2) - (float) y, O, paint);

      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(Color.parseColor("#CD5C5C"));
      // Height
      canvas.drawLine(X, Y + (D / 2), X + D, Y + (D / 2), paint);
      canvas.drawLine(X + (D / 2), Y, X + (D / 2), Y + D, paint);
      canvas.drawLine(X + (D / 2), Y + (D / 2), X + (D / 2) + (float) x, Y + (D / 2) - (float) y,
                      paint);
      canvas.drawCircle(X + (D / 2), Y + (D / 2), D / 2, paint);
      canvas.drawRect(X, Y, X + D, Y + D, paint);

      paint.setColor(Color.BLACK);
      output = String.format("ALTITUDE: %s\n", convertDec2Hour(t.getPos().h));
      canvas.drawText(output, X, Y - O, paint);
      canvas.drawText("+90", X + (D / 2), Y - O, paint);
      canvas.drawText("-90", X + (D / 2), Y + D + O, paint);
      canvas.drawText("0", X - O, Y + (D / 2), paint);
      canvas.drawText("0", X + D + O, Y + (D / 2), paint);
    }

    protected void drawAzimuth(int X, int Y, double x, double y, Canvas canvas)
    {
      String output;
      Paint  paint = new Paint();


      paint.setColor(Color.GREEN);
      canvas.drawCircle(X + (D / 2) + (float) x, Y + (D / 2) - (float) y, O, paint);

      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(Color.parseColor("#CD5C5C"));
      // Height
      canvas.drawLine(X, Y + (D / 2), X + D, Y + (D / 2), paint);
      canvas.drawLine(X + (D / 2), Y, X + (D / 2), Y + D, paint);
      canvas.drawLine(X + (D / 2), Y + (D / 2), X + (D / 2) + (float) x, Y + (D / 2) - (float) y,
                      paint);
      canvas.drawCircle(X + (D / 2), Y + (D / 2), D / 2, paint);
      canvas.drawRect(X, Y, X + D, Y + D, paint);

      paint.setColor(Color.BLACK);
      output = String.format("AZIMUTH: %s\n", convertDec2Hour(t.getPos().az));
      paint.setFakeBoldText(true);
      canvas.drawText(output, X, Y - O, paint);
      canvas.drawText("N", X + (D / 2), Y - O, paint);
      canvas.drawText("S", X + (D / 2), Y + D + 2 * O, paint);
      canvas.drawText("W", X - 2 * O, Y + (D / 2), paint);
      canvas.drawText("E", X + D + O, Y + (D / 2), paint);
    }

    // TODO:
    protected void drawStatus(int X, int Y, Canvas canvas)
    {
      String out    = "";
      int    i      = 1;
      int    offset = 20;
      Paint  paint  = new Paint();

      paint.setColor(Color.BLACK);
      canvas.drawRect(X, Y, X + D / 2, Y + D / 2, paint);

      // Telescope state
      switch (t.p.getStatus()) {
      case Telescope.ST_ERROR:
        paint.setColor(Color.RED);
        out = t.p.status.getString("arg1");
        break;
      case ST_READY:
        paint.setColor(Color.GREEN);
        out = "OK";
        break;
      case Telescope.ST_BTRY_LOW:
        paint.setColor(Color.BLUE);
        out = "LOW BATTERY";
        break;
      case Telescope.ST_MOVING:
        paint.setColor(Color.MAGENTA);
        out = "MOVING...";
        break;
      case Telescope.ST_TRACING:
        paint.setColor(Color.YELLOW);
        out = "TRACING...";
        break;
      case Telescope.ST_NOT_CAL:
        paint.setColor(Color.RED);
        out = "NOT CALIBRATED";
        break;
      }
      canvas.drawText(out, X + offset, Y + i * offset, paint);
      i++;

//      // BT Status
//      switch (bt.getStatus()) {
//      case BlueToothService.NOT_CONNECTED:
//        paint.setColor(Color.RED);
//        out = "NOT CONNECTED";
//        break;
//      case BlueToothService.CONNECTING:
//        paint.setColor(Color.MAGENTA);
//        out = "CONNECTING...";
//        break;
//      case BlueToothService.CONNECTED:
//        paint.setColor(Color.GREEN);
//        out = "CONNECTED";
//        break;
//      }
//      canvas.drawText(out, X + offset, Y + i * offset, paint);
//      i++;

      // GPS state
      if (gps.isGotLocation()) {
        paint.setColor(Color.GREEN);
        out = "GPS LOCKED";
      } else if (!gps.isGotLocation()) {
        paint.setColor(Color.RED);
        out = "GPS NOT LOCKED";
      }
      canvas.drawText(out, X + offset, Y + i * offset, paint);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
      super.onDraw(canvas);

      double x, y;
      double az = t.getPos().az;
      double h  = t.getPos().h;

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
  }

  public void connectionCanceled()
  {
    Toast.makeText(this, "Connection lost!", Toast.LENGTH_LONG).show();
  }

  public void messagePrompt(String title, String message)
  {
    AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
    builder1.setTitle(title);
    builder1.setMessage(message);
    builder1.setCancelable(true);
    builder1.setNeutralButton(android.R.string.ok,
                              new DialogInterface.OnClickListener()
                              {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                  dialog.cancel();
                                }
                              });
    AlertDialog alert11 = builder1.create();
    alert11.show();
  }

  void errorExit()
  {
    finish();
    System.exit(0);
  }

  void errorExit(String title, String message)
  {
    finish();
    System.exit(0);
  }
}

class MSDialog extends AlertDialog.Builder
{
  private String title = "Do you want to connect Telescope?";

  protected MSDialog(final MainActivity ctx)
  {
    super(ctx);
    setTitle(title);
    setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                          @Override public void onClick(DialogInterface dialogInterface, int i)
                          {
                            ctx.getBt().connect();
                          }
                        });
    setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                          @Override public void onClick(DialogInterface dialogInterface, int i)
                          {
                            ctx.errorExit();
                          }
                        });

    create();
    show();
  }

}
