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
  TextView         mainTextView;
  Button           sendButton;
  Button           devButton;
  Button           serButton;
  Button           exitButton;
  EditText         mainEditText;
  boolean BTServerStarted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    btService = new BlueToothService(this);
    mainEditText = (EditText) findViewById(R.id.edittext_msg);

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
    sendButton = (Button) findViewById(R.id.main_button);
    assert sendButton != null;
    sendButton.setOnClickListener(this);

    devButton = (Button) findViewById(R.id.button2);
    assert devButton != null;
    devButton.setOnClickListener(this);

    serButton = (Button) findViewById(R.id.button3);
    assert serButton != null;
    serButton.setOnClickListener(this);

    exitButton = (Button) findViewById(R.id.button4);
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
      serButton.setText("STOP BT SERVER");
      BTServerStarted = true;
    }
  }
  @SuppressLint("SetTextI18n")
  @Override
  public void onClick(View v)
  {
    switch (v.getId()) {

    case R.id.main_button:
      mainTextView.setText("Sent message: " + mainEditText.getText().toString());
      btService.write(mainEditText.getText().toString());
      break;
    case R.id.button2:
      btService.getPairedDevices();
      break;
    case R.id.button3:
      startStop();
      break;
    case R.id.button4:
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
    mainTextView = (TextView) findViewById(R.id.main_textview);
    assert mainTextView != null;
    mainTextView.setText(msg);
  }

  public void exit(String title, String message)
  {
    Log.d(TAG, title + " - " + message);
    finish();
    System.exit(0);
  }
}

