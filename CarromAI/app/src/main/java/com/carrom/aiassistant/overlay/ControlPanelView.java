package com.carrom.aiassistant.overlay;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ControlPanelView extends LinearLayout {

    private TextView titleText;
    private Button startButton;
    private Button stopButton;

    public ControlPanelView(Context context) {
        super(context);
        init(context);
    }

    public ControlPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ControlPanelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        setOrientation(VERTICAL);
        setPadding(40, 40, 40, 40);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.parseColor("#222222"));

        titleText = new TextView(context);
        titleText.setText("Carrom AI Assistant");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(20f);

        LayoutParams textParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        textParams.bottomMargin = 40;
        titleText.setLayoutParams(textParams);

        startButton = new Button(context);
        startButton.setText("Start AI");
        startButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        startButton.setTextColor(Color.WHITE);

        LayoutParams startParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );

        startParams.bottomMargin = 20;
        startButton.setLayoutParams(startParams);

        stopButton = new Button(context);
        stopButton.setText("Stop AI");
        stopButton.setBackgroundColor(Color.parseColor("#F44336"));
        stopButton.setTextColor(Color.WHITE);

        LayoutParams stopParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );

        stopButton.setLayoutParams(stopParams);

        addView(titleText);
        addView(startButton);
        addView(stopButton);
    }
}