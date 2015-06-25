/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.numix.calculator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.support.v4.widget.SlidingPaneLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.numix.calculator.view.AdvancedDisplay;
import com.numix.calculator.view.CalculatorDisplay;
import com.numix.calculator.view.CalculatorViewPager;
import com.numix.calculator.view.HistoryLine;
import com.xlythe.slider.Slider;
import com.xlythe.slider.Slider.Direction;

public class Calculator extends Activity implements Logic.Listener, OnClickListener, OnMenuItemClickListener, CalculatorViewPager.OnPageChangeListener {
    public EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private ListView mHistoryView;
    private BaseAdapter mHistoryAdapter;
    private Logic mLogic;
    private CalculatorViewPager mPager;
    private CalculatorViewPager mSmallPager;
    private CalculatorViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Slider mPulldown;
    private Graph mGraph;
    EventListener  cls2= new EventListener();
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    int currentapiVersion = android.os.Build.VERSION.SDK_INT;


    private boolean clingActive = false;

    public enum Panel {
        GRAPH, FUNCTION, HEX, BASIC, ADVANCED, MATRIX;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum SmallPanel {
        HEX, ADVANCED, FUNCTION;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum LargePanel {
        GRAPH, BASIC, MATRIX;

        int order;

        public void setOrder(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    private static final String STATE_CURRENT_VIEW = "state-current-view";
    private static final String STATE_CURRENT_VIEW_SMALL = "state-current-view-small";
    private static final String STATE_CURRENT_VIEW_LARGE = "state-current-view-large";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-63953479-3");
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        int sliderBackground = R.color.background_light;
            super.setTheme(R.style.Theme_Calculator_Light);

        setContentView(R.layout.main);

        mPager = (CalculatorViewPager) findViewById(R.id.panelswitch);
        Logic mHandler;


        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.mHistory;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if(mPersist.getMode() != null) mLogic.mBaseModule.setMode(mPersist.getMode());

        mLogic.setLineLength(mDisplay.getMaxDigits());

        mHistoryAdapter = new HistoryAdapter(this, mHistory);
        mHistory.setObserver(mHistoryAdapter);


        mGraph = new Graph(mLogic);

        if(mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager, mListener, mGraph, mLogic));
            mPager.setCurrentItem(state == null ? Panel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW, Panel.BASIC.getOrder()));
            mPager.setOnPageChangeListener(this);
            mListener.setHandler(this, mLogic, mPager);
        }
        else if(mSmallPager != null && mLargePager != null) {
            // Expanded UI
            mSmallPager.setAdapter(new SmallPageAdapter(mSmallPager, mLogic));
            mLargePager.setAdapter(new LargePageAdapter(mLargePager, mGraph, mLogic));
            mSmallPager.setCurrentItem(state == null ? SmallPanel.ADVANCED.getOrder() : state.getInt(STATE_CURRENT_VIEW_SMALL, SmallPanel.ADVANCED.getOrder()));
            mLargePager.setCurrentItem(state == null ? LargePanel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW_LARGE, LargePanel.BASIC.getOrder()));
            mSmallPager.setOnPageChangeListener(this);
            mLargePager.setOnPageChangeListener(this);
            mListener.setHandler(this, mLogic, mSmallPager, mLargePager);
        }

        mDisplay.setOnKeyListener(mListener);

