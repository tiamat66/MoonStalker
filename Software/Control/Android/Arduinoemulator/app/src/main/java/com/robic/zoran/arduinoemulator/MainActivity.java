package com.robic.zoran.arduinoemulator;


import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity1";

    BlueToothService btService;
    TextView mainTextView;
    Button sendButton;
    Button devButton;
    Button serButton;
    EditText mainEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btService = new  BlueToothService(this);
        mainTextView = (TextView) findViewById(R.id.main_textview);
        mainEditText = (EditText) findViewById(R.id.edittext_msg);

        main();
    }

    @Override
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

    @Override
    protected void onResume() {
        super.onResume();

        //btService.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //btService.onPause();
    }



    public void main() {

        sendButton = (Button) findViewById(R.id.main_button);
        sendButton.setOnClickListener(this);

        devButton = (Button) findViewById(R.id.button2);
        devButton.setOnClickListener(this);

        serButton = (Button) findViewById(R.id.button3);
        serButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.main_button:
                mainTextView.setText("Sent message: " +
                        mainEditText.getText().toString());
                btService.write(mainEditText.getText().toString());
                break;
            case R.id.button2:
                btService.getPairedDevices();
                break;
            case R.id.button3:
                btService.startBtServer();
                break;
        }
    }

}
