package org.numixproject.torch;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;


/*
 * Copyright (C) 2014 Francesco Azzola - Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MainActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            //Log.d("TORCH", "Check cam");
            // Get CAM reference
            cam = Camera.open();
            camParams = cam.getParameters();
            cam.startPreview();
            hasCam = true;
            //Log.d("TORCH", "HAS CAM ["+hasCam+"]");
        }
        catch(Throwable t) {
            t.printStackTrace();
        }

        // Seekbar
        SeekBar skBar = (SeekBar) findViewById(R.id.seekBar);
        skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freq = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
        }

    public Camera cam;
    private Camera.Parameters camParams;
    private boolean hasCam;
    private int freq;
    private StroboRunner sr;
    private Thread t;
    private boolean isChecked = false;


    public void turnOn(View v) {

        if (freq != 0) {
            sr = new StroboRunner();
            sr.freq = freq;
            t = new Thread(sr);
            t.start();
            return;
        } else
            camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        cam.setParameters(camParams);
        cam.startPreview();
    }

    private void turnOff(View v) {
        if (t != null) {
            sr.stopRunning = true;
            t = null;
            return ;
        }
        else {
            camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            cam.setParameters(camParams);
            cam.stopPreview();
        }
    }


    private class StroboRunner implements Runnable {

        int freq;
        boolean stopRunning = false;

        @Override
        public void run() {
            Camera.Parameters paramsOn = cam.getParameters();
            Camera.Parameters paramsOff = camParams;
            paramsOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            paramsOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            try {
                while (!stopRunning) {
                    cam.setParameters(paramsOn);
                    cam.startPreview();
                    // We make the thread sleeping
                    Thread.sleep(100 - freq);
                    cam.setParameters(paramsOff);
                    cam.startPreview();
                    Thread.sleep(freq);
                }
            }
            catch(Throwable t) {}
        }
    }
}

