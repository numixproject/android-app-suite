package org.numixproject.torch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class MainActivity extends Activity implements SurfaceHolder.Callback, BillingProcessor.IBillingHandler {

    BillingProcessor bp;
    public Camera cam;
    private Camera.Parameters camParams;
    private boolean hasCam;
    private int freq;
    private StroboRunner sr;
    private Thread t;
    private boolean isChecked = false;
    int counter = 1;
    private boolean yellow = true;
    private SurfaceHolder mHolder ;


    // Check if flashlight is present on device
    public boolean hasFlash() {
        if (cam == null) {
            return false;
        }

        Camera.Parameters parameters = cam.getParameters();

        if (parameters.getFlashMode() == null) {
            return false;
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes == null || supportedFlashModes.isEmpty() || supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
            return false;
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        bp = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn8CsUgsjvHzwR3bEiF5qcAXNlmjy/IcXILzQCaiHEF6wjkFCQEt8Nb0MfweiNjqTbM9j1kbgRz3dp2ZxMqDKtGkYtEV12txv86k2mf97NuN+HSG0aGjPX9A2QhdvHAVuWXhdhyA8nNbXc25S6H/kV+FXtjN5XHf6opxw3rOdVwUAyv0ARFdX+PYFjQ1GmfPZSlKbpY+9r9nQnIM96SuqCm1+PiwXWN6XG+ymCZrXeehn+rqzD1yPny67m9KSTWo8+FB2ygB6KgbNl/JCHJsn6vsdWgeIF/u1lVB81Z+m5Y7fRsG06aM3+Iq6pXimwqofjxO3WaLFbrU8sOZQIzWTYQIDAQAB", this);

        // Restore purchases
        bp.loadOwnedPurchasesFromGoogle();

        if (bp.isPurchased("remove_ads")){
            setContentView(R.layout.activity_main);
        } else {
            setContentView(R.layout.activity_main);
        }

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

        if (hasFlash()) {

        } else {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

            alertDialog.setCancelable(false);

            // Setting Dialog Title
            alertDialog.setTitle("Flashlight not available...");

            // Setting Dialog Message
            alertDialog.setMessage("Your device is not compatible with this app or doesn't have a flashlight. DO YOU WANT TO USE YOUR SCREEN AS A TORCH INSTEAD?");


            // Setting Positive "Yes" Button
            alertDialog.setPositiveButton("Yes, of course", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    FrameLayout legacy = (FrameLayout) findViewById(R.id.legacy);
                    // Write your code here to invoke YES event
                    legacy.setVisibility(View.VISIBLE);
                    WindowManager.LayoutParams layout = getWindow().getAttributes();
                    layout.screenBrightness = 1F;
                    getWindow().setAttributes(layout);
                }
            });

            // Setting Negative "NO" Button
            alertDialog.setNegativeButton("No, thanks", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Write your code here to invoke NO event
                    System.exit(0);
                }
            });

            // Showing Alert Message
            alertDialog.show();
        }

        // Active on press listener
        final ImageButton onDemandLamp = (ImageButton) findViewById(R.id.onDemandLamp);

        onDemandLamp.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == (MotionEvent.ACTION_UP)){
                    //Turn off the light after press
                    turnOffTorchDemand();
                }
                else if(event.getAction() == (MotionEvent.ACTION_MOVE)){
                    // Do nothing: background is still yellow
                } else {
                    //Turn on the light during press
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

    @Override
    public void onStart() {
        super.onStart();
        SurfaceView preview = (SurfaceView)findViewById(R.id.PREVIEW);
        SurfaceHolder mHolder = preview.getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (bp.isPurchased("remove_ads")){
            inflater.inflate(R.menu.main_activity_actions_no_ads, menu);
        } else {
            inflater.inflate(R.menu.main_activity_actions, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                bp.purchase(MainActivity.this, "remove_ads");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        Switch stroboSwitch = (Switch) findViewById(R.id.activeStrobo);

        if (on) {
            stroboSwitch.setChecked(false);
            stroboSwitch.setEnabled(false);
            fab2.setVisibility(View.VISIBLE);
            fab.setVisibility(View.INVISIBLE);
        } else {
            stroboSwitch.setEnabled(true);
            fab.setVisibility(View.VISIBLE);
            fab2.setVisibility(View.INVISIBLE);
        }
    }


    public void onStroboClicked(View view) {
        // Is the toggle on?
        boolean on = ((Switch) view).isChecked();
        SeekBar bar = (SeekBar) findViewById(R.id.seekBar);
        Switch stroboSwitch = (Switch) findViewById(R.id.activeOnTouch);
        TextView stroboText = (TextView) findViewById(R.id.textView2);

        if (on) {
            stroboSwitch.setChecked(false);
            stroboSwitch.setEnabled(false);
            bar.setVisibility(View.VISIBLE);
            stroboText.setVisibility(View.VISIBLE);

        } else {
            stroboSwitch.setEnabled(true);
            bar.setProgress(0);
            bar.setVisibility(View.INVISIBLE);
            stroboText.setVisibility(View.INVISIBLE);
        }
    }


    public void turnOn(View v) {
        showNotification();
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
        backgroundYellow();

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
        else if(event.getAction()==MotionEvent.ACTION_MOVE){
            Log.e("Touching up", "Moving on the Screen");}
        return true;
    }

    public void turnOff(View v) {
        stopNotification();
        stopAnimation();
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout);
        activeLayout.setBackgroundColor(0xFFFFC107);
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

    // SOS
    public void SOSstart(){
        String myString = "0101010101";
        long blinkDelay = 50;

        for (int i = 0; i < myString.length(); i++) {
            if (myString.charAt(i) == '0') {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            } else {
                camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            try {
                Thread.sleep(blinkDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        mHolder = holder;
        try {
            Log.i("SurfaceHolder", "setting preview");
            cam.setPreviewDisplay(mHolder);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        Log.i("SurfaceHolder", "stopping preview");
        cam.stopPreview();
        mHolder = null;
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
        backgroundGrey();
    }

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private void backgroundGrey() {
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout2);
        ObjectAnimator animator = ObjectAnimator.ofInt(activeLayout, "backgroundColor", 0xFFFFC107,0xFF333333 ).setDuration(200);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
    }

    private void backgroundYellow() {
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout2);
        ObjectAnimator animator = ObjectAnimator.ofInt(activeLayout, "backgroundColor", 0xFF333333,0xFFFFC107 ).setDuration(200);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
    }

    private void backgroundYellowFast() {
        final LinearLayout activeLayout = (LinearLayout) findViewById(R.id.activeLayout2);
        ObjectAnimator animator = ObjectAnimator.ofInt(activeLayout, "backgroundColor", 0xFF333333,0xFFFFC107 );
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
    }

    private void showNotification() {
        final int MY_NOTIFICATION_ID=1;
        NotificationManager notificationManager;
        Notification myNotification;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        myNotification = new Notification(R.drawable.fab, "Notification!", System.currentTimeMillis());
        Context context = getApplicationContext();
        String notificationTitle = "Numix Material Torch";
        String notificationText = "Flashlight on. Tap to disable.";
        Intent myIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, myIntent, Intent.FILL_IN_ACTION);
        myNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        myNotification.setLatestEventInfo(context, notificationTitle, notificationText, pendingIntent);
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
    }

    private void stopNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    protected void onNewIntent(Intent intent) {
        turnOffTorchDemand();
        stopNotification();
        stopAnimation();
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
    public static class AdFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_ad, container, false);
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            AdView mAdView = (AdView) getView().findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    // IBillingHandler implementation

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBillingInitialized() {
        /*
         * Called then BillingProcessor was initialized and its ready to purchase
         */
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        /*
         * Called then requested PRODUCT ID was successfully purchased
         */
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        /*
         * Called then some error occured. See Constants class for more details
         */
    }

    @Override
    public void onPurchaseHistoryRestored() {
        /*
         * Called then purchase history was restored and the list of all owned PRODUCT ID's
         * was loaded from Google Play
         */
    }

    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();

        super.onDestroy();
    }

}