        if(!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if(mMatrixPanel != null) mMatrixPanel.setVisible(!getMatrixVisibility() && CalculatorSettings.matrixPanel(getContext()));

        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if(mGraphPanel != null) mGraphPanel.setVisible(!getGraphVisibility() && CalculatorSettings.graphPanel(getContext()));

        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if(mFunctionPanel != null) mFunctionPanel.setVisible(!getFunctionVisibility() && CalculatorSettings.functionPanel(getContext())
                );

        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if(mBasicPanel != null) mBasicPanel.setVisible(!getBasicVisibility() && CalculatorSettings.basicPanel(getContext()));

        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if(mAdvancedPanel != null) mAdvancedPanel.setVisible(!getAdvancedVisibility() && CalculatorSettings.advancedPanel(getContext())
                );

        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if(mHexPanel != null) mHexPanel.setVisible(!getHexVisibility() && CalculatorSettings.hexPanel(getContext()));

        return true;
    }

    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if(mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.overflow_menu:
            PopupMenu menu = constructPopupMenu();
            if(menu != null) {
                menu.show();
            }
            break;
        }
    }

    public void deleteAnimation(){
        TextView colorView = (TextView) findViewById(R.id.deleteColor);
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getGraphVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        }
        return false;
    }

    private boolean getFunctionVisibility() {
        // if(mPager != null) {
        // return mPager.getCurrentItem() == Panel.FUNCTION.getOrder() &&
        // CalculatorSettings.functionPanel(getContext());
        // }
        // else if(mSmallPager != null) {
        // return mSmallPager.getCurrentItem() == SmallPanel.FUNCTION.getOrder()
        // && CalculatorSettings.functionPanel(getContext());
        // }
        return false;
    }

    private boolean getBasicVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        }
        return false;
    }

    private boolean getAdvancedVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        }
        return false;
    }

    private boolean getHexVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        }
        else if(mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        }
        return false;
    }

    private boolean getMatrixVisibility() {
        if(mPager != null) {
            return mPager.getCurrentItem() == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        }
        else if(mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        }
        return false;
    }

    public void onClickListenerSettings(View v) {
        Intent preferencesintent = (Intent) new Intent(this,
                Preferences.class);
        if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
            startActivity(preferencesintent,
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        } else{
            startActivity(preferencesintent);
        }
    }

    public void onClickListenerDel(View v) {
    }

    public void onClickListenerHistory(View v) {
        Toast.makeText(this, R.string.history, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

        case R.id.basic:
            if(!getBasicVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.BASIC.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            }
            break;

        case R.id.advanced:
            if(!getAdvancedVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.ADVANCED.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            }
            break;

        case R.id.function:
            if(!getFunctionVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.FUNCTION.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.FUNCTION.getOrder());
            }
            break;

        case R.id.graph:
            if(!getGraphVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.GRAPH.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.GRAPH.getOrder());
            }
            break;

        case R.id.matrix:
            if(!getMatrixVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.MATRIX.getOrder());
                else if(mLargePager != null) mLargePager.setCurrentItem(LargePanel.MATRIX.getOrder());
            }
            break;

        case R.id.hex:
            if(!getHexVisibility()) {
                if(mPager != null) mPager.setCurrentItem(Panel.HEX.getOrder());
                else if(mSmallPager != null) mSmallPager.setCurrentItem(SmallPanel.HEX.getOrder());
            }
            break;

        case R.id.settings:
            startActivity(new Intent(this, Preferences.class));
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        if(mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }

        if(mSmallPager != null) {
            state.putInt(STATE_CURRENT_VIEW_SMALL, mSmallPager.getCurrentItem());
        }

        if(mLargePager != null) {
            state.putInt(STATE_CURRENT_VIEW_LARGE, mLargePager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.setMode(mLogic.mBaseModule.getMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK && mPager != null && !getBasicVisibility() && CalculatorSettings.basicPanel(getContext()) && !clingActive) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && !(getAdvancedVisibility() && getBasicVisibility())
                && CalculatorSettings.basicPanel(getContext()) && CalculatorSettings.advancedPanel(getContext()) && !clingActive) {
            mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onDeleteModeChange() {
    }

    private void setUpHistory() {
        registerForContextMenu(mHistoryView);
        mHistoryView.setAdapter(mHistoryAdapter);
        mHistoryView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mHistoryView.setStackFromBottom(true);
        mHistoryView.setFocusable(false);
        mHistoryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mDisplay.getText().isEmpty())
                mDisplay.insert(((HistoryLine) view).getHistoryEntry().getEdited());
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View history = mHistoryAdapter.getView(info.position, null, null);
        if(history instanceof HistoryLine) ((HistoryLine) history).onCreateContextMenu(menu);
    }

    private Context getContext() {
        return Calculator.this;
    }

    /* Cling related */
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if(ActivityManager.isRunningInTestHarness()) return false;
        return true;
    }


    private void removeCling(int id) {
        setPagingEnabled(true);
        clingActive = false;

        final View cling = findViewById(id);
        if(cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
        }
    }


    private void setPagingEnabled(boolean enabled) {
        if(mPager != null) mPager.setPagingEnabled(enabled);
        if(mSmallPager != null) mSmallPager.setPagingEnabled(enabled);
        if(mLargePager != null) mLargePager.setPagingEnabled(enabled);
    }

    private boolean getPagingEnabled() {
        if(mPager != null) return mPager.getPagingEnabled();
        if(mSmallPager != null) return mSmallPager.getPagingEnabled();
        if(mLargePager != null) return mLargePager.getPagingEnabled();
        return true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(state == 0) {
            setPagingEnabled(true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {}
}
