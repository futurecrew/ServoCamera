package com.example.dj.btclient;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    TextView out;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "B8:27:EB:65:60:44";

    private int bt_port_to_connect = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        out = (TextView) findViewById(R.id.out);

        out.append("\n...In onCreate()...");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        Button buttonUp = (Button) findViewById(R.id.button_up);
        buttonUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                    sendMessage("UP");
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                    sendMessage("STOP");
                return true;
            }
        });

        Button buttonDown = (Button) findViewById(R.id.button_down);
        buttonDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                    sendMessage("DOWN");
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                    sendMessage("STOP");
                return true;
            }
        });

        Button buttonRight = (Button) findViewById(R.id.button_right);
        buttonRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                    sendMessage("RIGHT");
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                    sendMessage("STOP");
                return true;
            }
        });

        Button buttonLeft = (Button) findViewById(R.id.button_left);
        buttonLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                    sendMessage("LEFT");
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                    sendMessage("STOP");
                return true;
            }
        });
    }

    private void sendMessage(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            outStream.write(msgBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void onStart() {
        super.onStart();
        out.append("\n...In onStart()...");
    }

    @Override
    public void onResume() {
        super.onResume();

        out.append("\n...In onResume...\n...Attempting client connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            //btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

            // IMPORTANT: we create a reference to the 'createInsecureRfcommSocket' method
            // and not(!) to the 'createInsecureRfcommSocketToServiceRecord' (which is what the
            // android SDK documentation publishes
            Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});

            btSocket = (BluetoothSocket) m.invoke(device,bt_port_to_connect);

        } catch (Exception e) {
            AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
            out.append("\n...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        out.append("\n...Sending message to server...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            AlertBox("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

        String message = "Hello from Android.\n";
        byte[] msgBuffer = message.getBytes();
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            e.printStackTrace();

            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            AlertBox("Fatal Error", msg);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        out.append("\n...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                AlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        out.append("\n...In onStop()...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        out.append("\n...In onDestroy()...");
    }

    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                out.append("\n...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void AlertBox( String title, String message ){
        new AlertDialog.Builder(this)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }
}
