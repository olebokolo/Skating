package com.servocode.skating.presentation.activities;

import android.animation.ObjectAnimator;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.servocode.skating.R;
import com.servocode.skating.core.app.SkatingApplication;
import com.servocode.skating.core.bluetooth.BluetoothSkatingService;
import com.servocode.skating.core.events.NoBluetoothAdapterAvailableEvent;
import com.servocode.skating.core.events.SkateDataReceivedEvent;
import com.servocode.skating.core.events.SkateDeviceConnectedEvent;
import com.servocode.skating.core.events.SkateDeviceConnectionErrorEvent;
import com.servocode.skating.core.events.SkateDeviceStopListeningEvent;
import com.servocode.skating.core.model.SkatePosition;
import com.servocode.skating.presentation.utils.animation.ActivityNavigator;
import com.servocode.skating.presentation.utils.font.FontCollection;
import com.servocode.skating.presentation.utils.seekbar.DummyOnTouchListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {

    public FontCollection fontCollection;
    public BluetoothSkatingService bluetoothSkatingService;
    public ActivityNavigator navigator;
    private SeekBar verticalSeekbar;
    private SeekBar horizontalSeekbar;
    private int seekbarSmoothless = 100;
    private TextView magicValueIndicator;
    private TextView speedIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        injectDependencies();
        initLearnTricksButton();
        findTextViews();
        attachFonts();
        registerOnEvents();
        findSeekBars();
        setSeekBarsTintColor();
        setSeekBarsUntouchable();
    }

    private void findTextViews() {
        speedIndicator = ((TextView) findViewById(R.id.speed_indicator));
        magicValueIndicator = ((TextView) findViewById(R.id.magic_value_indicator));
    }

    private void setSeekBarsUntouchable() {
        verticalSeekbar.setOnTouchListener(new DummyOnTouchListener());
        horizontalSeekbar.setOnTouchListener(new DummyOnTouchListener());
    }

    private void findSeekBars() {
        verticalSeekbar = ((SeekBar) findViewById(R.id.vertical_seek_bar));
        horizontalSeekbar = ((SeekBar) findViewById(R.id.horizontal_seek_bar));
    }

    private void setSeekBarsTintColor() {
        verticalSeekbar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.seekbarColor), PorterDuff.Mode.SRC_IN);
        horizontalSeekbar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.seekbarColor), PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothSkatingService.runMagic();
    }

    private void injectDependencies() {
        ((SkatingApplication)getApplication()).inject(this);
    }

    private void initLearnTricksButton() {
        findViewById(R.id.learn_tricks_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigator.goForwardWithSlideAnimation(MainActivity.this, ShowTricksActivity.class);
            }
        });
    }

    private void attachFonts() {
        speedIndicator.setTypeface(fontCollection.getBigNoodleTitling());
        magicValueIndicator.setTypeface(fontCollection.getBigNoodleTitling());
        ((TextView)findViewById(R.id.toolbar_title)).setTypeface(fontCollection.getBigNoodleTitling());
        ((TextView)findViewById(R.id.speed_label)).setTypeface(fontCollection.getBigNoodleTitling());
        ((Button)findViewById(R.id.learn_tricks_button)).setTypeface(fontCollection.getBigNoodleTitling());
        ((Button)findViewById(R.id.learn_tricks_button)).setTypeface(fontCollection.getBigNoodleTitling());
    }

    private void registerOnEvents() {
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onEvent(NoBluetoothAdapterAvailableEvent event){
        showMessage(event.getMessage());
    }

    @Subscribe
    public void onEvent(SkateDeviceConnectionErrorEvent event){
        Log.i("Skating", ">>>>>> Connection error from Activity!");
        showMessage(event.getMessage());
    }

    @Subscribe
    public void onEvent(SkateDeviceConnectedEvent event){
        Log.i("Skating", ">>>>>> Connected!");
        showMessage(event.getMessage());
    }

    @Subscribe
    public void onEvent(SkateDataReceivedEvent event){
        SkatePosition skatePosition = event.getSkatePosition();
        float frontBackTint = skatePosition.getFrontBackTint();
        float leftRightTint = skatePosition.getLeftRightTint();
        Log.i("Skating", ">>>>>> front-back:" + frontBackTint + " left-right: " + leftRightTint);
        int adjustedFrontBackTint = 100 - Math.round((frontBackTint + 10) * 5);
        int adjustedLeftRightTint = Math.round((leftRightTint + 10) * 5);
        float magicValue = skatePosition.getMagicValue();
        showTintInSeekBars(adjustedFrontBackTint, adjustedLeftRightTint);
        showGuessedSpeedFrom(magicValue, adjustedFrontBackTint, adjustedLeftRightTint);
        showMagicValue(magicValue);
    }

    private void showGuessedSpeedFrom(float magicValue, float frontBackTint, float leftRightTint) {
        float guessedSpeed = Math.abs(-10 - magicValue) - 0.4f;
        boolean veryTintedFrontOrBack = Math.abs(50 - frontBackTint) > 40;
        boolean veryTintedLeftOrRight = Math.abs(50 - leftRightTint) > 30;
        float adjustedGuessedSpeed = guessedSpeed < 0
                || veryTintedFrontOrBack
                || veryTintedLeftOrRight ? 0 : guessedSpeed / 2;
        String displayableGuessedSpeed = new DecimalFormat("##.##").format(adjustedGuessedSpeed) + "MS";
        speedIndicator.setText(displayableGuessedSpeed);
    }

    private void showMagicValue(float magicValue) {
        magicValueIndicator.setText(new DecimalFormat("##.##").format(magicValue));
    }

    private void showTintInSeekBars(int adjustedFrontBackTint, int adjustedLeftRightTint) {
        animateProgress(verticalSeekbar, adjustedFrontBackTint);
        animateProgress(horizontalSeekbar, adjustedLeftRightTint);
    }

    private void animateProgress(SeekBar seekbar, int adjustedFrontBackTint) {
        ObjectAnimator animation = ObjectAnimator.ofInt(seekbar, "progress", adjustedFrontBackTint);
        animation.setDuration(seekbarSmoothless);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().post(new SkateDeviceStopListeningEvent());
    }

    private void showMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 240);
        toast.show();
    }
}
