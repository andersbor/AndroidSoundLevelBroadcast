package com.example.easj.soundlevelbroadcast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
    private TextView view;
    private EditText portView;
    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(R.id.mainTextViewMessage);
        portView = findViewById(R.id.mainEditTextUdpPort);
        portView.setText(Integer.toString(DEFAULT_UDP_PORT));
        try {
            setupMediaRecorder();
            final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                Thread thread;

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final DoIt doIt = new DoIt();
                    if (isChecked) {
                        thread = new Thread(doIt);
                        thread.start();
                        Log.d("MINE", "onCheckedChanged isChecked");
                        Log.d("MINE", "thread is null: " + (thread == null));
                    } else {
                        Log.d("MINE", "onCheckedChanged !isChecked");
                        doIt.stop();
                        thread.interrupt();
                        Log.d("MINE", "after interrupt");
                    }
                }
            });
        } catch (IOException ex) {
            Log.e("MINE", ex.getMessage(), ex);
        }
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");
        mediaRecorder.prepare();
        mediaRecorder.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    class DoIt implements Runnable {
        // Does not work! Why?
        private volatile boolean keepOnRunning = true;

        @Override
        public void run() {
            try {
                int i = 0;
                while (keepOnRunning) {
                    if (Thread.interrupted()) break;
                    Log.d("MINE", "run() keepOnRunning: " + keepOnRunning);
                    final int j = ++i;
                    Thread.sleep(1000);
                    final double amplitude = mediaRecorder.getMaxAmplitude();
                    sendBroadcast("SoundLevel\n" + amplitude);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        // Background thread is not allowed to modify the UI directly
                        @Override
                        public void run() {
                            view.setText(Double.toString(amplitude));
                        }
                    });
                }
            } catch (final IOException ex) {
                Log.e("MINE", ex.getMessage(), ex);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setText(ex.getMessage());
                    }
                });
            } catch (InterruptedException ex) {
                Log.d("MINE", "run()", ex);
            }
        }

        public void stop() {
            // not working! why?
            keepOnRunning = false;
            Log.d("MINE", "stop called. keepOnRunning: " + keepOnRunning);
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
