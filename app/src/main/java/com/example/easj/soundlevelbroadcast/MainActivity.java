package com.example.easj.soundlevelbroadcast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private SoundMeter soundMeter;
    private TextView view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = findViewById(R.id.mainTextViewMessage);
        soundMeter = new SoundMeter();
        final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    registerListeners();
                } else {
                    unregisterListeners();
                }
            }
        });
    }

    class RecordAsync extends AsyncTask<Void, Void, Double> {

        @Override
        protected Double doInBackground(Void... voids) {
            return null;
        }
    }


    private void record() {
        try {
            soundMeter.start();
            for (int i = 0; i < 10; i++) {

                double result = soundMeter.getAmplitude();
                String s = Double.toString(result);
                Log.d("MINE", s);

                view.setText(i + " " + s);
                Thread.sleep(1000);
            }
            soundMeter.stop();

        } catch (IOException | InterruptedException ex) {
            view.setText(ex.getMessage());
        }
    }


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
