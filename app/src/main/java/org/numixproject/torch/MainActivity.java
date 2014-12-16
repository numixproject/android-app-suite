package org.numixproject.torch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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

        // Active on press listener
        final ImageButton onDemandLamp = (ImageButton) findViewById(R.id.onDemandLamp);

        onDemandLamp.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == (MotionEvent.ACTION_UP)){
                    //Turn off the light after press
                    turnOffTorchDemand();
                    backgroundGrey();
                }
                else{
                    //Turn on the light during press
                    backgroundYellow();
                    turnOnTorchDemand();
                }
                return true;
            }
        });

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
    int counter = 1;
    private boolean yellow = true;




    public void startAnimation() {
        View homeView = findViewById(R.id.home_view);
        View fab = findViewById(R.id.notView);

        // Reveal Animation
        // get the center for the clipping circle
        int cx = (fab.getLeft() + fab.getRight()) / 2;
        int cy = (fab.getTop() + fab.getBottom()) / 2;

// get the final radius for the clipping circle
        int finalRadius = Math.max(homeView.getWidth(), homeView.getHeight());

// create the animator for this view (the start radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(homeView, cx, cy, 0, finalRadius);

// make the view visible and start the animation
        homeView.setVisibility(View.VISIBLE);
        anim.start();
    }

    public void startAnimationDemand() {
        View homeView2 = findViewById(R.id.home_view2);
        View fab = findViewById(R.id.notView2);

        // Reveal Animation
        // get the center for the clipping circle
        int cx = (fab.getLeft() + homeView2.getRight()) /2 ;
        int cy = (fab.getTop() + homeView2.getBottom()) /2 ;

// get the final radius for the clipping circle
        int finalRadius = Math.max(homeView2.getWidth(), homeView2.getHeight());

// create the animator for this view (the start radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(homeView2, cx, cy, 0, finalRadius);

// make the view visible and start the animation
        homeView2.setVisibility(View.VISIBLE);
        anim.start();
    }

    public void stopAnimation() {
        // previously visible view
        final View homeView = findViewById(R.id.home_view);
        View fab = findViewById(R.id.notView);

// get the center for the clipping circle
        int cx = (fab.getLeft() + fab.getRight()) / 2;
        int cy = (fab.getTop() + fab.getBottom()) / 2;

// get the initial radius for the clipping circle
        int initialRadius = homeView.getWidth();

// create the animation (the final radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(homeView, cx, cy, initialRadius, 0);

// make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                homeView.setVisibility(View.INVISIBLE);
            }
        });

// start the animation
        anim.start();
    }

    public void stopAnimationDemand() {
        // previously visible view
        final View homeView = findViewById(R.id.home_view2);
        View fab = findViewById(R.id.notView3);

// get the center for the clipping circle
        int cx = (fab.getLeft() + fab.getRight()) / 2;
        int cy = (fab.getTop() + fab.getBottom()) / 2;

// get the initial radius for the clipping circle
        int initialRadius = homeView.getWidth();

// create the animation (the final radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(homeView, cx, cy, initialRadius, 0);

// make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                homeView.setVisibility(View.INVISIBLE);
            }
        });

// start the animation
        anim.start();
    }

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((Switch) view).isChecked();
        FrameLayout fab2 = (FrameLayout) findViewById(R.id.fab2);
        FrameLayout fab = (FrameLayout) findViewById(R.id.fab);

        if (on) {
            fab2.setVisibility(View.VISIBLE);
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab2.setVisibility(View.INVISIBLE);
        }
    }


    public void turnOn(View v) {
        startAnimation();
        if (freq != 0) {
            sr = new StroboRunner();
            sr.freq = freq;
            t = new Thread(sr);
            t.start();
            startAnimation();
            return;
        } else
            camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        cam.setParameters(camParams);
        cam.startPreview();
    }

    public void turnOnTorchDemand() {
        if (freq != 0) {
            sr = new StroboRunner();
            sr.freq = freq;
            t = new Thread(sr);
            t.start();
            startAnimation();
            return;
        } else
            camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        cam.setParameters(camParams);
        cam.startPreview();
    }

    public void turnOnDemand(View v) {
        startAnimationDemand();
    }

    public void turnOffDemand(View v) {
        stopAnimationDemand();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            Log.e("Touching", "Touching the Screen");
        }
        else if(event.getAction()==MotionEvent.ACTION_UP){
            Log.e("Touching up", "Touching the Screen up");}
        return true;
    }

    public void turnOff(View v) {

        stopAnimation();
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout);
        activeLayout.setBackgroundColor(0xFFFFEB3B);        if (t != null) {
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

    public void turnOffTorchDemand() {
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

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    /*Start here*/
    private void backgroundGrey() {
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout2);
        ObjectAnimator animator = ObjectAnimator.ofInt(activeLayout, "backgroundColor", 0xFFFFEB3B,0xFF333333 ).setDuration(200);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
    }

    private void backgroundYellow() {
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout2);
        ObjectAnimator animator = ObjectAnimator.ofInt(activeLayout, "backgroundColor", 0xFF333333,0xFFFFEB3B ).setDuration(200);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
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

