package org.numixproject.torch;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

        /**
         * A placeholder fragment containing a simple view.
         */
    public static class PlaceholderFragment extends Fragment {
        private Camera cam;
        private Camera.Parameters camParams;
        private boolean hasCam;
        private int freq;
        private StroboRunner sr;
        private Thread t;
        private boolean isChecked = false;

        public PlaceholderFragment() {
         }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
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

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
           // Let's get the reference to the toggle
            final ImageView tBtn = (ImageView) rootView.findViewById(R.id.iconLight);
            tBtn.setImageResource(R.drawable.off);

            tBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isChecked = !isChecked;

                    if (isChecked)
                        tBtn.setImageResource(R.drawable.on);
                    else
                        tBtn.setImageResource(R.drawable.off);

                    turnOnOff(isChecked);
                }
            });


            // Seekbar
            SeekBar skBar = (SeekBar) rootView.findViewById(R.id.seekBar);
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
            return rootView;
        }

        private void turnOnOff(boolean on) {

            if (on) {
                if (freq != 0) {
                    sr = new StroboRunner();
                    sr.freq = freq;
                    t = new Thread(sr);
                    t.start();
                    return ;
                }
                else
                    camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            if (!on) {
               if (t != null) {
                   sr.stopRunning = true;
                   t = null;
                   return ;
               }
                else
                   camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            cam.setParameters(camParams);
            cam.startPreview();
        }



        private class StroboRunner implements Runnable {

            int freq;
            boolean stopRunning = false;

            @Override
            public void run() {
                Camera.Parameters paramsOn = PlaceholderFragment.this.cam.getParameters();
                Camera.Parameters paramsOff = PlaceholderFragment.this.camParams;
                paramsOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                paramsOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                try {
                    while (!stopRunning) {
                        PlaceholderFragment.this.cam.setParameters(paramsOn);
                        PlaceholderFragment.this.cam.startPreview();
                        // We make the thread sleeping
                        Thread.sleep(100 - freq);
                        PlaceholderFragment.this.cam.setParameters(paramsOff);
                        PlaceholderFragment.this.cam.startPreview();
                        Thread.sleep(freq);
                     }
                    }
                catch(Throwable t) {}
            }
        }


    }


}
