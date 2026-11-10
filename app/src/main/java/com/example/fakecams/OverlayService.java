package com.example.fakecams;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

public class OverlayService extends Service {

    public static final String ACTION_REFRESH = "com.example.fakecams.REFRESH";
    private static final String CHANNEL = "fakecams";

    private WindowManager wm;
    private CamsView view;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        if (view == null) addOverlay();
        else if (intent != null && ACTION_REFRESH.equals(intent.getAction())) view.invalidate();
        return START_STICKY;
    }

    private void startAsForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Fake Cams", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.front_camera)
                .setContentTitle("Fake Cams работает")
                .setOngoing(true)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }

    private void addOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new CamsView(this);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                view.neededHeight(),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 0;
        lp.y = 0;
        // окно может заходить в область выреза камеры
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);                       // не отодвигать окно от статус-бара
        lp.setTitle("FakeCams");
        wm.addView(view, lp);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (view == null) return;
        boolean portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        view.setVisibility(portrait ? android.view.View.VISIBLE : android.view.View.GONE);
        if (portrait) {
            view.reloadCutout();
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();
            lp.height = view.neededHeight();
            wm.updateViewLayout(view, lp);
        }
    }

    @Override
    public void onDestroy() {
        if (view != null && wm != null) { try { wm.removeView(view); } catch (Exception ignored) {} }
        view = null;
        super.onDestroy();
    }
}
