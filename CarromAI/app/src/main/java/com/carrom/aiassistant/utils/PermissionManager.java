package com.carrom.aiassistant.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class PermissionManager {
    
    private Context context;
    
    public PermissionManager(Context context) {
        this.context = context;
    }
    
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    public boolean isAccessibilityServiceEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
            return accessibilityEnabled == 1;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }
    
    public boolean hasAllPermissions() {
        return hasOverlayPermission() && isAccessibilityServiceEnabled();
    }
}