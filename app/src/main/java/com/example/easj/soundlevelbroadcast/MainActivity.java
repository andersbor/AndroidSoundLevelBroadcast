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
    private TextView messageView;
    private EditText udpPortView, sleepTimeView;
    private MediaRecorder mediaRecorder;
    private int udpPortNumber = DEFAULT_UDP_PORT, sleepTime = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageView = findViewById(R.id.mainTextViewMessage);
        udpPortView = findViewById(R.id.mainEditTextUdpPort);
        sleepTimeView = findViewById(R.id.mainEditTextSleepTime);
        udpPortView.setText(Integer.toString(DEFAULT_UDP_PORT));
        try {
            setupMediaRecorder();
            final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                Thread thread;

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final DoIt doIt = new DoIt(sleepTime);
                    if (isChecked) {
                        String udpPortStr = udpPortView.getText().toString();
                        if ("".equals(udpPortStr)) {
                            messageView.setText("Missing UDP udpPortNumber");
                            return;
                        }
                        udpPortNumber = Integer.parseInt(udpPortStr);
                        if (udpPortNumber > 65535) {
                            messageView.setText("Port number must be lower than 65536");
                            return;
                        }
                        String sleepTimeStr = sleepTimeView.getText().toString();
                        if ("".equals(sleepTimeStr)) {
                            messageView.setText("Missing sleep time");
                            return;
                        }
                        sleepTime = Integer.parseInt(sleepTimeStr);
                        udpPortView.setEnabled(false);
                        sleepTimeView.setEnabled(false);
                        thread = new Thread(doIt);
                        thread.start();
                    } else {
                        udpPortView.setEnabled(true);
                        sleepTimeView.setEnabled(true);
                        doIt.stop(); // no effects. why?
                        thread.interrupt();
                    }
                }
            });
        } catch (IOException ex) {
            Log.e("MINE", ex.getMessage(), ex);
            messageView.setText(ex.getMessage());
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
        private int sleepTime;

        DoIt(int sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            try {
                while (keepOnRunning) {
                    if (Thread.interrupted()) break;
                    Thread.sleep(sleepTime);
                    final double amplitude = mediaRecorder.getMaxAmplitude();
                    sendUdpBroadcast("SoundLevel\n" + amplitude, udpPortNumber);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        // Background thread is not allowed to modify the UI directly
                        @Override
                        public void run() {
                            messageView.setText("Sound level: " + Double.toString(amplitude));
                        }
                    });
                }
            } catch (final IOException ex) {
                Log.e("MINE", ex.getMessage(), ex);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageView.setText(ex.getMessage());
                    }
                });
            } catch (InterruptedException ex) {
                Log.d("MINE", "InterruptedException caught ... continuing");
            }
        }

        void stop() {
            // not working! why?
            keepOnRunning = false;
        }
    }

    public void sendUdpBroadcast(String messageStr, int port) throws IOException {
        String broadcastIP = "255.255.255.255";
        InetAddress inetAddress = InetAddress.getByName(broadcastIP);
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        byte[] sendData = messageStr.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
        socket.send(sendPacket);
        Log.d("MINE", "Broadcast sent: " + messageStr);
    }
}