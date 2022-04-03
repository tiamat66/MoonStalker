package com.robic.zoran.arduinoemulator;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
  private static final String TAG = "IZAA";

  BlueToothService btService;

  TextView mainTextView;
  Button   sendButton;
  Button   devButton;
  Button   serButton;
  Button   exitButton;
  EditText mainEditText;
  boolean  BTServerStarted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    btService = new BlueToothService(this);
    mainEditText = findViewById(R.id.edittext_msg);

    main();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
  }

  @Override
  protected void onPause()
  {
    super.onPause();
  }

  public void main()
  {
    sendButton = findViewById(R.id.main_button);
    assert sendButton != null;
    sendButton.setOnClickListener(this);

    devButton = findViewById(R.id.button2);
    assert devButton != null;
    devButton.setOnClickListener(this);

    serButton = findViewById(R.id.button3);
    assert serButton != null;
    serButton.setOnClickListener(this);

    exitButton = findViewById(R.id.button4);
    assert exitButton != null;
    exitButton.setOnClickListener(this);

    print("Arduino emulator");
  }

  public void startStop()
  {
    if (BTServerStarted) stopBTServer();
    else {
      try {
        btService.startBtServer();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      serButton.setText(tx(R.string.stop_server));
      BTServerStarted = true;
    }
  }

  @SuppressLint("SetTextI18n")
  @Override
  public void onClick(View v)
  {
    int id = v.getId();
    if (id == R.id.main_button) {
      mainTextView.setText("Sent message: " + mainEditText.getText().toString());
      btService.write(mainEditText.getText().toString());
    } else if (id == R.id.button2) {
      btService.getPairedDevices();
    } else if (id == R.id.button3) {
      startStop();
    } else if (id == R.id.button4) {
      exit("Aplication terminated", "");
    }
  }

  @SuppressLint("SetTextI18n")
  public void stopBTServer()
  {
    btService.stopBtServer();
    serButton.setText("START BT SERVER");
    BTServerStarted = false;
  }

  public void print(String msg)
  {
    mainTextView = findViewById(R.id.main_textview);
    assert mainTextView != null;
    mainTextView.setText(msg);
  }

  public void exit(String title, String message)
  {
    Log.d(TAG, title + " - " + message);
    finish();
    System.exit(0);
  }

  public String tx(int stringId, Object... formatArgs)
  {
    if (formatArgs.length > 0)
      return getString(stringId, formatArgs);
    return getString(stringId);
  }
}

