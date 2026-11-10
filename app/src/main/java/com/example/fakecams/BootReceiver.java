package com.example.fakecams;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        boolean on = c.getSharedPreferences(CamsView.PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false);
        if (on && Settings.canDrawOverlays(c)) {
            try { c.startForegroundService(new Intent(c, OverlayService.class)); } catch (Exception ignored) {}
        }
    }
}
