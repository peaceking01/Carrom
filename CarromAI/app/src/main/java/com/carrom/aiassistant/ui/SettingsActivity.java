package com.carrom.aiassistant.ui;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.carrom.aiassistant.R;

public class SettingsActivity extends AppCompatActivity {

    // Shared prefs key — read by PhysicsEngine / AutoPlayService
    public static final String PREFS         = "CarromAIPrefs";
    public static final String KEY_FRICTION   = "friction";
    public static final String KEY_POWER      = "power";
    public static final String KEY_COOLDOWN   = "cooldown";
    public static final String KEY_SHOW_COINS = "showCoins";
    public static final String KEY_FPS        = "fps";

    private SeekBar  sbFriction, sbPower, sbCooldown, sbFps;
    private TextView tvFriction, tvPower, tvCooldown, tvFps;
    private Switch   swShowCoins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Carrom AI — Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        loadPrefs();
        wireListeners();
    }

    private void bindViews() {
        sbFriction   = findViewById(R.id.sbFriction);
        sbPower      = findViewById(R.id.sbPower);
        sbCooldown   = findViewById(R.id.sbCooldown);
        sbFps        = findViewById(R.id.sbFps);
        tvFriction   = findViewById(R.id.tvFriction);
        tvPower      = findViewById(R.id.tvPower);
        tvCooldown   = findViewById(R.id.tvCooldown);
        tvFps        = findViewById(R.id.tvFps);
        swShowCoins  = findViewById(R.id.swShowCoins);
    }

    private void loadPrefs() {
        
android.content.SharedPreferences prefs =
        getSharedPreferences(PREFS, MODE_PRIVATE);
        // friction: 0..100 maps to 0.90..0.999
        int friction = prefs.getInt(KEY_FRICTION, 85);
        sbFriction.setProgress(friction);
        tvFriction.setText("Friction: " + frictionDisplay(friction));

        int power = prefs.getInt(KEY_POWER, 28);
        sbPower.setMax(60);
        sbPower.setProgress(power);
        tvPower.setText("Shot Power: " + power);

        int cooldown = prefs.getInt(KEY_COOLDOWN, 18); // 0..30 → 0..3000ms
        sbCooldown.setMax(30);
        sbCooldown.setProgress(cooldown);
        tvCooldown.setText("Shot Cooldown: " + (cooldown * 100) + "ms");

        int fps = prefs.getInt(KEY_FPS, 30);
        sbFps.setMax(60);
        sbFps.setProgress(fps);
        tvFps.setText("Capture FPS: " + fps);

        swShowCoins.setChecked(prefs.getBoolean(KEY_SHOW_COINS, true));
    }

    private void wireListeners() {
        sbFriction.setOnSeekBarChangeListener(simpleListener((val) -> {
            tvFriction.setText("Friction: " + frictionDisplay(val));
            saveInt(KEY_FRICTION, val);
        }));

        sbPower.setOnSeekBarChangeListener(simpleListener((val) -> {
            tvPower.setText("Shot Power: " + val);
            saveInt(KEY_POWER, val);
        }));

        sbCooldown.setOnSeekBarChangeListener(simpleListener((val) -> {
            tvCooldown.setText("Shot Cooldown: " + (val * 100) + "ms");
            saveInt(KEY_COOLDOWN, val);
        }));

        sbFps.setOnSeekBarChangeListener(simpleListener((val) -> {
            int fps = Math.max(1, val);
            tvFps.setText("Capture FPS: " + fps);
            saveInt(KEY_FPS, fps);
        }));

        swShowCoins.setOnCheckedChangeListener((btn, checked) ->
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_SHOW_COINS, checked).apply()
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String frictionDisplay(int val) {
        // 0..100 → 0.90..0.999
        float f = 0.90f + (val / 100f) * 0.099f;
        return String.format("%.3f", f);
    }

    private void saveInt(String key, int val) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(key, val).apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Simple lambda-friendly SeekBar listener
    private interface IntConsumer { void accept(int val); }

    private SeekBar.OnSeekBarChangeListener simpleListener(IntConsumer onProgress) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int val, boolean user) {
                if (user) onProgress.accept(val);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
    }
}
