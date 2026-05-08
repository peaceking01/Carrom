package com.carrom.aiassistant;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carrom.aiassistant.overlay.FloatingBubbleService;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_SCREEN_CAPTURE = 1002;
    
    private int mediaProjectionResultCode = 0;
    private Intent mediaProjectionData = null;
    
    private Button btnRequestOverlay;
    private Button btnRequestCapture;
    private Button btnAccessibility;
    private Button btnStartService;
    private Button btnStopService;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        updateStatus();
    }
    
    private void initViews() {
        btnRequestOverlay = findViewById(R.id.btnRequestOverlay);
        btnRequestCapture = findViewById(R.id.btnRequestCapture);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        tvStatus = findViewById(R.id.tvStatus);
    }
    
    private void setupListeners() {
        btnRequestOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermission();
            }
        });
        
        btnRequestCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScreenCapturePermission();
            }
        });
        
        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });
        
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFloatingService();
            }
        });
        
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopFloatingService();
            }
        });
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void requestScreenCapturePermission() {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(), 
            REQUEST_SCREEN_CAPTURE
        );
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
    
    private void startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        if (mediaProjectionData == null) {
            Toast.makeText(this, "Screen capture permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, FloatingBubbleService.class);
        intent.putExtra("resultCode", mediaProjectionResultCode);
        intent.putExtra("data", mediaProjectionData);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        Toast.makeText(this, "AI Assistant Started", Toast.LENGTH_SHORT).show();
        updateStatus();
    }
    
    private void stopFloatingService() {
        stopService(new Intent(this, FloatingBubbleService.class));
        Toast.makeText(this, "AI Assistant Stopped", Toast.LENGTH_SHORT).show();
        updateStatus();
    }
    
    private void updateStatus() {
        boolean hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || 
                             Settings.canDrawOverlays(this);
        boolean hasCapture = mediaProjectionData != null;
        
        String status = "Status: ";
        if (hasOverlay && hasCapture) {
            status += "Ready";
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            status += "Permissions Required";
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
        
        tvStatus.setText(status);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                mediaProjectionResultCode = resultCode;
                mediaProjectionData = data;
                Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show();
            }
        }
        
        updateStatus();
    }
}