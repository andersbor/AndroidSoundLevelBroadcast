package com.example.easj.soundlevelbroadcast;

import android.media.MediaRecorder;
import android.os.Bundle;
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
    public static final int DEFAULT_UDP_PORT = 14594;
    private TextView view;
    private EditText udpPortView, sleepTimeView;
    private MediaRecorder mediaRecorder;
    private int port = DEFAULT_UDP_PORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(R.id.mainTextViewMessage);
        udpPortView = findViewById(R.id.mainEditTextUdpPort);
        sleepTimeView = findViewById(R.id.mainEditTextSleepTime);
        udpPortView.setText(Integer.toString(DEFAULT_UDP_PORT));
        try {
            setupMediaRecorder();
            final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                Thread thread;

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final DoIt doIt = new DoIt();
                    if (isChecked) {
                        udpPortView.setEnabled(false);
                        String udpPortStr = udpPortView.getText().toString();
                        port = Integer.parseInt(udpPortStr);
                        thread = new Thread(doIt);
                        thread.start();
                        Log.d("MINE", "onCheckedChanged isChecked");
                        Log.d("MINE", "thread is null: " + (thread == null));
                    } else {
                        udpPortView.setEnabled(true);
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

    // https://stackoverflow.com/questions/14181449/android-detect-sound-level
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
                while (keepOnRunning) {
                    if (Thread.interrupted()) break;
                    Log.d("MINE", "run() keepOnRunning: " + keepOnRunning);
                    Thread.sleep(1000);
                    final double amplitude = mediaRecorder.getMaxAmplitude();
                    sendUdpBroadcast("SoundLevel\n" + amplitude, port);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        // Background thread is not allowed to modify the UI directly
                        @Override
                        public void run() {
                            view.setText("Sound level: " + Double.toString(amplitude));
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
                Log.d("MINE", "InterruptedException caught ... continuing");
            }
        }

        void stop() {
            // not working! why?
            keepOnRunning = false;
            Log.d("MINE", "stop called. keepOnRunning: " + keepOnRunning);
        }
    }

    public void sendUdpBroadcast(String messageStr, int port) throws IOException {
        String broadcastIP = "255.255.255.255";
        InetAddress inetAddress = InetAddress.getByName(broadcastIP);
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        byte[] sendData = messageStr.getBytes();
        // TODO use udpPortView
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
        socket.send(sendPacket);
        Log.d("MINE", "Broadcast sent: " + messageStr);
    }
}
