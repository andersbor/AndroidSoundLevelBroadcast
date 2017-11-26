package com.example.easj.soundlevelbroadcast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_UDP_PORT = 14593;
    private SoundMeter soundMeter;
    private TextView view;
    private EditText portView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = findViewById(R.id.mainTextViewMessage);
        portView = findViewById(R.id.mainEditTextUdpPort);
        portView.setText(Integer.toString(DEFAULT_UDP_PORT));
        soundMeter = new SoundMeter();
        final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
        final DoIt doIt = new DoIt();
        Thread thread = new Thread(doIt);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //registerListeners();
                    new Thread(doIt).start();
                } else {
                    if (doIt != null)
                        doIt.stop();
                }
            }
        });
    }

    class DoIt implements Runnable {
        private volatile boolean keepOnRunning = true;

        @Override
        public void run() {
            try {
                soundMeter.start();
                int i = 0;
                while (keepOnRunning) {
                    final int j = ++i;
                    Thread.sleep(1000);
                    double result = soundMeter.getAmplitude();
                    final String s = Double.toString(result);
                    Log.d("MINE", s);

                    sendBroadcast("SoundLevel\n" + result);

                    //view.setText(i + " " + s);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.setText(j + " " + s);
                        }
                    });
                }
                soundMeter.stop();
            } catch (IOException | InterruptedException ex) {
                Log.e("MINE", ex.getMessage(), ex);
                view.setText(ex.getMessage());
            }
        }

        public void go() {
            keepOnRunning = true;
            Thread.currentThread().interrupt();
        }

        public void stop() {
            keepOnRunning = false;
            Thread.currentThread().interrupt();
        }
    }

    private void sendBroadcast(String name, double value) {
        String message = name + "\n" + value;
        Log.d("MINE", message);
        sendBroadcastAsync(message);
    }

    private void sendBroadcastAsync(String message) {
        BroadCaster broadCaster = new BroadCaster();
        broadCaster.execute(message);
       /* try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Log.e("MINE", ex.getMessage(), ex);
        }*/
    }

    // Networking must be done in a background thread/task
    class BroadCaster extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            try {
                sendBroadcast(strings[0]);
            } catch (IOException e) {
                Log.e("MINE", e.getMessage(), e);
            }
            return null;
        }
    }

    public void sendBroadcast(String messageStr) throws IOException {
        String broadcastIP = "255.255.255.255";
        InetAddress inetAddress = InetAddress.getByName(broadcastIP);
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        byte[] sendData = messageStr.getBytes();
        // TODO use portView
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, DEFAULT_UDP_PORT);
        socket.send(sendPacket);
        Log.d("MINE", "Broadcast sent: " + messageStr);
    }

    // https://stackoverflow.com/questions/14181449/android-detect-sound-level
    class SoundMeter {
        private MediaRecorder mRecorder = null;

        public void start() throws IOException {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                        0);
            } else {

                if (mRecorder == null) {
                    mRecorder = new MediaRecorder();
                    mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                        @Override
                        public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                            Log.e("MINE", "Error: " + i + " " + i1);
                        }
                    });
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile("/dev/null");
                    mRecorder.prepare();
                    mRecorder.start();

                }
            }
        }

        public void stop() {
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        }

        public double getAmplitude() {
            if (mRecorder != null)
                return mRecorder.getMaxAmplitude(); // since the last call to this method.
            else
                return 0;

        }
    }
}
